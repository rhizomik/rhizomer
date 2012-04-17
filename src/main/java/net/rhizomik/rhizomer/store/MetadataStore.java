package net.rhizomik.rhizomer.store;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletConfig;

import com.hp.hpl.jena.query.ResultSet;

/**
 * Generic interface used to operate with different metadata stores.
 * @author: http://rhizomik.net/~roberto
 */
public interface MetadataStore 
{
    public static int INSTANCES = 1;
    public static int SCHEMAS = 2;
    public static int REASONING = 3;

    /**
     * Abstract method to initialise the metadata store using the parameters
     * from a servlet config .
     */
    void init(ServletConfig configResource) throws Exception;
    
    /**
     * Abstract method to initialise the metadata store using the parameters 
     * from a properties object.
     */
	void init(Properties props) throws Exception;
    
	/**
	 * Abstract method for querying a metadata store.
	 */
	String query(String query);

	/**
	 * Abstract method for querying a metadata store and getting JSON instead of RDF/XML.
	 */
	String queryJSON(String query);
	
	/**
	 * Abstract method for querying an retrieving Jena ResultSet, just for SPARQL select
     * and for a defined query scope.
     * The scopes, defined in MetadataStore, are:
     * - INSTANCES (if just to query instance data)
     * - SCHEMAS (if just to query schemas and ontologies)
     * - REASONING (instance plus schemas plus the reasoning provided by the store)
     */
	ResultSet querySelect(String query, int scope);
	
	/**
	 * Abstract method for performing a SPARQL ASK query an retrieve a boolean.
	 */
	boolean queryAsk(String query);

	/**
	 * Abstract method for storing input metadata in a store.
	 */
	String store(InputStream metadata, String contentType);
	
	/**
	 * Abstract method for storing the content of a URL in a metadata store.
	 */
	String store(URL metadataUrl);
	
	/**
	 * Abstract method for removing input metadata from a store.
	 */
	void remove(InputStream metadata, String contentType);
	
	/**
	 * Abstract method for removing the metadata coresponding to the
	 * Concise Bounded Description for the resource from a metadata store.
	 */
	void remove(URI resource);

	/**
	 * Abstract method called when the metadata store should be closed.
	 */
	void close();
}
