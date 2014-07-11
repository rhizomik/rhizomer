var facet;
facet = {};
//var activeURI;
var fm;
var facetBrowser;

facet.FacetBrowser = function(parameters){
	var self = this;
	/*var parser = inParser;*/
	var managers = {};
	var vars_uris = {};
	var activeManager = null;
	var mainManager = null;
	var varCount = 1;
	var autoCompleteProperty = null;
	var activeLabel = null;
    var numResults = 0;
    var totalResults = 0;
    var step = 10;
    var childs = null;
    var breadcrumbs = null;
    /* NOU: Parametres JSON a la url */
    var selectedResource = parameters.focus || null;
    var uri = parameters.type;
    var filters = parameters.filters;

    self.loadFacets = function(makeHistory){
		/*parser.parse();*/
		/*activeURI = parser.getActiveUri();*/
        /*activeVar = parser.getVariable();*/
        activeVar = "r1";
        activeURI = uri;

		self.addManager(activeURI, "", activeVar);
		mainManager = managers[activeURI].manager;
		activeManager = managers[activeURI].manager;
		$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
		$j("#facets").append("<img class='waitImage' src=\"images/black-loader.gif\"/>");
		parameters = {};
		parameters["facetURI"] = activeURI;
		activeLabel = makeLabel(activeURI);
		parameters["mode"] = "facets";
        $j.blockUI();
        rhz.getFacets(parameters,
				function(output) 
				{
					//var response = output.evalJSON();
                    var response = JSON.parse(output);
                    mainManager.setNumInstances(response.numInstances);
                    childs = response.childs;
                    breadcrumbs = response.breadcrumbs;
					$j.each(response.properties, 
						function(i, property)
						{
							managers[activeURI].manager.addFacet(property);
						});
					managers[activeURI].manager.renderFacets("facets");
					/*addToggle();*/
					self.setDefaultFilters();
                    mainManager.setSelectedResource(selectedResource);
					managers[activeURI].manager.reloadFacets();
					fm = managers[activeURI].manager;
                    self.printBreadcrumbs();
                    self.listResources(makeHistory);
                    /*self.printSubCategories();*/
                    self.printSort();
				}
		);
	};

    self.loadHistory = function(parameters){
        selectedResource = parameters.focus || null;
        uri = parameters.type;
        filters = parameters.filters;
        self.loadFacets(false);
    };

    self.updateHash = function(){
        var filters = mainManager.makeHash();
        var parameters = {type : uri,
            filters : filters
        };
        var hash = encodeURIComponent(JSON.stringify(parameters));
        console.log(hash);
        dhtmlHistory.add(hash, {type: 'facets', parameters: parameters});
    };

    self.getUri = function(){
        return uri;
    }
	
	self.getActiveManager = function(){
		return activeManager;
	};

    self.getSelectedResource = function(){
        return selectedResource;
    };
	
	self.getManager = function(uri){
		return managers[uri].manager;
	};
	
	self.getMainManager = function(){
		return mainManager;
	};

    self.getNumResults = function(){
        return numResults;
    }
	
	self.setDefaultFilters = function(){
        if(filters){
            for(var i=0; i<filters.length; i++){
                self.filterInitProperty("r1", filters[i].property, filters[i].value, filters[i].label);
            }
        }
	};
	
	self.toggleFacet = function(id){
		activeManager.toggleFacet(id);
	};
	
	self.filterProperty = function(facetID, propertyValue, vlabel){;
        propertyValue = unescape(propertyValue);
        vlabel = unescape(vlabel);
		activeManager.filterProperty(facetID, propertyValue, vlabel);
	};
	
	self.removeProperty = function(typeUri, propertyUri, propertyValue, vlabel){
        propertyValue = unescape(propertyValue);
        vlabel = unescape(vlabel);
		managers[typeUri].manager.filterProperty(propertyUri, propertyValue, vlabel);
	};

    self.removeRangeProperty = function(typeUri, propertyUri){
        managers[typeUri].manager.resetFacet(propertyUri);
    }

    self.removeSelectedResource = function(typeUri){
        managers[typeUri].manager.deleteSelectedResource();
    };
	
	self.reloadFacets = function(){
		activeManager.reloadFacets(true);
	};
	
	self.deletePivotFacet = function(range){
		delete(managers[range]);
		activeURI = mainManager.getTypeUri();
		activeManager = mainManager;
		//TODO: Missing property URI
		// mainManager.deletePivotedFacet(uri);
		activeManager.renderFacets("facets");		
		mainManager.reloadFacets(true);
		self.printBreadcrumbs();
	};

    /*
	self.printActiveFilters = function(){
		activeManager.printActiveInit();
	};
	*/
	
	self.filterInitProperty = function(variable, property, value, label){
		uri = vars_uris[variable];
		managers[uri].manager.filterInitProperty(property,value, label);
	};
	
	self.addManager = function(range, propertyURI, variable){
		var obj = {};
        obj.range = range;
        obj.propertyURI = propertyURI;
        obj.manager = new facet.FacetManager(range, variable);
        managers[range] = obj;
		//managers[range] = new facet.FacetManager(range, variable);
		vars_uris[variable] = range;
		varCount++;
	};

    self.printBreadcrumbs = function(){
        $j.blockUI();
        self.printActive();
    };

	self.makeRestrictions = function(uri){
		var query = "";
		for(var m in managers){
            if(managers.hasOwnProperty(m)){
			query += managers[m].manager.makeRestrictions(uri);
            }
		}
		return query;
	};

    self.printActive = function(){
        countQuery = "SELECT (COUNT(DISTINCT(?"+mainManager.getVariable()+")) as ?count) "+
            "WHERE { "+
            "?"+mainManager.getVariable()+" a <"+mainManager.getTypeUri()+"> . ";
        countQuery += self.makeRestrictions();
        countQuery += "}";


        rhz.sparqlJSON(countQuery, function(out){
            data = JSON.parse(out);
            numResults = data.results.bindings[0].count.value;
            self.printActiveCallback();
        });
    };
	
	self.printActiveCallback = function(){
		$j("#active_facets").empty();
		//$j("#active_facets").append("<span><a href=\"\">Reset all filters[x]</a></span><br/><br/>");
		var html = mainManager.printActive(true);
		for(m in managers){
			if(managers[m].manager!=mainManager){
				html += "<br/>has "+makeLabel(managers[m].propertyURI, false)+" ";
				html += managers[m].manager.printActive(false);
			}
		}
        html += "<span style=\"margin-left:10px;\"><a href=\"\">Reset all filters <img src='images/delete_blue.png'/></a></span>";
		$j("#active_facets").append(html);
        self.printRelated();
        /*self.printPagination("listResources",self.makeSPARQL(),10);*/
	};


    self.listResources = function(makeHistory){
        var resource = self.getSelectedResource();
        if(!$j.isEmptyObject(resource)){
            rhz.describeResource(resource.uri);
        }
        else{
            query = self.makeSPARQL();
            /*console.log(query);*/
            rhz.listResourcesNoHistory(query);
        }
        if(makeHistory)
            self.updateHash();

    };


    self.makeSPARQL = function(){
        var constraints = facetBrowser.makeRestrictions();
        var query = "SELECT DISTINCT ?"+activeManager.getVariable()+" "+
            "WHERE { "+
            "?"+activeManager.getVariable()+" a <"+activeManager.getTypeUri()+"> . ";
        query += constraints;
        query += "}";
        return query;
    };



    self.printPath = function (){
        var html = "<ol>";
        html += "<li><a href=\"/\" class=\"ttip\" title=\"Go to Homepage\">Home</a></li>";

        if(!$j.isEmptyObject(breadcrumbs) && breadcrumbs.length>0){
            $j.each(breadcrumbs,function(i, node){
                var link = {type : node.uri};
                link = "/facets.jsp?p="+encodeURIComponent(JSON.stringify(link))
                html += "<li class=\"path\">&gt;&gt;</li>";
                html += "<li><a class=\"ttip\" title=\"Back to "+node.label+" \" href=\""+link+"\">"+node.label;



                if(!$j.isEmptyObject(node.childs) && node.childs.length>0){
                    html += "&nbsp;<b class=\"caret\"></b></a>";
                    html += "<ul>"
                    $j.each(node.childs,function(i, child){
                        var link = {type : child.uri};
                        link = "/facets.jsp?p="+encodeURIComponent(JSON.stringify(link))
                        html += "<li><a href='"+link+"'>"+child.label+"&nbsp;("+child.instances+")</a></li>";
                    });
                    html += "</ul>";
                }
                else
                    html += "</a>";

                html += "</li>";
            });
        }

        for(m in managers){
            html += "<li class=\"pivot-path\">&gt;&gt;</li>";
            if(managers[m].manager==mainManager){
                html += "<li class='active'>"+managers[m].manager.getLabel();
                if(managers[m].manager.getTypeUri()==self.getUri()){
                    if(!$j.isEmptyObject(childs) && childs.length>0){
                        html += "&nbsp; <b class=\"caret\"></b>";
                        html += "<ul>"
                        $j.each(childs,function(i, child){
                            var link = {type : child.uri};
                            link = "/facets.jsp?p="+encodeURIComponent(JSON.stringify(link))
                            html += "<li><a href='"+link+"'>"+child.label+"&nbsp;("+child.instances+")</a></li>";
                        });
                        html += "</ul>";
                    }
                }

                html += "</li>"

            }
            else{
                html += "<li class='pivot'><a class=\"ttip\" title=\"Switch to "+managers[m].manager.getLabel()+" and apply current filters \" href=\"javascript:facetBrowser.pivotActiveFacet('"+m+"')\">"+managers[m].manager.getLabel()+"</a>";

                if(managers[m].manager.getTypeUri() == self.getUri()){
                    if(!$j.isEmptyObject(childs) && childs.length>0){
                        html += "&nbsp; <b class=\"caret\"></b>";
                        html += "<ul>"
                        $j.each(childs,function(i, child){
                            var link = {type : child.uri};
                            link = "/facets.jsp?p="+encodeURIComponent(JSON.stringify(link))
                            html += "<li><a href='"+link+"'>"+child.label+"&nbsp;("+child.instances+")</a></li>";
                        });
                        html += "</ul>";
                    }
                }
                html += "</li>";

            }
        }
        html += "</ol>";
        $j("#breadcrumbs").html(html);
        $j(".ttip").tooltip({animation:true, delay: { show: 100, hide: 500 }});
        /*
        var html = "<ul><li><a class=\"ttip\" title=\"Go to Homepage\" href=\"/\">Home</a></li>";
        for(m in managers){
            html += "<li class=\"path\"><span>>></span></li>";
            if(managers[m].manager==mainManager)
                html += "<li class='active'><span>"+managers[m].manager.getLabel()+" ("+self.getNumResults()+")</span></li>";
            else
                html += "<li><a class=\"ttip\" title=\"Switch to "+managers[m].manager.getLabel()+" and apply current filters \" href=\"javascript:facetBrowser.pivotActiveFacet('"+m+"')\">"+managers[m].manager.getLabel()+"</a></li>";
        }
        html += "</ul>";
        $j("#breadcrumbs").html(html);
        */
    };

    self.printRelatedCallback = function(){
        self.printPath();
        var connectionsHtml = "<div class=\"connections-header\"><h4>Active Connections</h4></div>";
        var html = "<ul>";
        for(m in managers){
            if(managers[m].manager!=mainManager)
                html += "<li><a class=\"ttip\" title=\"Switch to related "+managers[m].manager.getLabel()+"\" href=\"javascript:facetBrowser.pivotFacet('','','"+m+"');\">"+managers[m].manager.getLabel();+"</a></li>";
        }
        html += "</ul>";
        if(html!="<ul></ul>"){
            connectionsHtml += html;
        }
        else{
            connectionsHtml += "No active connections. ";
        }
        $j("#active-connections").html(connectionsHtml);


        var html = "<div class=\"connections-header\"><h4>More Connections</h4></div>";
        html += "<span>Click to add connections to related resources</span>";
        html += "<ul>";
        var navigableFacets = mainManager.getNavigableFacets();
        var links = {};
        for(f in navigableFacets){
            links[navigableFacets[f].getRange()] = true;
            if(!managers[navigableFacets[f].getRange()]){
                if(navigableFacets[f].isInverse())
                    html += "<li><a class=\"ttip\" title=\"Add connection to related "+makeLabel(navigableFacets[f].getRange())+"\" href=\"javascript:facetBrowser.pivotInverseFacet('"+navigableFacets[f].getClassUri()+"','"+navigableFacets[f].getUri()+"','"+navigableFacets[f].getRange()+"');\">"+makeLabel(navigableFacets[f].getRange())+"</a></li>";
                else
                    html += "<li><a class=\"ttip\" title=\"Add connection to related "+navigableFacets[f].getLabel()+"\" href=\"javascript:facetBrowser.pivotFacet('','"+navigableFacets[f].getUri()+"','"+navigableFacets[f].getRange()+"');\">"+navigableFacets[f].getLabel()+"</a></li>";
            }
        }

        html += "</ul>";

        $j("#connections").html(html);

        $j(".ttip").tooltip({animation:true, delay: { show: 100}});


        $j.unblockUI();

    };
	
	self.printRelated = function(){
        /*
        var filter = "";
        for(m in managers){
            if(managers[m].manager!=mainManager){
                if(filter=="")
                    filter="FILTER(?c=<"+m+">";
                else
                    filter+=" || ?c=<"+m+">";
            }
        }
        filter += ")";
        var counts = {};
        if(filter!=")"){

            var query = "SELECT ?c (COUNT(DISTINCT(?pr)) as ?count) "
            query += "WHERE {?pr a ?c . "+filter;
            query += "?"+mainManager.getVariable()+" a <"+mainManager.getTypeUri()+"> . ";
            query += self.makeRestrictions();
            query += "?"+mainManager.getVariable()+" ?p ?pr } GROUP BY ?c";
            console.log(query);

            rhz.sparqlJSON(query, function(out){
                data = JSON.parse(out);;
                for(i=0; i<data.results.bindings.length; i++){
                    var c = data.results.bindings[i].c.value;
                    var count = data.results.bindings[i].count.value;
                    counts[c] = count;
                }
                self.printRelatedCallback(counts);
            });
        }
        else*/
            self.printRelatedCallback();

    };
	
	self.setAutoCompleteProperty = function(id){
		autoCompleteProperty = id;
	};
	
	self.getAutoCompletePropertyID = function(){
		return autoCompleteProperty;
	};

    self.getAutoCompletePropertyURI = function(){
        return activeManager.getUriById(autoCompleteProperty);
    };

    self.pivotActiveFacet = function(range){
        activeManager = managers[range].manager;
        activeManager.renderFacets("facets");
        activeManager.reloadFacets(true);
        mainManager = activeManager;
        self.printBreadcrumbs();
    };
	
	self.pivotFacet = function(classURI, propertyURI, range){
        if(managers[range]){
			activeManager = managers[range].manager;
			activeManager.renderFacets("facets");
			activeManager.reloadFacets(true);
			mainManager = activeManager;
			//self.printRelated();
		}
		else{
			activeManager.addPivotedFacet(propertyURI, range, "r"+varCount);
			self.addManager(range, propertyURI, "r"+varCount);
			activeURI = range;
			activeManager = managers[range].manager;
			activeManager.loadFacets();
			mainManager = activeManager;
		}
        self.printBreadcrumbs();
        self.listResources(true);
	};
	
	self.pivotInverseFacet = function(classURI, propertyURI, range){
		if(managers[range]){
			activeManager = managers[range].manager;
			activeManager.renderFacets("facets");
			activeManager.reloadFacets(true);
			mainManager = activeManager;
			//self.printRelated();
		}
		else{
            activeManager.addPivotedInverseFacet(propertyURI, range, "r"+varCount);
            self.addManager(range, propertyURI, "r"+varCount);
            activeURI = range;
            activeManager = managers[range].manager;
			activeManager.loadFacets();
            mainManager = activeManager;
        }
        self.printBreadcrumbs();
        self.listResources(true);
    };

    self.printSubCategories = function(){
        if(childs && childs.length>0){
            var divWidth = $j("#subclasses").width();
            var contentWidth = 0;
            var html = "<span>Subcategories: </span>";
            $j("#subclasses").append(html);
            var more = "";
            $j.each(childs,function(i, child){
                var link = {type : child.uri};
                link = "/facets.jsp?p="+encodeURIComponent(JSON.stringify(link))
                /*
                if(divWidth-contentWidth<300){
                    more += "<li><a href='"+link+"'>"+child.label+"&nbsp;("+child.instances+")</a></li>";
                }
                else{
                */
                    html = "<a href='"+link+"'>"+child.label+"&nbsp;("+child.instances+")</a> | ";
                    $j("#subclasses span").append(html);
                    contentWidth = $j("#subclasses span").width();
                /*}*/
            });
            /*
            if(more!=""){
                html = "<span id='more_subclasses'>More...</span>";
                $j("#subclasses span").append(html);
                $j("#more_subclasses").popover('destroy');
                $j("#more_subclasses").popover({
                    offset: 10,
                    animation: true,
                    trigger: 'hover',
                    html: true,
                    placement: "bottom",
                    content: more});
            }
            */
        }
        else
            $j("#subclasses").hide();
    };

    self.printSort = function(){
        $j("#sort").empty();
        html = "<span style=\"font-weight:bold;\">Sort by: </span><select onchange=\"javascript:facetBrowser.sort('asc');\" id='sort_property'></select>";
        html += "<a href=\"javascript:facetBrowser.sort('asc');\"><img src='images/sort_ascending.png' alt='Sort ascensing'/></a>&nbsp;";
        html += "<a href=\"javascript:facetBrowser.sort('desc');\"><img src='images/sort_descending.png' alt='Sort descending'/></a>";
        $j("#sort").append(html);
        var sortProperties = activeManager.getSortProperties();
        if(sortProperties["http://www.w3.org/2000/01/rdf-schema#label"]){
            $j("#sort_property").append($j("<option />").val("http://www.w3.org/2000/01/rdf-schema#label").text("A-Z"));
        }
        else{
            /* If no label, order by uri */
            $j("#sort_property").append($j("<option />").val("uri").text("A-Z"));
        }

        $j.each(sortProperties,function(property, label){
            if(property!="http://www.w3.org/2000/01/rdf-schema#label")
                $j("#sort_property").append($j("<option />").val(property).text(label));
        });
    };

    self.sort = function(sort){
        /* CANVIAR PER METODE PROPI */
        var sortProperty = $j("#sort_property").val();
        activeManager.reloadResources(sortProperty, sort)
    };



	return self;
};