package life.genny.qwandaq.data;

import java.util.HashSet;
import java.util.Set;
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

	public static String BRIDGE_INFO_PREFIX = "BIF";

	public static Set<String> activeBridgeIds = new HashSet<String>();

	/**
	 * A child class used to store bridge mappings for individual users.
	 */
	public static class BridgeInfo {

		public BridgeInfo() {}

		public ConcurrentMap<String, String> mappings = new ConcurrentHashMap<>();
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
		String jti = gennyToken.getJTI();
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
			log.debug("No BridgeInfo object found for user " + gennyToken.getUserCode());
			return null;
		}

		// grab entry for jti
		String jti = gennyToken.getJTI();
		String bridgeId = info.mappings.get(jti);

		return bridgeId;
	}

}
