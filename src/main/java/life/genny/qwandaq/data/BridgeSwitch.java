package life.genny.qwandaq.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennyToken;

public class BridgeSwitch {

	static final Logger log = Logger.getLogger(BridgeSwitch.class);

	static Jsonb jsonb = JsonbBuilder.create();

	public static ConcurrentMap<String, String> bridges = new ConcurrentHashMap<>();

	/**
	* Update the BridgeSwitch using an incoming payload. 
	* Function will initially look for a field relating to 
	* the JTI found in the token, and if nothing is found, will try
	* looking for a bridgeId field since it might be an internal message.
	*
	* @param data An incoming stringified payload.
	 */
	public static void updateBridgeSwitchWithIncomingData(String data) {

		// deserialise msg into JsonObject
		JsonObject payload = jsonb.fromJson(data, JsonObject.class);
		String token = payload.getString("token");

		// grab userToken from message
		GennyToken userToken = new GennyToken(token);
		String jti = userToken.getUniqueId();

		if (jti == null) {
			log.error("JTI is null for token");
			return;
		}

		// look for field corresponding to the JTI
		String bridgeId = payload.getString(jti);

		// if null, this might be an internal msg object
		if (bridgeId == null) {
			bridgeId = payload.getString("bridgeId");
		}

		if (bridgeId == null) {
			log.error("Could not resolve bridgeId");
			return;
		}

		// update bridge switch
		BridgeSwitch.bridges.put(jti, bridgeId);
	}
}
