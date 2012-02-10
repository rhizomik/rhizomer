package net.rhizomik.rhizomer.autoia.generator;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import net.rhizomik.rhizomer.agents.RhizomerRDF;

import static net.rhizomik.rhizomer.autoia.generator.TypeHierarchy.RDF;
import static net.rhizomik.rhizomer.util.Namespaces.rdfs;
import static net.rhizomik.rhizomer.util.Namespaces.xsd;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;


public class TypeDetector {

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
	    "SELECT distinct(?type)"+NL+
	    "WHERE {"+NL+
	    "   ?x a <%1$s> ."+NL+
	    "   ?x <%2$s> ?o ."+NL+
	    "   ?o a ?type ."+NL+
	    "	?type a ?class"+NL+
	    "}" +
	    "LIMIT 1";	

    private String uri;
    private String property;

    public TypeDetector(String uri, String property) {
        this.uri = uri;
        this.property = property;
    }
    
    private String makeQueryForPivotingFacets(String uri, String property){
    	Formatter f = new Formatter();
        Object[] vars = {uri, property};
        f.format(queryForPivotingFacets, vars);
        return f.toString();
    }

    private String makeQueryForValues(String uri, String property) {
        Formatter f = new Formatter();
        Object[] vars = {uri, property};
        f.format(queryForValues, vars);
        return f.toString();
    }
    
    public String detectRange(){
    	String queryString = makeQueryForPivotingFacets(uri, property);
        ResultSet results = RhizomerRDF.instance().querySelect(queryString, true);
        if(results.hasNext()){
        	QuerySolution row = results.next();
        	return row.get("type").toString();
        }
        else
        	return null;
    }
    

    public String detectType() {
        String queryString = makeQueryForValues(uri, property);
        ResultSet results = RhizomerRDF.instance().querySelect(queryString, true);
        Set<String> types = getTypes(results);
        assert(types.size() > 0);
        return RDF.lowestCommonType(types);
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
