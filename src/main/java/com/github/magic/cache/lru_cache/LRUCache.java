package com.github.magic.cache.lru_cache;

import com.github.magic.cache.Cache;
import com.github.magic.cache.CacheEntry;
import com.github.magic.cache.ETagGenerator;
import com.github.magic.core.config.Config;
import com.github.magic.core.utils.FileAttributeRetriever;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache
        extends Cache implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /*
    * This is the implementation of LRU cache using the Double Ended Queue and a Hashmap. The algorithm was implemented from this article:
    * https://www.geeksforgeeks.org/lru-cache-implementation/#lru-cache-implementation-using-deque-hashmap
    * */

    /*
    The double ended queue that stores the Etag. Here, the front (first) end is considered to be the highest priority element, while
    the rear (last) is the least recently used, thus being the first one to be removed
     */
    private final Deque<String> queue;

    //Maps between the Etag and its freshness
    private final Map<String, CacheEntry> hashset;

    public LRUCache(){
        queue = new LinkedList<>();
        hashset = new Hashtable<>();
    }

    /*
    LRU cache is maintained per server, in a multi-threading environment, therefore it need a locking mechanism to manage race condition
    The implementation use the simple Lock instance for the sake of simplicity
     */

    private transient final Lock lock = new ReentrantLock();

    @Override
    public void cache(String ETag, CacheEntry value) {
        if (value == null)
            return;

        lock.lock();
        try {
            String evictedEtag;
            if (queue.size() >= Config.MAX_CACHE_SIZE) {
                //Cache full, evict the least recently used to make some spaces
                evictedEtag = queue.removeLast();

                //max_age and createdSince doesn't matter here since we only compare the etag
                hashset.remove(evictedEtag);
            }

            if (!hashset.containsKey(ETag)){
                hashset.put(ETag, value);
                queue.addFirst(ETag);
            }

        } finally {
            lock.unlock();
        }
    }

    @Override
    public CacheState exist(Date date) {
        return CacheState.FRESH; //TODO WIP
    }

    @Override
    public CacheState exist(String Etag) {
        if (Etag == null) return null;

        CacheEntry entry = hashset.get(Etag);
        CacheState state;

        if (entry != null){
            //It could be stale
           if (!entry.isStale()) {
               return CacheState.FRESH;
           }
            String oldEtag = Etag;

            //And recompute its Etag
            //"-d" suffix means dynamic content
            //"-s" suffix means static content
            if (Etag.endsWith("-d")){
                Etag = ETagGenerator.generateEtagForDynamicResponse(entry.content());
            }else{
                try {
                    Etag = ETagGenerator.generateEtagForStaticResponse(
                            Files.size(entry.file()),
                            Files.getLastModifiedTime(entry.file()).to(TimeUnit.SECONDS),
                            FileAttributeRetriever.getMimeType(entry.file().toFile()),
                            entry.encoding(),
                            entry.language()
                    );
                } catch (IOException ignored) {}
            }

            if (!oldEtag.equals(Etag)){
                CacheEntry refreshEntry = new CacheEntry(entry.max_age(), new Date(), entry.file(), entry.encoding(), entry.language(), entry.content());

                //If the Etag is different, we need to renew it
                hashset.remove(oldEtag);
                hashset.put(Etag, refreshEntry);
            }

            state = CacheState.STALE;
        }else{
            state = CacheState.MISS;
        }

        lock.lock();
        try{
            queue.remove(Etag);
            queue.addFirst(Etag);
        }finally {
            lock.unlock();
        }

        return state;
    }

    @Override
    public void saveCache(String filename) {
        if (!filename.startsWith("/")) //Add the forward slash
            filename = "/" + filename;

        if (!filename.endsWith(".ser"))
            filename = filename + ".ser";

        try (ObjectOutputStream fileWriter = new ObjectOutputStream(new FileOutputStream(Config.CACHE_DIR + filename))){
            fileWriter.writeObject(this);
        } catch (IOException e){
            System.out.println("[-] Unable to save cache: " + e.getMessage());
        }
    }

    public Deque<String> queue() {
        return queue;
    }
}
