/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.rhizomik.rhizomer.service;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.service.Service;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 *
 * @author kwal85
 */
public class ServiceManager {
private String rhizomerServiceBaseUri = "http://rhizomik.net/rhizomer/services#";
    private String NL = System.getProperty("line.separator");

    private ArrayList<Service> serviceList;

    
    protected String queryForServices =
    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
    "PREFIX process: <http://www.daml.org/services/owl-s/1.1/Process.owl#>"+
    "PREFIX rhizomer: <http://rhizomik.net/rhizomer/services#>"+
    "SELECT ?r ?input ?output ?label ?icon ?endPoint "
            + "WHERE {"
                + "?r a process:Process .?r process:hasInput ?input .?r process:hasOutput ?output ."
                + "OPTIONAL {?r rdfs:label ?label}"
                + "OPTIONAL {?r rdfs:icon ?icon}"
                + "OPTIONAL {?r rhizomer:endPoint ?endPoint}"
            + "}";

    protected String queryForResources =
    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
    "SELECT DISTINCT ?r "
            + "WHERE {"
                + "?r ?p ?o FILTER (!isBlank(?r) && ?p!=rdfs:label)"
            + "}";
    
    public ServiceManager(){
        this.serviceList = new ArrayList<Service>();
    }
    
    public void readServiceList(){
        ResultSet results = RhizomerRDF.instance().querySelect(queryForServices, true);
        while(results.hasNext()){
            QuerySolution row = results.next();
            String uri = row.get("r").toString();
            Service node = new Service(uri);
            node.setSparqlQuery(row.get("input").toString());
            if (row.contains("label")){
                node.setLabel(row.get("label").toString());
            }
            if (row.contains("icon")){
                node.setLabel(row.get("icon").toString());
            }
            if (row.contains("endPoint")){
                node.setEndpoint(row.get("endPoint").toString());
            }
            this.serviceList.add(node);
        }
    }
    
    public String addServices(String subrdf, HttpServletRequest request)
    {
        try 
        {
            Model model = ModelFactory.createMemModelMaker().createDefaultModel();
            Model modeltmp = ModelFactory.createMemModelMaker().createDefaultModel();
            try 
            { model.read(new StringReader(subrdf), ""); } 
            catch (Exception e) {}
            
            ArrayList<Service> serList = (ArrayList<Service>) request.getSession().getAttribute("service");
            Query query = QueryFactory.create(queryForResources);
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            ResultSet results = qexec.execSelect();
            while (serList!=null && results.hasNext()) 
            {
                QuerySolution row = results.next();
                ListIterator<Service> itservices = serList.listIterator();
                while (itservices.hasNext()) 
                {
                    Service s = itservices.next();
                    if (isVisualizable(model, s, row.getResource("r"))) 
                        updateResource(s, row.getResource("r"), modeltmp);
                }
            }
            
            model.add(modeltmp);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            model.write(out, "RDF/XML-ABBREV");
            subrdf = out.toString("UTF8");
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ServiceManager.class.getName()).log(Level.SEVERE, "Exception while retrieving external metadata ", ex);
        }
        return subrdf;
    }
    
    private void updateResource(Service s, Resource r, Model model)
    {
        Property serviceURI = model.createProperty(this.rhizomerServiceBaseUri,"hasService");
        Property serviceEndPoint = model.createProperty(this.rhizomerServiceBaseUri,"endPoint");
        
        Resource rs = model.createResource(s.getUri());
        model.add(rs,RDFS.label,s.getLabel());
        model.add(rs,serviceEndPoint,s.getEndpoint());

        model.add(r,serviceURI,rs);
    }
    
    // A Resource is visualizable using the Service if the associated ASK SPARQL query returns true,
    // either when querying the locally stored data or the response data (that might come from external
    // sources by following Linked Data principles
    private boolean isVisualizable(Model response, Service s,Resource r)
    {
	boolean visualizable = false;
        String askQuery = s.getSparqlQuery().replaceAll("\\[URI\\]", r.toString());
        
        // First query locally store data
        visualizable = RhizomerRDF.instance().queryAsk(askQuery);
        
        // If not visualizable considering local data, check with the response data
        if (!visualizable)
        {
            Query query = QueryFactory.create(askQuery);
            QueryExecution qexec = QueryExecutionFactory.create(query, response);
            visualizable = qexec.execAsk();
        }
        return visualizable;
    }
    
    /**
     * @return the serviceList
     */
    public ArrayList<Service> getServiceList() {
        return serviceList;
    }

    /**
     * @param serviceList the serviceList to set
     */
    public void setServiceList(ArrayList<Service> serviceList) {
        this.serviceList = serviceList;
    }
}
