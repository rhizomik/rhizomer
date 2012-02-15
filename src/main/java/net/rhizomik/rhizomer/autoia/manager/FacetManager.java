package net.rhizomik.rhizomer.autoia.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.hp.hpl.jena.query.QuerySolution;
import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.autoia.classes.FacetProperties;
import net.rhizomik.rhizomer.autoia.classes.FacetProperty;
import net.rhizomik.rhizomer.autoia.classes.FacetValue;

public class FacetManager
{

    class RevIntComp implements Comparator<Integer>
    {
	public int compare(Integer o1, Integer o2)
	{
	    return (o1>o2 ? -1 : (o1==o2 ? 0 : 1));
	}
    }	
		
    private String NL = System.getProperty("line.separator");
    protected Connection sqlconn = null;
    
    public FacetManager(String path) throws ClassNotFoundException, SQLException
    {
    	Class.forName("org.sqlite.JDBC");		
    	System.out.println(path);
	sqlconn = DriverManager.getConnection("jdbc:sqlite:"+path);
    }

    private static FacetProperty createPropertyfromResultSet(ResultSet rs) throws SQLException {
        return new FacetProperty(rs.getString("property"), rs.getInt("num_instances"), rs.getInt("different_values"), rs.getInt("max_cardinality"), rs.getString("value_range"), rs.getString("value_type"));
    }
    
    private static FacetProperty createInversePropertyFromResultSet(ResultSet rs) throws SQLException {
    	FacetProperty fp = new FacetProperty(rs.getString("property"), rs.getInt("num_instances"), rs.getInt("different_values"), rs.getInt("max_cardinality"), rs.getString("value_range"), rs.getString("value_type"));
    	fp.setInverse(rs.getString("class"));
    	return fp;
    }
    
    private ArrayList<String> getClasses() throws SQLException{
    	String query = "SELECT class from class_summary";
    	ArrayList<String> classes = new ArrayList<String>();
    	PreparedStatement ps = null;
    	boolean busy = false;
    	do {
        	try
        	{
        		busy = false;
    	    	ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    			ResultSet rs = ps.executeQuery();
    			while(rs.next()){
    				classes.add(rs.getString("class"));
    			}
        	}
        	catch (SQLException e)
        	{ if (e.toString().indexOf("SQLITE_BUSY")>0) busy = true;
        		else e.printStackTrace();}
        	finally
        	{ if (ps != null) ps.close(); }
		} while (busy);
    	return classes;
    }
    
    public FacetProperties getProperties(String uri, ArrayList<String> omitProperties) throws SQLException
    {
    	if(omitProperties==null)
    		omitProperties = new ArrayList<String>();
    	String query = "SELECT num_instances from class_summary where class=? ";
    	PreparedStatement ps = null;
    	FacetProperties properties = null;
    	boolean busy = false;
    	int limit = 15;
    	int i = 1;
    	do {
        	try
        	{
        		busy = false;
    	    	ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    			ps.setString(1, uri);
    			ResultSet rs = ps.executeQuery();
    			rs.next();
    			int numInstances = rs.getInt("num_instances");
    			rs.close();
    			ps.close();

    			query = "SELECT * FROM property_summary Where class=? and property not in (";
    	    	for(String omitProperty : omitProperties){
    	    		query+="'"+omitProperty+"',";
    	    	}
    	    	query = query.substring(0, query.length()-1);
    	    	//query +=") and num_instances > 0 and max_value > 1 "; //TODO: define criteria for facets with maxvalue = 1 but dataset with small number of instances
    	    	query += ") order by num_instances desc";
    	    	ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    			ps.setString(1, uri);
    			rs = ps.executeQuery();
    			properties = new FacetProperties(numInstances);
    			while(rs.next()){
    				FacetProperty property = createPropertyfromResultSet(rs);
    				if(!property.discardProperty() && i<=limit){
    					properties.addProperty(property);
    					i++;
    				}
    			}
    			rs.close();
    			ps.close();
    			
    			query = "SELECT * FROM property_summary where value_range=?";
    	    	ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    			ps.setString(1, uri);
    			rs = ps.executeQuery();
    			while(rs.next()){
    				FacetProperty property = createInversePropertyFromResultSet(rs);
    				System.out.println("INVERSE: "+property.getClassUri());
    				if(i<=limit){
    					properties.addProperty(property);
    					i++;
    				}
    			}
    			
        	}
        	catch (SQLException e)
        	{ if (e.toString().indexOf("SQLITE_BUSY")>0) busy = true;
        		else e.printStackTrace();}
        	finally
        	{ if (ps != null) ps.close(); }
		} while (busy);
    	return properties;
    }


    public FacetProperties getNumericProperties(String uri) throws SQLException {
        PreparedStatement ps = null;
        boolean busy = false;
        FacetProperties properties = new FacetProperties();
        String query = "SELECT * FROM property_summary" + " WHERE class=?" +
                       " AND  ( value_range IN (" + numericTypesString() + ")" +
                       "        OR " +
                       "        value_type  IN (" + numericTypesString() + "))";
        do {
            try {
                ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                ps.setString(1, uri);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    properties.addProperty(createPropertyfromResultSet(rs));
                }
            } catch (SQLException e) {
                if (e.toString().indexOf("SQLITE_BUSY") > 0) {
                    busy = true;
                } else {
                    e.printStackTrace();
                }
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        } while (busy);
        return properties;
    }

    private String numericTypesString() {
        String query = "";
        String[] numericTypes = {"http://www.w3.org/2001/XMLSchema#decimal",
                                 "http://www.w3.org/2001/XMLSchema#double",
                                 "http://www.w3.org/2001/XMLSchema#int",
                                 "http://www.w3.org/2001/XMLSchema#integer",
                                 "http://www.w3.org/2001/XMLSchema#float",
                                 "http://www.w3.org/2001/XMLSchema#long",
                                 "http://www.w3.org/2001/XMLSchema#short"};
        for(String type : numericTypes){
            query+="'"+type+"',";
        }
        query = query.substring(0, query.length()-1);
        return query;
    }


    public FacetProperties getInitialProperties(String uri, ArrayList<String> omitProperties) throws SQLException
    {
    	FacetProperties properties = new FacetProperties(0);;
    	PreparedStatement ps = null;
    	
    	if(omitProperties==null)
    		omitProperties = new ArrayList<String>();
    	String query = "SELECT num_instances from class_summary where class=? ";
    	System.out.println(uri);
    	boolean busy = false;
    	do {
        	try
        	{
        		busy = false;
    	    	ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    			ps.setString(1, uri);
    			ResultSet rs = ps.executeQuery();
    			rs.next();
    			int numInstances = rs.getInt("num_instances");
    			int minInstances = numInstances/3;
    			rs.close();
    			ps.close();
    			
    	    	query = "SELECT * FROM property_summary Where class=? and property not in (";
    	    	for(String omitProperty : omitProperties){
    	    		query+="'"+omitProperty+"',";
    	    	}
    	    	//query +=") and (num_instances > \"+minInstances+\")"; // and entropy < 0.99";
    	    	query += ") order by num_instances desc";
    			ps = sqlconn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    			ps.setString(1, uri);
    			rs = ps.executeQuery();
    			properties = new FacetProperties(numInstances);
    			while(rs.next()){
                    FacetProperty property = createPropertyfromResultSet(rs);
                    if(!property.discardProperty())
                    	properties.addProperty(property);
    			}
        	}
        	catch (SQLException e)
        	{ if (e.toString().indexOf("SQLITE_BUSY")>0) busy = true;}
        	finally
        	{ if (ps != null) ps.close(); }
		} while (busy);
    	
		return properties;
    } 
    
    public FacetProperties getProperties(String uri, Map<String, String> parameters, FacetProperties initialProperties){
    	for(String property : initialProperties.getProperties()){
    		TreeMultimap<Integer, FacetValue> map = getValuesForProperty(uri, property, initialProperties, parameters, 5, 0);
    		for(Entry<Integer, FacetValue> e : map.entries()){
    			FacetValue value = e.getValue();
    			initialProperties.addPropertyValue(property, value.getUri(), value.getLabel(), e.getKey());
    		}
    		int numValues = countValuesForProperty(uri, property,parameters,initialProperties);
    		initialProperties.getProperty(property).setNumValues(numValues);
    	}
    	return initialProperties;
    }
    
    public int countValuesForProperty(String classURI, String propertyURI, Map<String, String> parameters, FacetProperties properties){
    	String query = 
    	    "SELECT COUNT(DISTINCT(?o))"+NL+
            "WHERE {"+NL+
            "   ?x a <%1$s> ; <%2$s> ?o ."+NL+
            makeRestrictions(parameters,properties)+
            "}";
    	
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
    	Object[] vars = {classURI,propertyURI};
        f.format(query, vars);    	
    	
    	com.hp.hpl.jena.query.ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);	                
		QuerySolution row = results.next();
		return row.getLiteral("callret-0").getInt();
    }
    
    private String makeRestrictions(Map<String, String> parameters, FacetProperties properties)
    {
    	String query = "";
    	for(String property : parameters.keySet())
    	{
    	    String value = parameters.get(property);
    	    if(properties.getProperty(property).getRange()!=null && properties.getProperty(property).getRange().startsWith("http://www.w3.org/2001/XMLSchema#"))
    		query += "?x <"+property+"> \""+ value +"\"^^<"+properties.getProperty(property).getRange()+"> ."+NL;	
    	    else if((properties.getProperty(property).getRange()==null && value.startsWith("http://")))
    		query += "?x <"+property+"> <"+ value +"> ."+NL;
    	    else
    		query += "?x <"+property+"> \""+ value +"\" ."+NL;
    	}
    	return query;
    }
    
    public TreeMultimap<Integer, FacetValue> getValuesForProperty(String uri, String property, 
	    FacetProperties initialProperties, Map<String, String> parameters, int limit, int offset)
    {
    	String queryForValues = 
    	    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
            "SELECT ?o (COUNT(?o) AS ?n) ?label"+NL+
            "WHERE {"+NL+
            "   ?x a <%1$s> ."+NL+
            "   ?x <%2$s> ?o ."+NL+
            "   FILTER(?o!=\"\" && !isBlank(?o)) ."+NL+
    	    " OPTIONAL{ ?o rdfs:label ?label }"+NL;
    	    queryForValues+= makeRestrictions(parameters, initialProperties);
    	    queryForValues +=     		
            "}"+NL+
            "GROUP BY ?o ?label"+NL+
            "ORDER BY DESC(?n) LIMIT "+limit+" OFFSET "+offset+NL;
    	
	StringBuilder queryString = new StringBuilder();
	Formatter f = new Formatter(queryString);
	Object[] vars = {uri, property};
	f.format(queryForValues, vars);
	com.hp.hpl.jena.query.ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
	String strVar = results.getResultVars().get(0);
	String countVar = results.getResultVars().get(1);
	TreeMultimap<Integer, FacetValue> map = TreeMultimap.create(new RevIntComp(),Ordering.natural());
	while(results.hasNext())
	{
	    QuerySolution row = results.next();
	    if (!row.contains(strVar))
		continue;
	    String label = "";
	    if(row.contains("label"))
		label = row.get("label").toString();
	    map.put(row.getLiteral(countVar).getInt(), new FacetValue(row.get(strVar).toString(),label));   
    	}
    	return map;
    }
    
    public void getValuesForProperties(String uri, FacetProperties properties)
    {
	String queryForValues = 
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
		"SELECT ?o (COUNT(?o) AS ?n) ?label"+NL+
		"WHERE {"+NL+
		"   ?x a <%1$s> ."+NL+
		"   ?x <%2$s> ?o ."+NL+
		"   FILTER(?o!=\"\" && !isBlank(?o)) ."+NL+
		" OPTIONAL{ ?o rdfs:label ?label }"+NL+
    		"}"+NL+
    		"GROUP BY ?o ?label"+NL+
    		"ORDER BY DESC(?n) LIMIT 5"+NL;
		
	for(String property : properties.getProperties())
	{
	    StringBuilder queryString = new StringBuilder();
	    Formatter f = new Formatter(queryString);
	    Object[] vars = {uri, property};
	    f.format(queryForValues, vars);
	    com.hp.hpl.jena.query.ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), true);
	    String strVar = results.getResultVars().get(0);
	    String countVar = results.getResultVars().get(1);
	    while(results.hasNext())
	    {
		QuerySolution row = results.next();
	        if (!row.contains(strVar))
	            continue;
	        String label = "";
	        if(row.contains("label"))
	            label = row.get("label").toString();
	        properties.addPropertyValue(property, row.get(strVar).toString(), label, row.getLiteral(countVar).getInt());
	    }        
	}	
    }    
}
