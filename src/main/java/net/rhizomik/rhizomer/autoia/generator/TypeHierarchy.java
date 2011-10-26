package net.rhizomik.rhizomer.autoia.generator;

import java.util.ArrayList;
import java.util.HashMap;

public class TypeHierarchy {
	
	private HashMap<String,TypeNode> nodes;
	
	public TypeHierarchy(){
		this.nodes = new HashMap<String, TypeNode>();
		TypeNode stringNode = new TypeNode("string",null);
		this.nodes.put("string", stringNode);
		addChild("string","number");
		addChild("number","integer");
		addChild("number","double");
		addChild("string","resource");
		addChild("string","date");
		/* Afegir datetime, time, geo point, etc*/
		//addChild("date","datetime");
		//addChild("date","time");
	}
	
	public String getNearestParent(String type1, String type2){
		if(type1.equals("string") || type2.equals("string"))
			return "string";
		if(type1.equals(type2))
			return type1;
		TypeNode node1 = nodes.get(type1);
		TypeNode node2 = nodes.get(type2);
		if(node1 == node2)
			return node1.getType();
		
		while(node1!=node2 && !(node1.getType().equals("string")) && !(node2.getType().equals("string"))){
			if(node1.getLevel()>node2.getLevel())
				node1 = node1.getParent();
			else
				node2 = node2.getParent();
				
		}
		if(node1.getType().equals("string"))
			return node1.getType();
		else if(node2.getType().equals("string"))
			return node2.getType();
		else
			return node1.getType();
	}
		
	private void addChild(String parent, String child){
		TypeNode parentNode = nodes.get(parent);
		addChild(parentNode, child);
	}

	private void addChild(TypeNode parent, String child){
		TypeNode childNode = new TypeNode(child,parent);
		parent.addChild(childNode);
		nodes.put(child, childNode);
	}
}


class TypeNode{
	private String type;
	private TypeNode parent;
	private ArrayList<TypeNode> childs;
	private int level;
	
	protected TypeNode(String type, TypeNode parent){
		this.type = type;
		this.parent = parent;
		this.childs = new ArrayList<TypeNode>();
		if(parent==null)
			this.level = 0;
		else
			this.level = parent.level+1;
	}
	
	protected int getLevel(){
		return this.level;
	}
	
	protected String getType(){
		return this.type;
	}
	
	protected TypeNode getParent(){
		return this.parent;
	}
	
	protected void addChild(TypeNode child){
		this.childs.add(child);
	}
}
