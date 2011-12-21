facet.FacetManager = function (inParser){
	var self = this;
	
	/**
	 * Private Attributes
	 */
	var parser = inParser;
	var variable;
	var facets = {};
	var facetIds = {};
	var selectedFacets = {};
	var defaultFilters = {};
	var defaultLabels = {};
	
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
	
	self.setDefaultFilters = function(){
		restrictions = parser.getRestrictions();
		for(i=0; i<restrictions.length; i++){
			if(restrictions[i][0]!=null){
				property = restrictions[i][1].replace('<','').replace('>','');
				if(!property.startsWith("http://")){
					tmp = property.split(":");
					prefix = inverse_prefixes[tmp[0]];
					property = prefix+tmp[1];
				}
				for(x=0;x<restrictions[i][3].length;x++){					
					value = restrictions[i][3][x];
					self.filterInitProperty(property, value);
				}
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
		facet.addInitValue(propertyValue);
		var vlabel = makeLabel(propertyValue);
		var fvalue = new FacetValue(propertyValue, vlabel, 0);
		if(selectedFacets[propertyUri]){
			selectedFacets[propertyUri][propertyValue] = fvalue;
		}
		else{
			selectedFacets[propertyUri] = {};
			selectedFacets[propertyUri][propertyValue] = fvalue;
		}
	};
	
	self.setSelectedFacetLabel = function(propertyUri, propertyValue, propertyLabel){
		selectedFacets[propertyUri][propertyValue].setLabel(propertyLabel);
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
	};
	
	self.printActiveInit = function(){
		$j("#active_facets").empty();
		if(!$j.isEmptyObject(selectedFacets)){
			$j("#active_facets").append("<div>Your filters:</div>");
			for(f in selectedFacets){
				html = "<div class=\"selected_facet\"><span>"+facets[f].getLabel()+"</span>";
				html += "<ul id=\""+facets[f].getId()+"_active\">";
				html += "</ul></div>";
				$j("#active_facets").append(html);
			}
			
			for(f in selectedFacets){
				facets[f].printInitActiveLabels();
			}
		}		
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
			"?r a <"+activeURI+"> . ";
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
				"?r a <"+activeURI+"> . ";
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