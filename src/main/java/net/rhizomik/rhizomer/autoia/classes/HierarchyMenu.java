package net.rhizomik.rhizomer.autoia.classes;
import javax.servlet.http.HttpServletRequest;
import java.util.*;


public class HierarchyMenu {
	
	public static String ALPHABETICAL = "alphabetical";
	public static String INSTANCES = "instances";

    protected HierarchyNode first;
	protected Map<String, HierarchyNode> map;
	protected MenuConfig config;
    protected int numInstances;
	
	public void setConfig(MenuConfig config) {
		this.config = config;
	}

    public int getNumNodes(){
        return map.size();
    }

	public HierarchyMenu() {
		first = new HierarchyNode("http://www.w3.org/2002/07/owl#Thing");
		map = new HashMap<String, HierarchyNode>();
		map.put("http://www.w3.org/2002/07/owl#Thing", first);
	}
	
	public HierarchyMenu(MenuConfig config){
		first = new HierarchyNode("http://www.w3.org/2002/07/owl#Thing");
		map = new HashMap<String, HierarchyNode>();
		map.put("http://www.w3.org/2002/07/owl#Thing", first);
		this.config = config;
	}
	
	public MenuConfig getConfig() {
		return config;
	}

    public void calculateNumInstances(){
        this.numInstances = first.getNumInstances();
    }

    public int getNumInstances(){
        return this.numInstances;
    }

    public List<String> getInitials(){
        List<String> initials = new ArrayList<String>();
        List<HierarchyNode> nodes = new ArrayList<HierarchyNode>(map.values());
        java.util.Collections.sort(nodes);
        for(HierarchyNode node : nodes){
            if(node.getNumInstances()>0){
                String label = node.getLabel();
                String initial = label.substring(0,1);
                if(!initials.contains(initial))
                    initials.add(initial);
            }
        }
        return initials;
    }

    public Map<String, HierarchyNode> getMapNodes(){
        return map;
    }

    public HierarchyNode getFirst() {
        return first;
    }
	
	public HierarchyMenu(HierarchyNode first){
		this.first = new HierarchyNode(first);
		map = new HashMap<String, HierarchyNode>();
		map.put(first.getUri(), this.first);
	}
	
	
	public void addUriBlackList(String uri){
		config.getUriBlackList().add(uri);
	}
	
	public void addUriWhiteList(String uri){
		config.getUriWhiteList().add(uri);
	}
	
	public void addNamespaceBlackList(String namespace){
		config.getNamespaceBlackList().add(namespace);
	}
	
	public void addChild(HierarchyNode parent, HierarchyNode node){
		if (!parent.getChilds().contains(node))
		{
	    		parent.addChild(node);
                node.setHierarchyMenu(this);
	    		map.put(node.getUri(), node);
	    	}
	}
	
	public void deleteUri(String uri){
		map.remove(uri);
	}
	
	public void addNode(HierarchyNode node){
		first.addChild(node);
        node.setHierarchyMenu(this);
		map.put(node.getUri(), node);
	}
	
	public List<HierarchyNode> getNodes(){
		return first.getChilds();
	}

    public List<HierarchyNode> getAllNodes(){
        List<HierarchyNode> list = new ArrayList<HierarchyNode>(map.values());
        return list;
    }
	
	public HierarchyNode getByUri(String uri){
		return map.get(uri);
	}
	
	
	public void sort(int levels){		
		this.first.sort(this.config.getSort(), levels);
	}

    public void sortTopK(){
        this.first.sortTopK();
    }
	
	public void print(int levels){			
		for(HierarchyNode child : this.first.getChilds()){
			child.print(levels,1);
		}
	}
	
	public StringBuffer printAsUl(HttpServletRequest req, int levels)
	{
		StringBuffer out = new StringBuffer();
		String previousURI = "";
		for(HierarchyNode child : first.getChilds())
		{
		    if (!child.getUri().equals(previousURI)) // Patch to avoid duplicate nodes when multiple parents for node
		    {
			out.append("<li class=\"yui3-menuitem\">");
			child.printAsUl(req, levels, out);
			out.append("</li>");
			previousURI = child.getUri();
		    }
		}
		return out;
	}

    public StringBuffer printAsSitemap(HttpServletRequest req, String mode)
    {
        StringBuffer out = new StringBuffer();
        String previousURI = "";
        if(mode.equals("full"))
            out.append("<ul id=\"tree\">");
        else
            out.append("<ul>");
        java.util.Collections.sort(first.getChilds());
        for(HierarchyNode child : first.getChilds())
        {
            if (!child.getUri().equals(previousURI)) // Patch to avoid duplicate nodes when multiple parents for node
            {
                //out.append("<li>");
                if(mode.equals("full")){
                    child.printAsFullSitemap(req ,out);
                }
                else
                    child.printAsSitemap(req, out);
                previousURI = child.getUri();
            }
        }
        return out;
    }
	
	public StringBuffer printAsUl(HttpServletRequest req, int levels, String property)
	{
		StringBuffer out = new StringBuffer();
		String previousURI = "";
		for(HierarchyNode child : first.getChilds())
		{
		    if (!child.getUri().equals(previousURI)) // Patch to avoid duplicate nodes when multiple parents for node
		    {
			out.append("<li class=\"yui3-menuitem\">");
			child.printAsUl(req, levels, out, property);
			out.append("</li>");
			previousURI = child.getUri();
		    }
		}
		return out;
	}

    public String printAsJSON(){
        StringBuffer out = new StringBuffer();
        out.append("{\"id\": \"root\", \"name\": \"Classes\", \"$area\": 0, \"data\": {\"instances\": 0, \"size\": 0}, \"children\": [");
        for(HierarchyNode node : first.getChilds()){
            String uri = node.getUri();
            int instances = node.getNumInstances();
            String label = node.getLabel();
            int pos;
            if ((pos = label.indexOf('@')) > 0)
                label = label.substring(0, pos);

            out.append("\n{ data :{\"parent\":\"root\", \"instances\": "+instances+", \"$area\": "+instances+"}, \"id\": \""+uri+"\" , \"name\":\""+label.replace("'","\'")+"\"," +

                    "\"children\":[");

            out.append(node.printAsJSON());
            out.append("]},");
        }
        out.append("]}");

        return out.toString();
    }
	
	private boolean isUriInNamespace(HierarchyNode node){
		if (this.config!=null)
		{	
			for(String namespace : this.config.getNamespaceBlackList()){
				if(node.getUri().contains(namespace) && !this.config.getUriWhiteList().contains(node.getUri()))
					return true;
			}
		}
		return false;
	}
	
	public void clearEmpty(){
		Iterator<HierarchyNode> i = this.first.getChilds().iterator();
		while(i.hasNext()){
			HierarchyNode node = i.next();
			if((this.config!=null && this.config.getUriBlackList().contains(node.getUri()) && !this.config.getUriWhiteList().contains(node.getUri()) ) 
			    || isUriInNamespace(node) || isEmpty(node) ){
				map.remove(node.getUri());				
				i.remove();
			}
		}
	}
	
	private void removeEmptyNode(HierarchyNode node){
		if(node.getOwnedInstances()==0 && node.countChildsWithInstances()==1){
			HierarchyNode child = node.getChildWithInstances();
			node.setUri(child.getUri());
			node.setLabel(child.getLabel());
			node.setNumInstances(child.getOwnedInstances());			
			node.aliases = child.aliases;
			node.childs = child.childs;
			removeEmptyNode(node);
		}
	}
	
	
	public boolean isEmpty(HierarchyNode node){
		if(node.getNumInstances()==0){
			return true;
		}
		else{
			Iterator<HierarchyNode> i = node.getChilds().iterator();
			while(i.hasNext()){
				HierarchyNode tmp = i.next();
				if((this.config!=null && this.config.getUriBlackList().contains(tmp.getUri()) && !this.config.getUriWhiteList().contains(node.getUri()))
				    || isUriInNamespace(node) || isEmpty(tmp))
				{
					map.remove(tmp.getUri());
					i.remove();
				}
			}			
			removeEmptyNode(node);
		}
		return false;
	}

    public boolean isChildOf(String childURI, String parentURI){
        if(childURI.equals("http://www.w3.org/2002/07/owl#Thing"))
            return false;
        if(parentURI.equals("http://www.w3.org/2002/07/owl#Thing"))
            return true;
        HierarchyNode child = this.getByUri(childURI);
        HierarchyNode parent = this.getByUri(parentURI);
        if(parent==null)
            return true;
        if(child==null){
            return false;
        }
        if(child.getParent().getUri().equals(parent.getUri())){
            //System.out.println(child.getLabel() + " is child of "+parent.getLabel());
            return true;
        }

        while(!child.getUri().equals("http://www.w3.org/2002/07/owl#Thing")){
            if(child.getUri().equals(parent.getUri())){
                //System.out.println(child.getLabel() + " is child of "+parent.getLabel());
                return true;
            }
            child = child.getParent();
        }
        return false;
    }

    public List<HierarchyNode> getBreadcrumbs(String uri){
        List<HierarchyNode> breadcrumbs = new ArrayList<HierarchyNode>();
        HierarchyNode node = this.getByUri(uri);
        HierarchyNode parent = node.getParent();
        while(parent.getUri()!="http://www.w3.org/2002/07/owl#Thing"){
            breadcrumbs.add(0, parent);
            parent = parent.getParent();
        }
        return breadcrumbs;
    }

}