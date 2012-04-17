package net.rhizomik.rhizomer.autoia.generator;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.store.MetadataStore;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.rhizomik.rhizomer.autoia.generator.TypeHierarchy.RDF;
import static net.rhizomik.rhizomer.util.Namespaces.rdfs;
import static net.rhizomik.rhizomer.util.Namespaces.xsd;

/*
Alternative to find common superclasses, more than one when disjoint subclasses...

PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT DISTINCT ?common
WHERE {
GRAPH <http://msp.sonydadc.com/examples/schema/>  { ?type rdfs:subClassOf{1} ?common. }
FILTER (!isBlank(?common))
{SELECT ?type
WHERE{ GRAPH <http://msp.sonydadc.com/examples/>  {
   ?x a owl:Class .
   ?x <http://rhizomik.net/ontologies/2009/09/copyrightonto.owl#theme> ?o .
   ?o a ?type .
} } }
*/

public class TypeDetector {

    private static final Logger log = Logger.getLogger(TypeDetector.class.getName());
    private static String NL = System.getProperty("line.separator");
	private static String queryForValues = 
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT ?o (COUNT(?o) AS ?n) "+NL+
        "WHERE {"+NL+
        "   ?x a <%1$s> ."+NL+
        "   ?x <%2$s> ?o ."+NL+
        "   FILTER(?o!=\"\") ."+NL+
    	"}"+NL+
    	"GROUP BY ?o"+NL+
    	"ORDER BY DESC(?n) LIMIT 5";
	
	private static String queryForPivotingFacets = 
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
	    "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
	    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
	    "SELECT ?type (COUNT(?type) AS ?n) "+NL+
	    "WHERE {"+NL+
	    "   ?x a <%1$s> ."+NL+
	    "   ?x <%2$s> ?o ."+NL+
	    "   ?o a ?type ."+NL+
// TODO: is this restriction really necessary?	    "	?type a ?class"+NL+
	    "}"+NL+
        "GROUP BY ?type"+NL+
        "ORDER BY DESC(?n) LIMIT 1";

    private static String queryForFacetTypes =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
        "SELECT ?type (COUNT(?type) AS ?n) \n" +
        "WHERE { \n" +
        "   ?x a <%1$s> .\n" +
        "   ?x <%2$s> ?o .\n" +
        "   ?o a ?type .\n" +
        "   ?type a ?class.\n" +
        "   FILTER ( !isBlank(?type) && (?class = owl:Class || ?class = rdfs:Class) ) } \n"+
        "GROUP BY ?type"+NL+
        "ORDER BY DESC(?n) LIMIT 5";

    private static String queryForInverseFacetTypes =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
        "SELECT ?type (COUNT(?type) AS ?n) \n" +
        "WHERE { \n" +
        "   ?o a <%1$s> .\n" +
        "   ?x <%2$s> ?o .\n" +
        "   ?x a ?type .\n" +
        "   ?type a ?class.\n" +
        "   FILTER ( !isBlank(?type) && (?class = owl:Class || ?class = rdfs:Class) ) } \n"+
        "GROUP BY ?type"+NL+
        "ORDER BY DESC(?n) LIMIT 5";

    private static String queryForSuperClassInversePropertyPath =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "SELECT DISTINCT ?common\n" +
        "WHERE {\n" +
        "   ?common ^rdfs:subClassOf %1$s.\n" +  //comma separated list of subclasses
        "   OPTIONAL {\n" +
        "      ?intermediate ^rdfs:subClassOf %1$s.\n" +
        "      ?intermediate rdfs:subClassOf ?common." +
        "      FILTER (?intermediate!=?common && !isBlank(?intermediate)) } \n" +
        "   FILTER (!BOUND(?intermediate) && !isBlank(?common)) }";

    private static String queryForSuperClass =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
        "SELECT ?common (COUNT(?i) AS ?n) \n" +
        "WHERE { \n" +
        "   ?i a ?common. \n" +
        "   ?subclasses rdfs:subClassOf ?common. \n" +
        "   ?common a ?class. \n" +
        "   OPTIONAL {\n" +
        "      ?subclass rdfs:subClassOf ?intermediate.\n" +
        "      ?intermediate rdfs:subClassOf ?common." +
        "      FILTER (?intermediate!=?common && !isBlank(?intermediate)) } \n" +
        "   FILTER (!BOUND(?intermediate) && !isBlank(?common) &&" +
        "           (%1$s) ) \n"+                        // disjunction of subclasses
        "   FILTER ( ?class = owl:Class || ?class = rdfs:Class ) } \n" +
        "GROUP BY ?common \n"+
        "ORDER BY DESC(?n) LIMIT 1";

    private String uri;
    private String property;
    private boolean isInverse;

    public TypeDetector(String uri, String property, boolean isInverse) {
        this.uri = uri;
        this.property = property;
        this.isInverse = isInverse;
    }
    
    private String makeQueryForPivotingFacets(String uri, String property){
    	Formatter f = new Formatter();
        Object[] vars = {uri, property};
        f.format(queryForPivotingFacets, vars);
        return f.toString();
    }

    private String makeQueryForFacetTypes(String uri, String property){
        Formatter f = new Formatter();
        Object[] vars = {uri, property};
        f.format(queryForFacetTypes, vars);
        return f.toString();
    }

    private String makeQueryForInverseFacetTypes(String uri, String property){
        Formatter f = new Formatter();
        Object[] vars = {uri, property};
        f.format(queryForInverseFacetTypes, vars);
        return f.toString();
    }

    private String makeQueryForSuperClass(String facetTypes){
        Formatter f = new Formatter();
        f.format(queryForSuperClass, facetTypes);
        return f.toString();
    }

    private String makeQueryForValues(String uri, String property) {
        Formatter f = new Formatter();
        Object[] vars = {uri, property};
        f.format(queryForValues, vars);
        return f.toString();
    }
    
    public String detectRange(){
        String range = rdfs("Resource");
        String queryString = "";
        if (this.isInverse)
            queryString = makeQueryForInverseFacetTypes(uri, property);
        else
    	    queryString = makeQueryForFacetTypes(uri, property);
        
        ResultSet results = RhizomerRDF.instance().querySelect(queryString, MetadataStore.REASONING);
        String typeVar = results.getResultVars().get(0);
        String facetTypes = "";
        while(results.hasNext()){
        	QuerySolution row = results.next();
            if (row.get(typeVar)!=null) {
        	    range = row.get(typeVar).toString();
                if (!range.contains("http://www.w3.org/2002/07/owl#Thing") &&
                    !range.contains("http://www.w3.org/2002/07/owl#Class") &&
                    !range.contains("http://www.w3.org/2000/01/rdf-schema#Resource") &&
                    !range.contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#Resource") ) {
                    if (facetTypes.length() > 0) facetTypes += " || ";
                    facetTypes += "?subclass = <"+range+">";
                }
            }
        }
        log.log(Level.INFO, "Facet types: "+facetTypes);
        if (facetTypes.length()>0) {
            queryString = makeQueryForSuperClass(facetTypes);
            results = RhizomerRDF.instance().querySelect(queryString, MetadataStore.REASONING);
            String classVar = results.getResultVars().get(0);
            if (results.hasNext()) {
                QuerySolution row = results.next();
                if (row.get(classVar)!=null)
                    range = row.get(classVar).toString();
            }
        }
        return range;
    }
    

    public String detectType() {
        String queryString = makeQueryForValues(uri, property);
        ResultSet results = RhizomerRDF.instance().querySelect(queryString, MetadataStore.REASONING);
        Set<String> types = getTypes(results);
        assert(types.size() > 0);
        if (types.size() == 0)
            log.log(Level.WARNING, "No values for class: "+uri+" and property: "+property);
        return types.size()>0? RDF.lowestCommonType(types):rdfs("Resource");
    }

    private Set<String> getTypes(ResultSet results) {
        Set<String> types = new HashSet<String>();
        String objVar = results.getResultVars().get(0);
        while(results.hasNext()){
			QuerySolution row = results.next();
            RDFNode current = row.get(objVar);
            if(current == null)
				continue;
            String type = inferType(current);
            types.add(type);
		}
        return types;
    }

    private String inferType(RDFNode current) {
        String type;
        if(current.isResource()) {
            type = rdfs("Resource");
        } else {
            type = current.asNode().getLiteralDatatypeURI();
            if(type == null){
                String value = current.toString();	
                type = inferTypeByParsing(value);
            }
        }
        return type;
    }

    private String inferTypeByParsing(String value){
    	try{
    		Integer.parseInt(value);
    		return xsd("integer");
    	}
    	catch(Exception e){}
    	try{
    		Double.parseDouble(value);
    		return xsd("double"); // Double, float or decimal
    	}
    	catch(Exception e){}
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			formatter.parse(value);
			return xsd("date");
		} catch (ParseException e) {}    	
    	
    	/* Parse Geo points */
    	return xsd("string");
    }

}
