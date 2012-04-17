package net.rhizomik.rhizomer.autoia.classes;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


public class HierarchyMenu {
	
	public static String ALPHABETICAL = "alphabetical";
	public static String INSTANCES = "instances";

    protected HierarchyNode first;
	protected Map<String, HierarchyNode> map;
	protected MenuConfig config;
	
	public void setConfig(MenuConfig config) {
		this.config = config;
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
	    		map.put(node.getUri(), node);
	    	}
	}
	
	public void deleteUri(String uri){
		map.remove(uri);
	}
	
	public void addNode(HierarchyNode node){
		first.addChild(node);
		map.put(node.getUri(), node);
	}
	
	public List<HierarchyNode> getNodes(){
		return first.getChilds();
	}
	
	public HierarchyNode getByUri(String uri){
		return map.get(uri);
	}
	
	
	public void sort(int levels){		
		this.first.sort(this.config.getSort(), levels);
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

}