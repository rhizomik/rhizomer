package net.rhizomik.rhizomer.agents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import net.rhizomik.rhizomer.store.MetadataStore;
import net.rhizomik.rhizomer.store.MetadataStoreFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Singleton that encapsulates access to the RDF Storage layer
 * 
 * @author  : http://rhizomik.net/~roberto
 */

public class RhizomerRDF
{
	private MetadataStore store = null;
	private int MAX_INPUT_LENGTH = 500000;
	static private RhizomerRDF instance = null;
    private static final Logger log = Logger.getLogger(RhizomerRDF.class.getName());
    
    private RhizomerRDF()
    {
    }
    
    static public RhizomerRDF instance() // throws Exception 
    {
    	if(instance == null) 
        	instance = new RhizomerRDF();
        return instance;
    }
    
    public void addStore(ServletConfig config) throws Exception
    {
    	store = MetadataStoreFactory.getMetadataStore(config);
    }
    
    public void addStore(Properties props) throws Exception
    {
    	store = MetadataStoreFactory.getMetadataStore(props);
    }

    public MetadataStore getStore()
    {
        return store;
    }
 
    public void destroy()
    {
    	getStore().close();
    }
    
    public String store (InputStream input, String contenttype) throws IOException
    {
    	String storedData = "";
    	storedData = getStore().store(input, contenttype);
    	return storedData;
    }

	public void remove(URI uri) 
	{
		getStore().remove(uri);
	}
	
	public String query(String query) 
	{
		return getStore().query(query);
	}
	
	public ResultSet querySelect(String query, int scope)
	{
		return getStore().querySelect(query, scope);
	}
	
	public boolean queryAsk(String query)
	{
		return getStore().queryAsk(query);
	}
	
	public String getMetadata(HttpServletRequest request)
	{
    	// Get semantic metadata
    	String rdf = "";
    	// If there is a query to the SPARQL endpoint resolve and return result metadata
    	if (request.getParameter("query") != null)
    	{
    		String query = request.getParameter("query");
        	rdf = getStore().query(query);
        	String userAgent = request.getHeader("User-Agent").toLowerCase();
        	String botsRegex = "^$|.*bot.*|.*crawler.*|.*spider.*|.*slurp.*|.*ask.*|.*teoma.*";
        	
        	// When DESCRIBE URL, if no result in local store, 
        	// retrieve metadata from the URL and filter it by requerying it
        	// Do not try to do so if the URI queried is from this webapp or from a bot/crawler
        	// TODO: In order to check the latter, include also servlet name, port,...
            // TODO: improve way to check that there is no metadata locally,
            // currently just checking returned RDF/XML smaller than 500 chars, which corresponds roughly
            // to metadata just containing the resource label
        	if (rdf.length()<500 && query.matches("DESCRIBE <.*>") &&
        		query.indexOf(request.getServerName())<0 && !userAgent.matches(botsRegex))
        	{
        		int uriStart = query.indexOf('<')+1;
        		int uriEnd = query.indexOf('>');
        		String described = query.substring(uriStart, uriEnd);        		
        		try
        		{
        			URL describedURL = new URL(described);
        			URLConnection conn = describedURL.openConnection();
        			conn.setRequestProperty("accept", "application/rdf+xml");
        			String contenttype=conn.getContentType();
        			int length = conn.getContentLength();
        			
        			if (contenttype!=null && contenttype.indexOf("rdf+xml")>0 && length < MAX_INPUT_LENGTH)
        			{
	        		 	Model temp = ModelFactory.createDefaultModel();
	    				temp.read(conn.getInputStream(), "");
	    				
	        			QueryExecution qexec2 = QueryExecutionFactory.create(query, temp);
	        			Model results = ModelFactory.createDefaultModel();
	        			results = qexec2.execDescribe(results);
	        		
	        			ByteArrayOutputStream out = new ByteArrayOutputStream();
	        			results.write(out, "RDF/XML-ABBREV");
	        			
	        			rdf = out.toString("UTF8");
        			}
        		}
        		catch (Exception e)
        		{ log.log(Level.SEVERE, "Exception while retrieving external metadata for: "+described, e); }
        	}
    	}
    	// Otherwise, return metadata for the requested URL
    	else
    	{
    		String requestURL = request.getRequestURL().toString();
    		if (requestURL.indexOf("%7E")>0)
    			requestURL = requestURL.substring(0, requestURL.indexOf("%7E"))+"~"+
				 requestURL.substring(requestURL.indexOf("%7E")+3, requestURL.length());
    		
    		String query = "DESCRIBE <"+requestURL+">";
    		
    		if (requestURL.indexOf("/html")>0)
    		{
    			String requestURI = requestURL.substring(0, requestURL.indexOf("/html"))+
				 requestURL.substring(requestURL.indexOf("/html")+5, requestURL.length());
    			
    			query = query + " <"+requestURI+">";
    		}
            else if (requestURL.indexOf("/data")>0)
            {
                String requestURI = requestURL.substring(0, requestURL.indexOf("/data"))+
                        requestURL.substring(requestURL.indexOf("/data")+5, requestURL.length());

                query = query + " <"+requestURI+">";
            }
    			
    		rdf = getStore().query(query);
    	}
    	return rdf;
	}
	
	public String getMetadataJSON(HttpServletRequest request)
	{
    	// Get semantic metadata
    	String json = "";
    	// If there is a query to the SPARQL endpoint resolve and return result metadata
    	if (request.getParameter("query") != null)
    	{
    		String query = request.getParameter("query");
        	json = getStore().queryJSON(query);
    	}
    	return json;
	}
}
