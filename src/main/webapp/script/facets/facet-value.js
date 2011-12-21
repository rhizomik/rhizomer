function FacetValue(uri, label, instances){
	this.uri = uri;
	this.label = label;
	this.instances = instances;
	
	this.setLabel = function(inLabel){
		this.label = inLabel;
	};
	
};