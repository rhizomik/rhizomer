facet.StringFacet = function(property, fm, typeUri){
		
	var that = new facet.Facet(property, fm, typeUri);
	var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";	

	that.dataSource = new YAHOO.util.XHRDataSource(rhz.getBaseURL());
	that.dataSource.connMgr.initHeader('Accept', 'application/rdf+xml', true);
	that.dataSource.responseType = YAHOO.util.XHRDataSource.TYPE_XML;
	that.dataSource.parseXMLData = function ( oRequest , oFullResponse ) {
		var rsArray = processResults(oFullResponse);
		return { results: rsArray, error: false};
	};
	that.dataSource.responseSchema = {fields : ["label","uri","n"]};


    that.printActive = function(){
        html = "<b>"+that.getLabel()+"</b> is ";
        var i=0;
        values = that.getSelectedValues();
        console.log(values);
        for(uri in values){
            if(i>0)
                html += " or ";
            html += "<b>"+values[uri].label+"&nbsp;</b>"
            html += "<a class=\"pointer\" onclick=\"javascript:facetBrowser.removeProperty('"+that.getClassURI()+"','"+that.getId()+"','"+escape(uri)+"'); return false;\"><img src='/images/delete_blue.png'/></a>";
            i++;
        }
        return html;
    }

		
	that.handler = function(sType, aArgs) {
        var myAC = aArgs[0]; // reference back to the AC instance
        var elLI = aArgs[1]; // reference to the selected LI element
        var oData = aArgs[2]; // object literal of selected item's result data
        facetBrowser.filterProperty(facetBrowser.getAutoCompletePropertyID(),escape(oData.uri),escape(oData.label));
        $j("#"+that.getId()+"_search").val("");
    };
    
    that.makeSPARQL = function (varCount, varName){
    	var query = "?"+varName+" <"+that.getUri()+"> ?"+varName+"var"+varCount+ " FILTER(";
    	for(value in that.getSelectedValues()){
    		query+="str(?"+varName+"var"+varCount+")=\""+addSlashes(value)+"\" ||";
    	}
    	query = query.substring(0,query.length-2);
    	query += ") ."
    	return query;
	};    
    
	that.render = function (target){
		that.renderBase(target);
		/*that.renderString(that.getId()+"_toggle");*/
        that.renderString(that.getId()+"_facet");
        /*that.renderString(target);*/
        /*that.renderValueList(target);*/
		that.renderValueList(that.getId()+"_facet");
		that.renderEnd(target);		
	};
	
	that.renderString = function (target){
        /*var inputElDefault = "Search "+makeLabel(property.range)+" values...";*/
        var inputElDefault = "Search "+makeLabel(property.range)+"...";

		var html = "<div class=\"facet_form\">";
		html += "<input class=\"text-box\" type=\"text\" id=\""+that.getId()+"_search\" value=\""+inputElDefault+"\" />";
		html += "<div class=\"search_loading\" id=\""+that.getId()+"_search_loading\"></div>";
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
        that.autoComplete.textboxFocusEvent.subscribe(function(sType, aArgs) {
            var inputEl = aArgs[0].getInputEl();
            if (inputEl.value.indexOf("Search ")>=0)
                inputEl.value = "";
            facetBrowser.setAutoCompleteProperty((this.getInputEl().id).replace("_search",""));
        });
        that.autoComplete.textboxBlurEvent.subscribe(function(sType, aArgs) {
            var inputEl = aArgs[0].getInputEl();
            if (inputEl.value == "")
                inputEl.value = inputElDefault;
        });
		
		that.autoComplete.formatResult = function(oResultData, sQuery, sResultMatch) {
			    //return (sResultMatch + " (" +  oResultData.n + ")");
            return (sResultMatch);
			};
		
		that.autoComplete.maxResultsDisplayed = 20;
		that.autoComplete.minQueryLength = 2;
		that.autoComplete.queryDelay = 0.5;
		that.autoComplete.typeAhead = false;
		that.autoComplete.generateRequest = function(sQuery) {
			$j("#"+facetBrowser.getAutoCompletePropertyID()+"_search_loading").append("<img class=\"autocompleting\" src=\"images/black-loader.gif\"/>");
			var query = 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
				"SELECT distinct(?uri) ?label \n"+
				"WHERE{"+
				"?[variable] a <[uri]>; <[property]> ?uri . \n"+
				facetBrowser.makeRestrictions(facetBrowser.getAutoCompletePropertyURI())+
				"OPTIONAL{ \n"+
				"?uri rdfs:label ?label . FILTER(LANG(?label)='en' || LANG(?label)='') } . \n"+
				"FILTER (REGEX(str(?label), '[query]','i') || REGEX(str(?uri), '[query]','i')) \n"+  
				"}";
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
	
	return that;
};