var facet;
facet = {};
var searchProperty;
var activeURI;
var fm;
var facetBrowser;

facet.FacetBrowser = function(inParser){
	var self = this;
	var parser = inParser;
	var managers = {};
	var vars_uris = {};
	var activeManager = null;
	var mainManager = null;
	var varCount = 1;
	var autoCompleteProperty = null;
	var activeLabel = null;
	
	self.loadFacets = function(){
		parser.parse();
		activeURI = parser.getActiveUri();
		activeVar = parser.getVariable();
		self.addManager(activeURI, activeVar);
		mainManager = managers[activeURI];
		activeManager = managers[activeURI];
		$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
		$j("#facets").append("<img id='waitImage' src=\"images/black-loader.gif\"/>");	
		parameters = {};
		parameters["facetURI"] = activeURI;
		activeLabel = makeLabel(activeURI);
		parameters["mode"] = "facets";
		rhz.getFacets(parameters, 
				function(output) 
				{
					var response = output.evalJSON();		
					$j.each(response.properties, 
						function(i, property)
						{
							managers[activeURI].addFacet(property);
						});
					managers[activeURI].renderFacets("facets");
					addToggle();
					self.setDefaultFilters();
					managers[activeURI].reloadFacets();
					fm = managers[activeURI];
					self.printActiveFilters();
				}
		);
	};
	
	self.getActiveManager = function(){
		return activeManager;
	}
	
	self.getManager = function(uri){
		return managers[uri];
	};
	
	self.setDefaultFilters = function(){
		restrictions = parser.getRestrictions();
		for(i=0; i<restrictions.length; i++){
			if(restrictions[i][0]!=null){
				property = restrictions[i][1].replace('<','').replace('>','');
				if(!property.startsWith("http://")){
					var tmp = property.split(":");
					var prefix = inverse_prefixes[tmp[0]];
					var property = prefix+tmp[1];
				}
				for(x=0;x<restrictions[i][3].length;x++){
					var variable = restrictions[i][0];
					var value = restrictions[i][3][x];
					self.filterInitProperty(variable, property, value);
				}
			}
		}
	};
	
	self.toggleFacet = function(id){
		activeManager.toggleFacet(id);
	};
	
	self.filterProperty = function(propertyUri, propertyValue, vlabel){
		activeManager.filterProperty(propertyUri, propertyValue, vlabel);
	};
	
	self.removeProperty = function(typeUri, propertyUri, propertyValue, vlabel){
		managers[typeUri].filterProperty(propertyUri, propertyValue, vlabel);
	};
	
	self.reloadFacets = function(){
		activeManager.reloadFacets();
	};
	
	self.pivotFacet = function(uri){
		if(managers[uri]){
			activeManager = managers[uri];
			activeManager.renderFacets("facets");
			activeManager.reloadFacets();
			self.printActive();
		}
		else{
			activeManager.addPivotedFacet(uri, "r"+varCount);		
			self.addManager(uri, "r"+varCount);
			activeURI = uri;
			activeManager = managers[uri];
			activeManager.loadFacets();
		}
	};
	
	self.deletePivotFacet = function(uri){
		delete(managers[uri]);
		activeManager.deletePivotedFacet(uri);
		activeManager.reloadFacets();
		self.printActive();
	};
	
	self.printActiveFilters = function(){
		activeManager.printActiveInit();
	};
	
	self.filterInitProperty = function(variable, property, value){
		uri = vars_uris[variable];
		managers[uri].filterInitProperty(property,value);
	};
	
	self.addManager = function(uri, variable){
		managers[uri] = new facet.FacetManager(uri, variable);
		vars_uris[variable] = uri;
		varCount++;
	};
	
	self.makeRestrictions = function(){
		var query = "";
		for(var m in managers){
			query += managers[m].makeRestrictions2();
		}
		return query;
	};
	
	self.printActive = function(){
		$j("#active_facets").empty();
		$j("#active_facets").append("<div>Your filters:</div>");
		var html = mainManager.printActive2();
		$j("#active_facets").append(html);
	};	
	
	self.setAutoCompleteProperty = function(id){
		autoCompleteProperty = activeManager.getUriById(id);
	};
	
	self.getAutoCompleteProperty = function(){
		return autoCompleteProperty;
	};
	
	return self;
};