package net.rhizomik.rhizomer.autoia.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Properties;
import java.util.logging.Level;
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
        "   { ?i rdf:type ?root } UNION { ?c rdfs:subClassOf ?root }"+NL+
        "   OPTIONAL {"+NL+
        "       ?root rdfs:subClassOf ?super."+NL+
        "       FILTER (?root!=?super && ?super!=owl:Thing &&?super!=rdfs:Resource && !isBlank(?super))"+NL+
        "   }"+NL+
        "   OPTIONAL { ?root rdfs:label ?label FILTER(LANG(?label)='en' || LANG(?label)='')}"+NL+
        "   FILTER (!bound(?super) && !isBlank(?root) && isURI(?root) && ?root!=owl:Thing )"+NL+
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
        "SELECT DISTINCT ?class (COUNT(?x) AS ?n)"+NL+
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
    
    protected ArrayList<HierarchyNode> getRootClasses(){
		ArrayList<HierarchyNode> roots = new ArrayList<HierarchyNode>();
		
        ResultSet results = RhizomerRDF.instance().querySelect(queryForRoots, MetadataStore.REASONING);
        while(results.hasNext()){
			   QuerySolution row = results.next();
			   String uri = row.get("root").toString();
               log.log(Level.INFO, "Root class: "+uri);
			   HierarchyNode node = new HierarchyNode(uri);
			   if (row.contains("label"))
				   node.setLabel(row.get("label").toString());
			   roots.add(node);
		}
        log.log(Level.INFO, "End roots");
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
    
    protected void countInstances(){
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
        // Això seria per si no hi hagués inferència i s'han de sumar instàncies dels fills.
        for(HierarchyNode node : menu.getNodes()){
			calculateInstances(node);
		}
    }
    
    private void calculateInstances(HierarchyNode node){
		int childInstances = 0;

        for(HierarchyNode child : node.getChilds()){
            calculateInstances(child);
            childInstances += child.getOwnedInstances();
        }
		int total = node.getOwnedInstances()+childInstances;
		node.setNumInstances(total);
	}

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.err.println("Usage: ClassMenu.sh (build|recount)");
            System.exit(-1);
        }

        Properties props = new Properties();
        props.put("store_class", "net.rhizomik.rhizomer.store.virtuoso.VirtuosoStore");
        props.put("db_graph", "http://rhizomik.net/");
        //props.put("db_schema", "http://dbpedia.org/schema/");
        props.put("db_url", "jdbc:virtuoso://localhost:1111");
        props.put("db_user", "user");
        props.put("db_pass", "password");
        RhizomerRDF.instance().addStore(props);

        int menuHash = props.getProperty("db_graph").hashCode();
        HierarchyManagerSPARQL manager = new HierarchyManagerSPARQL();


        if (args[0].equalsIgnoreCase("build"))
            manager.readModel();
        else if (args[0].equalsIgnoreCase("recount"))
        {
            manager.readXML("menu-"+menuHash+".xml");
            manager.countInstances();
        }
        else
        {
            System.err.println("Usage: ClassMenu.sh (build|recount)");
            System.exit(-1);
        }
        manager.writeXMLFile("menu-"+menuHash+".xml");
    }
}
