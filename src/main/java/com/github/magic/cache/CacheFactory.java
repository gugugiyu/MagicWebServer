package com.github.magic.cache;

import com.github.magic.cache.lru_cache.LRUCache;


import java.io.IOException;

public class CacheFactory {
    public enum CacheEvictionPolicy {
        LEAST_RECENTLY_USED,
        LEAST_FREQUENTLY_USED,
        FIRST_IN_FIRST_OUT
    }

    /**
     * Returns the cache based on the given type of caching
     *
     * @param cacheType the type of the cache
     * @return {@link Cache} instance that was implemented, or {@code null} is the type given is invalid
     */
    public static Cache getCache(CacheEvictionPolicy cacheType) {
        return switch (cacheType) {
            case LEAST_RECENTLY_USED -> new LRUCache();
            case LEAST_FREQUENTLY_USED -> null; //Not implemented
            case FIRST_IN_FIRST_OUT -> null; // Not implemented
            default -> null;
        };
    }
}
