package net.rhizomik.rhizomer.autoia.classes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MenuConfig {
	
	public static String ALPHABETICAL = "alphabetical";
	public static String INSTANCES = "instances";	
	
	protected int numItemsGlobal;
	protected int numItemsLocal;
	protected List<String> uriBlackList;
	protected List<String> uriWhiteList;
	protected List<String> namespaceBlackList;
	protected String sort;

	public MenuConfig(String path) throws ParserConfigurationException, SAXException, IOException{
		uriBlackList = new ArrayList<String>();
		uriWhiteList = new ArrayList<String>();
		namespaceBlackList = new ArrayList<String>();
		this.loadFromXML(path);
	}
		
	public int getNumItemsGlobal() {
		return numItemsGlobal;
	}

	public void setNumItemsGlobal(int numItemsGlobal) {
		this.numItemsGlobal = numItemsGlobal;
	}

	public int getNumItemsLocal() {
		return numItemsLocal;
	}

	public void setNumItemsLocal(int numItemsLocal) {
		this.numItemsLocal = numItemsLocal;
	}

	public List<String> getUriBlackList() {
		return uriBlackList;
	}

	public void setUriBlackList(List<String> uriBlackList) {
		this.uriBlackList = uriBlackList;
	}
	
	public List<String> getUriWhiteList() {
		return uriWhiteList;
	}

	public void setUriWhiteList(List<String> uriWhiteList) {
		this.uriWhiteList = uriWhiteList;
	}

	public List<String> getNamespaceBlackList() {
		return namespaceBlackList;
	}

	public void setNamespaceBlackList(List<String> namespaceBlackList) {
		this.namespaceBlackList = namespaceBlackList;
	}
		
	public void loadFromXML(String path) throws ParserConfigurationException, SAXException, IOException{
		
		File file = new File(path);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		NodeList nodeLst = doc.getElementsByTagName("uri");
		for (int s = 0; s < nodeLst.getLength(); s++) {
		    Node xmlNode = nodeLst.item(s);
		    uriBlackList.add(xmlNode.getTextContent());
		}
		nodeLst = doc.getElementsByTagName("adduri");
		for (int s = 0; s < nodeLst.getLength(); s++) {
		    Node xmlNode = nodeLst.item(s);
		    uriWhiteList.add(xmlNode.getTextContent());
		}
		nodeLst = doc.getElementsByTagName("namespace");
		for (int s = 0; s < nodeLst.getLength(); s++) {
		    Node xmlNode = nodeLst.item(s);
		    namespaceBlackList.add(xmlNode.getTextContent());
		}
		nodeLst = doc.getElementsByTagName("numitemsglobal");
		Node xmlNode = nodeLst.item(0);
		this.numItemsGlobal = Integer.parseInt(xmlNode.getTextContent());
		nodeLst = doc.getElementsByTagName("numitemslocal");
		xmlNode = nodeLst.item(0);
		this.numItemsLocal = Integer.parseInt(xmlNode.getTextContent());
		nodeLst = doc.getElementsByTagName("sort");
		xmlNode = nodeLst.item(0);
		this.sort = xmlNode.getTextContent();
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

}
