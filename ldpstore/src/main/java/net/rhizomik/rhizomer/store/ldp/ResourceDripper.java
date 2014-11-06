package net.rhizomik.rhizomer.store.ldp;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by davidkaste on 04/11/14.
 */
public class ResourceDripper {
    private Model model = ModelFactory.createDefaultModel();

    public ResourceDripper (/*String file*/) {
        model.read("file:///home/davidkaste/Documents/administracions-export-20140922-100143.rdf", "RDF/XML");
    }

    public void importResources2LDP (URL serverURI, String containerName) {
        model.write(System.out, "RDF/XML");
        /*String queryString = "...";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        Model resultModel = qexec.execDescribe();
        resultModel.write(System.out);*/
    }

    public static void main(String[] args) throws MalformedURLException {
        ResourceDripper rd = new ResourceDripper();
        URL url = new URL("http://localhost:8080/marmotta/ldp/");
        rd.importResources2LDP(url, "c1");
    }
}
