package net.rhizomik.rhizomer.autoia.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class FacetProperties{
	
	private TreeMap<String, FacetProperty> properties;
	private ArrayList<FacetProperty> propertyList;
	private int numInstancesTotal;
	
	public FacetProperties(){
		this.properties = new TreeMap<String, FacetProperty>();
		this.propertyList = new ArrayList<FacetProperty>();
	}
	
	public void calculateMetrics(){
		for(FacetProperty property : propertyList)
			property.calculateMetrics();
	}
	
	public void setNumInstancesTotal(int numInstances){
		this.numInstancesTotal = numInstances;
	}
	
	public FacetProperties(int numInstances){
		this.properties = new TreeMap<String, FacetProperty>();
		this.propertyList = new ArrayList<FacetProperty>();
		this.numInstancesTotal = numInstances;
	}
	
	public void addProperty(String property, int numInstances, int numValues, String range, String dataType){
		FacetProperty fp = new FacetProperty(property, numInstances, numValues, range, dataType);
		properties.put(property, fp);
		propertyList.add(fp);		
	}
		
	public void addPropertyValue(String property, String valueURI, String valueLabel, int num){
		FacetProperty fp = properties.get(property);
		fp.addValue(valueURI, valueLabel, num);
	}
	
	public FacetProperty getProperty(String uri){
		return properties.get(uri);
	}
	
	public ArrayList<FacetProperty> getSortedProperties(){
		Collections.sort(propertyList);
		return this.propertyList;
	}
	
	public ArrayList<String> getProperties(){
		return new ArrayList(properties.keySet());
	}
	
	public TreeMap<String, Integer> getPropertyValues(String property){
		return properties.get(property).getValues();
	}
	
	public int getTotalInstances(){
		return this.numInstancesTotal;
	}
	
	public StringBuffer printJSON(){
		StringBuffer out = new StringBuffer();
		out.append("[");
		
		ArrayList<FacetProperty> properties = getSortedProperties();
		for(FacetProperty property : properties){
			//if(property.getValues().size()>0){
				out.append("{");
				out.append("\"numValues\":\""+property.getNumValues()+"\",\n");
				out.append("\"range\":\""+property.getRange()+"\",\n");
				out.append("\"type\":\""+property.getFacetType()+"\",\n");
				out.append("\"uri\": \""+property.getUri()+"\",\n ");
				out.append("\"label\": \""+property.getLabel()+"\",\n ");
				out.append("\"values\": ["+property.printJSON()+"]\n");
				out.append("}\n");
				out.append(",\n");
			//}
		}
		if(out.charAt(out.length()-1)==','){
			out.substring(0, out.length()-2);
		}
			
		out.append("]");
		return out;
	}

}