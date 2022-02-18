package life.genny.qwandaq.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.CacheContainerAdmin;

@RegisterForReflection
@ApplicationScoped
public class GennyCache {
	
	static final Logger log = Logger.getLogger(GennyCache.class);

    Set<String> realms = new HashSet<String>();

    Map<String, RemoteCache> caches = new HashMap<>();

    RemoteCacheManager remoteCacheManager;

	@Inject GennyCache(RemoteCacheManager remoteCacheManager) {
       this.remoteCacheManager = remoteCacheManager;
    }

	@PostConstruct
	public void init() { 
		log.info("Cache Initialized!");
	}

	/**
	* Return a remote cache for the given realm.
	*
	* @param realm
	* @return
	 */
	public RemoteCache<String, String> getRemoteCache(final String realm) {

		if (realms.contains(realm)) {
			return caches.get(realm); 
		} else {
			remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(realm, DefaultTemplate.DIST_SYNC);
			realms.add(realm);
			caches.put(realm, remoteCacheManager.getCache(realm)); 
			return caches.get(realm); 
		}
	}

}

