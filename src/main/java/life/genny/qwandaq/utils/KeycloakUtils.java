package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A static utility class used for standard requests and
 * operations involving Keycloak.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@RegisterForReflection
public class KeycloakUtils {

    static final Logger log = Logger.getLogger(KeycloakUtils.class);
    static Jsonb jsonb = JsonbBuilder.create();

    /**
     * Fetch an access token for a user using a username and password.
     * 
     * @param keycloakUrl the keycloakUrl to use
     * @param realm       the realm to use
     * @param clientId    the clientId to use
     * @param secret      the secret to use
     * @param username    the username to use
     * @param password    the password to use
     * @return GennyToken
     */
    public static GennyToken getToken(String keycloakUrl, String realm, String clientId, String secret, String username,
            String password) {
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";

        HashMap<String, String> params = new HashMap<>();

        params.put("username", username);
        params.put("password", password);
        params.put("grant_type", "password");
        params.put("client_id", clientId);

        // TODO: MAke this more elegant ok ty
        if (!"nosecret".equals(secret) && (!StringUtils.isBlank(secret))) {
            params.put("client_secret", secret);
        }

        String token = fetchOIDCToken(keycloakUrl, realm, params);

        GennyToken gennyToken = new GennyToken(token);
        return gennyToken;
    }

    /**
     * Fetch an access token for a user using a refresh token.
     * 
     * @param keycloakUrl  the keycloakUrl to use
     * @param realm        the realm to use
     * @param clientId     the clientId to use
     * @param secret       the secret to use
     * @param refreshToken the refreshToken to use
     * @return GennyToken
     */
    public static GennyToken getToken(String keycloakUrl, String realm, String clientId, String secret,
            String refreshToken) {
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";

        HashMap<String, String> params = new HashMap<>();

        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        params.put("client_id", clientId);

        if (!"nosecret".equals(secret) && (!StringUtils.isBlank(secret))) {
            params.put("client_secret", secret);
        }

        String token = fetchOIDCToken(keycloakUrl, realm, params);

        GennyToken gennyToken = new GennyToken(token);
        return gennyToken;
    }

    /**
     * Fetch an Impersonated Token for a user.
     *
     * @param userBE     the userBE to get a token for
     * @param gennyToken the gennyToken to use to fetch the token
     * @param project    the project to use to fetch the token
     * @return String
     */
    public static String getImpersonatedToken(BaseEntity userBE, GennyToken gennyToken, BaseEntity project) {

        String realm = gennyToken.getRealm();
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";
        String token = gennyToken.getToken();
        String keycloakUrl = gennyToken.getKeycloakUrl();
        if (userBE == null) {
            log.error(ANSIColour.RED + "User BE is NULL" + ANSIColour.RESET);
            return null;
        }

        // grab uuid to fetch token
        String uuid = userBE.getValue("PRI_UUID", null);

        if (uuid == null) {

            log.warn(ANSIColour.YELLOW + "No PRI_UUID found for user " + userBE.getCode()
                    + ", attempting to use PRI_EMAIL instead" + ANSIColour.RESET);

            // grab email to fetch token
            String email = userBE.getValue("PRI_EMAIL", null);

            if (email == null) {
                log.error(ANSIColour.RED + "No PRI_EMAIL found for user " + userBE.getCode() + ANSIColour.RESET);
                return null;
            }

            // use email as backup
            uuid = email;
        } else {
            // use lowercase UUID
            uuid = uuid.toLowerCase();
        }

        // fetch keycloak json from porject entity
        String keycloakJson = project.getValueAsString("ENV_KEYCLOAK_JSON");
        if (false) {
            JsonReader reader = Json.createReader(new StringReader(keycloakJson));
            String secret = reader.readObject().getJsonObject("credentials").getString("secret");
            reader.close();
        }
        String secret = System.getenv("GENNY_CLIENT_SECRET");
        // TODO: its disgusting im sorry
        // String secret = "nosecret";
        // setup param map
        HashMap<String, String> params = new HashMap<>();
        params.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.put("subject_token", token);
        params.put("requested_subject", uuid);
        params.put("client_id", gennyToken.getRealm());

        if (!"nosecret".equals(secret) && (!StringUtils.isBlank(secret))) {
            log.info("[!] Using secret: " + secret);
            params.put("client_secret", secret);
        }

        return fetchOIDCToken(keycloakUrl, realm, params);
    }

    /**
     * Fetch an OIDC access token from keycloak.
     *
     * @param keycloakUrl the keycloakUrl to fetch from
     * @param realm       the realm to fetch in
     * @param params      the params to use
     * @return String
     */
    public static String fetchOIDCToken(String keycloakUrl, String realm, HashMap<String, String> params) {
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";

        String uri = keycloakUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token";
        log.debug("Fetching OIDC Token from " + uri);
        log.info("Fetching OIDC Token from: " + uri);
        String str = executeEncodedPostRequest(uri, params);

        JsonObject json = jsonb.fromJson(str, JsonObject.class);
        String token = json.getString("access_token");

        return token;
    }

    /**
     * Perform Custom encoded POST request.
     *
     * @param uri            the uri to request from
     * @param postDataParams the postDataParams to use in rquest
     * @return String
     */
    public static String executeEncodedPostRequest(String uri, HashMap<String, String> postDataParams) {

        try {

            // setup connection
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            // flush and close
            writer.flush();
            writer.close();
            os.close();

            String response = "";
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                response += line;
            }
            if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                log.info("Successful Token Request!");
                return response;
            } else {
                log.error("Bad Token Request: " + conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Build POST query data string.
     *
     * @param params the params to construct the query with
     * @return String
     */
    public static String getPostDataString(HashMap<String, String> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                log.debug("key: " + entry.getKey() + ", value: " + entry.getValue());
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding Post Data String: " + e.getStackTrace());
        }

        return result.toString();
    }

    /**
     * Initialise a Dummy User in keycloak.
     *
     * @param token the token to use to create tje user
     * @param realm the realm to create the user in
     * @return String
     */
    public static String createDummyUser(String token, String realm) {
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";

        String randomCode = UUID.randomUUID().toString().substring(0, 18);
        String defaultPassword = "password1";

        String json = "{ " + "\"username\" : \"" + randomCode + "\"," + "\"email\" : \"" + randomCode
                + "@gmail.com\" , "
                + "\"enabled\" : true, " + "\"emailVerified\" : true, " + "\"firstName\" : \"" + randomCode + "\", "
                + "\"lastName\" : \"" + randomCode + "\", " + "\"groups\" : [" + " \"users\" " + "], "
                + "\"requiredActions\" : [\"terms_and_conditions\"], "
                + "\"realmRoles\" : [\"user\"],\"credentials\": [{"
                + "\"type\":\"password\","
                + "\"value\":\"" + defaultPassword + "\","
                + "\"temporary\":true }]}";

        log.debug("CreateUserjsonDummy = " + json);

        String uri = GennySettings.keycloakUrl + "/auth/admin/realms/" + realm + "/users";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.info("Create keycloak user - url:" + uri + ", token:" + token);

        try {

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response == null) {
                log.error("Response was null from keycloak!!!");
                return null;
            }

            int statusCode = response.statusCode();
            log.info("StatusCode: " + statusCode);

            if (statusCode == 409) {
                log.warn("Email is already taken for " + randomCode);
                // fetch existing email user
                String userId = getKeycloakUserId(token, realm, randomCode);
                return userId;

            } else if (statusCode == 401) {
                log.warn("Unauthorized token used to create " + randomCode);
                // fetch existing email user
                String userId = getKeycloakUserId(token, realm, randomCode);
                return userId;

            } else {
                String keycloakUserId = getKeycloakUserId(token, realm, randomCode);
                log.info("Keycloak User ID: " + keycloakUserId);
                return keycloakUserId;
            }

        } catch (IOException | InterruptedException e) {
            log.error(e);
        }

        return null;
    }

    /**
     * Fetch a keycloak users Id using a username.
     *
     * @param token    the token to fetch with
     * @param realm    the realm to fetch in
     * @param username the username to fetch for
     * @return Strign
     * @throws IOException if id could not be fetched
     */
    public static String getKeycloakUserId(final String token, String realm, final String username) throws IOException {
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";

        final List<LinkedHashMap> users = fetchKeycloakUser(token, realm, username);
        if (!users.isEmpty()) {
            return (String) users.get(0).get("id");
        }
        return null;
    }

    /**
     * Fetch a keycloak user using a username.
     *
     * @param token    the token to fetch with
     * @param realm    the realm to fetch in
     * @param username the username to fetch for
     * @return List
     */
    public static List<LinkedHashMap> fetchKeycloakUser(final String token, String realm, final String username) {
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";

        String uri = GennySettings.keycloakUrl + "/auth/admin/realms/" + realm + "/users?username=" + username;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Bearer " + token)
                .GET().build();

        try {

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response == null) {
                log.error("Response was null from keycloak!!!");
                return null;
            }

            int statusCode = response.statusCode();
            log.info("StatusCode: " + statusCode);

            if (statusCode != 200) {
                log.error("Failed to get users from Keycloak, url:" + uri + ", response code:" + statusCode + ", token:"
                        + token);
                return null;
            }

            List<LinkedHashMap> results = new ArrayList<LinkedHashMap>();

            final InputStream is = new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8));
            try {
                results = JsonSerialization.readValue(is, (new ArrayList<UserRepresentation>()).getClass());
            } finally {
                is.close();
            }

            return results;

        } catch (IOException | InterruptedException e) {
            log.error(e);
        }

        return null;
    }

    /**
     * Update a keycloak user field.
     *
     * @param userToken the userToken to update with
     * @param user      the user to update for
     * @param field     the field to update
     * @param value     the value to update to
     * @return int statusCode
     */
    public static int updateUserField(GennyToken userToken, BaseEntity user, String field, String value) {

        String realm = userToken.getRealm();
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";
        String token = userToken.getToken();

        String uuid = user.getValue("PRI_UUID", null);

        String json = "{\"" + field + "\":\"" + value + "\"}";
        String uri = GennySettings.keycloakUrl + "/auth/admin/realms/" + realm + "/users/" + uuid;
        HttpResponse<String> response = HttpUtils.put(uri, json, token);

        return response.statusCode();
    }

    /**
     * Update a keycloak user email.
     *
     * @param userToken the userToken to update with
     * @param user      the user to update for
     * @param email     the email to set
     * @return int statusCode
     */
    public static int updateUserEmail(GennyToken userToken, BaseEntity user, String email) {

        String realm = userToken.getRealm();
        // TODO: Please for the love of god lets fix this
        realm = "internmatch";
        String token = userToken.getToken();

        String uuid = user.getValue("PRI_UUID", null);
        if (StringUtils.isBlank(uuid)) {
            uuid = user.getValue("PRI_KEYCLOAK_UUID", null);
            if (uuid == null) {
                log.error("User " + user.getCode() + " has NO keycloak uuid ");
            }
        }

        String json = "{ \"email\" : \"" + email + "\" , \"enabled\" : true, \"emailVerified\" : true}";
        String uri = GennySettings.keycloakUrl + "/auth/admin/realms/" + realm + "/users/" + uuid;
        HttpResponse<String> response = HttpUtils.put(uri, json, token);

        return response.statusCode();
    }

}
