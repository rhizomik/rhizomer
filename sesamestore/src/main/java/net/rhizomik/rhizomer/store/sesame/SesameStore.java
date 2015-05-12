package net.rhizomik.rhizomer.store.sesame;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import net.rhizomik.rhizomer.store.MetadataStore;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.*;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.sail.memory.MemoryStore;

import javax.servlet.ServletConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OWLIM implementation of the Rhizomer metadata store.
 * 
 * @author: http://rhizomik.net/~roberto
 * 
 **/

public class SesameStore implements MetadataStore 
{
    protected String graphURI = "";
    protected String schemaURI = "";
    protected Repository repository;
    protected RepositoryManager repositoryManager;
    protected RepositoryConnection repositoryConnection;

    private static final Logger log = Logger.getLogger(SesameStore.class.getName());

    public SesameStore() 
    {
	super();
    }

    public void init(String manager_url, String repository_id, String db_graph, String db_schema) throws Exception 
    {
        graphURI = db_graph;
        // If schema for reasoning explicitly stated in web.xml, otherwise build from db_graph
        if (db_schema!=null)
            schemaURI = db_schema;
        else
            schemaURI = graphURI+(graphURI.endsWith("/")?"":"/")+"schema/";

        repositoryManager = new RemoteRepositoryManager(manager_url);
        repositoryManager.initialize();
        repository = repositoryManager.getRepository(repository_id);
        repositoryConnection = repository.getConnection();
        repositoryConnection.setAutoCommit(false);
    }

    public void init(ServletConfig config) throws Exception 
    {
        if (config.getServletContext().getInitParameter("manager_url") == null)
            throw new Exception("Missing parameter for SesameStore init: manager_url");
        else if (config.getServletContext().getInitParameter("repository_id") == null)
            throw new Exception("Missing parameter for SesameStore init: repository_id");
        else if (config.getServletContext().getInitParameter("db_graph")==null)
            throw new Exception("Missing parameter for SesameStore init: db_graph");
        else
        {
            init(config.getServletContext().getInitParameter("manager_url"),
             config.getServletContext().getInitParameter("repository_id"),
             config.getServletContext().getInitParameter("db_graph"),
             config.getServletContext().getInitParameter("db_schema"));
        }
    }

    public void init(Properties props) throws Exception
    {
        if (props.getProperty("manager_url") == null)
            throw new Exception("Missing parameter for SesameStore init: manager_url");
        else if (props.getProperty("repository_id") == null)
            throw new Exception("Missing parameter for SesameStore init: repository_id");
        else if (props.getProperty("db_graph")==null)
            throw new Exception("Missing parameter for SesameStore init: db_graph");
        else
        {
            init(props.getProperty("manager_url"),
                    props.getProperty("repository_id"),
                    props.getProperty("db_graph"),
                    props.getProperty("db_schema"));
        }
    }

    protected void finalize() throws Throwable {
	this.close();
    }

    public String query(String queryString) {
	return query(queryString, "application/rdf+xml");
    }

    public String queryJSON(String queryString) {
	return query(queryString, "application/json");
    }

    /**
     * Perform input query and return output as RDF/XML or JSON (warning, just
     * for SELECT queries)
     * 
     * @return java.lang.String
     * @param queryString
     *            java.lang.String
     * @param format
     *            java.lang.String
     */
    public String query(String queryString, String format) 
    {
	String response = "";
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	// Filter DESCRIBE query to implement CBD: make them SELECT, get selected resources and build CBD for them
	boolean isDescribe = false;
	boolean withoutWhere = false;
	String[] uris = null;
	if (queryString.indexOf("DESCRIBE")>=0 && queryString.indexOf("SELECT")>0)
	{
	    queryString = queryString.substring(queryString.indexOf("SELECT"), queryString.lastIndexOf('}'));
	    queryString = queryString.substring(0, queryString.lastIndexOf('}'));
//	    int wherePos = queryString.indexOf("WHERE");
//    	    if (wherePos>0)
//    	        queryString = queryString.substring(0, wherePos) +
//    		    "FROM <"+graphURI+">\n"+ "FROM <http://www.ontotext.com/implicit>\n"+ queryString.substring(wherePos);
	    isDescribe = true;
	}
	else
	{
        	if (queryString.indexOf("DESCRIBE")>=0) 
        	{
        	    queryString = queryString.replace("DESCRIBE", "SELECT");
        	    isDescribe = true;
        	}
        	else if (queryString.indexOf("describe")>=0)
        	{
        	    queryString = queryString.replace("describe", "select");
        	    isDescribe = true;
        	}
        	if (isDescribe && queryString.indexOf("WHERE")<0 && queryString.indexOf("where")<0)
        	{
        	    queryString = queryString.substring(queryString.indexOf('<'));
        	    queryString = queryString.replaceAll("<", "");
        	    uris = queryString.split(">");
        	    for(int i=0; i<uris.length; i++)
        	    {
        		uris[i] = uris[i].trim();
        	    }
        	    withoutWhere = true;
        	}
	}
	
	try 
	{
	    if (isDescribe && withoutWhere)
		buildCBDs(uris, out);
	    else
	    {
		log.log(Level.INFO, "SesameStore.query: " + queryString);
		Query query = prepareQuery(queryString);
		if (query == null) {
		    log.log(Level.WARNING, "Unable to parse query: " + queryString);
		    return response;
		}
		

		if (query instanceof TupleQuery) 
		{
		    if (isDescribe)
		    {
			TupleQueryResult result = ((TupleQuery) query).evaluate();
			buildCBDs(result, out);
		    }
		    else if (format.equals("application/json")) 
			((TupleQuery) query).evaluate(new SPARQLResultsJSONWriter(out));
		    else 
		    {
			// No way to generate RDFXML rendering of tuple query results with Sesame, transform to 
			// Jena ResultSet and from there get RDFXML
			ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
			((TupleQuery)query).evaluate(new SPARQLResultsXMLWriter(tmpOut));
			tmpOut.flush();
			ResultSet results = ResultSetFactory.fromXML(new ByteArrayInputStream(tmpOut.toByteArray()));
			ResultSetFormatter.outputAsRDF(out, "RDF/XML-ABBREV", results);
		    }
		} 
		else if (query instanceof GraphQuery) 
		{
		    ((GraphQuery) query).evaluate(new RDFXMLPrettyWriter(out));
		}
	    }
	    out.flush();
	    response = out.toString("UTF8");
	}
	catch (Exception e) 
	{
	    log.log(Level.SEVERE, "Exception in SesameStore.query for: " + queryString, e);
	    response = e.getMessage();
	} 
	finally {}

	return response;
    }
    
    private void buildCBDs(TupleQueryResult result, ByteArrayOutputStream out) 
	    throws QueryEvaluationException, RepositoryException, IOException, RDFHandlerException
    {
	RDFXMLPrettyWriter writer = new RDFXMLPrettyWriter(out);
	writer.startRDF();
	while (result.hasNext()) 
	{
	    BindingSet tuple = result.next();
	    for (Binding binding: tuple)
	    {
		Value value = binding.getValue();
		if (value instanceof Resource)
		    buildCBD((Resource)value, writer);
	    }
	}
	writer.endRDF();
	writer.close();
    }
    
	private void buildCBDs(String[] uris, ByteArrayOutputStream out)
	    throws QueryEvaluationException, RepositoryException, IOException, RDFHandlerException
    {
	RDFXMLPrettyWriter writer = new RDFXMLPrettyWriter(out);
	writer.startRDF();
	for(String uri: uris) 
	{
	    Resource r = ValueFactoryImpl.getInstance().createURI(uri);
	    buildCBD(r, writer);
	}
	writer.endRDF();
	writer.close();
    }
    
    private void buildCBD(Resource uri, RDFXMLPrettyWriter writer) throws RepositoryException, RDFHandlerException
    {
	RepositoryResult<Statement> triples = 
		repositoryConnection.getStatements(uri, null, null, false, new URIImpl(graphURI), new URIImpl(schemaURI));
	for(Statement s: triples.asList())
	{
	    writer.handleStatement(s);
	    Value value = s.getObject();
	    if (value instanceof BNode)
			buildCBD((Resource)value, writer);
	    else if (value instanceof Resource)
	    {
		RepositoryResult<Statement> labels = 
			repositoryConnection.getStatements((Resource)value, RDFS.LABEL, null, false, new URIImpl(graphURI), new URIImpl(schemaURI));
		for(Statement ls: labels.asList())
		    writer.handleStatement(ls);
	    }
		
	}
    }

    /**
     * Perform input SPARQL SELECT query and return result as ResultSet
     * The scopes, defined in MetadataStore, are: 
     * - INSTANCES (if just to query instance data) 
     * - SCHEMAS (if just to query schemas and ontologies) 
     * - REASONING (instance plus schemas plus the reasoning provided by the store)
     */
    public ResultSet querySelect(String queryString, int scope)
    {
        ResultSet results = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // If just to query instance or schema data, restrict to the corresponding graph:
        //    SELECT ... WHERE { GRAPH <InstanceOrSchemaGraph> { ... } } ...
        if (scope != MetadataStore.REASONING) {
            String queryGraph;
            if (scope == MetadataStore.INSTANCES)
                queryGraph = graphURI;
            else
                queryGraph = schemaURI;

            int startWhere = queryString.indexOf("WHERE")+"WHERE".length();
            int endWhere = queryString.lastIndexOf('}');
            if (startWhere>0)
                queryString = queryString.substring(0, startWhere) +
                        "{ GRAPH <"+queryGraph+"> "+
                        queryString.substring(startWhere, endWhere)+"} "+
                        queryString.substring(endWhere);
        }
        // If scope is REASONING, no need to constraint the query graph, leave it unchanged
           
        try
        {
            log.log(Level.INFO, "SesameStore.query: "+queryString);
            Query query = prepareQuery(queryString);
            if (query == null) 
            {
        	log.log(Level.WARNING, "Unable to parse query: " + queryString);
    		return results;
            }
            
            if (query instanceof TupleQuery)
            {
        	((TupleQuery)query).evaluate(new SPARQLResultsXMLWriter(out));
        	results = ResultSetFactory.fromXML(new ByteArrayInputStream(out.toByteArray()));
            }
        }
        catch(Exception e)
        { log.log(Level.WARNING, e.toString()); }
        finally {}	

        return results;
    }

    /**
     * Perform input SPARQL ASK query and return result as boolean
     * 
     * @return boolean
     * @param queryString java.lang.String
     */
    public boolean queryAsk(String queryString) 
    {
	boolean result = false;
	try
        {
            Query query = prepareQuery(queryString);
            if (query == null) 
            {
        	log.log(Level.WARNING, "Unable to parse query: " + query);
    		return result;
            }
            log.log(Level.INFO, "SesameStore.query: "+query);
            
            if (query instanceof BooleanQuery)
            {
        	result = ((BooleanQuery)query).evaluate();
            }
        }
        catch(Exception e)
        { log.log(Level.INFO, e.toString()); }
        finally {}
        
	return result;
    }

    private Query prepareQuery(String queryString) throws RepositoryException
    {
	Repository tempRepository = new SailRepository(new MemoryStore());
	tempRepository.initialize();
	RepositoryConnection tempConnection = tempRepository.getConnection();

	String error = "";
	Query query = null;
	
	try {
	    try {
		tempConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		query = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    } catch (Exception e1) 
	    { 
		error = e1.toString(); 
    	    	try {
    	    	    tempConnection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
    	    	    query = repositoryConnection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
    	    	} catch (Exception e2) 
    	    	{
    	    	    error = e2.toString(); 
    	    	    try {
    	    		tempConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
    	    		query = repositoryConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
    	    	    } catch (Exception e3) { error = e3.toString(); }
    	    	}
	    }
	} finally {
	    try {
		tempConnection.close();
		tempRepository.shutDown();
	    } catch (Exception e) {}
	}

	if (query == null)
	    log.log(Level.WARNING, "Error in query: "+error);
	
	return query;
    }

    /**
     * Store the input metadata.
     */
    public String store(InputStream metadata, String contentType) 
    {
        String response = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RDFFormat format = RDFFormat.RDFXML; // Default
        if (contentType.indexOf("application/n-triples") >= 0)
            format = RDFFormat.NTRIPLES;
        else if (contentType.indexOf("application/n3") >= 0)
            format = RDFFormat.N3;

        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = metadata.read(buffer)) > -1) {
                copy.write(buffer, 0, len);
            }
            copy.flush();

            //TODO: if metadata to be added about classes and properties, add to schema graph instead of instance graph
            repositoryConnection.add(new ByteArrayInputStream(copy.toByteArray()), graphURI, format, new URIImpl(graphURI));
            repositoryConnection.commit();

            RDFParser parser = new RDFXMLParser();
            parser.setRDFHandler(new RDFXMLPrettyWriter(out));
            parser.parse(new ByteArrayInputStream(copy.toByteArray()), graphURI);
            response = out.toString("UTF8");
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Exception in SesameStore.store", e);
            response = e.toString();
        }

        return response;
    }

    /**
     * Store the metadata at URL.
     * 
     * @return java.lang.String
     * @param metadataURL
     *            java.net.URL
     */
    public String store(URL metadataURL)
    {
	String response = "";
	boolean loaded = false;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	RDFFormat format = RDFFormat.RDFXML; // Default

	try 
	{
	    //TODO: if metadata to be added about classes and properties, add to schema graph instead of instance graph
	    repositoryConnection.add(metadataURL, graphURI, format, new URIImpl(graphURI));
	    repositoryConnection.commit();
	    loaded = true;
	    
	    Repository tempRepository = new SailRepository(new MemoryStore());
	    tempRepository.initialize();
	    RepositoryConnection tempConnection = tempRepository.getConnection();
	    tempConnection.add(metadataURL, graphURI, format, new URIImpl(graphURI));
	    tempConnection.commit();
	    tempConnection.export(new RDFXMLPrettyWriter(out), new URIImpl(graphURI));
	    out.close();
	    response = out.toString("UTF8");
	} 
	catch (Exception e) 
	{
	    log.log(Level.SEVERE, "Exception in SesameStore.store", e);
	    response = e.toString();
	}
	
	if (!loaded)
	    try 
	    { 
		repositoryConnection.rollback(); 
	    } 
	    catch (RepositoryException e) 
	    {
		log.log(Level.SEVERE, "Exception in SesameStore.store", e);
	    	response = e.toString();
	    }

	return response;
    }

    /**
     * Remove all available metadata for the input URI, i.e. the Concise Bounded
     * Description for the URI resource
     */
    public void remove(java.net.URI uri) 
    {
        Resource r = ValueFactoryImpl.getInstance().createURI(uri.toString());
        remove(r);
        try {
            repositoryConnection.commit();
        }
        catch (RepositoryException e) {
            log.log(Level.SEVERE, "Exception in SesameStore.remove", e);
        }
    }

    public void remove(Resource r)
    {
        try {
            RepositoryResult<Statement> triples =
                repositoryConnection.getStatements(r, null, null, false, new URIImpl(graphURI), new URIImpl(schemaURI));
            for(Statement s: triples.asList())
            {
                Value value = s.getObject();
                if (value instanceof BNode)
                    remove((Resource)value);
                repositoryConnection.remove(s, new URIImpl(graphURI), new URIImpl(schemaURI));
            }
        }
        catch (RepositoryException e) {
            log.log(Level.SEVERE, "Exception in SesameStore.remove", e);
        }
    }

    /**
     * Remove the input metadata from the store. TODO: Are literals also
     * removed?
     * 
     * @return java.lang.String
     * @param metadata
     *            java.io.InputStream
     */
    public void remove(InputStream metadata, String contentType) 
    {
	RDFFormat format = RDFFormat.RDFXML; // Default
	boolean loaded = false;
	try 
	{
	    Repository tempRepository = new SailRepository(new MemoryStore());
	    tempRepository.initialize();
	    RepositoryConnection tempConnection = tempRepository.getConnection();
	    tempConnection.add(metadata, graphURI, format, new URIImpl(graphURI));
	    tempConnection.commit();

	    repositoryConnection.remove(tempConnection.getStatements(null, null, null, true), new URIImpl(graphURI));
	    repositoryConnection.commit();
	    loaded = true;
	    tempConnection.close();
	}
	catch (Exception e) 
	{
	    log.log(Level.SEVERE, "Exception in SesameStore.remove", e);
	}
	
	if (!loaded)
	    try 
	    { 
		repositoryConnection.rollback(); 
	    } 
	    catch (RepositoryException e) 
	    {
		log.log(Level.SEVERE, "Exception in SesameStore.remove", e);
	    }
    }

    public void close() 
    {
	try 
	{
	    repositoryConnection.close();
	} 
	catch (RepositoryException e)
	{
	    log.log(Level.SEVERE, "Exception in SesameStore.close", e);
	}
    }
}
