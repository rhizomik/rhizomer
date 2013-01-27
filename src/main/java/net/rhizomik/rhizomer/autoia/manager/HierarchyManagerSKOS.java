package net.rhizomik.rhizomer.autoia.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Properties;
import java.util.logging.Logger;

import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.autoia.classes.HierarchyNode;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import net.rhizomik.rhizomer.store.MetadataStore;


public class HierarchyManagerSKOS extends HierarchyManager 
{
    private static final Logger log = Logger.getLogger(HierarchyManagerSKOS.class.getName());
	private String NL = System.getProperty("line.separator");
    
	protected String queryForRoots =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT DISTINCT ?root ?label"+NL+
        "WHERE {"+NL+
        "   ?root rdf:type skos:Concept"+NL+
        "   OPTIONAL {"+NL+
        "       ?root skos:broader ?super1."+NL+
        "       FILTER (?root!=?super1 && !isBlank(?super1))"+NL+
        "   }"+NL+
        "   OPTIONAL {"+NL+
        "       ?super2 skos:narrower ?root."+NL+
        "       FILTER (?root!=?super2 && !isBlank(?super2))"+NL+
        "   }"+NL+
        "   OPTIONAL { ?root skos:prefLabel ?label FILTER(LANG(?label)='en')}"+NL+
        "   FILTER (!bound(?super1) && !bound(?super2) && isURI(?root) && !isBlank(?root) )"+NL+
        "}";
	
	protected String queryForDirectSubs =
   	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT DISTINCT ?sub ?label"+NL+
        "WHERE {"+NL+
        "{  <%1$s> skos:narrower ?sub"+NL+
        "   OPTIONAL {"+NL+
        "       <%1$s> skos:narrower ?sub2. ?sub2 skos:narrower ?sub."+NL+
        "       FILTER (?sub!=?sub2 && ?sub2!=<%1$s> && !isBlank(?sub2) )"+NL+
        "   }"+NL+
        "   OPTIONAL { ?sub skos:prefLabel ?label FILTER(LANG(?label)='en')}"+NL+
        "   FILTER (!bound(?sub2))"+NL+
        "} UNION"+NL+
	    "{  ?sub skos:broader <%1$s>"+NL+
	    "   OPTIONAL {"+NL+
	    "       ?sub skos:broader ?sub2. ?sub2 skos:broader <%1$s>."+NL+
	    "       FILTER (?sub!=?sub2 && ?sub2!=<%1$s> && !isBlank(?sub2) )"+NL+
	    "   }"+NL+
	    "   OPTIONAL { ?sub skos:prefLabel ?label FILTER(LANG(?label)='en')}"+NL+
	    "   FILTER (!bound(?sub2))"+NL+
	    "} }";
	
    protected String queryForCountAll = 
    	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "PREFIX dc: <http://purl.org/dc/elements/1.1/>"+NL+
        "SELECT DISTINCT ?concept (COUNT(?x) AS ?n)"+NL+
        "WHERE {"+NL+
        "   ?x ?subject ?concept."+NL+
        "   FILTER(?subject=dc:subject || ?subject=skos:subject)"+NL+
		"}"+NL+
		"GROUP BY ?concept";
    
    protected String queryForCount = 
    	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "PREFIX dc: <http://purl.org/dc/elements/1.1/>"+NL+
        "PREFIX dct:  <http://purl.org/dc/terms/>"+NL+
        "SELECT (COUNT(?x) AS ?n)"+NL+
        "WHERE {"+NL+
        "   ?x ?subject <%1$s>."+NL+
        "   FILTER(?subject=dc:subject || ?subject=skos:subject || ?subject=dct:subject)"+NL+
		"} GROUP BY ?x";
    
    public HierarchyManagerSKOS(){
    	super();
    }
    
	public void readModel() throws SQLException {
        ArrayList<HierarchyNode> roots = getRootClasses();
        
        for(HierarchyNode node : roots){
			menu.addNode(node);
			getSubClasses(node, 1);
		}
        countInstancesAll();
    }
    
    private ArrayList<HierarchyNode> getRootClasses(){
		ArrayList<HierarchyNode> roots = new ArrayList<HierarchyNode>();
		//HierarchyNode node = new HierarchyNode("http://dbpedia.org/resource/Category:Main_topic_classifications");
		//roots.add(node);
        ResultSet results = RhizomerRDF.instance().querySelect(queryForRoots, MetadataStore.REASONING);
        while(results.hasNext()){
			   QuerySolution row = results.next();
			   String uri = row.get("root").toString();
			   HierarchyNode node = new HierarchyNode(uri);
			   if (row.contains("label"))
				   node.setLabel(row.get("label").toString());
			   roots.add(node);
		}
        return roots;
    }
    
    private void getSubClasses(HierarchyNode node, int depth)
    {
    	//if (depth < 2)
    	//{	
	    	populateChilds(node);
	        
	        for(HierarchyNode child : node.getChilds()){
	        	getSubClasses(child, depth+1);
	        }
    	//}
    }
    
    private void populateChilds(HierarchyNode node)
    {
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {node.getUri()};
        f.format(queryForDirectSubs, vars);    
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), MetadataStore.REASONING);
        while(results.hasNext()){
        	QuerySolution row = results.next();
        	String uri = row.get("sub").toString();
        	if(!uri.equals("http://www.w3.org/2002/07/owl#Nothing") && !uri.equals(node.getUri()) &&
        		menu.getByUri(uri)==null)
        	{
        		HierarchyNode child = new HierarchyNode(uri);
        		if (row.contains("label"))
 				   child.setLabel(row.get("label").toString());
        		menu.addChild(node, child);
        	}
        }
    }
    
    private void countInstancesAll(){
        ResultSet results = RhizomerRDF.instance().querySelect(queryForCountAll, MetadataStore.INSTANCES);
        // The second var is the count value
        String countVar = results.getResultVars().get(1);
        while(results.hasNext())
        {
        	QuerySolution row = results.next();
        	if (!row.contains("concept"))
        		continue;
        	String uri = row.get("concept").toString();
        	Literal numInstances = row.getLiteral(countVar);
        	HierarchyNode node = menu.getByUri(uri);
        	if(node!=null && numInstances!=null)
        	{
        		node.setNumInstances(numInstances.getInt());
        	}
        }
        for(HierarchyNode node : menu.getNodes()){
			calculateInstances(node);
		}
    }

    private void countInstances()
    {    
        for(HierarchyNode node : menu.getNodes())
        {
			calculateInstances(node);
		}
    }
    
    private void calculateInstances(HierarchyNode node)
    {
		int descendantsInstances = 0;
		for(HierarchyNode child : node.getChilds())
		{
			calculateInstances(child);
			descendantsInstances += child.getOwnedInstances();
		}
		node.setNumInstances(countInstancesNode(node.getUri())+descendantsInstances);
	}
    
    private int countInstancesNode(String uri)
    {
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {uri};
        f.format(queryForCount, vars);    
        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), MetadataStore.REASONING);
	    // The second var is the count value
        String countVar = results.getResultVars().get(0);
        int count = 0;
        while(results.hasNext())
        {
        	QuerySolution row = results.next();
        	if (!row.contains(countVar))
        		continue;
        	Literal numInstances = row.getLiteral(countVar);
        	if(numInstances!=null)
        		count = numInstances.getInt();
        }
        return count;
    } 
    
    //Remove those trees in the menu that do not have any instance (in the root or descendants)
    private void prune()
    {
    	menu.clearEmpty();
    }
    
    public static void main(String[] args) throws Exception 
    {
    	if (args.length < 1)
    	{
    		System.err.println("Usage: SKOSMenu.sh (build|recount|prune)");
    		System.exit(-1);
    	}
    		
    	Properties props = new Properties();
    	props.put("store_class", "net.rhizomik.rhizomer.store.virtuoso.VirtuosoStore");
    	props.put("db_graph", "http://dbpedia.org");
    	//props.put("db_schema", "http://dbpedia.org/schema/");
    	props.put("db_url", "jdbc:virtuoso://localhost:1111");
    	props.put("db_user", "rhizomer");
    	props.put("db_pass", "rhizomer");
    	RhizomerRDF.instance().addStore(props);
    	
    	int menuHash = props.getProperty("db_graph").hashCode();
    	HierarchyManagerSKOS manager = new HierarchyManagerSKOS();
    	

    	if (args[0].equalsIgnoreCase("build"))
    		manager.readModel();
    	else if (args[0].equalsIgnoreCase("recount"))
    	{
    		manager.readXML("menu-"+menuHash+".skos.xml");
    		manager.countInstances();
    	}
    	else if (args[0].equalsIgnoreCase("prune") && args.length > 1)
    	{
    		manager.readXML("menu-"+menuHash+".skos.xml");
    		manager.setMaxDepth(Integer.parseInt(args[1]));
    		manager.prune();
    	}
    	else
    	{
    		System.err.println("Usage: SKOSMenu.sh (build|recount|prune)");
    		System.exit(-1);
    	}
		manager.writeXMLFile("menu-"+menuHash+".skos.xml");
	}
}
