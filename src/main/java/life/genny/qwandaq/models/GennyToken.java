package life.genny.qwandaq.models;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RegisterForReflection
public class GennyToken implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	String code;
	String userCode;
	String userUUID;
	String token;
	Map<String, Object> adecodedTokenMap = null;
	String realm = null;
	Set<String> userRoles = new HashSet<String>();

	public GennyToken(final String token) {

		if ((token != null) && (!token.isEmpty())) {
			// Getting decoded token in Hash Map from QwandaUtils
			adecodedTokenMap = getJsonMap(token);

			if (adecodedTokenMap == null) {
				log.error("Token is not able to be decoded in GennyToken ..");

			} else {
				// Extracting realm name from iss value
				String realm = null;
				if (adecodedTokenMap.get("iss") != null) {
					String[] issArray = adecodedTokenMap.get("iss").toString().split("/");
					realm = issArray[issArray.length - 1];
				} else if (adecodedTokenMap.get("azp") != null) {
					// clientid
					realm = (adecodedTokenMap.get("azp").toString());
				}

				// Adding realm name to the decoded token
				adecodedTokenMap.put("realm", realm);
				this.token = token;
				this.realm = realm;
				String uuid = adecodedTokenMap.get("sub").toString();
				String username = (String) adecodedTokenMap.get("preferred_username");
				String normalisedUsername = getNormalisedUsername(username);
				this.userUUID = "PER_" + this.getUuid().toUpperCase();

				if ("service".equals(username)) {
					this.userCode = "PER_SERVICE";
				} else {
					this.userCode = userUUID;
				}
				setupRoles();
			}
		} else {
			log.error("Token is null or zero length in GennyToken ..");
		}

	}

	public GennyToken(final String code, final String token) {

		this(token);
		this.code = code;
		if ("PER_SERVICE".equals(code)) {
			this.userCode = code;
		}
	}

	public String getToken() {
		return token;
	}

	public Map<String, Object> getAdecodedTokenMap() {
		return adecodedTokenMap;
	}

	public void setAdecodedTokenMap(Map<String, Object> adecodedTokenMap) {
		this.adecodedTokenMap = adecodedTokenMap;
	}

	private void setupRoles() {
		String realm_accessStr = "";
		if (adecodedTokenMap.get("realm_access") == null) {
			userRoles.add("user");
		} else {
			realm_accessStr = adecodedTokenMap.get("realm_access").toString();
			Pattern p = Pattern.compile("(?<=\\[)([^\\]]+)(?=\\])");
			Matcher m = p.matcher(realm_accessStr);

			if (m.find()) {
				String[] roles = m.group(1).split(",");
				for (String role : roles) {
					userRoles.add((String) role.trim());
				}
				;
			}
		}

	}

	public boolean hasRole(final String role) {
		return userRoles.contains(role);
	}

	@Override
	public String toString() {
		return getRealm() + ": " + getCode() + ": " + getUserCode() + ": " + this.userRoles;
	}

	public String getRealm() {
		return realm;
	}

	public String getString(final String key) {
		return (String) adecodedTokenMap.get(key);
	}

	public String getCode() {
		return code;
	}

	public String getSessionCode() {
		return getString("session_state");
	}

	public String getUsername() {
		return getString("preferred_username");
	}

	public String getJti() {
		return getString("jti");
	}
	public String getKeycloakUrl() {
		String fullUrl = getString("iss");
		URI uri;
		try {
			uri = new URI(fullUrl);
			String domain = uri.getHost();
			String proto = uri.getScheme();
			Integer port = uri.getPort();
			return proto + "://" + domain + ":" + port;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return "http://keycloak.genny.life";
	}

	public String getClientCode() {
		return getString("aud");
	}

	public String getEmail() {
		return getString("email");
	}

	/**
	 * @return the userCode
	 */
	public String getUserCode() {
		return userCode;
	}

	/**
	* Set the userCode
	*
	* @param userCode
	* @return
	 */
	public String setUserCode(String userCode) {
		return this.userCode = userCode;
	}

	public String getUserUUID() {
		return userUUID;
	}

	public LocalDateTime getAuthDateTime() {
		Long auth_timestamp = ((Number) adecodedTokenMap.get("auth_time")).longValue();
		LocalDateTime authTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(auth_timestamp),
				TimeZone.getDefault().toZoneId());
		return authTime;
	}

	public LocalDateTime getExpiryDateTime() {
		Long exp_timestamp = ((Number) adecodedTokenMap.get("exp")).longValue();
		LocalDateTime expTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(exp_timestamp),
				TimeZone.getDefault().toZoneId());
		return expTime;
	}

	public OffsetDateTime getExpiryDateTimeInUTC() {

		Long exp_timestamp = ((Number) adecodedTokenMap.get("exp")).longValue();
		LocalDateTime expTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(exp_timestamp),
				TimeZone.getDefault().toZoneId());
		ZonedDateTime ldtZoned = expTime.atZone(ZoneId.systemDefault());
		ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of("UTC"));

		return utcZoned.toOffsetDateTime();
	}

	public Integer getSecondsUntilExpiry() {

		OffsetDateTime expiry = getExpiryDateTimeInUTC();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		Long diff = expiry.toEpochSecond() - now.toEpochSecond(ZoneOffset.UTC);
		return diff.intValue();
	}

	/**
	* @return 	the JWT Issue datetime object
	 */
	public LocalDateTime getiatDateTime() {
		Long iat_timestamp = ((Number) adecodedTokenMap.get("iat")).longValue();
		LocalDateTime iatTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(iat_timestamp),
				TimeZone.getDefault().toZoneId());
		return iatTime;
	}

	/**
	* @return 	the unique token id
	 */
	public String getUniqueId() {
		return (String) adecodedTokenMap.get("jti");
	}

	public String getUuid() {
		String uuid = null;

		try {
			uuid = (String) adecodedTokenMap.get("sub");
		} catch (Exception e) {
			log.info("Not a valid user");
		}

		return uuid;
	}

	public String getEmailUserCode() {
		String username = (String) adecodedTokenMap.get("preferred_username");
		String normalisedUsername = getNormalisedUsername(username);
		return "PER_" + normalisedUsername.toUpperCase();

	}

	public String getNormalisedUsername(final String rawUsername) {
		if (rawUsername == null) {
			return null;
		}
		String username = rawUsername.replaceAll("\\&", "_AND_").replaceAll("@", "_AT_").replaceAll("\\.", "_DOT_")
				.replaceAll("\\+", "_PLUS_").toUpperCase();
		// remove bad characters
		username = username.replaceAll("[^a-zA-Z0-9_]", "");
		return username;

	}

	public Boolean checkUserCode(String userCode) {
		if (getUserCode().equals(userCode)) {
			return true;
		}
		if (getEmailUserCode().equals(userCode)) {
			return true;
		}
		return false;

	}

	/**
	 * @return the userRoles
	 */
	public Set<String> getUserRoles() {
		return userRoles;
	}

	/**
	* @return the realm and usercode concatenated
	 */
	public String getRealmUserCode() {
		return getRealm() + "+" + getUserCode();
	}

	// Send the decoded Json token in the map
	public Map<String, Object> getJsonMap(final String json) {
		final JsonObject jsonObj = getDecodedToken(json);
		return getJsonMap(jsonObj);
	}

	public static Map<String, Object> getJsonMap(final JsonObject jsonObj) {
		final String json = jsonObj.toString();
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			final ObjectMapper mapper = new ObjectMapper();
			// convert JSON string to Map
			final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
			};

			map = mapper.readValue(json, typeRef);

		} catch (final JsonGenerationException e) {
			e.printStackTrace();
		} catch (final JsonMappingException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return map;
	}

	public JsonObject getDecodedToken(final String bearerToken) {
		Jsonb jsonb = JsonbBuilder.create();

		final String[] chunks = bearerToken.split("\\.");
		Base64.Decoder decoder = Base64.getDecoder();
//		String header = new String(decoder.decode(chunks[0]));
		String payload = new String(decoder.decode(chunks[1]));
		JsonObject json = jsonb.fromJson(payload, JsonObject.class);
		return json;
	}
}
