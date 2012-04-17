package net.rhizomik.rhizomer.store.jena;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;

import net.rhizomik.rhizomer.store.MetadataStore;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerRegistry;


/**
 * Jena implementation for the Rhizomer metadata store.
 * 
 * @author: http://rhizomik.net/~roberto
 */
public class JenaStore implements MetadataStore
{
    private Model model = null;
    private static final boolean includeLabels = true;
    private static final Logger log = Logger.getLogger(JenaStore.class.getName());
    private static int SPARQL_LIMIT = 15;

    /**
     * 
     */
    public JenaStore()
    {
    	super();
    	DescribeHandlerRegistry.get().clear();
    	DescribeHandlerRegistry.get().add(new RhizomerDescribeHandlerFactory(includeLabels));
    }
    
    public void init(ServletConfig config) throws Exception
    {
    	if (config.getServletContext().getInitParameter("file_name")!=null && config.getServletContext().getInitParameter("dir_name")!=null)
    	{
    		String basePath =config.getServletContext().getRealPath(config.getServletContext().getInitParameter("dir_name"));
            String fileName = config.getServletContext().getInitParameter("file_name");
            ModelMaker maker = ModelFactory.createFileModelMaker(basePath);
            Model baseModel = maker.openModel(fileName, true);
            model = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), baseModel);
    	}
    	else if (config.getServletContext().getInitParameter("db_url")==null)
    		throw new Exception("Missing parameter for JenaStore init: db_url if database model or file and ir name if file model");
    	else if (config.getServletContext().getInitParameter("db_user")==null)
    		throw new Exception("Missing parameter for JenaStore init: db_user");
    	else if (config.getServletContext().getInitParameter("db_pass")==null)
    		throw new Exception("Missing parameter for JenaStore init: db_pass");
    	else if (config.getServletContext().getInitParameter("db_type")==null)
    		throw new Exception("Missing parameter for JenaStore init: db_type");
    	else if (config.getServletContext().getInitParameter("db_driver")==null)
    		throw new Exception("Missing parameter for JenaStore init: db_driver");
    	else if (config.getServletContext().getInitParameter("db_model")==null)
    		throw new Exception("Missing parameter for JenaStore init: db_model");
    	else
    	{
    		//TODO: additional parameters for kind of reasoning,...
    		Class.forName(config.getServletContext().getInitParameter("db_driver"));
        	IDBConnection conn = new DBConnection(config.getServletContext().getInitParameter("db_url"),
        			config.getServletContext().getInitParameter("db_user"), config.getServletContext().getInitParameter("db_pass"),
        			config.getServletContext().getInitParameter("db_type"));
        	ModelMaker maker = ModelFactory.createModelRDBMaker( conn );
            model = maker.createModel(config.getServletContext().getInitParameter("db_model"), false );
    	}
    }
    
    public void init(Properties props) throws Exception
    {
    	//TODO: missing implementation...
    }
    
    protected void finalize() throws Throwable
    {
    	model.close();
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
     */
    public String query(String queryString, String format)
    {
        String response = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
    	Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
    	//if (!query.hasLimit())
        //	query.setLimit(SPARQL_LIMIT);
        log.log(Level.INFO, "JenaStore.query: "+query);

        model.enterCriticalSection(Lock.READ);
        try
        {
        	QueryExecution qexec = QueryExecutionFactory.create(query, model);
        	try
        	{
		        if (query.isSelectType())
		        {
		        	ResultSet results = qexec.execSelect(); 
		        	if (format.equals("application/json"))
		        		ResultSetFormatter.outputAsJSON(out, results);
		        	else
		        		ResultSetFormatter.outputAsRDF(out, "RDF/XML-ABBREV", results);
		        }
		        if (query.isConstructType())
		        {
		        	Model results = qexec.execConstruct();
			        results.write(out, "RDF/XML-ABBREV");
		        }
		        if (query.isDescribeType())
		        {
		        	Model results = qexec.execDescribe();
			        results.write(out, "RDF/XML-ABBREV");
		        }
	        	out.flush();
	        	response = out.toString("UTF8");
        	}
        	finally { qexec.close(); }
        }
        catch (Exception e)
        {
        	log.log(Level.SEVERE, "Exception in JenaStore.query for: "+queryString, e);
        	response = e.getMessage();
        }
        finally { model.leaveCriticalSection(); }

        return response;
    }
    
    /** Perform input SPARQL SELECT query and return result as ResultSet.
     * TODO: implement the possibility to constraint the scope of the query.
     * The scopes, defined in MetadataStore, are:
     * - INSTANCES (if just to query instance data)
     * - SCHEMAS (if just to query schemas and ontologies)
     * - REASONING (instance plus schemas plus the reasoning provided by the store)
     */
	public ResultSet querySelect(String queryString, int scope)
	{
        ResultSet results = null;

    	Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
        log.log(Level.INFO, "JenaStore.query: "+query);

        model.enterCriticalSection(Lock.READ);
        try
        {
        	QueryExecution qexec = QueryExecutionFactory.create(query, model);
	        if (query.isSelectType())
	        	results = qexec.execSelect();
        }
        finally { model.leaveCriticalSection(); }

        return results;
	}
	
    /** Perform input SPARQL ASK query and return result as boolean
     * @return com.hp.hpl.jena.query.ResultSet
     * @param queryString java.lang.String 
     */
	public boolean queryAsk(String queryString)
	{
        boolean result = false;

    	Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
        log.log(Level.INFO, "JenaStore.query: "+query);

        model.enterCriticalSection(Lock.READ);
        try
        {
        	QueryExecution qexec = QueryExecutionFactory.create(query, model);
	        if (query.isAskType())
	        	result = qexec.execAsk();
        }
        finally { model.leaveCriticalSection(); }

        return result;
	}
    
    /**
     * Store the input metadata.
     * @return java.lang.String
     * @param metadata java.io.InputStream
     */
    public String store(InputStream metadata, String contentType)
    {
    	String response = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
    	String format = "RDF/XML"; //Default
    	
    	if (contentType.indexOf("application/n-triples")>=0)
    		format = "N-TRIPLE";
    	else if (contentType.indexOf("application/n3")>=0)
    		format = "N3";
    	
    	try
		{
    		Model temp = ModelFactory.createDefaultModel();
    		temp.read(metadata, "", format);
    		model.enterCriticalSection(Lock.WRITE);
    		model.add(temp);

			temp.write(out, "RDF/XML-ABBREV");
			out.close();
			response = out.toString("UTF8");
		}
		catch (Exception e) 
		{
			log.log(Level.SEVERE, "Exception in JenaStore.store", e);
			response = e.toString();
		}
		finally { model.leaveCriticalSection(); }
		
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
	    	model.enterCriticalSection(Lock.WRITE);
			model.add(temp);

			temp.write(out, "RDF/XML-ABBREV");
			out.close();
			response = out.toString("UTF8");
		}
		catch (Exception e) 
		{
			log.log(Level.SEVERE, "Exception in JenaStore.store for: "+metadataURL, e);
			response = e.toString(); 
		}
		finally { model.leaveCriticalSection(); }
		
    	return response;
    }
	/**
     * Remove all available metadata for the input URI, 
     * i.e. the Concise Bounded Description for the URI resource
     */
    public void remove(java.net.URI uri)
    {
    	try
    	{
    		log.log(Level.INFO, "JenaStore.remove: URI "+uri);
	    	model.enterCriticalSection(Lock.READ);
	    	Resource r = model.getResource(uri.toString());
	    	RhizomerDescribeHandler describer = new RhizomerDescribeHandler(false);
	    	Model remove = ModelFactory.createDefaultModel();
	    	describer.start(remove, null);
	    	describer.describe(r);
	    	model.leaveCriticalSection();
	    	model.enterCriticalSection(Lock.WRITE);
	    	model.remove(describer.acc);
    	}
    	finally { model.leaveCriticalSection(); }
    }
	/**
     * Remove the input metadata from the store.
     * TODO: Are literals also removed?
     * @return java.lang.String
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
    	try
    	{
	    	remove.read(metadata, "", metadataFormat);
	        model.enterCriticalSection(Lock.WRITE);
        	model.remove(remove);
        }
        finally { model.leaveCriticalSection(); }
    }
    
	public void close() 
	{
		model.close();
	}
}
