package net.rhizomik.rhizomer.store.ldp;

import com.hp.hpl.jena.query.ResultSet;
import net.rhizomik.rhizomer.store.MetadataStore;

import javax.servlet.ServletConfig;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

/**
 * Created by davidkaste on 27/10/14.
 */
public class LDPStore implements MetadataStore {

    private URL baseURL = null;
    private String containerName = null;

    public LDPStore () {
        super();
    }
    /**
     * Abstract method to initialise the metadata store using the parameters
     * from a servlet config .
     *
     * @param configResource
     */
    @Override
    public void init(ServletConfig configResource) throws Exception {

    }

    /**
     * Abstract method to initialise the metadata store using the parameters
     * from a properties object.
     *
     * @param props
     */
    @Override
    public void init(Properties props) throws Exception {

    }

    /**
     * Abstract method for querying a metadata store.
     *
     * @param query
     */
    @Override
    public String query(String query) {
        return null;
    }

    /**
     * Abstract method for querying a metadata store and getting JSON instead of RDF/XML.
     *
     * @param query
     */
    @Override
    public String queryJSON(String query) {
        return null;
    }

    /**
     * Abstract method for querying an retrieving Jena ResultSet, just for SPARQL select
     * and for a defined query scope.
     * The scopes, defined in MetadataStore, are:
     * - INSTANCES (if just to query instance data)
     * - SCHEMAS (if just to query schemas and ontologies)
     * - REASONING (instance plus schemas plus the reasoning provided by the store)
     *
     * @param query
     * @param scope
     */
    @Override
    public ResultSet querySelect(String query, int scope) {
        return null;
    }

    /**
     * Abstract method for performing a SPARQL ASK query an retrieve a boolean.
     *
     * @param query
     */
    @Override
    public boolean queryAsk(String query) {
        return false;
    }

    /**
     * Abstract method for storing input metadata in a store.
     *
     * @param metadata
     * @param contentType
     */
    @Override
    public String store(InputStream metadata, String contentType) {
        return null;
    }

    /**
     * Abstract method for storing the content of a URL in a metadata store.
     *
     * @param metadataUrl
     */
    @Override
    public String store(URL metadataUrl) {
        return null;
    }

    /**
     * Abstract method for removing input metadata from a store.
     *
     * @param metadata
     * @param contentType
     */
    @Override
    public void remove(InputStream metadata, String contentType) {

    }

    /**
     * Abstract method for removing the metadata coresponding to the
     * Concise Bounded Description for the resource from a metadata store.
     *
     * @param resource
     */
    @Override
    public void remove(URI resource) {

    }

    /**
     * Abstract method called when the metadata store should be closed.
     */
    @Override
    public void close() {

    }
}
