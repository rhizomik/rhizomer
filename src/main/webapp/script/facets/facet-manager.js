var facet;
facet = {};
var searchProperty;

facet.FacetManager = function (){
	var self = this;
	
	/**
	 * Private Attributes
	 */
	var facets = {};
	var facetIds = {};
	var selectedFacets = {};
	var defaultFilters = {};
	
	self.getFacet = function (propertyURI){
		return facets[propertyURI];
	};
	
	self.getFacetById = function (id){
		return facets[facetIds[id]];
	};
	
	self.getUriById = function (id){
		return facetIds[id];
	};
	
	self.makeUrl = function(){
		if(!$j.isEmptyObject(selectedFacets)){
			//var url = "&f=";
			var url = "";
			for(f in selectedFacets){
				for(key in selectedFacets[f]){
					prefix = prefixes[getPrefix(facets[f].getUri())];
					url += encodeURIComponent(facets[f].getUri());
					value = selectedFacets[f][key];
					url += "/"+encodeURIComponent(value.uri)+"/";
				}			
			}
			return url;
		}
		else
			return "";
	};
	
	self.addDefaultFilter = function(iproperty, ivalue){
		if(defaultFilters[iproperty]){
			defaultFilters[iproperty].push(ivalue);
		}
		else{
			var a = new Array(ivalue);
			defaultFilters[iproperty] = a;
		}
	};
	
	self.setDefaultFilters = function(){
		for(i in defaultFilters){
			for(var x=0; x<defaultFilters[i].length; x++){
				self.filterInitProperty(i,defaultFilters[i][x]);
			}
		}
	};
	
	self.addFacet = function(property){
		if(property.type == "number")
			facets[property.uri] = facet.NumberFacet(property);
		else if(property.type == "string")
			facets[property.uri] = facet.StringFacet(property);
		else
			facets[property.uri] = facet.StringFacet(property);	
		facetIds[hex_md5(property.uri)] = property.uri;
	};
	
	self.renderFacets = function(target){
		for(f in facets){
			facets[f].render(target);
		}
	};
	
	self.toggleFacet = function(id) {
		facet = self.getFacetById(id);
		facet.toggleFacet();	
	};
	
	self.filterInitProperty = function(propertyUri, propertyValue){
		facet = facets[propertyUri];
		facet.toggleValue(propertyValue);
		var vlabel = makeLabel(propertyValue); //Obtenir label via sparql
		var fvalue = new FacetValue(propertyValue, vlabel, 0);
		if(selectedFacets[propertyUri]){
			selectedFacets[propertyUri][propertyValue] = fvalue;
		}
		else{
			selectedFacets[propertyUri] = {};
			selectedFacets[propertyUri][propertyValue] = fvalue;
		}
	};
	
	self.filterProperty = function(propertyUri, propertyValue, vlabel){
		facet = facets[propertyUri];
		valueReturn = facet.toggleValue(propertyValue);
		if(selectedFacets[propertyUri]){
			if(valueReturn){
				selectedFacets[propertyUri][propertyValue] = valueReturn;
			}
			else{
				delete(selectedFacets[propertyUri][propertyValue]);
				if($j.isEmptyObject(selectedFacets[propertyUri])){
					delete(selectedFacets[propertyUri]);
				}
			}
		}
		else{
			if(vlabel)
				valueReturn = new FacetValue(propertyValue, vlabel, 0);
			selectedFacets[propertyUri] = {};
			selectedFacets[propertyUri][propertyValue] = valueReturn;
		}
		facet.setSelected(true);
		self.reloadFacets();
		facet.setSelected(false);
		self.printActive();
		location.hash = self.makeUrl();;
	};
	
	self.printActive = function(){
		$j("#active_facets").empty();
		if(!$j.isEmptyObject(selectedFacets)){
			html = "<div>Your filters:</div>";
			for(f in selectedFacets){
				html += "<div class=\"selected_facet\"><span>"+facets[f].getLabel()+"</span>";
				html += "<ul>";
				for(key in selectedFacets[f]){
					value = selectedFacets[f][key];
					html += "<li id=\""+makeLabel(value.uri)+"\"><a onclick=\"javascript:fm.filterProperty('"+f+"','"+value.uri+"'); return false;\">";
					html += value.label+ " [x]</a></li>";
				}			
				html += "</ul></div>";
			}
			html += "</ul>";
			$j("#active_facets").append(html);
		}
	};	
	
	self.reloadFacets = function(){		
		var query = "SELECT DISTINCT ?r "+
			"WHERE { "+
			"?r a <"+facetURI+"> . ";
		query += self.makeRestrictions();
		query += "}";
		rhz.listResourcesNoHistory(query);
		self.reloadProperties();		
	};	
	
	self.reloadProperties = function(){
		var f;
		for(f in facets){
			restrictions = self.makeRestrictions(f);					
			if(facets[f].isOpened() && !facets[f].isSelected()){
					facets[f].reloadValues(restrictions);
			}
			facets[f].setSelected(false);
		};
	};	
	
	/**SPARQL Queries**/
	
	self.makeRestrictions = function(uri){
		var f;
		var query = "";
		var varCount = 0;
		for(f in facets){
			if(facets[f].isActive() && f != uri){
				query += facets[f].makeSPARQL(varCount);
				varCount++;
			}
		}
		return query;
	};	
	
	self.makeSPARQL = function(){
		var query = "SELECT ?r "+
				"WHERE { "+
				"?r a <"+facetURI+"> . ";
		for(f in facets){
			if(facets[f].getSelectedValue()!=null){
				if(facets[f].getRange().indexOf("http://www.w3.org/2001/XMLSchema#")>=0)
					query += "?r <"+facets[f].getUri()+"> \""+facets[f].getSelectedValue()+"\"^^<"+encodeURIComponent(facets[f].getRange())+"> . ";
				else if(rhizomik.Utils.isURI(facets[f].getSelectedValue()))
					query += "?r <"+facets[f].getUri()+"> <"+encodeURIComponent(facets[f].getSelectedValue())+"> . ";
				else
					query += "?r <"+facets[f].getUri()+"> \""+facets[f].getSelectedValue()+"\" . ";
			}
		}
		query += "}"
		return query; 
	};
	
	return self;
};