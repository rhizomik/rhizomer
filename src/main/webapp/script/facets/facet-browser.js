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
	
	self.loadFacets = function(){
		parser.parse();
		activeURI = parser.getActiveUri();
		activeVar = parser.getVariable();
		self.addManager(activeURI, activeVar);
		$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
		$j("#facets").append("<img id='waitImage' src=\"images/black-loader.gif\"/>");	
		parameters = {};
		parameters["facetURI"] = activeURI;
		parameters["mode"] = "facets";
		rhz.getFacets(parameters, 
				function(output) 
				{
					var response = output.evalJSON();		
					html = "<div class='filter_by'>Filter by:</div>";
					html += "<div class='reset_facets'><a href=''>Reset filters</a></div>";
					
					$j("#facets").html(html);
					$j.each(response.properties, 
						function(i, property)
						{
							managers[activeURI].addFacet(property);
						});
					managers[activeURI].renderFacets("facets");
					addToggle();
					self.setDefaultFilters();
					//managers[activeURI].setDefaultFilters();  
					managers[activeURI].reloadFacets();
					fm = managers[activeURI];
					//managers[activeURI].printActiveInit();
				}
		);
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
	
	self.printActiveFilters = function(){
		
	};
	
	self.filterInitProperty = function(variable, property, value){
		uri = vars_uris[variable];
		managers[uri].filterInitProperty(property,value);
	};
	
	self.addManager = function(uri, variable){
		managers[uri] = facet.FacetManager("");
		vars_uris[variable] = uri;
	};
};