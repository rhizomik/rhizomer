facet.StringFacet = function(property){
		
	var that = new facet.Facet(property);
	var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";	

	that.dataSource = new YAHOO.util.XHRDataSource(rhz.getBaseURL());
	that.dataSource.connMgr.initHeader('Accept', 'application/rdf+xml', true);
	that.dataSource.responseType = YAHOO.util.XHRDataSource.TYPE_XML;
	that.dataSource.parseXMLData = function ( oRequest , oFullResponse ) {
		var rsArray = processResults(oFullResponse);
		return { results: rsArray, error: false};
	};
	that.dataSource.responseSchema = {fields : ["label","uri","n"]};
		
	that.handler = function(sType, aArgs) {
        var myAC = aArgs[0]; // reference back to the AC instance
        var elLI = aArgs[1]; // reference to the selected LI element
        var oData = aArgs[2]; // object literal of selected item's result data
        fm.filterProperty(searchProperty,oData.uri,oData.label);
        $j("#"+that.getId()+"_search").val("");
    };
    
    that.makeSPARQL = function (varCount){
    	var query = "?r <"+that.getUri()+"> ?var"+varCount+ " FILTER(";
    	for(value in that.getSelectedValues()){
    		query+="str(?var"+varCount+")=\""+addSlashes(value)+"\" ||";
    	}
    	query = query.substring(0,query.length-2);
    	query += ") ."
    	return query;
	};    
    
	that.render = function (target){
		that.renderBase(target);
		that.renderString(that.getId()+"_div");		
		that.renderValueList(that.getId()+"_div");
		that.renderEnd(target);		
	};
	
	that.renderString = function (target){
		var html = "<div class=\"facet_form\">";
		html += "<input class=\"text-box\" type=\"text\" id=\""+that.getId()+"_search\" title=\"search...\" />";
		html += "<div id=\""+that.getId()+"_container\">";
		html += "</div>";
		html += "<input type=\"hidden\" id=\""+that.getId()+"_hidden\"/>";
		html += "<input type=\"hidden\" id=\""+that.getId()+"_hidden_label\"/>";
		html += "</div>";
		$j("#"+target).append(html);
		
		that.autoComplete = new YAHOO.widget.AutoComplete(that.getId()+"_search",that.getId()+"_container", that.dataSource);
		that.autoComplete.itemSelectEvent.subscribe(that.handler);
		that.autoComplete.animVert = false;
		that.autoComplete.resultTypeList = false;
		
		that.autoComplete.formatResult = function(oResultData, sQuery, sResultMatch) {
			    return (sResultMatch + " (" +  oResultData.n + ")");
			};
			
		that.autoComplete.textboxFocusEvent.subscribe ( function () {
			searchProperty = fm.getUriById([(this.getInputEl().id).replace("_search","")]);
		} );
		
		that.autoComplete.maxResultsDisplayed = 20;
		that.autoComplete.minQueryLength = 2;
		that.autoComplete.queryDelay = 0.5;
		that.autoComplete.typeAhead = false;
		that.autoComplete.generateRequest = function(sQuery) {
			var query = 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
				"SELECT ?uri ?label (COUNT(?uri) AS ?n) \n"+
				"WHERE{"+
				"?r a <[uri]>; <[property]> ?uri . \n"+
				fm.makeRestrictions(searchProperty)+
				"OPTIONAL{ \n"+
				"?uri rdfs:label ?label . FILTER(LANG(?label)='en' || LANG(?label)='') } . \n"+
				"FILTER (REGEX(str(?label), '[query]','i') || REGEX(str(?uri), '[query]','i')) \n"+  
				"} GROUP BY ?uri ?label ORDER BY DESC (?n)";
			query = query.replace(/\[query\]/g, replaceDot(addSlashes(decodeURIComponent(sQuery))));
			query = query.replace(/\[uri\]/g, activeURI);
			query = query.replace(/\[property\]/g, searchProperty);
		    return "?query="+encodeURIComponent(query);
		};	
	};
	
	function processResults(resultsXMLDoc)
	{
		var solutions = resultsXMLDoc.getElementsByTagNameNS(rsNS,"solution");
			
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
		var bindings = solutionElem.getElementsByTagNameNS(rsNS,"binding");
		for (var i = 0; i < bindings.length; i++) 
        {
            var variable = bindings[i].getElementsByTagNameNS(rsNS,"variable")[0].textContent;
            var valueEl = bindings[i].getElementsByTagNameNS(rsNS,"value")[0];
            var value;
            if (valueEl.hasAttribute("rdf:resource"))
                    value = valueEl.getAttribute("rdf:resource");
            else
                    value = valueEl.textContent;
            solution[variable] = value;
    }
		if(!solution["label"])
			solution["label"] = makeLabel(solution["uri"]);
		return solution;
	};	
	
	return that;
};