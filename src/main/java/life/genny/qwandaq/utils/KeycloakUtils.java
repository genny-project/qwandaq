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
import java.util.*;

@RegisterForReflection
public class KeycloakUtils {

    private static final Logger log = Logger.getLogger(KeycloakUtils.class);
    private static Jsonb jsonb = JsonbBuilder.create();

    /**
     * Fetch an access token for a user.
     *
     * @param keycloakUrl
     * @param realm
     * @param clientId
     * @param secret
     * @param username
     * @param password
     * @param refreshToken
     * @return
     */
    public static GennyToken getToken(String keycloakUrl, String realm, String clientId, String secret, String username,
                               String password, String refreshToken) {

        HashMap<String, String> postDataParams = new HashMap<>();

        if (refreshToken == null) {
            postDataParams.put("username", username);
            postDataParams.put("password", password);
            log.debug("using username " + username);
            log.debug("using password " + password);
            log.debug("using client_id " + clientId);
            log.debug("using client_secret " + secret);
            postDataParams.put("grant_type", "password");
        } else {
            postDataParams.put("refresh_token", refreshToken);
            postDataParams.put("grant_type", "refresh_token");
            log.debug("using refresh token");
            log.debug(refreshToken);
        }

        postDataParams.put("client_id", clientId);
        if (!StringUtils.isBlank(secret)) {
            postDataParams.put("client_secret", secret);
        }

        String requestURL = keycloakUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token";

        String str = performPostCall(requestURL, postDataParams);

        log.debug("keycloak auth url = " + requestURL);
        log.debug(username + " token= " + str);

        JsonObject json = jsonb.fromJson(str, JsonObject.class);
        String accessToken = json.getString("access_token");
        GennyToken token = new GennyToken(accessToken);
        return token;
    }

    /**
     * Custom POST request for keycloak connection.
     *
     * @param requestURL
     * @param postDataParams
     * @return
     */
    public static String performPostCall(String requestURL, HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

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

            writer.flush();
            writer.close();
            os.close();

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                response += line;
            }
            if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                log.info("Successful Token Request!");
            } else {
                log.error("Bad Token Request: " + conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Build a POST query.
     *
     * @param params
     * @return
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
     * Fetch an Impersonated Token for a user.
     *
     * @param keycloakUrl
     * @param realm
     * @param project
     * @param userBE
     * @param exchangedToken
     * @return
     * @throws IOException
     */
    public static String getImpersonatedToken(String keycloakUrl, String realm, BaseEntity project,
                                              BaseEntity userBE, String exchangedToken) throws IOException {

        if (userBE == null) {
            log.error(ANSIColour.RED + "User BE is NULL" + ANSIColour.RESET);
            return null;
        }

        // grab uuid to fetch token
        String uuid = userBE.getValue("PRI_UUID", null);

        if (uuid != null) {
            // use lowercase UUID
            uuid = uuid.toLowerCase();
            return getImpersonatedToken(keycloakUrl, realm, project, uuid, exchangedToken);
        }

        log.warn(ANSIColour.YELLOW + "No PRI_UUID found for user " + userBE.getCode() + ", attempting to use PRI_EMAIL instead" + ANSIColour.RESET);

        // grab email to fetch token
        String email = userBE.getValue("PRI_EMAIL", null);

        if (email != null) {
            return getImpersonatedToken(keycloakUrl, realm, project, email, exchangedToken);
        }

        log.error(ANSIColour.RED + "No PRI_EMAIL found for user " + userBE.getCode() + ANSIColour.RESET);
        return null;
    }

    /**
     * Fetch an Impersonated Token for a user.
     *
     * @param keycloakUrl
     * @param realm
     * @param project
     * @param uuid
     * @param exchangedToken
     * @return
     * @throws IOException
     */
    public static String getImpersonatedToken(String keycloakUrl, String realm, BaseEntity project, String uuid, String exchangedToken) throws IOException {

        // fetch keycloak json from porject entity
        String keycloakJson = project.getValueAsString("ENV_KEYCLOAK_JSON");
        JsonReader reader = Json.createReader(new StringReader(keycloakJson));
        JsonObject json = reader.readObject();

        // grab client secret
        JsonObject credentials = json.getJsonObject("credentials");
        String secret = credentials.getString("secret");
        reader.close();

        return getImpersonatedToken(keycloakUrl, realm, realm, secret, uuid, exchangedToken);

    }

    /**
     * Fetch an Impersonated Token for a user.
     *
     * @param keycloakUrl
     * @param realm
     * @param clientId
     * @param secret
     * @param username
     * @param exchangedToken
     * @return
     * @throws IOException
     */
    public static String getImpersonatedToken(String keycloakUrl, String realm, String clientId, String secret, String username, String exchangedToken) throws IOException {

        HashMap<String, String> params = new HashMap<>();

		params.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
		params.put("subject_token", exchangedToken);
		params.put("requested_subject", username);
        params.put("client_id", clientId);

        if (secret != null && !StringUtils.isBlank(secret)) {
            params.put("client_secret", secret);
        }

        String requestURL = keycloakUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token";

        String str = performPostCall(requestURL, params);

        log.debug("keycloak auth url = " + requestURL);
        log.debug(username + " token= " + str);

        JsonObject json = jsonb.fromJson(str, JsonObject.class);
        String accessToken = json.getString("access_token");
        return accessToken;
    }

    /**
     * Initialise a Dummy User in keycloak.
     *
     * @param token
     * @param realm
     * @return
     */
    public static String createDummyUser(String token, String realm) {

        String randomCode = UUID.randomUUID().toString().substring(0, 18);
        String defaultPassword = "password1";

        String json = "{ " + "\"username\" : \"" + randomCode + "\"," + "\"email\" : \"" + randomCode + "@gmail.com\" , "
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
     * @param token
     * @param realm
     * @param username
     * @return
     * @throws IOException
     */
    public static String getKeycloakUserId(final String token, final String realm, final String username) throws IOException {

        final List<LinkedHashMap> users = fetchKeycloakUser(token, realm, username);
        if (!users.isEmpty()) {
            return (String) users.get(0).get("id");
        }
        return null;
    }

    /**
     * Fetch a keycloak user using a username.
     *
     * @param token
     * @param realm
     * @param username
     * @return
     */
    public static List<LinkedHashMap> fetchKeycloakUser(final String token, final String realm, final String username) {

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
                log.error("Failed to get users from Keycloak, url:" + uri + ", response code:" + statusCode + ", token:" + token);
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

}
