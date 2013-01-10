package net.rhizomik.rhizomer.store;

/**
 * @author http://rhizomik.net/~roberto/
 */

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class ResultSetCache {
    private static int hitcount = 0;
    private static int defaultCacheSize = 5000;
    private static net.sf.ehcache.Cache cache;
    private static Logger log = Logger.getLogger(ResultSetCache.class.getName());

    public ResultSetCache(String cacheID) {
        new ResultSetCache(cacheID, defaultCacheSize);
    }

    public ResultSetCache(String cacheID, int cacheSize) {
        CacheManager manager = CacheManager.create();
        cache = new Cache(cacheID, cacheSize, false, false, 500, 200);
        manager.addCache(cache);
    }

    public static int getHitcount() {
        return hitcount;
    }

    public static int getDefaultCacheSize() {
        return defaultCacheSize;
    }

    public static void setDefaultCacheSize(int defaultCacheSize) {
        ResultSetCache.defaultCacheSize = defaultCacheSize;
    }

    public boolean isCached(String query) {
        return cache.isKeyInCache(query);
    }

    public ResultSet getCached(String query) {
        Element hit = cache.get(query);
        ResultSet result = new ResultSetMem();
        if (hit != null) {
            result = ResultSetFactory.fromXML(hit.getObjectValue().toString());
            log.log(Level.WARNING, "Cache Hit No." + hitcount++);
        }
        return result;
    }

    public void put(String query, ResultSet result) {
        String resultXML = ResultSetFormatter.asXMLString(result);
        cache.put(new Element(query, resultXML));
    }

    public void remove(Set<String> invalidQueries) {
        for (String query : invalidQueries)
            cache.remove(query);
    }
}
