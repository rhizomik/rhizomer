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
	var pivotedFacets = {};
	var label = makeLabel(uri);
    var numInstances = 0;
    var selectedResource = null;
    var sortProperties = {};
    var numVisibleFacets = 10;
    var numActiveFacets = 1;
    var numTotalFacets = 1;

	self.getVariable = function(){
		return variable;
	};
	
	self.getTypeUri = function(){
		return typeUri;
	};
	
	self.getLabel = function(){
		return label;
	};

    self.setNumInstances = function(inNumInstances){
        numInstances = inNumInstances;
    };
	
	self.needsToPrint = function(){
		if(!$j.isEmptyObject(selectedFacets))
			return true;
		else
			return false;
	};

    self.getSortProperties = function(){
        return sortProperties;
    }

    self.getActiveFacets = function(){
        activeFacets = {};
        for(f in facets){
            if(facets[f].isActive()){
                activeFacets[f] = facets[f];
            }
        }
        return activeFacets;
    }
	
	self.getNavigableFacets = function(){
		var navigableFacets = {};
		for(f in facets){
			if(facets[f].isNavigable())
				navigableFacets[f] = facets[f];
		}
		return navigableFacets;
	};

    self.resetFacet = function(facetID){
        var facet = facets[facetIds[facetID]];
        facet.reset();
    }

    self.getPivotedFacets = function(){
      return pivotedFacets;
    };

    self.setSelectedResource = function(object){
        selectedResource = object;
    };

    self.deleteSelectedResource = function(){
        selectedResource = null;
        facetBrowser.reloadFacets();
        facetBrowser.printBreadcrumbs();
    };
	
	self.addPivotedFacet = function(propertyURI, range, pivotedVar){
        var obj = {};
        obj.range = range;
        obj.pivotedVar = pivotedVar;
		pivotedFacets[propertyURI] = obj;
	};

    self.addPivotedInverseFacet = function(propertyURI, range, pivotedVar){
        var obj = {};
        obj.range = range;
        obj.pivotedVar = pivotedVar;
        pivotedFacets[propertyURI+range] = obj;
    };

    self.deletePivotedFacet = function(propertyURI){
        delete(pivotedFacets[propertyURI]);
    }

    self.deletePivotedFacet = function(propertyURI, range){
		delete(pivotedFacets[propertyURI+range]);
	}

    self.getFacet = function (propertyURI){
        return facets[propertyURI];
    };

	self.getFacet = function (propertyURI, range){
		return facets[propertyURI+range];
	};
	
	self.getFacetById = function (id){
		return facets[facetIds[id]];
	};
	
	self.getUriById = function (id){
		return facets[facetIds[id]].getUri();
	};

	self.addFacet = function(property){
        /*console.log(property.uri + " " + property.type);*/

        if(property.range=="null")
            property.range = property.type;

        if(property.isInverse == "true"){
            facets[property.uri+property.range] = facet.InverseFacet(property, self.getVariable(), typeUri);
            facetIds[hex_md5(property.uri+property.range)] = property.uri+property.range;
        }

        else if(property.range == "http://www.w3.org/2001/XMLSchema#integer" || 
                property.range=="http://www.w3.org/2001/XMLSchema#float" ||
                property.range == "http://www.w3.org/2001/XMLSchema#double" || 
                property.range=="http://www.w3.org/2001/XMLSchema#int" || 
                property.range=="http://www.w3.org/2001/XMLSchema#decimal" || 
                property.range=="http://www.w3.org/2001/XMLSchema#float") {
                facets[property.uri] = facet.NumberFacet(property, self.getVariable(), typeUri);
                //facets[property.uri] = facet.StringFacet(property, self.getVariable(), typeUri);
                facetIds[hex_md5(property.uri)] = property.uri;
                sortProperties[property.uri] = property.label;
        }

        /*
        else if(property.type == "http://www.w3.org/2001/XMLSchema#date"){
            alert(property.uri + " date");
        }
        */
        else{
            /*console.log(property.uri + " " + property.type);*/
            facets[property.uri] = facet.StringFacet(property, self.getVariable(), typeUri);
            facetIds[hex_md5(property.uri)] = property.uri;
                if(property.uri == "http://www.w3.org/2000/01/rdf-schema#label")
                    sortProperties[property.uri] = "A-Z";
                /*else
                    sortProperties[property.uri] = property.label;
                */
        }


        if(numActiveFacets>numVisibleFacets){
            if(property.isInverse == "true")
                facets[property.uri+property.range].setVisible(false);
            else
                facets[property.uri].setVisible(false);
        }
        else{
            numActiveFacets++;
        }


        numTotalFacets++;

    };
	
	self.renderFacets = function(target){
		html = "<div class='filter_by'>Filter <strong>"+label+"</strong> by:</div>";
		/*html += "<div class='reset_facets'><a href=''>Reset filters</a></div>";*/
        html += "<div class='facet_list'>";
		$j("#"+target).html(html);		
		for(f in facets){
			facets[f].render(target);
		}
        $j("#"+target).append("</div>");

        if(numActiveFacets<numTotalFacets){
            html = "<div class=\"more\"><span id=\"more_facets\">More filters...</span></div>";
            $j("#"+target).append(html);
        }

        $j("#more_facets").click(function (){
            var i=1;
            $j(".facet:hidden").each(function (){
                if(i<=numVisibleFacets){
                    $j(this).show();
                    numActiveFacets++;
                }
                i++;
            });

            if(numActiveFacets>=numTotalFacets)
                $j("#more_facets").hide();
        });

	};
	
	self.toggleFacet = function(id) {
		self.getFacetById(id).toggleFacet();
	};


	self.filterInitProperty = function(propertyUri, propertyValue, valueLabel){
        /*
		var facet = facets[propertyUri];
		facet.addInitValue(propertyValue);
        if(valueLabel)
            var vlabel = valueLabel;
        else
		    var vlabel = makeLabel(propertyValue);
		var fvalue = new FacetValue(propertyValue, vlabel, 0);
        facetID = hex_md5(propertyUri);
		if(selectedFacets[facetID]){
			selectedFacets[facetID][propertyValue] = fvalue;
		}
		else{
			selectedFacets[facetID] = {};
			selectedFacets[facetID][propertyValue] = fvalue;
		}
        console.log(selectedFacets);
        */
        self.filterProperty(hex_md5(propertyUri), propertyValue, valueLabel);
	};

	
	self.setSelectedFacetLabel = function(facetID, propertyValue, propertyLabel){
		selectedFacets[facetID][propertyValue].setLabel(propertyLabel);
	};
	
	self.filterProperty = function(facetID, propertyValue, vlabel){
        console.log(vlabel);
		var facet = facets[facetIds[facetID]];
		if(selectedFacets[facetID]){
            valueReturn = facet.toggleValue(propertyValue, vlabel);
			if(valueReturn){
				selectedFacets[facetID][propertyValue] = valueReturn;
			}
			else{
				delete(selectedFacets[facetID][propertyValue]);
				if($j.isEmptyObject(selectedFacets[facetID])){
					delete(selectedFacets[facetID]);
				}
			}
		}
		else{
			/*if(vlabel)
				valueReturn = new FacetValue(propertyValue, vlabel, 0);
				*/
            valueReturn = facet.toggleValue(propertyValue, vlabel);
			selectedFacets[facetID] = {};
			selectedFacets[facetID][propertyValue] = valueReturn;
		}
		facet.setSelected(true);
		facetBrowser.reloadFacets();
		facet.setSelected(false);
        facetBrowser.listResources(true);
        facetBrowser.printBreadcrumbs();
	};
	
	self.printActive = function(main){
        self.getActiveFacets();
        /*console.log(label);
        console.log(self.getPivotedFacets());
        */
		if(main){
            var numResults = facetBrowser.getNumResults();
			var html = "Showing "+numResults+" <b>"+label+"</b> filtered from "+numInstances + "<br/>";
        }
		else{
			var html = "<a href=\"javascript:facetBrowser.pivotFacet('','','"+typeUri+"');\">"+label+"</a>&nbsp;";
            html += "<a class=\"pointer\" onclick=\"javascript:facetBrowser.deletePivotFacet('"+typeUri+"'); return false;\"><img src='images/delete_blue.png'/></a>";
        }

        activeFacets = self.getActiveFacets();

        if(!$j.isEmptyObject(activeFacets) || !$j.isEmptyObject(selectedResource)){
            html += "where ";

            if(!$j.isEmptyObject(selectedResource)){
                html+= "<b>"+label+"</b> is <b>"+selectedResource.label+"&nbsp;</b>";
                html += "<a class=\"pointer\" onclick=\"javascript:facetBrowser.removeSelectedResource('"+typeUri+"'); return false;\"><img src='images/delete_blue.png'/></a>";
            }

            if(!$j.isEmptyObject(selectedResource) && !$j.isEmptyObject(activeFacets))
                html+= " and ";

            var x=0;
            for(f in activeFacets){
                if(x>0)
                    html += " and ";
                html += activeFacets[f].printActive();
                x++;
            }
        }

		return html;
	};

	
	self.reloadFacets = function(){
		self.reloadProperties();
	};

    self.reloadResources = function(property, sort){
        var constraints = facetBrowser.makeRestrictions();
        var query = "SELECT DISTINCT ?"+variable+" "+
            "WHERE { "+
            "?"+variable+" a <"+typeUri+"> . ";
        query += constraints;
        if(property!="uri")
            query += "?"+variable+" <"+property+"> ?sort";
        query += "}";
        if(property=="uri")
            query += "ORDER BY "+sort+"(?"+variable+")";
        else
            query += "ORDER BY "+sort+"(?sort)";
        console.log(query);
        rhz.listResourcesNoHistory(query);
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
		for(m in pivotedFacets) {
            if (facets[m].isInverse())
                query += " ?"+pivotedFacets[m].pivotedVar+" <"+facets[m].getUri()+"> ?"+variable+" .";
            else
                query += " ?"+variable+" <"+facets[m].getUri()+"> ?"+pivotedFacets[m].pivotedVar+" .";
            query += " ?"+variable+" a <"+typeUri+"> .";
		}
        //console.log(selectedResource);
        if(selectedResource)
            query += "FILTER(?"+variable+"=<"+selectedResource.uri+">) .";
        //console.log(query);
		return query;
	};
	
	self.makeSPARQL = function(){
		return activeManager.makeSPARQL();
	};

    self.makeHash = function(uri){
        var f;
        var filters = [];
        for(f in facets){
            if(facets[f].isActive() && f != uri){
                values = facets[f].getSelectedValues();
                for(v in values){
                    var value = values[v];
                    filters.push(
                        {property : f,
                            value : value.uri,
                            label : value.label
                        }
                    );
                }
            }
        }
        return filters;
        /*
        for(m in pivotedFacets) {
            if (facets[m].isInverse())
                query += " ?"+pivotedFacets[m].pivotedVar+" <"+facets[m].getUri()+"> ?"+variable+" .";
            else
                query += " ?"+variable+" <"+facets[m].getUri()+"> ?"+pivotedFacets[m].pivotedVar+" .";
            query += " ?"+variable+" a <"+typeUri+"> .";
        }
        */

    };
	
	self.loadFacets = function(){
		facets = {};
		$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
		$j("#facets").append("<img class='waitImage' src=\"images/black-loader.gif\"/>");
		parameters = {};
		parameters["facetURI"] = typeUri;
		parameters["mode"] = "facets";
		rhz.getFacets(parameters, 
				function(output) 
				{
					var response = JSON.parse(output);
                    numInstances = response.numInstances;
					$j.each(response.properties, 
						function(i, property)
						{
							self.addFacet(property);
						});
					self.renderFacets("facets");
					/*addToggle();*/
					self.reloadFacets(true);
                    facetBrowser.printSort();
					facetBrowser.printRelated();
				}
		);
	};
	
	return self;
};
