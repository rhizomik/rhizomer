package net.rhizomik.rhizomer.store.ldp;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by David Castellà david.castella@udl.cat on 04/11/14.
 *
 */
public class ResourceDripper {
    private OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    public ResourceDripper (String fileUri) {
        model.read(fileUri, "RDF/XML");
    }

    private String describeURI(OntResource resource) {
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

        wr = c.resource(serverUri.toString() + "/" + containerName + "/");
        String identifier = resourceUri.split("/")[resourceUri.split("/").length - 1];
        cr = wr.type("application/rdf+xml")
                .header("Slug", identifier)
                .post(ClientResponse.class, resourceRDF);
    }

    public void importResources2LDP (URL serverUri, String containerName, String classUri) {
        final OntClass governmentOrganization;
        String resourceRDF;
        try {
            governmentOrganization = model.getOntClass(classUri);
            for ( final ExtendedIterator<? extends OntResource> gvs = governmentOrganization.listInstances(); gvs.hasNext(); ) {
                OntResource or = gvs.next();
                resourceRDF = describeURI(or);
                simpleImport2LDP(or.getURI(), resourceRDF, serverUri, containerName);
            }
        } catch (NullPointerException npe) {
            System.out.println("There's no class with uri: <" + classUri + ">");
        }
    }

    public static void main(String[] args) throws MalformedURLException {
        ResourceDripper rd = new ResourceDripper("file:///home/davidkaste/Documents/administracions-old.rdf");
        URL url = new URL("http://localhost:8080/marmotta/ldp");
        rd.importResources2LDP(url, "administracions", "http://schema.org/GovernmentOrganization");
    }
}
