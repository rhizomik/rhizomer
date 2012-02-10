package net.rhizomik.rhizomer.autoia.generator;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import static net.rhizomik.rhizomer.util.Namespaces.rdfs;
import net.rhizomik.rhizomer.agents.RhizomerRDF;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class FacetGenerator {
	
    private String NL = System.getProperty("line.separator");
    private static final Logger log = Logger.getLogger(FacetGenerator.class.getName());
    private Connection conn;    
    
    private String queryForClasses = 
	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT DISTINCT ?c"+NL+
        "WHERE {"+NL+
        "   ?c rdf:type ?class. FILTER (!isBlank(?c) && (?class=owl:Class || ?class=rdfs:Class) )"+NL+
        "}";
    
    private String queryForCountInstancesProperty =
	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT (COUNT(?x) AS ?n)"+NL+
        "WHERE {"+NL+
        "   ?x a <%1$s> ; <%2$s> ?o"+NL+
        "}";
    
    private String queryForCountTotalInstances =
	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT (COUNT(?x) AS ?n)"+NL+
        "WHERE {"+NL+
        "   ?x a <%1$s>"+NL+
        "}";
    
    private String queryForProperties = 
	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT DISTINCT ?p ?r"+NL+
        "WHERE {"+NL+
        "   ?x a <%1$s> ; ?p ?o"+NL+
        "   OPTIONAL { ?p rdfs:range ?r }"+NL+
        "   FILTER (?o != \"\")"+NL+
        "   FILTER (?p!=owl:differentFrom && ?p!=owl:sameAs)"+NL+            
        "}";
    
    private String queryForEntropy = 
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT (COUNT(?o) AS ?n)"+NL+
        "WHERE {"+NL+
        "   ?x a <%1$s> ; <%2$s> ?o"+NL+
        "}";
    
	private String queryForCountValues =
	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT (COUNT(distinct(?o)) AS ?n)"+NL+ 
        "WHERE {"+NL+
        "   ?x a <%1$s> ; <%2$s> ?o ."+NL+
        "   FILTER (?o!=\"\")"+NL+   
        "}";
	
	private String queryIsNotInverseFunctional =
	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "ASK"+NL+ 
        "WHERE {"+NL+
        "   ?x a <%1$s> ; <%2$s> ?o ."+NL+
        "   ?y a <%1$s> ; <%2$s> ?o ."+NL+
        "   FILTER (?x!=?y)"+NL+   
        "}";    
	
	private String queryForMaxCardinality = 
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
	    "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
	    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
	    "SELECT (COUNT(?o) AS ?n)"+NL+ 
	    "WHERE {"+NL+
	    "   ?x a <%1$s> ; <%2$s> ?o ."+NL+
	    "   FILTER (?o!=\"\")"+NL+   
	    "}"+NL+
	    "GROUP BY ?o ORDER BY DESC(?n) LIMIT 1";
    
    public FacetGenerator(ServletConfig config) throws ClassNotFoundException, SQLException{
    	
    	String path = config.getServletContext().getRealPath("WEB-INF");
		String datasetId = "";
		if (config.getServletContext().getInitParameter("db_graph")!=null)
			datasetId = config.getServletContext().getInitParameter("db_graph");
		else if (config.getServletContext().getInitParameter("db_url")!=null)
			datasetId = config.getServletContext().getInitParameter("db_url");
		else if (config.getServletContext().getInitParameter("file_name")!=null)
			datasetId = config.getServletContext().getInitParameter("file_name");
		int facetHash = datasetId.hashCode();
		String filePath = path+="/facets-"+facetHash+".db";
		
		if(!fileExists(filePath)){
	        Class.forName("org.sqlite.JDBC"); // TODO: get db class from web.xml
	    	conn = DriverManager.getConnection("jdbc:sqlite:"+filePath);
	    	System.out.println("File \""+filePath+"\" created, generating facets");
	    	Statement stat = conn.createStatement(); 
	        stat.execute("CREATE TABLE if not exists class_summary(class varchar(255), num_instances int, " +
	        		"primary key(class))");
	        /*stat.execute("CREATE TABLE if not exists property_summary(id int auto_increment, " +
	        		"class varchar(255), property varchar(255), num_instances int, different_values int, " +
	        		"entropy float, value_range varchar(255), value_type varchar(255), primary key(id))");
	        */
	        stat.execute("CREATE TABLE if not exists property_summary(id int auto_increment, " +
	        		"class varchar(255), property varchar(255), num_instances int, different_values int, " +
	        		"max_value int, max_cardinality int, value_range varchar(255), value_type varchar(255), primary key(id))");
	        stat.close(); 
	        generateFacets();
		}
		else
			System.out.println("File \""+filePath+"\" already exists");
    }
    
    private void generateFacets() throws SQLException{
    	ArrayList<String> classes = getClasses();
    	ArrayList<String> omitClasses = new ArrayList<String>();
        omitClasses.add("http://www.w3.org/2002/07/owl#Thing");
        omitClasses.add("http://www.w3.org/2002/07/owl#Nothing");
    	for(String classUri : classes){
    		if(!omitClasses.contains(classUri)){
    			System.out.println("Generating facets for "+classUri);
    			generateFacetsForClass(classUri);
    		}
    		else
    			System.out.println("Omiting facets for "+classUri);
    	}
    }
    
    private boolean fileExists(String filename){
    	File f = new File(filename);
		return f.exists();
    }
    
    
    private ArrayList<String >getClasses(){
    	ArrayList<String> classes = new ArrayList<String>();
        ResultSet results = RhizomerRDF.instance().querySelect(queryForClasses, true);
        while(results.hasNext()){
			   QuerySolution row = results.next();
			   classes.add(row.get("c").toString());
		}
        return classes;
    }
    
    private int countInstancesForProperty(String uri, String property){
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri, property};
        f.format(queryForCountInstancesProperty, vars);    	
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
        int count = 0;
        if (results.hasNext())
        {
			QuerySolution row = results.next();
			// The first var is the count value
	        String countVar = results.getResultVars().get(0);
	        count = row.getLiteral(countVar).getInt();
        }
        return count;
    }
    
    private int countMaxCardinalityForProperty(String uri, String property){
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri, property};
        f.format(queryForMaxCardinality, vars);    	
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
        int count = 0;
        if (results.hasNext())
        {
			QuerySolution row = results.next();
			// The first var is the count value
	        String countVar = results.getResultVars().get(0);
	        count = row.getLiteral(countVar).getInt();
        }
        return count;
    }    
    
    private boolean isInverseFunctionalForValues(String uri, String property){
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri, property};
        f.format(queryIsNotInverseFunctional, vars);    	
        return !RhizomerRDF.instance().queryAsk(queryString.toString());
    }
    
    private int countTotalInstances(String uri){
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri};
        f.format(queryForCountTotalInstances, vars);
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
        int count = 0;
        if (results!=null && results.hasNext())
		{
        	QuerySolution row = results.next();
        	// The first var is the count value
        	String countVar = results.getResultVars().get(0);
        	count = row.getLiteral(countVar).getInt();
		}
        return count;
    }
    
    private HashMap<String,String> getProperties(String uri){
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri};
        f.format(queryForProperties, vars);    
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
        HashMap<String, String> properties = new HashMap<String, String>();
        while(results!=null && results.hasNext()){
			QuerySolution row = results.next();
			String property = row.get("p").toString();
			String range;
			if(row.get("r")!=null)
				range = row.get("r").toString();
			else
				range = null;			
			properties.put(property, range);
        }
        return properties;
    }    
    
    private double calculateEntropy(String uri, String property) throws SQLException{    	
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri, property};
        f.format(queryForEntropy, vars);
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
        int total = 0;
        ArrayList<Integer> values = new ArrayList<Integer>();
        int numRows = 0;
        while(results.hasNext()){
			QuerySolution row = results.next();        
			// The first var is the count value
	        String countVar = results.getResultVars().get(0);			
			int count = row.getLiteral(countVar).getInt();
			total += count;
			numRows++;
			values.add(count);
        }
        double entropy = 0;
        for(int i : values){
        	double prob = (double)i/(double)total;
        	double log = prob*log(prob,numRows);
        	entropy += log;
        }
        entropy*=-1;
		if(Double.isNaN(entropy))
			entropy = 0;
		return entropy;
    }
    
    private int countValues(String uri, String property){
    	int count = 0;
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri, property};
        f.format(queryForCountValues, vars);
        try
        {
	        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
			QuerySolution row = results.next();
			// The first var is the count value
	        String countVar = results.getResultVars().get(0);			
			count = row.getLiteral(countVar).getInt();
        }
        catch (Exception e)
        {
        	log.log(Level.SEVERE, "Exception in SPARQL query: "+queryString.toString());
        }
		return count;
    }        
    
    private double log(double number, double base){
    	return Math.log(number)/Math.log(base);
    }    
    
    public void generateFacetsForClass(String classUri) throws SQLException{
		int total = this.countTotalInstances(classUri);
		
		PreparedStatement st = conn.prepareStatement("INSERT INTO class_summary VALUES(?,?)");
		st.setString(1, classUri);
		st.setInt(2, total);
		st.executeUpdate();
		
		st = conn.prepareStatement("INSERT INTO property_summary VALUES(NULL,?,?,?,?,?,?,?,?)");
		//PreparedStatement st = conn.prepareStatement("UPDATE property_summary SET num_instances = ? where class = ? and property = ?");
		
		HashMap<String, String>  properties = this.getProperties(classUri);
    	for(String property : properties.keySet()){
    		
    		String range = properties.get(property);
    		//double entropy = calculateEntropy(classUri, property);
    		int instances = 0;
    		int values = 0;
    		int maxValue = 2;
    		int maxCardinality = 0;
    		
	    	System.out.println("COUNT INSTANCES: "+ classUri + " - "+ property);
	    	instances = this.countInstancesForProperty(classUri, property); 
	    	System.out.println("COUNT VALUES: "+ classUri + " - "+ property);
	    	values = this.countValues(classUri, property);
	    	
	    	System.out.println("COUNT MAX CARDINALITY: "+ classUri + " - "+ property);
	    	maxCardinality = this.countMaxCardinalityForProperty(classUri, property);
	    	
	    	if(isInverseFunctionalForValues(classUri, property))
	    	{
    			System.out.println("Inverse functional property for "+ property +" for class "+ classUri);
    			maxValue = 1;
    		}
    			
    		st.setString(1, classUri);
    		st.setString(2, property);
    		st.setInt(3, instances);
    		st.setInt(4, values);
    		//st.setDouble(5, entropy);
    		st.setInt(5, maxValue); //TODO: make it a boolean
    		st.setInt(6, maxCardinality); //TODO: make it a boolean
    		String type = null;
    		if(range==null){
    			type = new TypeDetector(classUri, property).detectType();
    			if(type.equals(rdfs("Resource")))
    				range = new TypeDetector(classUri, property).detectRange();
    		}
    		st.setString(7, range);    		
    		st.setString(8, type);
    		
    		st.executeUpdate();

    	}
    	st.close();
    }


}
