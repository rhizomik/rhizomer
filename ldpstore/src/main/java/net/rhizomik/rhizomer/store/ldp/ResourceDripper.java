package net.rhizomik.rhizomer.store.ldp;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.codec.binary.Hex;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rhizomer RDF resource harvester for LDP store
 *
 * @author: David Castellà <david.castella@udl.cat>
 * @version: 0.1
 */
public class ResourceDripper {
    private OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    private static final Logger log = Logger.getLogger(ResourceDripper.class.getName());

    public ResourceDripper (String fileUri) {
        model.read(fileUri, "RDF/XML");
    }

    /*private String describeURI(OntResource resource) {
        String sparql;
        Query q;
        QueryExecution qe;
        Model resultModel;
        StringWriter out;

        sparql = "DESCRIBE <" + resource.getURI() + ">";
        q = QueryFactory.create(sparql);
        qe = QueryExecutionFactory.create(q, resource.getModel());
        resultModel = qe.execDescribe();
        out = new StringWriter();
        resultModel.write(out, "RDF/XML");

        return out.toString();
    }*/

    private String describeURI(Resource resource) {
        String sparql;
        Query q;
        QueryExecution qe;
        Model resultModel;
        StringWriter out;

        sparql = "DESCRIBE <" + resource.getURI() + ">";
        q = QueryFactory.create(sparql);
        qe = QueryExecutionFactory.create(q, resource.getModel());
        resultModel = qe.execDescribe();
        out = new StringWriter();
        resultModel.write(out, "RDF/XML");

        return out.toString();
    }

    private void simpleImport2LDP(String resourceUri, String resourceRDF, URL serverUri, String containerName) {
        WebResource wr;
        Client c = Client.create();
        ClientResponse cr;
        MessageDigest md;
        String hexDump;

        try {
            wr = c.resource(serverUri.toString() + "/" + containerName);
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(resourceUri.getBytes("UTF-8"));
            hexDump = Hex.encodeHexString(md.digest());
            log.log(Level.INFO, "Digest: " + hexDump);
            cr = wr.type("application/rdf+xml")
                    .header("Slug", hexDump)
                    .post(ClientResponse.class, resourceRDF);

            if (cr.getStatus() != 201) {
                log.log(Level.SEVERE, "Failed : HTTP Error Code: " + cr.getStatus());
                throw new RuntimeException("Failed : HTTP Error Code: " + cr.getStatus());
            }
        } catch (RuntimeException re) {
            re.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            log.log(Level.SEVERE, "MD5 isn't a valid algorithm");
            nsae.printStackTrace();
        } catch (UnsupportedEncodingException uee) {
            log.log(Level.SEVERE, "UTF-8 is unsupported");
            uee.printStackTrace();
        }
    }

    public void importResources2LDP (URL serverUri, String containerName, String classUri) {
        final OntClass rdfClass;
        String resourceRDF;
        try {
            rdfClass = model.getOntClass(classUri);
            for ( final ExtendedIterator<? extends OntResource> gvs = rdfClass.listInstances(); gvs.hasNext(); ) {
                OntResource or = gvs.next();
                resourceRDF = describeURI(or);
                simpleImport2LDP(or.getURI(), resourceRDF, serverUri, containerName);
            }
        } catch (NullPointerException npe) {
            log.log(Level.SEVERE, "There's no class with uri: <" + classUri + ">");
            System.out.println("There's no class with uri: <" + classUri + ">");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
    }

    public void importResources2LDP(URL serverURI) {
        String query;
        Query q;
        QueryExecution qe;
        QuerySolution qs;

        query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?instance ?class WHERE { ?instance rdf:type ?class } ORDER BY ?class ?instance";
        /*query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?r1 ?type\n" +
                "WHERE { \n" +
                "   ?r1 a ?type \n" +
                "   OPTIONAL {\n" +
                "      ?r1 a ?type2. ?type2 rdfs:subClassOf ?type. \n" +
                "      FILTER (?type!=?type2 && !isBlank(?type2)) } \n" +
                "   FILTER(!isBlank(?type) && bound(?type2))\n" +
                "}";*/
        q = QueryFactory.create(query);
        qe = QueryExecutionFactory.create(q, model);
        ResultSet result = qe.execSelect();
        while (result.hasNext()) {
            qs = result.next();
            String className = qs.getResource("?class").toString().split("/")[qs.getResource("?class").toString().split("/").length - 1];
            if(className.contains("#")) {
                className = className.split("#")[1];
            }
            simpleImport2LDP(qs.getResource("?instance").getURI(), describeURI(qs.getResource("?instance")), serverURI, className);
            log.log(Level.INFO, "Importing resource with uri <" + qs.getResource("?instance").getURI() + "> to container " + className);
        }

    }

    public static void main(String[] args) throws MalformedURLException {
        log.setLevel(Level.ALL);
        ResourceDripper rd = new ResourceDripper("file:///home/davidkaste/Projects/rhizomer/src/main/webapp/metadata/nasa-apollo.rdf");
        URL url = new URL("http://localhost:8080/marmotta/ldp");
        //rd.importResources2LDP(url, "missions", "http://purl.org/net/schemas/space/Mission");
        rd.importResources2LDP(url);
        //rd.importResources2LDP(url, "administracions/address", "http://schema.org/PostalAddress");
    }
}
