package net.rhizomik.rhizomer.autoia.manager;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import net.rhizomik.rhizomer.autoia.classes.HierarchyMenu;
import net.rhizomik.rhizomer.autoia.classes.HierarchyNode;
import net.rhizomik.rhizomer.autoia.classes.MenuConfig;
import net.rhizomik.rhizomer.util.FacetUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class HierarchyManager 
{

    private static Property classPartition = null;
    private static Property distinctSubjects = null;
	private static int MAX_DEPTH = 3;
	protected HierarchyMenu menu;
	protected HierarchyMenu out;
	protected List<OntClass> occurs;
	
	public HierarchyManager() {
		this.menu = new HierarchyMenu();
		this.occurs = new ArrayList<OntClass>();
	}
	
	public void setMenuConfig(MenuConfig config){
		this.menu.setConfig(config);
	}
	
	public void addUriBlackList(String uri){
		menu.addUriBlackList(uri);
	}
	
	public void addNamespaceBlackList(String namespace){
		menu.addNamespaceBlackList(namespace);
	}
		
	public void readModel(OntModel m){
		
		Iterator<OntClass> i = m.listHierarchyRootClasses()
        .filterDrop( new Filter<OntClass>() {
                      @Override
                      public boolean accept( OntClass r ) {
                          return r.isAnon();
                      }} );

		while (i.hasNext()) {
			OntClass c = i.next();
			occurs.add(c);
			HierarchyNode hn = new HierarchyNode(c.getURI());
			hn.setNumInstances(countInstances(c));
			menu.addNode(hn);
			getClass(c, hn);
		}		
	}
	
	private void getClass(OntClass c, HierarchyNode hnb){
		if (c.canAs( OntClass.class )) {
            for (Iterator<OntClass> i = c.listSubClasses(true);  i.hasNext(); ) {
                OntClass sub = i.next();
                if(!(sub.getURI()==null) && !sub.getURI().equals("http://www.w3.org/2002/07/owl#Nothing") && !occurs.contains(sub)){
                	boolean eqFound = false;
                	for (Iterator<OntClass> i2 = sub.listEquivalentClasses();  i2.hasNext(); ) {
                		OntClass eq = i2.next();
                		if(!eq.equals(sub) && occurs.contains(eq)){
                			eqFound = true;
                			HierarchyNode node = menu.getByUri(eq.getURI());
                			node.addAlias(sub.getURI());
                		}
                	}
    				occurs.add(sub);
                	if(!eqFound){
	                	HierarchyNode hn = new HierarchyNode(sub.getURI());
	    				hn.setNumInstances(countInstances(sub));
	    				menu.addChild(hnb, hn);
	    				getClass(sub, hn);
                	}
            	}
            }
        }
	}
	
	private int countInstances(OntClass c){
		int x = 0;
    	for(Iterator i = c.listInstances(true); i.hasNext(); ){
    		i.next();
    		x++;
    	}
    	return x;
	}

	public HierarchyMenu getHierarchyMenu() {
		return menu;
	}

    public HierarchyMenu generateMenu2(){
        return this.generateFullMenu2(7,7,null);
    }
	
	public HierarchyMenu generateFullMenu(String uri){
		return this.generateFullMenu(menu.getConfig().getNumItemsGlobal(),menu.getConfig().getNumItemsLocal(),uri);
	}

    public HierarchyMenu generateFullMenu2(int numItemsGlobal, int numItemsLocal, String uri){
        if(uri == null)
            uri = "http://www.w3.org/2002/07/owl#Thing";
        HierarchyNode base = menu.getFirst(); //getByUri(uri);
        out = new HierarchyMenu();
        menu.clearEmpty();
        base.sort("instances",10);
        List<HierarchyNode> childs = base.getChilds();
        if(numItemsGlobal < childs.size()){
            for(int i=0;i<childs.size();i++){
                HierarchyNode child = childs.get(i);
                if(i<numItemsGlobal)
                    out.addNode(child);
                else if(i==numItemsGlobal){
                    if(i==base.getChilds().size())
                        out.addNode(base.getChilds().get(i));
                    else{
                        HierarchyNode other = getOtherGlobal();
                        other.addChild(child);
                    }
                }
                else{
                    HierarchyNode other = getOtherGlobal();
                    other.addChild(child);
                }
            }
        }

        for(HierarchyNode child : out.getNodes()){
            generateSubMenus(child,numItemsLocal);
        }
        return out;
    }

    private void generateSubMenus(HierarchyNode node, int numItemsLocal){
        List<HierarchyNode> childs = node.getChilds();
        if(numItemsLocal < childs.size()){
            for(int i=0;i<childs.size();i++){
                HierarchyNode child = childs.get(i);
                if(i<numItemsLocal)
                    out.addNode(child);
                else if(i==numItemsLocal){
                    if(i==childs.size())
                        out.addNode(child);
                    else{
                        HierarchyNode other = getOtherLocal(node);
                        other.addChild(child);
                    }
                }
                else{
                    HierarchyNode other = getOtherLocal(node);
                    other.addChild(child);
                }
            }
        }
    }
	
	
	public HierarchyMenu generateFullMenu(int numItemsGlobal, int numItemsLocal, String uri){
		if(uri == null)
			uri = "http://www.w3.org/2002/07/owl#Thing";
		HierarchyNode base = menu.getFirst(); //getByUri(uri);
		menu.clearEmpty();
		menu.clearEmpty(); // Això és un parche temporal!!

				
		out = copyMenu(base);
		base = out.getByUri(uri);
		
		
		if(numItemsGlobal < base.getChilds().size()){
			while(base.getChilds().size()>numItemsGlobal){
				HierarchyNode other = getOtherGlobal();
				HierarchyNode min = this.getMinGroup(base);
				joinGroup(min,other);
			}
		}
		else{
			while(base.getChilds().size()<numItemsGlobal){
				HierarchyNode max = getMaxGroup(numItemsGlobal, base);
				if(max==null)
					break;
				divideNode(max, base);
				while(base.getChilds().size()>numItemsGlobal){
					HierarchyNode other = getOtherNode(max);
					HierarchyNode min = this.getMinGroup(base, max);
					joinGroup(min,other);
				}
			}
		}
		
		for(HierarchyNode node : out.getNodes())
			generateLocalMenu(node, numItemsLocal);
		
		out.sort(2);
		return out;
	}
	

	private void generateLocalMenu(HierarchyNode base, int numItemsLocal) {
		if(numItemsLocal < base.getChilds().size()){
			while(base.getChilds().size()>numItemsLocal){
				HierarchyNode other = getOtherLocal(base);
				HierarchyNode min = this.getMinGroup(base);
				joinGroup(min,other);
			}
		}
		else{
			while(base.getChilds().size()<numItemsLocal){
				HierarchyNode max = getMaxGroup(numItemsLocal, base);
				if(max==null)
					break;
				divideNode(max, base);
				while(base.getChilds().size()>numItemsLocal){
					HierarchyNode other = getOther(out,max,base);			
					HierarchyNode min = this.getMinGroup(base,max);
					joinGroup(min,other);
				}
			}			
		}
	}

	
	private HierarchyNode getOther(HierarchyMenu menu, HierarchyNode base, HierarchyNode parent){
		HierarchyNode other = menu.getByUri("Other#"+base.getUri());
		if(other == null){
			other = new HierarchyNode("Other#"+base.getUri(),true);
			out.addChild(parent, other);
		}
		return other;
	}	
	
	/*
	 * Obtindre un node "Other" local
	 */
	private HierarchyNode getOtherLocal(HierarchyNode base){
		HierarchyNode other = out.getByUri("Other#"+base.getUri());
		if(other == null){
			other = new HierarchyNode("Other#"+base.getUri(), true);
			out.addChild(base, other);
		}
		other.setLabel("Other "+base.getLabel());
		return other;
	}
	
	/*
	 * Obtindre un node "Other + uri"
	 */
	private HierarchyNode getOtherNode(HierarchyNode node){
		HierarchyNode other = out.getByUri("#Other#"+node.getUri());
		if(other == null){
			other = new HierarchyNode("#Other#"+node.getUri(),true);
			out.addNode(other);
		}
		other.setLabel("Other "+node.getLabel());
		return other;
	}	
	
	/*
	 * Obtindre un node "Other" global
	 */
	private HierarchyNode getOtherGlobal(){
		HierarchyNode other = out.getByUri("#Other");
		if(other == null){
			other = new HierarchyNode("#Other",true);
			out.addNode(other);
		}
		return other;
	}
		
	private void joinGroup(HierarchyNode node, HierarchyNode other){
		HierarchyNode parent = node.getParent();
		parent.deleteChild(node);
		other.addChild(node);
	}
	
	
	private HierarchyNode getMinGroup(HierarchyNode base, HierarchyNode lastParent) {
		HierarchyNode min = null;
		int minSize = 0;
		for(HierarchyNode node : base.getChilds()){
			if(minSize == 0 && node.getLastParent()==lastParent){
				minSize = node.getNumInstances();
				min = node;
			}
			if(node.getNumInstances()<minSize && node.getLastParent()==lastParent){
				min = node;
				minSize = node.getNumInstances();
			}
		}
		return min;
	}	
	
	
	private HierarchyNode getMinGroup(HierarchyNode base){
		HierarchyNode min = null;
		int minSize = 0;
		for(HierarchyNode node : base.getChilds()){
			if(minSize == 0 && !node.isAbstractNode()){
				minSize = node.getNumInstances();
				min = node;
			}
			if(!node.isAbstractNode() && node.getNumInstances()<minSize){
				min = node;
				minSize = node.getNumInstances();
			}
		}
		return min;
	}	
	
	public void divideNode(HierarchyNode node, HierarchyNode parent){	
		for(HierarchyNode child : node.getChilds()){
			child.setLastParent(node);
			parent.addChildBeforeNode(child, node);
		}
		out.deleteUri(node.getUri());
		parent.deleteChild(node);
	}
	

	private HierarchyNode getMaxGroup(int numItems, HierarchyNode base){
		HierarchyNode max = null;
		int maxSize = 0;
		for(HierarchyNode node : base.getChilds()){
			if(node.getNumInstances()>maxSize && node.getChilds().size()>0){
				max = node;
				maxSize = node.getNumInstances();
			}
		}
		return max;
	}
		
	private HierarchyMenu copyMenu(HierarchyNode base){
		HierarchyMenu out = new HierarchyMenu(base);
		out.setConfig(menu.getConfig());
		for(HierarchyNode node : base.getChilds()){
			HierarchyNode copy = new HierarchyNode(node);
			out.addNode(copy);
			copyChilds(out, copy, node);
		}
		return out;
	}
	
	private void copyChilds(HierarchyMenu menu, HierarchyNode copy, HierarchyNode node){
		for(HierarchyNode child : node.getChilds()){
			HierarchyNode childCopy = new HierarchyNode(child);
			menu.addChild(copy, childCopy);
			copyChilds(menu, childCopy, child);
		}
	}
	
	
	public void readXML(String path) throws ParserConfigurationException, SAXException, IOException{
		File file = new File(path);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		NodeList nodeLst = doc.getElementsByTagName("branch");
		for (int s = 0; s < nodeLst.getLength(); s++) {
		    Node xmlNode = nodeLst.item(s);
		    HierarchyNode node = getXMLNode(xmlNode);
		    menu.addNode(node);
		    getChildNodes(xmlNode,node);
		}
	}
	
	private HierarchyNode getXMLNode(Node xmlNode){
		String uri = xmlNode.getAttributes().getNamedItem("uri").getTextContent();
	    int instances = Integer.parseInt(xmlNode.getAttributes().getNamedItem("instances").getTextContent());
		String label = xmlNode.getAttributes().getNamedItem("label").getTextContent();
		
		//Parxe temporal
		label = FacetUtil.makeLabel(label);
		
	    HierarchyNode node = new HierarchyNode(uri);
	    node.setLabel(label);
	    node.setNumInstances(instances);
	    return node;
	}
	
	
	private void getChildNodes(Node xmlNode, HierarchyNode node){
		NodeList childNodes = xmlNode.getChildNodes();
	    for(int i=0; i<childNodes.getLength(); i++){
		    Node childNode = childNodes.item(i);
		    if(childNode.getNodeName().equals("node")){
		    	HierarchyNode child = getXMLNode(childNode);
		    	menu.addChild(node, child);
		    	getChildNodes(childNode, child);
		    }
	    }
	}	
	
	public void setMaxDepth(int maxDepth)
	{
		MAX_DEPTH = maxDepth;
	}
	
	public void writeXMLFile(String filename) throws ParserConfigurationException, SAXException, IOException, TransformerException{
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element root = doc.createElement("tree");
        doc.appendChild(root);
        
		for(HierarchyNode node : menu.getNodes()){
			Element branch = doc.createElement("branch");
			branch.setAttribute("uri", node.getUri());
			branch.setAttribute("instances", String.valueOf(node.getOwnedInstances()));
			branch.setAttribute("label", node.getLabel());
            root.appendChild(branch);
            writeNode(doc, branch, node, 1);
		}
		
		TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new File(filename));
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
	
	}
	
	public void writeNode(Document doc, Element xmlNode, HierarchyNode node, int depth){
		if (depth <= MAX_DEPTH)
		{
			for(HierarchyNode child : node.getChilds()){
				Element xmlChild = doc.createElement("node");
				xmlChild.setAttribute("uri", child.getUri());
				xmlChild.setAttribute("instances", String.valueOf(child.getOwnedInstances()));
				xmlChild.setAttribute("label", child.getLabel());
		        xmlNode.appendChild(xmlChild);
		        writeNode(doc, xmlChild, child, depth+1);
			}
		}
	}

    public void writeVoid(String filename) throws FileNotFoundException {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("void","http://rdfs.org/ns/void#");
        Resource dataset = model.createResource("http://rhizomik.net/dataset"); //Change for dataset uri
        dataset.addProperty(RDF.type,"http://rdfs.org/ns/void#Dataset");
        //Property p = model.createProperty("http://rdfs.org/ns/void#classes");
        //dataset.addProperty(p,Integer.toString(menu.getNumNodes()), XSDDatatype.XSDinteger);
        Property classPartition = model.createProperty("http://rdfs.org/ns/void#classPartition");
        Property distinctSubjects = model.createProperty("http://rdfs.org/ns/void#distinctSubjects");

        for(HierarchyNode node : menu.getNodes()){
            Resource klass = model.createResource(node.getUri());
            klass.addProperty(distinctSubjects,Integer.toString(node.getNumInstances()),XSDDatatype.XSDinteger);
            klass.addProperty(RDFS.label,node.getLabel());
            dataset.addProperty(classPartition,klass);
            writeVoidNode(model, klass, node);
        }

        PrintStream p = new PrintStream(filename);

        //model.write(System.out,"RDF/XML-ABBREV");
        model.write(p, "RDF/XML-ABBREV");
    }

    public void writeVoidNode(Model model, Resource resource, HierarchyNode node){
        Property distinctSubjects = model.createProperty("http://rdfs.org/ns/void#distinctSubjects");
        Property classPartition = model.createProperty("http://rdfs.org/ns/void#classPartition");
        for(HierarchyNode child : node.getChilds()){
            Resource klass = model.createResource(child.getUri());
            klass.addProperty(distinctSubjects,Integer.toString(child.getNumInstances()),XSDDatatype.XSDinteger);
            klass.addProperty(RDFS.label,child.getLabel());
            resource.addProperty(classPartition,klass);
            writeVoidNode(model, klass, child);
        }
    }

    public void readVoid(String path){
        InputStream in = FileManager.get().open(path);
        Model model = ModelFactory.createDefaultModel();
        model.read(in,null);
        Resource dataset = model.getResource("http://rhizomik.net/dataset");
        classPartition = model.createProperty("http://rdfs.org/ns/void#classPartition");
        distinctSubjects = model.createProperty("http://rdfs.org/ns/void#distinctSubjects");

        StmtIterator iter = dataset.listProperties(classPartition);
        while (iter.hasNext()) {
            Resource r = iter.nextStatement().getResource();
            HierarchyNode node = getVoidNode(r);
            menu.addNode(node);
            getChildNodesVoid(r, node);
        }
    }

    private HierarchyNode getVoidNode(Resource r){
        String uri = r.getURI();
        int instances = r.getProperty(distinctSubjects).getInt();
        String label = r.getProperty(RDFS.label).getString();
        HierarchyNode node = new HierarchyNode(uri);
        node.setLabel(label);
        node.setNumInstances(instances);
        return node;
    }

    private void getChildNodesVoid(Resource r, HierarchyNode node){
        StmtIterator iter = r.listProperties(classPartition);
        while (iter.hasNext()) {
            Resource rchild = iter.nextStatement().getResource();
            HierarchyNode child = getVoidNode(rchild);
            menu.addChild(node, child);
            getChildNodesVoid(rchild, child);
        }
    }
	

}
