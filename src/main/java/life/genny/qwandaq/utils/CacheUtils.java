package life.genny.qwandaq.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.data.GennyCache;

public class CacheUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	public static GennyCache cache = null;

	public static void init(GennyCache gennyCache) {
		cache = gennyCache;
	}

	/**
	* Read a stringified item from a realm cache.
	*
	* @param realm
	* @param key
	* @return
	 */
	public static Object readCache(String realm, String key) {

		Object ret = cache.getRemoteCache(realm).get(key);
		return ret;
	}

	/**
	* Write a stringified item to a realm cache.
	*
	* @param realm	The realm cache to use.
	* @param key	The key to save under.
	* @param value	The value to save.
	 */
	public static void writeCache(String realm, String key, String value) {

		cache.getRemoteCache(realm).put(key, value);
	}

	/**
	* Get an object from a realm cache.
	*
	* @param <T>
	* @param realm
	* @param key
	* @param c
	* @return
	 */
	public static <T> T getObject(String realm, String key, Class c) {

        String data = (String) readCache(realm, key);
		Object object = jsonb.fromJson(data, c);
		return (T) object;
	}
}
