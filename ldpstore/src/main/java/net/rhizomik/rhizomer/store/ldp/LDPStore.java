package net.rhizomik.rhizomer.store.ldp;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import net.rhizomik.rhizomer.store.MetadataStore;
import org.apache.commons.codec.binary.Hex;
import org.apache.jena.riot.RDFLanguages;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.MarmottaClient;
import org.apache.marmotta.client.clients.SPARQLClient;
import org.apache.marmotta.client.exception.MarmottaClientException;

import javax.servlet.ServletConfig;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.rhizomik.rhizomer.store.ldp.JenaAdapter.*;

/**
 * LDP implementation of the Rhizomer metadata store
 *
 * @author: David Castellà <david.castella@udl.cat>
 * @version: 0.1
 */
public class LDPStore implements MetadataStore {

    private String baseURI = "";
    private String containerName = "";
    private String sparqlEndpoint = "";
    private String rdfData = "";
    private String rdfType = "";

    private MarmottaClient mc;

    private static final Logger log = Logger.getLogger(LDPStore.class.getName());

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
        if (configResource.getServletContext().getInitParameter("base_uri") == null)
            throw new Exception("Missing parameter for LDP init: manager_url");
        else if (configResource.getServletContext().getInitParameter("container_name") == null)
            throw new Exception("Missing parameter for LDP init: container_name");
        else if (configResource.getServletContext().getInitParameter("sparql_endpoint")==null)
            throw new Exception("Missing parameter for LDP init: sparql_endpoint");
        else if (configResource.getServletContext().getInitParameter("rdf_data")==null)
            throw new Exception("Missing parameter for LDP init: rdf_data");
        else if (configResource.getServletContext().getInitParameter("rdf_type")==null)
            throw new Exception("Missing parameter for LDP init: rdf_type");
        else
        {
            init(configResource.getServletContext().getInitParameter("base_uri"),
                    configResource.getServletContext().getInitParameter("container_name"),
                    configResource.getServletContext().getInitParameter("sparql_endpoint"),
                    configResource.getServletContext().getInitParameter("rdf_data"),
                    configResource.getServletContext().getInitParameter("rdf_type"));
        }

        initMarmotta();
    }

    private void init(String base_uri, String container_name, String sparql_endpoint, String rdf_data, String rdf_type) throws MalformedURLException {
        ResourceDripper rd;
        baseURI = base_uri;
        containerName = container_name;
        sparqlEndpoint = sparql_endpoint;
        rdfData = rdf_data;
        rdfType = rdf_type;

        /*rd = new ResourceDripper(rdf_data);
        rd.importResources2LDP(new URL(baseURI), containerName, rdfType);*/

        initMarmotta();
    }

    /**
     * Abstract method to initialise the metadata store using the parameters
     * from a properties object.
     *
     * @param props
     */
    @Override
    public void init(Properties props) throws Exception {
        if (props.getProperty("base_uri") == null)
            throw new Exception("Missing parameter for LDP init: base_uri");
        else if (props.getProperty("container_name") == null)
            throw new Exception("Missing parameter for LDP init: container_name");
        else if (props.getProperty("sparql_endpoint")==null)
            throw new Exception("Missing parameter for LDP init: sparql_endpoint");
        else if (props.getProperty("rdf_data")==null)
            throw new Exception("Missing parameter for LDP init: rdf_data");
        else if (props.getProperty("rdf_type")==null)
            throw new Exception("Missing parameter for LDP init: rdf_type");
        else
        {
            init(props.getProperty("base_uri"),
                    props.getProperty("container_name"),
                    props.getProperty("sparql_endpoint"),
                    props.getProperty("rdf_data"),
                    props.getProperty("rdf_type"));
        }
        initMarmotta();
    }

    private void initMarmotta() {
        ClientConfiguration cc = new ClientConfiguration(baseURI, "", "");
        mc = new MarmottaClient(cc);
    }

    private String getMD5(String resourceUri) {
        MessageDigest md = null;
        String hexDump = "";
        try {
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(resourceUri.getBytes("UTF-8"));
            hexDump = Hex.encodeHexString(md.digest());
        } catch(NoSuchAlgorithmException nsae) {
            log.log(Level.SEVERE, "Digest algorithm " + md.getAlgorithm() + " does not exist.");
        } catch(UnsupportedEncodingException uee) {
            log.log(Level.SEVERE, "UTF-8 encoding is not valid.");
        }

        return hexDump;
    }

    private Model getContainer(String uri) {
        WebResource wr;
        Client c = Client.create();
        ClientResponse cr;

        wr = c.resource(uri);
        /*cr = wr.get(ClientResponse.class);
        if(cr.getStatus() != 200) {
            log.log(Level.SEVERE, "Failed : HTTP Error Code: " + cr.getStatus());
            throw new RuntimeException("Failed : HTTP Error Code: " + cr.getStatus());
        }*/


        String response = wr.header("Accept", "application/rdf+xml").get(String.class);
        return ModelFactory.createDefaultModel().read(new ByteArrayInputStream(response.getBytes()), RDFLanguages.strLangRDFXML);
        //return ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM).read(response, RDFLanguages.strLangRDFXML);
    }

    private String describeJenaModel(Model model, String instanceUri) {
        String queryString = "DESCRIBE <" + instanceUri + ">";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        Model result = qexec.execDescribe();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.write(baos, RDFLanguages.strLangRDFXML);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Abstract method for querying a metadata store.
     *
     * @param query
     */
    @Override
    public String query(String query) {
        String response;
        String uri = "";
        String describeResult;
        String dquery;
        OntModel model;
        Query q;
        QueryExecution qe;
        QuerySolution qs;
        ResultSet result;
        //ResourceDripper rd = new ResourceDripper(this.rdfData);
        if(query.contains("DESCRIBE <")
                && !query.equals("DESCRIBE <http://localhost:8000/html/> <http://localhost:8000/>")
                /*&& !query.contains("dbpedia.org")*/) {
            qs = getQuerySolution(query);
            Model marmottaModel = getMarmottaModel(qs);
            return describeJenaModel(marmottaModel, qs.getResource("?instance").getURI());
        }
        response = queryMarmotta(query, sparqlEndpoint);
        return response;
    }

    private Model getMarmottaModel(QuerySolution qs) {
        String className = getClassName(qs.getResource("?class").toString().split("/")[qs.getResource("?class").toString().split("/").length - 1]);
        log.log(Level.INFO, "Detected class: " + className);
        log.log(Level.INFO, "Detected instance: " + qs.getResource("?instance").getURI());
        log.log(Level.INFO, "MD5 digest of resource: " + getMD5(qs.getResource("?instance").getURI()));
        // Get function
        log.log(Level.INFO, "URI: " + baseURI + "/ldp/" +
                className + "/" +
                getMD5(qs.getResource("?instance").getURI()));
        return getContainer(baseURI + "/ldp/" +
                className + "/" +
                getMD5(qs.getResource("?instance").getURI()));
    }

    private String getClassName(String s) {
        String className = s;
        if(className.contains("#")) {
            className = className.split("#")[1];
        }
        return className;
    }

    private QuerySolution getQuerySolution(String query) {
        String uri;
        String describeResult;
        OntModel model;
        String dquery;
        Query q;
        QueryExecution qe;
        ResultSet result;
        QuerySolution qs;
        uri = query.substring(query.indexOf("<") + 1, query.indexOf(">"));
        log.log(Level.INFO, "Query: " + query);
        log.log(Level.INFO, "Parsed URI: " + uri);
        //describeResult = rd.describeURI(ResourceFactory.createResource(uri));
        describeResult = queryMarmotta(query, this.sparqlEndpoint);
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        InputStream stream = new ByteArrayInputStream(describeResult.getBytes(StandardCharsets.UTF_8));
        model.read(stream, "RDF/XML");
        dquery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?instance ?class WHERE { ?instance rdf:type ?class } ORDER BY ?class ?instance";
        q = QueryFactory.create(dquery);
        qe = QueryExecutionFactory.create(q, model);
        result = qe.execSelect();
        qs = result.next();
        return qs;
    }

    /**
     * Abstract method for querying a metadata store and getting JSON instead of RDF/XML.
     *
     * @param query
     */
    @Override
    public String queryJSON(String query) {
        String response;
        response = queryJSONMarmotta(query, sparqlEndpoint);
        return response;
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
        ResultSet response = null;
        response = querySelectMarmotta(query, sparqlEndpoint);
        return response;
    }

    /**
     * Abstract method for performing a SPARQL ASK query an retrieve a boolean.
     *
     * @param query
     */
    @Override
    public boolean queryAsk(String query) {
        SPARQLClient client = mc.getSPARQLClient();
        boolean response;
        try {
            response = client.ask(query);
        } catch (MarmottaClientException mce) {
            //log.log(Level.SEVERE, "MarmottaClientException in LDPStore.query for: " + query, mce);
            response = false;
        } catch (IOException ioe) {
            //log.log(Level.SEVERE, "IOException in LDPStore.query for: " + query, ioe);
            response = false;
        }
        return response;
    }

    /**
     * Abstract method for storing input metadata in a store.
     *
     * @param metadata
     * @param contentType
     */
    @Override
    public String store(InputStream metadata, String contentType) {
        return "done";
    }

    /**
     * Abstract method for storing the content of a URL in a metadata store.
     *
     * @param metadataUrl
     */
    @Override
    public String store(URL metadataUrl) {
        /*String response = "";
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
        return null;*/
        return "done";
    }

    /**
     * Abstract method for removing input metadata from a store.
     *
     * @param metadata
     * @param contentType
     */
    @Override
    public void remove(InputStream metadata, String contentType) {
        int pass;
    }

    /**
     * Abstract method for removing the metadata corresponding to the
     * Concise Bounded Description for the resource from a metadata store.
     *
     * @param resource
     */
    @Override
    public void remove(URI resource) {
        int pass;
    }

    /**
     * Abstract method called when the metadata store should be closed.
     */
    @Override
    public void close() {
        int pass;
    }
}
