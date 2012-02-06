facet.FacetManager = function (uri, inVariable){
	var self = this;
	
	/**
	 * Private Attributes
	 */
	var typeUri = uri;
	var variable = inVariable;
	var facets = {};
	var facetIds = {};
	var selectedFacets = {};
	var defaultFilters = {};
	var defaultLabels = {};
	var pivotedFacets = {};
	var label = makeLabel(uri);
		
	self.getVariable = function(){
		return variable;
	};
	
	self.getTypeUri = function(){
		return typeUri;
	};
	
	self.addPivotedFacet = function(propertyURI, pivotedVar){
		pivotedFacets[propertyURI] = pivotedVar;
	};
	
	self.deletePivotedFacet = function(propertyURI){
		delete(pivotedFacets[propertyURI]);
	}
	
	self.getFacet = function (propertyURI){
		return facets[propertyURI];
	};
	
	self.getFacetById = function (id){
		return facets[facetIds[id]];
	};
	
	self.getUriById = function (id){
		return facetIds[id];
	};
	
	/*
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
	*/	
	
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
		//facets[property.uri] = facet.StringFacet(property, self.getVariable(), typeUri);	
		if(property.type == NS.xsd("integer"))
			facets[property.uri] = facet.NumberFacet(property, self.getVariable(), typeUri);
		else if(property.type == NS.xsd("string"))
			facets[property.uri] = facet.StringFacet(property, self.getVariable(), typeUri);
		else
			facets[property.uri] = facet.StringFacet(property, self.getVariable(), typeUri);	
		facetIds[hex_md5(property.uri)] = property.uri;
	};
	
	self.renderFacets = function(target){
		html = "<div class='filter_by'>Filter <strong>"+label+"</strong> by:</div>";
		html += "<div class='reset_facets'><a href=''>Reset filters</a></div>";
		$j("#"+target).html(html);		
		for(f in facets){
			facets[f].render(target);
		}
	};
	
	self.toggleFacet = function(id) {
		self.getFacetById(id).toggleFacet();
	};
	
	self.filterInitProperty = function(propertyUri, propertyValue){
		var facet = facets[propertyUri];
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
		var facet = facets[propertyUri];
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
		facetBrowser.reloadFacets();
		facet.setSelected(false);
		facetBrowser.printActive();
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
		var html = "";
		if(!$j.isEmptyObject(selectedFacets) || !$j.isEmptyObject(pivotedFacets)){
			html += "<span class=\"class_active\">"+label+"</span>";
			if(typeUri==facetBrowser.getMainManager().getTypeUri()){
				if(typeUri!=facetBrowser.getActiveManager().getTypeUri())
					html += "<span style=\"margin-left:20px;\"><a href=\"javascript:facetBrowser.pivotFacet('"+typeUri+"');\">&laquo; Back to</a></span>";
				html += "<span style=\"margin-left:20px;\"><a href=\"\">Reset all filters[x]</a></span>";
			}
			else if(typeUri!=facetBrowser.getActiveManager().getTypeUri()){
				html += "<span style=\"margin-left:20px;\"><a href=\"javascript:facetBrowser.pivotFacet('"+typeUri+"');\">&laquo; Back to</a></span>";
				html += "<span style=\"margin-left:20px;\"><a href=\"javascript:facetBrowser.deletePivotFacet('"+typeUri+"');\">[x]</a></span>";
			}
			else{
				html += "<span style=\"margin-left:20px;\"><a href=\"javascript:facetBrowser.deletePivotFacet('"+typeUri+"');\">[x]</a></span>";
			}
			html += "<ul>";		
			if(!$j.isEmptyObject(selectedFacets)){
				for(f in selectedFacets){
					html += "<li><span>"+facets[f].getLabel()+"</span>";
					html += "<ul class=\"inline\">";
					for(key in selectedFacets[f]){
						value = selectedFacets[f][key];
						html += "<li id=\""+makeLabel(value.uri)+"\"><a onclick=\"javascript:facetBrowser.removeProperty('"+typeUri+"','"+f+"','"+value.uri+"'); return false;\">";
						html += value.label+ " [x]</a></li>";
					}			
					html += "</ul></li>";
				}
			}
			for(m in pivotedFacets){
				var tmp = facetBrowser.getManager(m).printActive();
				if(tmp){
					html += "<li>";
					html += tmp;
					html += "</li>";
				}
			}
			html += "</ul>";
		}
		return html;
	};		
	
	self.reloadFacets = function(){
		var query = "SELECT DISTINCT ?"+variable+" "+
			"WHERE { "+
			"?"+variable+" a <"+typeUri+"> . ";
		query += facetBrowser.makeRestrictions();
		query += "}";
		rhz.listResourcesNoHistory(query);
		self.reloadProperties();		
	};	
	
	self.reloadProperties = function(){
		var f;
		for(f in facets){
			restrictions = facetBrowser.makeRestrictions(f);
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
		var varCount2 = 0;
		for(f in facets){
			if(facets[f].isActive() && f != uri){
				query += facets[f].makeSPARQL(varCount2, variable);
				varCount2++;
			}
		}
		for(m in pivotedFacets){
			query += " ?"+variable+" <"+m+"> ?"+pivotedFacets[m]+" .";
		}
		return query;
	};	
	
	self.makeSPARQL = function(){
		return activeManager.makeSPARQL();
	};
	
	self.loadFacets = function(){
		facets = {};
		$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
		$j("#facets").append("<img id='waitImage' src=\"images/black-loader.gif\"/>");	
		parameters = {};
		parameters["facetURI"] = typeUri;
		parameters["mode"] = "facets";
		rhz.getFacets(parameters, 
				function(output) 
				{
					var response = output.evalJSON();		
					$j.each(response.properties, 
						function(i, property)
						{
							self.addFacet(property);
						});
					self.renderFacets("facets");
					addToggle();  
					self.reloadFacets();
				}
		);
	};
	
	return self;
};
