package net.rhizomik.rhizomer.agents;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import net.rhizomik.rhizomer.autoia.classes.AutoComplete.AutoCompleteOption;
import net.rhizomik.rhizomer.autoia.classes.HierarchyMenu;
import net.rhizomik.rhizomer.autoia.classes.HierarchyNode;
import net.rhizomik.rhizomer.store.MetadataStore;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class RhizomerAutocompleteSearch extends HttpServlet {

    private String NL = System.getProperty("line.separator");


    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String query = request.getParameter("q");
        int numResults = 15;
        System.out.println(query);
        HierarchyMenu menu = (HierarchyMenu) request.getSession(false).getAttribute("originalMenu");
        ArrayList<HierarchyNode> selectedNodes = new ArrayList<HierarchyNode>();
        for(HierarchyNode node : menu.getAllNodes()){
            if(node.getLabel().toLowerCase().contains(query.toLowerCase())){
                selectedNodes.add(node);
                System.out.println(node.getLabel());
            }
        }

        StringBuffer output = new StringBuffer();

        output.append("{\"results\" : [");
        for(HierarchyNode node : selectedNodes){
            String link = request.getContextPath()+node.getFacetsLink();
            output.append("{\"label\" : \""+node.getLabel()+"\", \"link\" : \""+link+"\", \"instances\" : "+node.getNumInstances()+"},");
        }
        numResults = numResults - selectedNodes.size();
        output.append(queryInstances(request,query,menu,numResults));
        output.append("]}");
        PrintWriter out = response.getWriter();
        System.out.println(output);
        out.println(output);
    }

    private StringBuffer queryInstances(HttpServletRequest request, String query, HierarchyMenu menu, int numResults){
        StringBuffer output = new StringBuffer();
        String queryForInstances =
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
            "SELECT DISTINCT ?uri ?label ?c"+NL+
                "WHERE {"+NL+
                "   ?uri rdf:type ?c . "+NL+
                "   ?uri rdfs:label ?label . "+NL+
                "   FILTER(contains(str(?label), \""+query+"\") || contains(str(?uri), \""+query+"\")) ."+NL+
                /*"    FILTER( NOT EXISTS {" +NL+
                "   ?sub rdfs:subClassOf ?c ." + NL +
                "   })" + NL +*/
                "FILTER(?c!=owl:Class && ?c!=rdf:Property && ?c!=owl:ObjectProperty)"+NL+
                "} ORDER BY ?uri LIMIT 50";
        ResultSet results = RhizomerRDF.instance().querySelect(queryForInstances, MetadataStore.REASONING);
        List<AutoCompleteOption> options = new ArrayList<AutoCompleteOption>();
        String lastUri = null;
        AutoCompleteOption finalOption = null;

        while(results.hasNext()){
            QuerySolution row = results.next();
            String uri = row.get("uri").toString();
            String label = row.get("label").toString();
            int pos = 0;
            if ((pos = label.indexOf('@')) > 0)
                label = label.substring(0, pos);
            String c = row.get("c").toString();

            AutoCompleteOption option = new AutoCompleteOption(uri, label, c);

            /** Miro que no apareixi un mateix recurs per diferents classes **/
            if(lastUri == null || !lastUri.equals(uri)){
                options.add(option);
            }
            else if(lastUri.equals(uri)){
                if(menu.isChildOf(c,options.get(options.size()-1).getKlass())){
                    //System.out.println("REMOVE "+options.get(options.size()-1).getUri());
                    options.remove(options.size()-1);
                    options.add(option);
                }
                else if(!menu.isChildOf(options.get(options.size()-1).getKlass(),c))
                    options.add(option);
            }
            lastUri = uri;
        }

        if(options.size()>numResults){
            options = new ArrayList<AutoCompleteOption>(options.subList(0,numResults));
            /*options = (ArrayList<AutoCompleteOption>) options.subList(0,numResults);*/
        }

        for(AutoCompleteOption option : options){
            String obj = "{\"type\":\""+option.getKlass()+"\","+
            "\"focus\":{\"uri\":\""+option.getUri()+"\",\"label\":\""+option.getLabel()+"\"}}";

            try {
                obj = URLEncoder.encode(obj, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String link = request.getContextPath()+"/facets.jsp?q="+option.getKlass()+"#"+obj;

            //String link = request.getContextPath()+"/facets.jsp?q=SELECT ?r1 WHERE{?r1 a <"+option.getKlass().replace("#", "%23")+">}&o="+option.getUri().replace("#", "%23");
            output.append("{\"label\" : \""+option.getLabel().replace("\"","\\\"")+"\", \"link\" : \""+link+"\", \"c\" : \""+option.getKlass()+"\"},");
        }
        return output;
    }
}
