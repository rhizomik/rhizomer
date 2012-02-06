
// This is the counterpart of net.rhizomik.rhizomer.util.Namespaces.java

var NS = {
    RDF:  "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    RDFS: "http://www.w3.org/2000/01/rdf-schema#",
    OWL:  "http://www.w3.org/2002/07/owl#",
    XSD:  "http://www.w3.org/2001/XMLSchema#",
    RHZ:  "http://rhizomik.net/rhizomer/dataType#"
};

(function() {
    var ns;

    var makeAppender = function (prefix) {
        return function (suffix) {
            return prefix + suffix;
        }
    }

    for (ns in NS) {
        if (NS.hasOwnProperty(ns) && ns === ns.toUpperCase()) {
            NS[ns.toLowerCase()] = makeAppender(NS[ns]);
        }
    }
}());