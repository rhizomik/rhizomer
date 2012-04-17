package net.rhizomik.rhizomer.autoia.generator;

import net.rhizomik.rhizomer.agents.RhizomerRDF;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TypeDetectorSesameStoreTest {
    
    static String co = "http://rhizomik.net/ontologies/2009/09/copyrightonto.owl#";
    static String msp = "http://rhizomik.net/ontologies/2011/06/mspontology.owl#";
    static String ddex = "http://rhizomik.net/ontologies/2011/06/ddex.owl#";

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("store_class", "net.rhizomik.rhizomer.store.sesame.SesameStore");
        props.put("manager_url", "http://localhost/openrdf-sesame");
        props.put("repository_id", "msp-reasoner-examples");
        props.put("db_graph", "http://msp.sonydadc.com/examples/");
        RhizomerRDF.instance().addStore(props);
    }

    @Test
    public void coInstance_coIsInstanceOf() {
        TypeDetector td = new TypeDetector(co+"Instance", co+"hasInstance", true);
        assertEquals(msp+"Asset", td.detectRange());
    }

    @Test
    public void ddexMusicalWorkSoundRecording_mspCreator() {
        TypeDetector td = new TypeDetector(ddex+"MusicalWorkSoundRecording", msp+"creator", false);
        assertEquals(msp+"Talent", td.detectRange());
    }

    @Test
    public void ddexMusicalWorkSoundRecording_coTheme() {
        TypeDetector td = new TypeDetector(ddex+"MusicalWorkSoundRecording", co+"theme", true);
        assertEquals(msp+"Deal", td.detectRange());
    }
}

