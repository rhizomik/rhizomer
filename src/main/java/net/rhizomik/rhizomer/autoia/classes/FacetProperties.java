package net.rhizomik.rhizomer.autoia.classes;

import java.util.*;
import java.util.Map.Entry;

public class FacetProperties{
	
	private Map<String, FacetProperty> properties;
	private List<FacetProperty> propertyList;
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

    public void addProperty(FacetProperty property) {
        properties.put(property.getUri(), property);
        propertyList.add(property);
    }

	public void addPropertyValue(String property, String valueURI, String valueLabel, int num){
		FacetProperty fp = properties.get(property);
		fp.addValue(valueURI, valueLabel, num);
	}
	
	public FacetProperty getProperty(String uri){
		return properties.get(uri);
	}
	
	public List<FacetProperty> getSortedProperties(){
		Collections.sort(propertyList);
		return this.propertyList;
	}
	
	public List<String> getProperties(){
		return new ArrayList(properties.keySet());
	}
	
	public Map<String, Integer> getPropertyValues(String property){
		return properties.get(property).getValues();
	}
	
	public int getTotalInstances(){
		return this.numInstancesTotal;
	}
	
	public StringBuffer printJSON(){
		StringBuffer out = new StringBuffer();
		List<FacetProperty> properties = getSortedProperties();
		int total = 0;
		out.append("[");
		for(FacetProperty property : properties){
            total++;
            out.append(property.printJSON());
		    if (total < properties.size())
                out.append(",\n");
		}
		out.append("]");
		return out;
	}

}