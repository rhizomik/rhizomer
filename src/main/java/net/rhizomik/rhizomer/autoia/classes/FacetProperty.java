package net.rhizomik.rhizomer.autoia.classes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import net.rhizomik.rhizomer.util.FacetUtil;

public class FacetProperty implements Comparable{
	
	class RevIntComp implements Comparator<Integer>
	{
	    public int compare(Integer o1, Integer o2)
	    {
	        return (o1>o2 ? -1 : (o1==o2 ? 0 : 1));
	    }
	}
	
	private String facetType;
	private String uri;
	private String label;
	private String dataType;
	private String range;
	private int numInstances;
	private double metric;
	private double entropy;
	private double cardinalityGaussian;
	private TreeMap<String, Integer> valuesByString;
	private TreeMultimap<Integer, String> valuesByInteger;
	private TreeMultimap<Integer, FacetValue> values;
	//private HashMap<String, String> uriToLabel;
	private int numValues;
	
	public FacetProperty(String uri, int numInstances, int numValues, String range, String dataType){
		this.numInstances = numInstances;
		this.numValues = numValues;
		this.uri = uri;
		this.range = range;
		this.dataType = dataType;
		this.valuesByString = new TreeMap<String, Integer>();
		valuesByInteger = TreeMultimap.create(new RevIntComp(),Ordering.natural());
		//uriToLabel = new HashMap<String, String>();
		values = TreeMultimap.create(new RevIntComp(), Ordering.natural());
		label = FacetUtil.makeLabel(uri);
		this.facetType = getType();
	}
	
	public void setNumValues(int numValues){
		this.numValues = numValues;
	}
	
	public double getMetric() {
		return this.metric;
	}
	
	public String getFacetType(){
		return this.facetType;
	}
	
	public void setMetric(double metric){
		this.metric = metric;
	}
	
	public int getCardinality() {
		return this.valuesByString.size();
	}
	
	public double getCardinalityGaussian(){
		return this.cardinalityGaussian;
	}
	
	public double getEntropy() {
		return entropy;
	}

	public void setEntropy(double entropy) {
		this.entropy = entropy;
	}	
	
	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}
	
	public String getUri() {
		return uri;
	}

	public String getLabel() {
		return label;
	}
	
	public int getNumValues() {
		return numValues;
	}	
	
	public int getNumInstances(){
		int total = 0;
		for(int x : valuesByInteger.keys())
			total+=x;
		return total;
	}
	
	public int compareTo(Object o) { 
		FacetProperty other = (FacetProperty) o;
		if(other.entropy >= this.entropy)
			return -1;
		else
			return 1;
		/*
		if(other.metric > this.metric)
			return 1;
		else	
			return -1;
		*/
	}
	
	public void addValue(String uri, String label, int num){
		if (!valuesByString.containsKey(uri)) // Avoid repeating values when there is more than one label for them
		{
			valuesByString.put(uri, num);
			valuesByInteger.put(num, uri);
			values.put(num, new FacetValue(uri, label));
		}
	}
	
	public TreeMap<String, Integer> getValues(){
		return this.valuesByString;
	}
	/*
	public TreeMultimap getSorteValues2(){
		return this.values;
	}
	*/
	
	public TreeMultimap getSortedValues(){
		return this.valuesByInteger;
	}
	
	
	public StringBuffer printJSON(){
		StringBuffer out = new StringBuffer();
		int total=0;
		for(Entry<Integer, FacetValue> e : values.entries()){
				total++;
				FacetValue value = e.getValue();
				int instances = e.getKey();
				out.append("{\"value\": \""+value.getUri()+"\", \"label\": \""+value.getLabel()+"\", \"instances\": \""+instances+"\"}");
				if(total<valuesByInteger.size())
					out.append(",\n");
		}
		return out;
	}
	
	protected void calculateMetrics(){
		/*
		calculateNumInstances();
		calculateCardinality();
		calculateEntropy();
		*/
	}
	
	private void calculateNumInstances(){
		int numInstances = 0;
		for(int value : valuesByString.values())
			numInstances += value;
		this.numInstances = numInstances;
	}
	
	private void calculateEntropy(){
		double entropy = 0;
		for(int value : valuesByString.values()){
        	double prob = (double)value/(double)this.numInstances;
        	double log = prob*Math.log(prob)/Math.log(valuesByString.size());
        	entropy += log;
		}
		entropy*=-1;
		if(Double.isNaN(entropy))
			entropy = 0;
		this.entropy = entropy;
	}
	
	private void calculateCardinality(){
		int param1 = 2;
		int param2 = 20;
		
		if(this.valuesByString.size()<=1)
			this.cardinalityGaussian = 0;
		else
			this.cardinalityGaussian = (float) Math.pow(Math.E,-(Math.pow(this.valuesByString.size()-param1,2)/(2*Math.pow(param2,2))));
	}

	private String getType(){
		if(this.dataType!=null && this.dataType.equals("integer"))
			return "number";
		else
			return "string";
	}

}

