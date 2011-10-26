package net.rhizomik.rhizomer.autoia.generator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;

import net.rhizomik.rhizomer.agents.RhizomerRDF;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class TypeDetector {
	
	private static TypeHierarchy th = new TypeHierarchy();
	
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
	
	public static String detectType(String uri, String property, int instances){	
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri, property};
        f.format(queryForValues, vars);
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);    	
        HashMap<String, Integer> types = new HashMap<String, Integer>();
        String objVar = results.getResultVars().get(0);
        String type = "";
        while(results.hasNext()){
			QuerySolution row = results.next();
			if(row.get(objVar)==null)
				continue;
			if(row.get(objVar).isResource())
				type = "resource";
			else{
				type = row.get(objVar).asNode().getLiteralDatatypeURI();
				if(type == null){
					String value = row.get(objVar).toString();	
					type = checkTypes(value);
				}
			}	
			if(types.containsKey(type))
				types.put(type, types.get(type)+1);
			else
				types.put(type, 1);
		}
        return getCommonDataType(types);    
    }
    
    
    private static String getCommonDataType(HashMap<String, Integer> types){
    	String commonType = "";
    	 for(String type : types.keySet()){
    		 if(commonType.equals(""))
    			 commonType = type;
    		 else
    			 commonType = th.getNearestParent(commonType, type);
    	 }
		return commonType;
    }
        
    
    private static String checkTypes(String value){
    	try{
    		Integer.parseInt(value);
    		return "integer";
    	}
    	catch(Exception e){}
    	try{
    		Double.parseDouble(value);
    		return "double"; // Double, float or decimal
    	}
    	catch(Exception e){}
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			formatter.parse(value);
			return "date";
		} catch (ParseException e) {}    	
    	
    	/* Parse Geo points */
    	return "string";
    }

}
