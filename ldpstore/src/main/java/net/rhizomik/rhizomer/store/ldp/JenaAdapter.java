package net.rhizomik.rhizomer.store.ldp;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.apache.http.client.utils.URIBuilder;

import java.io.ByteArrayInputStream;

/**
 * Created by davidkaste on 04/02/15.
 * Gets Apache Marmotta Objects and returns Apache Jena Objects
 */
public abstract class JenaAdapter {
    public static ResultSet querySelectMarmotta(String query, String SPARQLEndpoint) {
        URIBuilder ub = new URIBuilder();
        Client c = Client.create();
        WebResource wr = c.resource(SPARQLEndpoint + "select" + ub.addParameter("query", query).addParameter("output", "application/sparql-results+xml"));
        wr.toString();
        String cr = wr.get(String.class);
        System.out.println(cr);
        ResultSet ret = ResultSetFactory.fromXML(new ByteArrayInputStream(cr.getBytes()));
        return ret;
    }
}
