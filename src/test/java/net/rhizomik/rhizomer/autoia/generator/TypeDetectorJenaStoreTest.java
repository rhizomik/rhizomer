package net.rhizomik.rhizomer.autoia.generator;

import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.util.Namespaces;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TypeDetectorJenaStoreTest {
    
    static String space = "http://purl.org/net/schemas/space/";
    static String foaf = "http://xmlns.com/foaf/0.1/";

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("store_class", "net.rhizomik.rhizomer.store.jena.JenaStore");
        props.put("dir_name", "metadata");
        props.put("file_name", "nasa-apollo.rdf");
        RhizomerRDF.instance().addStore(props);
    }

    @Test
    public void testSpacecraft_isSpaceCraftOf() {
        TypeDetector td = new TypeDetector(space+"Spacecraft", space+"spacecraft", true);
        assertEquals(space+"Launch", td.detectRange());
    }

    @Test
    public void testMissionRole_actor() {
        TypeDetector td = new TypeDetector(space+"MissionRole", space+"actor", false);
        assertEquals(foaf+"Person", td.detectRange());
    }
}

