package com.github.magic.cache;

import java.util.Date;
import java.util.Deque;

/**
 * <p>This class serve as the base class to implement the caching functionality for this webserver.</p>
 *
 * <p>All <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Content_negotiation">Content Negotiation headers</a> are taken into consideration here, namely {@code Accept}, {@code Accept-Encoding}, and {@code Accept-Language}
 * which will affect the generation of the ETag. In other words, a document with two representations in two different languages will be cached independently</p>
 */
public abstract class Cache {
    /*
    Return this enum when the method exist() is invoked.
     */
    public enum CacheState {
        MISS,  /* Return this value when the cache has missed, and therefore a new response should be generated and cached */
        STALE, /* The cache entry does exist, however, the value has been stale after either the Etag or the modified date has been recalibrated, or when the age of the cache entry exceeded max-age */
        FRESH, /* The cache hit, and the content is still fresh */
    }

    /**
     * Cache a file through its MIME-type, language (default to {@link Locale#English}), and encoding (optional).
     *
     * @param value The value to be put in the cache
     */
    public abstract void cache(String Etag, CacheEntry value);

    /**
     * Checks the cache if the current request URI has been modified since this time
     *
     * @param date if the cache has been modified from this date
     * @return {@link CacheState#MISS} if the cache missed,  {@link CacheState#STALE} if the cache is found but stale, and {@link CacheState#FRESH} for cache hit
     */
    public abstract CacheState exist(Date date);

    /**
     * Checks the cache if the any response served with this eTag existed
     *
     * @param Etag the eTag of the resource
     * @return the {@link CacheEntry} instance if found, or {@code null} if not found
     */
    public abstract CacheState exist(String Etag);

    /**
     * Since all {@link CacheEntry} implement the {@link java.io.Serializable} interface, we can save the cache to the disk and later retrieve it in the server
     *
     * @param filename The name of the cache file (this name will be prefixed with .ser, so don't specify a suffix for it when passing this argument)
     */
    public abstract void saveCache(String filename);
}
