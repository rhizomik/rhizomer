function FacetValue(uri, label, instances){

    var self = this;

	self.uri = uri;
    self.label = label;
    self.instances = instances;

    self.setLabel = function(inLabel){
        self.label = inLabel;
	};
	
	return self;
};