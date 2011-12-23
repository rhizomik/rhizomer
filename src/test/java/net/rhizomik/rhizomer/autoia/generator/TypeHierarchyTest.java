package net.rhizomik.rhizomer.autoia.generator;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static net.rhizomik.rhizomer.util.Namespaces.*;
import static org.junit.Assert.assertEquals;

public class TypeHierarchyTest {
    
    private TypeHierarchy th;

    @Before
    public void setUp() throws Exception {
        th = TypeHierarchy.RDF;
    }

    @Test
    public void returns_itself_for_singletons() {
        Collection<String> types = Arrays.asList(
                rdfs("Resource"), rdfs("Literal"), xsd("string"));
        for (String only: types) {
            Collection<String> singleton = Arrays.asList(only);
            assertEquals(only, th.lowestCommonType(singleton));
        }
    }

    @Test
    public void returns_itself_for_repeated_types() {
        Collection<String> types = Arrays.asList(
                rdfs("Resource"), rdfs("Literal"), xsd("string"));
        for (String only: types) {
            Collection<String> singleton = Arrays.asList(only, only, only);
            assertEquals(only, th.lowestCommonType(singleton));
        }
    }

    @Test
    public void returns_parent_if_siblings() {
        String parent = rdfs("Resource");
        Collection<String> siblings = Arrays.asList(
                rdfs("Class"), rdfs("Literal"), rdf("Property"));
        assertEquals(parent, th.lowestCommonType(siblings));
    }

    @Test
    public void general_case() {
        Collection<String> types = Arrays.asList(xsd("integer"), xsd("string"));
        assertEquals(xsd("anySimpleType"), th.lowestCommonType(types));
    }
}
