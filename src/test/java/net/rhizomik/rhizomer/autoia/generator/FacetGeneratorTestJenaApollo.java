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
public class FacetGeneratorTestJenaApollo {

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
    public void testGetPropertiesApolloMissionRole() throws Exception {
        FacetGenerator fg = new FacetGenerator();
        HashMap<String,String> properties = new HashMap<String, String>();
        properties.put(Namespaces.rdf("type"), Namespaces.rdfs("Class"));
        properties.put(Namespaces.rdfs("label"), Namespaces.rdfs("Literal"));
        properties.put(space+"actor", foaf+"Person");
        properties.put(space+"mission", space+"Mission");
        properties.put(space+"role", space+"Role");
        assertEquals(properties, fg.getProperties(space+"MissionRole"));
    }

    @Test
    public void testGetInversePropertiesApolloMissionRole() throws Exception {
        FacetGenerator fg = new FacetGenerator();
        HashMap<String,String> properties = new HashMap<String, String>();
        properties.put(space+"missionRole", space+"MissionRole");
        properties.put(space+"performed", space+"MissionRole");
        assertEquals(properties, fg.getInverseProperties(space+"MissionRole"));
    }
}
