facet.InverseFacet = function(property, inVariable, classURI){
		
	var self = new facet.Facet(property, fm, classURI);

	/**
	 * Private Attributes
	 */	
	var id = hex_md5(property.uri+property.range);
	var uri = property.uri;
	var range = property.range;
	var valueList = {};
	var selectedValues = {};
	var numValues = 0;
	var numSelectedValues = 0;
	var selected = false;
	var opened = false;
	var initValues = new Array();
	var variable = inVariable;
	var classURI = classURI
	var type = property.type;
	var range = property.range;
	var inverseClassUri = property.classUri;
	//var propertyLabel = property.label;
	//var label = "Is "+propertyLabel+ " in "+makeLabel(inverseClassUri);
	var label = property.label;
	var inverseVariable = "i1";

    var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";

    var inverse = true;

    self.dataSource = new YAHOO.util.XHRDataSource(rhz.getBaseURL());
    self.dataSource.connMgr.initHeader('Accept', 'application/rdf+xml', true);
    self.dataSource.responseType = YAHOO.util.XHRDataSource.TYPE_XML;
    self.dataSource.parseXMLData = function ( oRequest , oFullResponse ) {
        var rsArray = processResults(oFullResponse);
        return { results: rsArray, error: false};
    };
    self.dataSource.responseSchema = {fields : ["label","uri","n"]};
	
	self.getId = function(){
		return id;
	};
	
	self.getClassUri = function(){
		return classURI;
	};

	self.getLabel = function(){
		return label;
	};
	
	self.getUri = function(){
		return uri;
	};
	
	self.isInverse = function(){
		return inverse;
	}
	
	self.getRange = function(){
		return range;
	};
	
	self.getSelectedValues = function(){
		return selectedValues;
	};	
	
	self.isActive = function(){
		if(numSelectedValues>0)
			return true;
		else 
			return false;
	};
	
	self.setSelected = function(value){
		selected = value;
	};
	
	self.isSelected = function(){
		return selected;
	}
	
	self.getCurrentValues = function(){
		return numValues;
	}
	
	self.isOpened = function(){
		return opened;
	};

    self.isNavigable = function(){
        if(range.indexOf("http://www.w3.org/2001/XMLSchema#")<0 &&
           range.indexOf("http://www.w3.org/2000/01/rdf-schema#Literal")<0)
            return true;
        else
            return false;
    };
	
	self.printInitActiveLabels = function(){
		var queryValues = new Array();
		for(i=0; i<initValues.length; i++){
			if(initValues[i].startsWith("http://"))
				queryValues.push("<"+initValues[i]+">");
			else{
				html = "<li><a onclick=\"javascript:facetBrowser.filterProperty('"+id+"','"+initValues[i]+"'); return false;\">";
				html += makeLabel(initValues[i])+ " [x]</a></li>";
				$j("#"+id+"_active").append(html);						
			}
		}
		if(queryValues.length>0){
			var query = "SELECT ?r ?label where{?r <http://www.w3.org/2000/01/rdf-schema#label> ?label . FILTER(?r = " + queryValues.join(" || ?r = ") + ")}"; 
			rhz.sparqlJSON(query, function(out){
				data = out.evalJSON();
				for(i=0; i<data.results.bindings.length; i++){
					r = data.results.bindings[i].r.value;
					var label = data.results.bindings[i].label.value;				
					html = "<li><a onclick=\"javascript:facetBrowser.filterProperty('"+id+"','"+r+"'); return false;\">";
					html += label+ " [x]</a></li>";
					$j("#"+id+"_active").append(html);		
					fm.setSelectedFacetLabel(id,r,label);
				}
			});
		}
	};
	
	self.addInitValue = function(value){
		initValues.push(value);
		self.toggleValue(value);
	};
	
	self.resetFacet = function(){
		numValues = 0;
		selected = false;
		valueList = {};
		$j("#"+id+"_ul").empty();
	};	
	
	self.renderBase = function(target){
		var html = "<div id=\""+id+"_facet\" class=\"facet\">";
		html += "<div id=\""+id+"_title\" class=\"facet_header\">";
		html += "<span id=\""+id+"_toggle\" class=\"facet_title\">" +
				"<h4 onclick=\"facetBrowser.toggleFacet('"+id+"'); return false;\">"+label+"</h4></span>";
        html += "<span id=\""+id+"_showvalues\" class=\"showvalues\" onclick=\"facetBrowser.toggleFacet('"+id+"'); return false;\">Common values</span>";
		html += "<span id=\""+id+"_inversepivot\" class=\"pivot\">Filter "+makeLabel(range)+"</span>";
		html += "<div class=\"clear\"></div>";
		html += "</div>";
		html +="<div id=\""+id+"_loading\"></div>";
		html +="<div class=\"facet_options\" id=\""+id+"_div\"></div>";
		$j("#"+target).append(html);
		$j("#"+id+"_inversepivot").click(function (){
			self.pivotInverseFacet();
		});		
	};
	
	self.renderValueList = function(target){
		var html = "<div id=\""+id+"_values\"><ul id=\""+id+"_ul\" class=\"values\"></ul>";
		html += "<div class=\"more\"><a id=\""+id+"_more\" href=\"#\" >more values</a></div>";
		html+="</div>"
		$j("#"+target).append(html);
		$j("#"+id+"_more").click(function (){
			self.getMoreValues();
		});
	};
	
	self.renderEnd = function(target){
		html ="</div><div class=\"facet_sep\"></div>";
		$j("#"+target).append(html);	
	};	
	
	self.toggleFacet = function(){
		if(opened){
			opened = false;
			$j("#"+id+"_div").hide();
			$j("#"+id+"_loading").empty().hide();
		}
		else{
			opened = true;
			if(numValues==0)
				self.getMoreValues();
			else
				$j("#"+id+"_div").show();	
		}
	};
	
	self.toggleValue = function(value){
		valueId = hex_md5(value);
		if(selectedValues[value]){
			delete selectedValues[value];
			numSelectedValues--;
			$j("#"+valueId).removeClass("selected_item");
			$j("#"+valueId).addClass("item");			
			return false;
		}
		else{
			selectedValues[value] = true;
			numSelectedValues++;
			$j("#"+valueId).removeClass("item");
			$j("#"+valueId).addClass("selected_item");
			return valueList[value];
		}	
	};	
	
	self.pivotInverseFacet = function(){
		facetBrowser.pivotInverseFacet(classURI, uri, range);
	};	
	
	self.reloadValues = function(restrictions){
		$j("#"+id+"_div").css('display','none');
		$j("#"+id+"_loading").append("<img src=\"images/black-loader.gif\"/>");
		$j("#"+id+"_loading").show();			
		self.resetFacet();
		query =
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		    "SELECT (?"+inverseVariable+" as ?r) (COUNT(?"+inverseVariable+") AS ?n) ?label "+
		    "WHERE {"+
		    "	?"+variable+" a <"+inverseClassUri+"> . "+
		    "   ?"+inverseVariable+" <"+uri+"> ?"+variable+" . "+
		    "   OPTIONAL{ ?"+inverseVariable+" rdfs:label ?label " +
		    "      FILTER(LANG(?label)='en' || LANG(?label)='') } ."+
		    restrictions+
		    " } GROUP BY ?"+inverseVariable+" ?label ORDER BY DESC(?n) LIMIT 6";
		rhz.sparqlJSON(query, self.processMoreValues);
	};
	
	self.getMoreValues = function(){
		$j("#"+id+"_div").css('display','none');
		$j("#"+id+"_loading").append("<img src=\"images/black-loader.gif\"/>");
		$j("#"+id+"_loading").show();				
		query =
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
			"SELECT (?"+inverseVariable+" as ?r) (COUNT(?"+inverseVariable+") AS ?n) ?label "+
		    "WHERE { "+
		    "   ?"+variable+" a <"+inverseClassUri+"> . "+
		    "   ?"+inverseVariable+" <"+uri+"> ?"+variable+" . "+
		    "   OPTIONAL{ ?"+inverseVariable+" rdfs:label ?label . " +
		    "      FILTER(LANG(?label)='en' || LANG(?label)='') } " +
		    facetBrowser.makeRestrictions(uri) +
		    " } GROUP BY ?"+inverseVariable+" ?label ORDER BY DESC(?n) LIMIT 6 OFFSET "+self.getCurrentValues();
		rhz.sparqlJSON(query,self.processMoreValues);
	};
	
	self.processMoreValues = function(output){
		data = output.evalJSON();
		if(data.results.bindings.length > 0){
			$j("#"+id+"_facet").show();
			$j.each(data.results.bindings, function(i, option){
				if(i<5){
					if(option.label)
						tlabel = option.label.value;
					else
						tlabel = makeLabel(option.r.value);
					self.addValueToList(option.r.value, tlabel, option.n.value);
				}
			});
			$j("#"+id+"_loading").empty();
			$j("#"+id+"_loading").hide();
			$j("#"+id+"_div").show();
			$j("#"+id+"_values div.loading").remove();
			if(data.results.bindings.length > 5)
				$j("#"+id+"_more").show();
			else
				$j("#"+id+"_more").hide();
		}
		else{
			$j("#"+id+"_loading").empty();
			$j("#"+id+"_loading").append("<div>No facet values for the current selection</div>");
		}
	};	
	
	self.addValueToList = function(value, vlabel, instances){
		valueList[value] = new FacetValue(value, vlabel, instances); 
		if(selectedValues[value])
			cls = "selected_item";
		else
			cls = "item";
		html = "<li class=\""+cls+"\" id=\""+hex_md5(value)+"\" onclick=\"javascript:facetBrowser.filterProperty('"+id+"','"+value+"'); return false;\">";
		html += "<div class='item_text'>"+vlabel+" ("+instances+")</div></li>";
		$j("#"+id+"_ul").append(html);
		numValues++;
	};	

	self.render = function (target){
		self.renderBase(target);
		self.renderString(self.getId()+"_toggle");
		self.renderValueList(self.getId()+"_div");
		self.renderEnd(target);		
	};	
	
	self.makeSPARQL = function (varCount, varName){
		var query = "";
    	for(value in self.getSelectedValues()){
    		query+= "<"+value+"> <"+self.getUri()+"> ?"+varName + " . ";
    	}
    	return query;
	};	
	
	self.inversePivotFacet = function(){
		facetBrowser.inversePivotFacet(inverseClassUri, uri, classURI);
	};

    self.handler = function(sType, aArgs) {
        var myAC = aArgs[0]; // reference back to the AC instance
        var elLI = aArgs[1]; // reference to the selected LI element
        var oData = aArgs[2]; // object literal of selected item's result data
        facetBrowser.filterProperty(facetBrowser.getAutoCompletePropertyID(),oData.uri,oData.label);
        $j("#"+self.getId()+"_search").val("");
    };

    self.renderString = function (target){
        var html = "<div class=\"facet_form\">";
        html += "<input class=\"text-box\" type=\"text\" id=\""+self.getId()+"_search\" value=\"Search "+makeLabel(range)+" values...\" />";
        html += "<div class=\"search_loading\" id=\""+self.getId()+"_search_loading\"></div>";
        html += "<div id=\""+self.getId()+"_container\">";
        html += "</div>";
        html += "<input type=\"hidden\" id=\""+self.getId()+"_hidden\"/>";
        html += "<input type=\"hidden\" id=\""+self.getId()+"_hidden_label\"/>";
        html += "</div>";
        $j("#"+target).append(html);

        self.autoComplete = new YAHOO.widget.AutoComplete(self.getId()+"_search",self.getId()+"_container", self.dataSource);
        self.autoComplete.itemSelectEvent.subscribe(self.handler);
        self.autoComplete.animVert = false;
        self.autoComplete.resultTypeList = false;
        self.autoComplete.textboxFocusEvent.subscribe(function(sType, aArgs) {
            var inputEl = aArgs[0].getInputEl();
            if (inputEl.value.indexOf("Search ")>=0 && inputEl.value.indexOf("values...")>0)
                inputEl.value = "";
            facetBrowser.setAutoCompleteProperty((this.getInputEl().id).replace("_search",""));
        });
        self.autoComplete.textboxBlurEvent.subscribe(function(sType, aArgs) {
            var inputEl = aArgs[0].getInputEl();
            if (inputEl.value == "")
                inputEl.value = inputElDefault;
        });

        self.autoComplete.formatResult = function(oResultData, sQuery, sResultMatch) {
            return (sResultMatch + " (" +  oResultData.n + ")");
        };

        self.autoComplete.maxResultsDisplayed = 20;
        self.autoComplete.minQueryLength = 2;
        self.autoComplete.queryDelay = 0.5;
        self.autoComplete.typeAhead = false;
        self.autoComplete.generateRequest = function(sQuery) {
            $j("#"+facetBrowser.getAutoCompletePropertyID()+"_search_loading").append("<img class=\"autocompleting\" src=\"images/black-loader.gif\"/>");
            var query =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
                    "SELECT ?uri ?label (COUNT(?uri) AS ?n) \n"+
                    "WHERE{"+
                    "?[variable] a <[uri]>. ?uri <[property]> ?[variable] . \n"+
                    facetBrowser.makeRestrictions(facetBrowser.getAutoCompletePropertyURI())+
                    "OPTIONAL{ \n"+
                    "?uri rdfs:label ?label . FILTER(LANG(?label)='en' || LANG(?label)='') } . \n"+
                    "FILTER (REGEX(str(?label), '[query]','i') || REGEX(str(?uri), '[query]','i')) \n"+
                    "} GROUP BY ?uri ?label ORDER BY DESC (?n)";
            query = query.replace(/\[query\]/g, replaceDot(addSlashes(decodeURIComponent(sQuery))));
            query = query.replace(/\[uri\]/g, facetBrowser.getActiveManager().getTypeUri());
            query = query.replace(/\[variable\]/g, facetBrowser.getActiveManager().getVariable());
            query = query.replace(/\[property\]/g, facetBrowser.getAutoCompletePropertyURI());
            return "?query="+encodeURIComponent(query);
        };
    };

    function xBrowserGetElementsByTagNameNS(elem, namespace, alias, name)
    {
        var elements;
        if (YAHOO.env.ua.ie > 0)
            elements = elem.getElementsByTagName(alias+":"+name);
        else
            elements = elem.getElementsByTagNameNS(namespace, name);
        return elements;
    };

    function xBrowserGetText(elem)
    {
        var text;
        if (YAHOO.env.ua.ie > 0)
            text = elem.text;
        else
            text = elem.textContent;
        return text;
    }

    function processResults(resultsXMLDoc)
    {
        $j("#"+facetBrowser.getAutoCompletePropertyID()+"_search_loading").empty();
        var solutions = xBrowserGetElementsByTagNameNS(resultsXMLDoc, rsNS, "rs", "solution");
        var results=[];
        for(var i=0; i<solutions.length; i++)
        {
            var result = processSolution(solutions[i]);
            if(result.label == null || result.label == "")
                result.label = result.uri;
            results[i] = result;
        }
        return results;
    };

    function processSolution(solutionElem)
    {
        var solution = {};
        var bindings = xBrowserGetElementsByTagNameNS(solutionElem, rsNS, "rs", "binding");
        for (var i = 0; i < bindings.length; i++)
        {
            var variableEl = xBrowserGetElementsByTagNameNS(bindings[i], rsNS, "rs", "variable")[0];
            var variable = xBrowserGetText(variableEl);
            var valueEl = xBrowserGetElementsByTagNameNS(bindings[i], rsNS, "rs", "value")[0];
            var value;
            if (valueEl.attributes.getNamedItem("rdf:resource")!=null)
                value = valueEl.attributes.getNamedItem("rdf:resource").value;
            else
                value = xBrowserGetText(valueEl);
            solution[variable] = value;
        }
        if(!solution["label"])
            solution["label"] = makeLabel(solution["uri"]);
        return solution;
    };

	return self;
};