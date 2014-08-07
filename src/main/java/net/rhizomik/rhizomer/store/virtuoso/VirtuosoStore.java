package net.rhizomik.rhizomer.store.virtuoso;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;
import net.rhizomik.rhizomer.store.MetadataStore;
import net.rhizomik.rhizomer.store.ResultSetCache;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


/**
 * Virtuoso implementation for the Rhizomer metadata store.
 * 
 * In order to support "CBD plus rdfs:labels" mode for DESCRIBE SPARQL queries, it is necessary to load and execute into
 * Virtuoso using the "isql" command line tool or the "conductor" web tool the file "src/main/resources/cbdlabels.sql"
 * 
 * Then, you should grant the two functions it defines to the "rhizomer" user:
 * $isql> grant execute on DB.DBA.SPARQL_DESC_DICT_CBDL_PHYSICAL to "rhizomer";
 * $isql> grant execute on DB.DBA.SPARQL_DESC_DICT_CBDL to "rhizomer";
 * 
 * Finally, enable inference by granting the rdfs_rule_set function to "rhizomer" 
 * $isql> grant execute on rdfs_rule_set to "rhizomer";
 * 
 * @author: http://rhizomik.net/~roberto
 */

public class VirtuosoStore implements MetadataStore
{
    private VirtGraph graph = null;
    private String graphURI = "";
    private String schema = "";
    private String ruleSet = "";
    private ResultSetCache cache = null;
    private static final Logger log = Logger.getLogger(VirtuosoStore.class.getName());
    private static int SPARQL_LIMIT = 15;

    /**
     * 
     */
    public VirtuosoStore()
    {
    	super();
    }
    
    public void init(String db_url, String db_user, String db_pass, String db_graph, String db_schema, int cacheSize)
            throws SQLException
    {
    	graphURI = db_graph;
		// If schema for reasoning explicitly stated in web.xml, otherwise build from db_graph
		if (db_schema!=null)
			schema = db_schema;
		else
			schema = graphURI+(graphURI.endsWith("/")?"":"/")+"schema/";
		ruleSet = schema+"rules/";
		graph = new VirtGraph (db_graph, db_url, db_user, db_pass);
		// Add or recalculate inference for the graph
		// NOTE: to grant the "rhizomer" Virtuoso user rights to execute the rdfs_rule_set, execute
		// the following command from Virtuoso iSQL: grant execute on rdfs_rule_set to "rhizomer"
		String sqlStatement = "DB.DBA.RDFS_RULE_SET('"+ruleSet+"', '"+schema+"')";
		graph.getConnection().prepareCall(sqlStatement).execute();
		//sqlStatement = "set result_timeout = 0";
		//graph.getConnection().prepareCall(sqlStatement).execute();

        if (cacheSize > 0)
            cache = new ResultSetCache(graphURI, cacheSize);
    }
    
    public void init(ServletConfig config) throws Exception
    {
    	if (config.getServletContext().getInitParameter("db_url")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_url");
    	else if (config.getServletContext().getInitParameter("db_user")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_user");
    	else if (config.getServletContext().getInitParameter("db_pass")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_pass");
    	else if (config.getServletContext().getInitParameter("db_graph")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_graph");
    	else
    	{
            int cacheSize = 0;
            if (config.getServletContext().getInitParameter("cache_size")!=null)
                cacheSize = Integer.parseInt(config.getServletContext().getInitParameter("cache_size"));

    		init(config.getServletContext().getInitParameter("db_url"), config.getServletContext().getInitParameter("db_user"), 
    				config.getServletContext().getInitParameter("db_pass"), config.getServletContext().getInitParameter("db_graph"), 
    				config.getServletContext().getInitParameter("db_schema"), cacheSize);
    	}
    }
    
    public void init(Properties props) throws Exception
    {
    	if (props.getProperty("db_url")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_url");
    	else if (props.getProperty("db_user")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_user");
    	else if (props.getProperty("db_pass")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_pass");
    	else if (props.getProperty("db_graph")==null)
    		throw new Exception("Missing parameter for VirtuosoStore init: db_graph");
    	else
    	{
            int cacheSize = 0;
            if (props.getProperty("cache_size")!=null)
                cacheSize = Integer.parseInt(props.getProperty("cache_size"));

    		init(props.getProperty("db_url"), props.getProperty("db_user"), 
    				props.getProperty("db_pass"), props.getProperty("db_graph"), 
    				props.getProperty("db_schema"), cacheSize);
    	}
    }
    
    protected void finalize() throws Throwable
    {
    	graph.close();
    }
    
    public String query(String queryString)
    {
    	return query(queryString, "application/rdf+xml");
    }
    
    
    public String queryJSON(String queryString)
    {
    	return query(queryString, "application/json");
    }
    
    /** Perform input query and return output as RDF/XML or JSON (warning, just for SELECT queries)
     * @return java.lang.String
     * @param queryString java.lang.String
     * @param format java.lang.String
     */
    public String query(String queryString, String format)
    {
        String response = "";

		VirtuosoQueryExecution qexec = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {

            Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
            query.addGraphURI(graphURI);
            query.addGraphURI(schema);
            //if (!query.hasLimit())
            //	query.setLimit(SPARQL_LIMIT);

            queryString = query.toString();
            if (query.hasGroupBy())
                if (query.getGroupBy().isEmpty())
                    queryString = query.toString().substring(0, query.toString().indexOf("GROUP BY"));

            queryString = "DEFINE input:inference \""+ruleSet+"\"\n"+queryString;
            if (query.isDescribeType())
            	queryString = "DEFINE sql:describe-mode \"CBDL\"\n"+queryString;

            // TODO: Replace contains with bif:contains to profit from Virtuoso free text indexing
            /* if (queryString.indexOf("contains(")>0)
            {
            	queryString = queryString.replaceAll("contains\\(","bif:contains(");
            } */
            
            log.log(Level.INFO, "VirtuosoStore.query2: "+queryString);
            
            qexec = VirtuosoQueryExecutionFactory.create(queryString, graph);
        
	        if (query.isSelectType())
	        {
	        	ResultSet results = qexec.execSelect();
	        	if (format.equals("application/json"))
	        		ResultSetFormatter.outputAsJSON(out, results);
	        	else
	        		ResultSetFormatter.outputAsRDF(out, "RDF/XML-ABBREV", results);
	        }
	        else if (query.isConstructType())
	        {
	        	Model results = qexec.execConstruct();
		        results.write(out, "RDF/XML-ABBREV");
	        }
	        else if (query.isDescribeType())
	        {
	        	Model results = qexec.execDescribe();
		        results.write(out, "RDF/XML-ABBREV");
	        }
        	out.flush();
        	response = out.toString("UTF8");
        }
        catch (Exception e)
        {
        	log.log(Level.SEVERE, "Exception in VirtuosoStore.query for: "+queryString, e);
        	response = e.getMessage();
        }
        finally 
        { 
        	if (qexec != null) qexec.close();
        }

        return response;
    }
    
    /** Perform input SPARQL SELECT query and return result as ResultSet
     * The scopes, defined in MetadataStore, are:
     * - INSTANCES (if just to query instance data)
     * - SCHEMAS (if just to query schemas and ontologies)
     * - REASONING (instance plus schemas plus the reasoning provided by the store)
     */
	public ResultSet querySelect(String queryString, int scope)
	{
        ResultSet results = new ResultSetMem();

        if (cache!=null && cache.isCached(queryString))
            return cache.getCached(queryString);

        VirtuosoQueryExecution qexec = null;
        Query query = null;

        try {
            query = QueryFactory.create(queryString, Syntax.syntaxARQ);

            if (scope == MetadataStore.INSTANCES || scope == MetadataStore.REASONING)
                query.addGraphURI(graphURI);
            if (scope == MetadataStore.SCHEMAS || scope == MetadataStore.REASONING)
                query.addGraphURI(schema);

            queryString = query.toString();

            if (scope == MetadataStore.REASONING)
                queryString = "DEFINE input:inference \""+ruleSet+"\"\n" + queryString;

            if (query.hasGroupBy())
                if (query.getGroupBy().isEmpty())
                    queryString = query.toString().substring(0, query.toString().indexOf("GROUP BY"));

            //TODO: Replace contains with bif:contains to profit from Virtuoso free text indexing
            /* if (queryString.indexOf("contains(")>0)
            {
                queryString = queryString.replaceAll("contains\\(","bif:contains(");
            }*/

            log.log(Level.INFO, queryString);

            qexec = VirtuosoQueryExecutionFactory.create(queryString, graph);

            if (query.isSelectType())
                results = qexec.execSelect();
        } catch (Exception e) {
            log.log(Level.SEVERE, e.toString());
        }

        if (cache!=null)
            cache.put(queryString, results);

        return results;
    }
	
    /** Perform input SPARQL ASK query and return result as boolean
     * @return boolean
     * @param queryString java.lang.String 
     */
	public boolean queryAsk(String queryString)
	{
        boolean result = false;
        VirtuosoQueryExecution qexec = null;
        Query query = null;

        try {
            query = QueryFactory.create(queryString, Syntax.syntaxARQ);
        } catch (QueryParseException e) {
            log.log(Level.SEVERE, e.toString());
            return result;
        }

        query.addGraphURI(graphURI);
        query.addGraphURI(schema);
        queryString = "DEFINE input:inference \""+ruleSet+"\"\n"+query.toString();
        log.log(Level.INFO, "VirtuosoStore.query: "+query.toString());

        qexec = VirtuosoQueryExecutionFactory.create(query.toString(), graph);

        if (query.isAskType())
            result = qexec.execAsk();

        return result;
	}
    
    /**
     * Store the input metadata.
     * TODO: If it is a class, property,... store into the schema graph instead of in the instance one
     * @return java.lang.String
     * @param metadata java.io.InputStream
     */
    public String store(InputStream metadata, String contentType)
    {
    	String response = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
    	String format = "RDF/XML"; //Default
    	
    	if (contentType.contains("application/n-triples"))
    		format = "N-TRIPLE";
    	else if (contentType.contains("application/n3"))
    		format = "N3";
    	
    	try
		{
    		Model temp = ModelFactory.createDefaultModel();
    		temp.read(metadata, "", format);
    		graph.getTransactionHandler().begin();
    		graph.getBulkUpdateHandler().add(temp.getGraph());
    		graph.getTransactionHandler().commit();

			temp.write(out, "RDF/XML-ABBREV");
			out.close();
			response = out.toString("UTF8");
		}
		catch (Exception e) 
		{
			log.log(Level.SEVERE, "Exception in JenaStore.store", e);
			response = e.toString();
		}
		
    	return response;
    }
    /**
     * Store the metadata at URL.
     * @return java.lang.String
     * @param metadataURL java.net.URL
     */
    public String store(URL metadataURL)
    {
    	String response = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
		try
		{
	    	Model temp = ModelFactory.createDefaultModel();
	    	temp.read(metadataURL.toString());
    		graph.getTransactionHandler().begin();
    		graph.getBulkUpdateHandler().add(temp.getGraph());
    		graph.getTransactionHandler().commit();

			temp.write(out, "RDF/XML-ABBREV");
			out.close();
			response = out.toString("UTF8");
		}
		catch (Exception e) 
		{
			log.log(Level.SEVERE, "Exception in JenaStore.store for: "+metadataURL, e);
			response = e.toString(); 
		}
		
    	return response;
    }
	/**
     * Remove all available metadata for the input URI, 
     * i.e. the Concise Bounded Description for the URI resource
     */
    public void remove(java.net.URI uri)
    {
    	Query query = QueryFactory.create("DESCRIBE <"+uri+">");
        query.addGraphURI(graphURI);
        String queryString = "DEFINE sql:describe-mode \"CBD\"\n"+query.toString();
    	VirtuosoQueryExecution qexec = VirtuosoQueryExecutionFactory.create(queryString, graph);
    	Model remove = qexec.execDescribe();

    	graph.getTransactionHandler().begin();
    	graph.getBulkUpdateHandler().delete(remove.getGraph());
   		graph.getTransactionHandler().commit();
    }
	/**
     * Remove the input metadata from the store.
     * TODO: Remove triples also from schema graph, check if it is class, property,...?
     * @param metadata java.io.InputStream
     */
    public void remove(InputStream metadata, String contentType)
    {
    	String metadataFormat = "RDF/XML"; //Default
    	
    	if (contentType.equalsIgnoreCase("application/n-triples"))
    		metadataFormat = "N-TRIPLE";
    	else if (contentType.equalsIgnoreCase("application/n3"))
    		metadataFormat = "N3";
        
        Model remove = ModelFactory.createDefaultModel();
        remove.read(metadata, "", metadataFormat);
        
    	graph.getTransactionHandler().begin();
    	graph.getBulkUpdateHandler().delete(remove.getGraph());
   		graph.getTransactionHandler().commit();
    }
    
	public void close() 
	{
		graph.close();
	}
}
