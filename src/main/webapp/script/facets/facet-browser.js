var facet;
facet = {};
//var activeURI;
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
    var numResults = 0;
    var totalResults = 0;
	
	self.loadFacets = function(){
		parser.parse();
		activeURI = parser.getActiveUri();
		activeVar = parser.getVariable();
		self.addManager(activeURI, "", activeVar);
		mainManager = managers[activeURI].manager;
		activeManager = managers[activeURI].manager;
		$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
		$j("#facets").append("<img class='waitImage' src=\"images/black-loader.gif\"/>");
		parameters = {};
		parameters["facetURI"] = activeURI;
		activeLabel = makeLabel(activeURI);
		parameters["mode"] = "facets";
		rhz.getFacets(parameters, 
				function(output) 
				{
					//var response = output.evalJSON();
                    var response = JSON.parse(output);
                    mainManager.setNumInstances(response.numInstances);
					$j.each(response.properties, 
						function(i, property)
						{
							managers[activeURI].manager.addFacet(property);
						});
					managers[activeURI].manager.renderFacets("facets");
					addToggle();
					self.setDefaultFilters();
					managers[activeURI].manager.reloadFacets();
					fm = managers[activeURI].manager;
					//self.printActiveFilters();
                    self.printBreadcrumbs();
                    /*
					self.printActive();
                    self.printPath();
                    self.printRelated();
                    */
				}
		);
	};
	
	self.getActiveManager = function(){
		return activeManager;
	}
	
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
	
	self.filterProperty = function(facetID, propertyValue, vlabel){
		activeManager.filterProperty(facetID, propertyValue, vlabel);
	};
	
	self.removeProperty = function(typeUri, propertyUri, propertyValue, vlabel){
		managers[typeUri].manager.filterProperty(propertyUri, propertyValue, vlabel);
	};
	
	self.reloadFacets = function(){
		activeManager.reloadFacets();
	};
	
	self.deletePivotFacet = function(range){
		delete(managers[range]);
		activeURI = mainManager.getTypeUri();
		activeManager = mainManager;
		//TODO: Missing property URI
		// mainManager.deletePivotedFacet(uri);
		activeManager.renderFacets("facets");		
		mainManager.reloadFacets();
		self.printBreadcrumbs();
	};

    /*
	self.printActiveFilters = function(){
		activeManager.printActiveInit();
	};
	*/
	
	self.filterInitProperty = function(variable, property, value){
		uri = vars_uris[variable];
		managers[uri].manager.filterInitProperty(property,value);
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
        /*self.printPath();*/
        /*
        $j("#context").block({
            message: 'Processing... <img src="/images/black-loader.gif"/>',
            css: { border: '1px solid grey', backgroundColor: '#ffffff' }
        });
        */
        $j.blockUI();
        self.printActive();
        //self.printRelated();
    };

	self.makeRestrictions = function(uri){
		var query = "";
		for(var m in managers){
			query += managers[m].manager.makeRestrictions(uri);
		}
		return query;
	};

    self.printActive = function(){
        countQuery = "SELECT (COUNT(DISTINCT(?"+mainManager.getVariable()+")) as ?count) "+
            "WHERE { "+
            "?"+mainManager.getVariable()+" a <"+mainManager.getTypeUri()+"> . ";
        countQuery += self.makeRestrictions();
        countQuery += "}";

        console.log(countQuery);


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
                pivotedText = " is actor of "+makeLabel(managers[m].propertyURI, false);
				html += "<br/>has "+makeLabel(managers[m].propertyURI, false)+" ";
				html += managers[m].manager.printActive(false);
				
			}
		}
        html += "<span style=\"margin-left:10px;\"><a href=\"\">Reset all filters <img src='/images/delete_blue.png'/></a></span>";
		$j("#active_facets").append(html);
        self.printRelated();
	};



    self.printPath = function (counts){
        var html = "<ul><li><a href=\"/\">Home</a></li>";
        for(m in managers){
            html += "<li class=\"path\"><span>>></span></li>";
            if(managers[m].manager==mainManager)
                html += "<li class='active'><span>"+managers[m].manager.getLabel()+" ("+self.getNumResults()+")</span></li>";
            else
                html += "<li><a href=\"javascript:facetBrowser.pivotActiveFacet('"+m+"')\">"+managers[m].manager.getLabel()+"</a></li>";
        }
        html += "</ul>";
        $j("#breadcrumbs").html(html);
    };

    self.printRelatedCallback = function(){
        self.printPath();
        var html = "";
        var navigableFacets = mainManager.getNavigableFacets();
        var links = {};
        for(f in navigableFacets){
            links[navigableFacets[f].getRange()] = true;
            if(navigableFacets[f].isInverse())
                html += "<li><a href=\"javascript:facetBrowser.pivotInverseFacet('"+navigableFacets[f].getClassUri()+"','"+navigableFacets[f].getUri()+"','"+navigableFacets[f].getRange()+"');\">"+makeLabel(navigableFacets[f].getRange())+"</a></li>";
            else
                html += "<li><a href=\"javascript:facetBrowser.pivotFacet('','"+navigableFacets[f].getUri()+"','"+navigableFacets[f].getRange()+"');\">"+navigableFacets[f].getLabel()+"</a></li>";
        }

        for(m in managers){
            if(managers[m].manager!=mainManager && !links[m])
                html += "<li><a href=\"javascript:facetBrowser.pivotFacet('','','"+m+"');\">"+managers[m].manager.getLabel();+"XX</a></li>";
        }

        html += "</ul>";

        $j("#connections").popover('destroy');
        $j("#connections").popover({
            offset: 10,
            title: 'Connections to related resources',
            animation: true,
            trigger: 'manual',
            html: true,
            placement: "bottom",
            content: html,
            template: '<div id="connections-popover" class="popover" onmouseover="$j(this).mouseleave(function() {$j(this).hide(); });"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'

        }).click(function(e) {
                e.preventDefault() ;
            }).mouseenter(function(e) {
                $j(this).popover('show');
            });
        /*
         $j("#connections").mouseout(function(e){
         if ($j('#connections-popover').is(':hover')) {
         alert("hello");
         }
         else
         $j(this).popover('hide');
         });
         */
        $j.unblockUI();
        /*$j("#context").unblock();*/
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
        activeManager.reloadFacets();
        mainManager = activeManager;
        self.printBreadcrumbs();
    };
	
	self.pivotFacet = function(classURI, propertyURI, range){
        $j('#connections').popover('hide');
        if(managers[range]){
			activeManager = managers[range].manager;
			activeManager.renderFacets("facets");
			activeManager.reloadFacets();
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
        /*
		self.printActive();
        self.printPath();
        */
	};
	
	self.pivotInverseFacet = function(classURI, propertyURI, range){
        $j('#connections').popover('hide');
		if(managers[range]){
			activeManager = managers[range].manager;
			activeManager.renderFacets("facets");
			activeManager.reloadFacets();
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
        /*
        self.printActive();
        self.printPath();
        */
    };
	
	return self;
};