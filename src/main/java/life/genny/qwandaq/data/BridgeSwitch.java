package life.genny.qwandaq.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.CacheUtils;

/**
 * A Bridge ID management class for data message route selection.
 * 
 * @author Byron Aguirre
 * @author Jasper Robison
 */
public class BridgeSwitch {

	static final Logger log = Logger.getLogger(BridgeSwitch.class);

	static Jsonb jsonb = JsonbBuilder.create();

	public static ConcurrentMap<String, String> mappings = new ConcurrentHashMap<>();

	public static String BRIDGE_INFO_PREFIX = "BIF";

	public static class BridgeInfo {

		public BridgeInfo() {}

		private ConcurrentMap<String, String> mappings = new ConcurrentHashMap<>();
	}

	/**
	* Put an entry into the users BridgeInfo item in the cache
	*
	* @param gennyToken The users GennyToken
	* @param bridgeId The ID of the bridge used in communication
	 */
	public static void put(GennyToken gennyToken, String bridgeId) {

		String realm = gennyToken.getRealm();
		String key = BRIDGE_INFO_PREFIX + "_" + gennyToken.getUserCode();
		
		// grab from cache or create if null
		BridgeInfo info = CacheUtils.getObject(realm, key, BridgeInfo.class);
		
		if (info == null) {
			info = new BridgeInfo();
		}

		// add entry for jti and update in cache
		String jti = gennyToken.getUniqueId();
		info.mappings.put(jti, bridgeId);

		CacheUtils.putObject(realm, key, info);
	}

	/**
	* Get the corresponding bridgeId from the users BridgeInfo 
	* object in the cache.
	*
	* @param gennyToken The users GennyToken
	* @return String The corresponding bridgeId
	 */
	public static String get(GennyToken gennyToken) {

		String realm = gennyToken.getRealm();
		String key = BRIDGE_INFO_PREFIX + "_" + gennyToken.getUserCode();
		
		// grab from cache
		BridgeInfo info = CacheUtils.getObject(realm, key, BridgeInfo.class);
		
		if (info == null) {
			log.error("No BridgeInfo object found for user " + gennyToken.getUserCode());
		}

		// grab entry for jti
		String jti = gennyToken.getUniqueId();
		String bridgeId = info.mappings.get(jti);

		return bridgeId;
	}

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
		BridgeSwitch.mappings.put(jti, bridgeId);
	}
}
