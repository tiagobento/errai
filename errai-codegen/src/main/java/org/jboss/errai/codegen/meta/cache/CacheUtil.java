package org.jboss.errai.codegen.meta.cache;

import org.jboss.errai.codegen.meta.impl.java.JavaReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class CacheUtil {
  private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);

  private CacheUtil() {
  }

  private static final Map<Class<? extends CacheStore>, CacheStore> CACHE_STORE_MAP
          = new HashMap<Class<? extends CacheStore>, CacheStore>();

  public static <T extends CacheStore> T getCache(final Class<T> type) {
    synchronized (type) {
      T cacheStore = (T) CACHE_STORE_MAP.get(type);
      if (cacheStore == null) {
        try {
          cacheStore = type.newInstance();
          CACHE_STORE_MAP.put(type, cacheStore);
        }
        catch (Throwable e) {
          throw new RuntimeException("failed to instantiate new type: " + type.getName(), e);
        }
      }

      return cacheStore;
    }
  }

  public static synchronized void clearAll() {
    log.info("clearing all generation caches...");

    for (Map.Entry<Class<? extends CacheStore>, CacheStore> entry : CACHE_STORE_MAP.entrySet()) {
      synchronized (entry.getKey()) {
        entry.getValue().clear();
      }
    }
  }
}
