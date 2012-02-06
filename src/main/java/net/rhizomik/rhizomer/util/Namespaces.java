// This is the counterpart of script/util/namespaces.js

package net.rhizomik.rhizomer.util;

/**
 * Common namespaces and basic generation methods for uris.
 */

public class Namespaces {
    
    public static final String RDF  = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String OWL  = "http://www.w3.org/2002/07/owl#";
    public static final String XSD  = "http://www.w3.org/2001/XMLSchema#";
    public static final String RHZ  = "http://rhizomik.net/rhizomer/dataType#";
    
    public static String rdf(String suffix) {
        return RDF + suffix;
    }
    
    public static String rdfs(String suffix) {
        return RDFS + suffix;
    }
    
    public static String owl(String suffix) {
        return OWL + suffix;
    }

    public static String xsd(String suffix) {
        return XSD + suffix;
    }
    
    public static String rhz(String suffix) {
        return RHZ + suffix;
    }
}
