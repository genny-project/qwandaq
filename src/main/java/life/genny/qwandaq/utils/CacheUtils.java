package life.genny.qwandaq.utils;

import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.data.GennyCache;

@RegisterForReflection
public class CacheUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	public static GennyCache cache = null;

	
	/** 
	 * @param gennyCache
	 */
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
	 * @param realm The realm cache to use.
	 * @param key   The key to save under.
	 * @param value The value to save.
	 */
	public static void writeCache(String realm, String key, String value) {

		cache.getRemoteCache(realm).put(key, value);
	}

	/**
	 * Get an object from a realm cache using a {@link Class}.
	 *
	 * @param <T>
	 * @param realm
	 * @param key
	 * @param c
	 * @return
	 */
	public static <T> T getObject(String realm, String key, Class c) {

		String data = (String) readCache(realm, key);
		if (data == null) {
			return null;
		}
		Object object = jsonb.fromJson(data, c);
		return (T) object;
	}

	/**
	 * Get an object from a realm cache using a {@link Type}.
	 *
	 * @param <T>
	 * @param realm
	 * @param key
	 * @param type
	 * @return
	 */
	public static <T> T getObject(String realm, String key, Type t) {

		String data = (String) readCache(realm, key);
		if (data == null) {
			return null;
		}
		Object object = jsonb.fromJson(data, t);
		return (T) object;
	}

	/**
	 * Put an object into the cache.
	 *
	 * @param realm
	 * @param key
	 * @param obj
	 */
	public static void putObject(String realm, String key, Object obj) {

		String json = jsonb.toJson(obj);
		cache.getRemoteCache(realm).put(key, json);
	}
}
