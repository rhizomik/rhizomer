package net.rhizomik.rhizomer.autoia.generator;

import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.util.Namespaces;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TypeDetectorVirtuosoStoreTest {
    
    static String lmdb = "http://data.linkedmdb.org/resource/movie/";
    static String foaf = "http://xmlns.com/foaf/0.1/";

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("store_class", "net.rhizomik.rhizomer.store.virtuoso.VirtuosoStore");
        props.put("db_graph", "http://rhizomik.net/linkedmdb/");
        props.put("db_url", "jdbc:virtuoso://omediadis.udl.cat:1111");
        props.put("db_user", "rhizomer");
        props.put("db_pass", "griho");
        RhizomerRDF.instance().addStore(props);
    }

    @Test
    public void performance_isPerformanceOf() {
        TypeDetector td = new TypeDetector(lmdb+"performance", lmdb+"performance", true);
        assertEquals(Namespaces.rdfs("Resource"), td.detectRange());
    }

    @Test
    public void film_actor() {
        TypeDetector td = new TypeDetector(lmdb+"film", lmdb+"actor", false);
        assertEquals(lmdb+"actor", td.detectRange());
    }
}

