/**
 * Tests for module src/main/webapp/script/util/namespaces.js
 */

TestCase("Namespaces", {

    "test should define RDF uri": function () {
        assertNotUndefined(NS.RDF);
    },

    "test should assign RDF its right uri": function () {
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", NS.RDF);
    },

    "test should define rdf as prepending RDF uri": function () {
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#anything", NS.rdf("anything"));
    }

});