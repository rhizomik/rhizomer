package net.rhizomik.rhizomer.autoia.generator;

import com.hp.hpl.jena.vocabulary.DCTerms;
import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.util.Namespaces;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author http://rhizomik.net/~roberto/
 */
public class FacetGeneratorTest {

    static String co = "http://rhizomik.net/ontologies/2009/09/copyrightonto.owl#";
    static String msp = "http://rhizomik.net/ontologies/2011/06/mspontology.owl#";
    static String dct = "http://purl.org/dc/terms/";

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
    public void testGetProperties() throws Exception {
        FacetGenerator fg = new FacetGenerator();
        HashMap<String,String> properties = new HashMap<String, String>();
        properties.put(Namespaces.rdf("type"), null);
        properties.put(Namespaces.rdfs("label"), null);
        properties.put(msp+"propietaryId", null);
        properties.put(msp+"recordLabel", null);
        properties.put(msp+"isrc", null);
        properties.put(msp+"creator", null);
        properties.put(msp+"parentalWarning", null);
        properties.put(msp+"genre", null);
        properties.put(dct+"title", null);
        properties.put(dct+"extent", null);
        properties.put(dct+"language", null);
        properties.put(dct+"copyright", null);
        properties.put(dct+"alternative", null);
        properties.put(co+"hasInstance", null);
        assertEquals(properties, fg.getProperties(msp + "Asset"));
    }

    @Test
    public void testGetInverseProperties() throws Exception {
        FacetGenerator fg = new FacetGenerator();
        HashMap<String,String> properties = new HashMap<String, String>();
        properties.put(Namespaces.rdf("first"), null);
        properties.put(co+"theme", null);
        properties.put(co+"hasPart", null);
        assertEquals(properties, fg.getInverseProperties(msp + "Asset"));
    }
}
