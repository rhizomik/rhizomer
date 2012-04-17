package net.rhizomik.rhizomer.autoia.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.logging.Logger;

import net.rhizomik.rhizomer.agents.RhizomerRDF;
import net.rhizomik.rhizomer.autoia.classes.HierarchyNode;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import net.rhizomik.rhizomer.store.MetadataStore;


public class HierarchyManagerSPARQL extends HierarchyManager 
{
    private static final Logger log = Logger.getLogger(HierarchyManagerSPARQL.class.getName());
	private String NL = System.getProperty("line.separator");
    
	protected String queryForRoots =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT DISTINCT ?root ?label"+NL+
        "WHERE {"+NL+
        "   ?root rdf:type ?class. FILTER (?class=owl:Class || ?class=rdfs:Class)"+NL+
        "   OPTIONAL {"+NL+
        "       ?root rdfs:subClassOf ?super."+NL+
        "       FILTER (?root!=?super && ?super!=owl:Thing &&?super!=rdfs:Resource && !isBlank(?super))"+NL+
        "   }"+NL+
        "   OPTIONAL { ?root rdfs:label ?label FILTER(LANG(?label)='en' || LANG(?label)='')}"+NL+
        "   FILTER (!bound(?super) && isURI(?root) && !isBlank(?root) && ?root!=owl:Thing )"+NL+
        "}";
	
	protected String queryForRootsMinus = 
	"PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
	"PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n" + 
	"PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
	"SELECT DISTINCT ?root ?label \n" + 
	"WHERE\n" + 
	"  { \n" + 
	"    ?root rdf:type ?class. FILTER ( ( ?class = owl:Class || ?class = rdfs:Class ) && !isBlank(?root) && ?root!=owl:Thing && isURI(?root) )\n" +
        "    OPTIONAL { ?root rdfs:label ?label FILTER(LANG(?label)='en' || LANG(?label)='')}\n" +
		"    MINUS\n" + 
		"    { \n" + 
		"        ?root rdfs:subClassOf ?super. FILTER ( ?root!=?super && ?super!=owl:Thing && ?super!=rdfs:Resource && !isBlank(?super) )\n" + 
		"    }\n" + 
		"  }";
	
	protected String queryForDirectSubs =
   	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT DISTINCT ?sub ?label"+NL+
        "WHERE {"+NL+
        "   ?sub rdfs:subClassOf <%1$s>"+NL+
        "   OPTIONAL {"+NL+
        "       ?sub rdfs:subClassOf ?sub2. ?sub2 rdfs:subClassOf <%1$s>."+NL+
        "       OPTIONAL { <%1$s> owl:equivalentClass ?sub3 . ?sub3 rdfs:subClassOf ?sub2  }"+NL+
        "       FILTER (?sub!=?sub2 && ?sub2!=<%1$s> && !isBlank(?sub2) && !bound(?sub3) )"+NL+
        "   }"+NL+
        "   OPTIONAL { ?sub rdfs:label ?label FILTER(LANG(?label)='en' || LANG(?label)='')}"+NL+
        "   FILTER (!isBlank(?sub) && !bound(?sub2))"+NL+
        "}";
	
    protected String queryForCount = 
    	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+NL+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>"+NL+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+NL+
        "SELECT ?class (COUNT(?x) AS ?n)"+NL+
        "WHERE {"+NL+
        "   ?x a ?class"+NL+
		"}"+NL+
		"GROUP BY ?class";
    
    public HierarchyManagerSPARQL(){
    	super();
    }
    
	public void readModel() throws SQLException {
        ArrayList<HierarchyNode> roots = getRootClasses();
        
        for(HierarchyNode node : roots){
			menu.addNode(node);
			getSubClasses(node);
		}
        countInstances();
    }
    
    private ArrayList<HierarchyNode> getRootClasses(){
		ArrayList<HierarchyNode> roots = new ArrayList<HierarchyNode>();
		
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
    
    private void getSubClasses(HierarchyNode node)
    {
    	populateChilds(node);
        
        for(HierarchyNode child : node.getChilds()){
        	getSubClasses(child);
        }
    }
    
    private void populateChilds(HierarchyNode node)
    {
    	StringBuilder queryString = new StringBuilder();
        Formatter f = new Formatter(queryString);
        Object[] vars = {node.getUri()};
        f.format(queryForDirectSubs, vars);
        try
        {
	        ResultSet results = RhizomerRDF.instance().querySelect(queryString.toString(), MetadataStore.REASONING);
	        while(results.hasNext()){
	        	QuerySolution row = results.next();
	        	String uri = row.get("sub").toString();
	        	if(!uri.equals("http://www.w3.org/2002/07/owl#Nothing") && !uri.equals(node.getUri()))
	        	{
	        	    HierarchyNode child = menu.getByUri(uri);
	        	    if (child == null)
	        		child = new HierarchyNode(uri);
	        	    if (row.contains("label"))
	        	    	child.setLabel(row.get("label").toString());
	        	    menu.addChild(node, child);
	        	}
	        }
        }
	    catch(Exception e)
	    { System.out.println(e); }
    }
    
    private void countInstances(){
        ResultSet results = RhizomerRDF.instance().querySelect(queryForCount, MetadataStore.INSTANCES);
        // The second var is the count value
        String countVar = results.getResultVars().get(1);
        while(results.hasNext())
        {
        	QuerySolution row = results.next();
        	if (!row.contains("class"))
        		continue;
        	String uri = row.get("class").toString();
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
    
    private void calculateInstances(HierarchyNode node){
		int childInstances = 0;
		for(HierarchyNode child : node.getChilds()){
			childInstances += child.getOwnedInstances();
		}
		int total = node.getOwnedInstances()-childInstances;
		if(total<0) 
			total = 0;
		node.setNumInstances(total);
		for(HierarchyNode child : node.getChilds()){
			childInstances += child.getOwnedInstances();
			calculateInstances(child);
		}
	}    

}
