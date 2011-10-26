package net.rhizomik.rhizomer.autoia.classes;

public class FacetValue implements Comparable {
	


	private String uri;
	private String label;
	
	public FacetValue(String uri, String label) {
		this.uri = uri;
		if(label!=null && label!="")
			this.label = label;
		else
			makeLabel();		
	}
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}	
	
	private void makeLabel(){
		String[] uriSplitted = this.uri.split("#");
		if(uriSplitted.length>1)
			this.label = uriSplitted[uriSplitted.length-1];
		else{
			uriSplitted = this.uri.split("/");
			if(uriSplitted.length>1)
				this.label = uriSplitted[uriSplitted.length-1];
			else
				this.label = this.uri;					
				
		}
	}

	public int compareTo(Object arg0) {
		return -1; // Fer bé el comparador
	}	
	
	

}
