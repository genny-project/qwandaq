package life.genny.qwanda.qwandautils;

public interface GennyCacheInterface {
    Object readCache(final String realm, final String key, final String token);

    void writeCache(final String realm, final String key, final String value, final String token, long ttl_seconds);

    void clear(final String realm);
}


