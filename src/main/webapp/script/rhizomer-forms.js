/*
 * Rhizomer AJAX
 *
 * Author: http://rhizomik.net/~roberto
 * 
 */

// TODO: convert from singleton to prototype initialised with rhizomer object and the SPARQLEndpoints to 
// autocomplete from.

/****************************************************************************
 * Rhizomer SemanticForms Singleton
 ****************************************************************************/
rhizomik.SemanticForms = function()
{
	/**
	 * Private Attributes
	 */
	var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";
	
	// Load the list of generic properties, applicable to any resource, just when the edit
	// form for RDF is triggered (rdf2form.xsl) using the getGenericProperties() method
	var genericProperties = [];
	
	/**
	 * Private Methods
	 */
	function isAnon(uri)
	{
		return (uri.indexOf("_:")==0);
	};

	function baseURL(url)
	{
	    return url.substring(0, url.lastIndexOf("/")+1);
	};

	function clearSelect(options)
	{
	    for(i=0; i<options.length; i++)
		options[i].selected = false;
	    options[0].selected = true;
	};
	
	function selectText(options, text)
	{
	    for(i=0; i<options.length; i++)
	    {
		if (options[i].text == text)
		    options[i].selected = true;
	    }
	};
	
	function processResults(resultsXMLDoc)
	{
		var solutions = resultsXMLDoc.getElementsByTagNameNS(rsNS,"solution");
			
		var results=[];					
		for(var i=0; i<solutions.length; i++)
		{
			var result = processSolution(solutions[i]);
			if (!result.label && result.uri) 
				result.label = rhizomik.Utils.uriLocalname(result.uri);
			if (result.range && !result.rlabel)
				result.rlabel = rhizomik.Utils.uriLocalname(result.range);
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
		return solution;
	};
		
	/**
	 * Public Methods
	 */
    return {
	    formToNTriples: function (form) {
			// If identifier not defined, use blank node
			var bNodeNum = 1;
			var baseID = '_:blank' + bNodeNum++;
			
		    var triples = '';
		    for (i=0; i<form.elements.length; i++)
		    {
				if (form.elements[i].name == 'http://www.w3.org/1999/02/22-rdf-syntax-ns#about' ||
						 form.elements[i].name == 'http://www.w3.org/1999/02/22-rdf-syntax-ns#ID')
				{
					if (form.elements[i].type=='text') 					// Get Base ID
						baseID = identifier = form.elements[i].value;
					else if (form.elements[i].value=='')				// Return to Base ID
						identifier = baseID;
					else
						identifier = form.elements[i].value;			// Use local ID, usually anonymous
					
					if (!isAnon(identifier)) 							// If not anonymous ID
						identifier = '<'+identifier+'>';
				}
				else if (form.elements[i].name == 'newProperty')
					;//Ignore, input field for new property, process  its value in corresponding input field below (object or literal)
				else if (form.elements[i].name == 'lang')
					;//Ignore, already processed with the corresponding literal
				else if ((form.elements[i].className=='object' || form.elements[i].className=='literal') && 
						 form.elements[i].value!='')
				{
					triples += identifier+' <'+form.elements[i].name+'> ';
					if (form.elements[i].className=='literal')
					{
						var literal = form.elements[i].value.replace(/\"/g,"'");
						if (literal.indexOf("'")==0 && literal.lastIndexOf("'")==literal.length-1)
							literal = literal.substring(1, literal.length-1);
						triples += '"'+literal+'"';
						// Literal with language
						if (form.elements.length>i+1 && form.elements[i+1].name=='lang' && form.elements[i+1].value!='')
							triples += '@'+form.elements[i+1].value;
// TODO: datatyped literals: e.g. "23"^^http://www.w3.org/2001/XMLSchema#int
					}
					else if (form.elements[i].className=='object')
					{
						if (isAnon(form.elements[i].value))
							triples += form.elements[i].value;
						else //(isURI(form.elements[i].value))
							triples += '<'+form.elements[i].value+'>';
					}
					triples += ' .\n';
				}
		    }
		    return triples;
		},
		
		// Get the generic properties, those that can be applied to any resource, and
		// cache them in the genericProperties var
		getGenericProperties: function(rhz)
		{
			var genericPropertiesQuery = 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"+
				"SELECT DISTINCT ?uri ?label ?range WHERE \n"+ 
				"{ ?uri rdf:type ?t. \n"+
	   			"  OPTIONAL { ?uri rdfs:label ?label } \n"+
				"  OPTIONAL { ?uri rdfs:domain ?d } \n"+
				"  OPTIONAL { ?uri rdfs:range ?range } \n"+
				"  OPTIONAL { ?range rdfs:label ?rlabel } \n"+
				"  FILTER ( (?d=rdfs:Resource || !bound(?d)) && \n"+
				"           (?t = rdf:Property || ?t = owl:DatatypeProperty || ?t=owl:ObjectProperty || ?t=owl:AnnotationProperty) ) } \n"+
				"LIMIT 500";
						
			rhz.sparqlRDF(genericPropertiesQuery, function(out){ rhizomik.SemanticForms.getGenericPropertiesCallBack(out); });
		},
		getGenericPropertiesCallBack: function(response)
		{
			var XML = new rhizomik.XMLFactory();
			var doc = XML.createXMLDocFromText(response);
			genericProperties = processResults(doc);
		},
		
		// Show popup with recommended properties depending on the types of the resource
		addProperty: function(resource, resourceTypes)
		{
			var queryPattern = 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"+
				"SELECT DISTINCT ?uri ?label ?range ?rlabel WHERE { \n"+ 
				"{ ?uri rdfs:domain ?d. <[types]> rdfs:subClassOf ?d. \n"+
				"  OPTIONAL { ?uri rdfs:label ?label } \n"+
				"  OPTIONAL { ?uri rdfs:range ?range } \n"+
				"  OPTIONAL { ?range rdfs:label ?rlabel } \n"+
				"  FILTER (?d != rdfs:Resource) } \n"+
				"UNION { ?r rdf:type owl:Restriction; owl:onProperty ?uri. <[types]> rdfs:subClassOf ?r. \n"+ //OPTION(TRANSITIVE)
				"  OPTIONAL { ?uri rdfs:label ?label } \n"+
				"  OPTIONAL { ?r owl:allValuesFrom ?range } \n"+
				"  OPTIONAL { ?r owl:someValuesFrom ?range } \n"+
// TODO: owl:hasValue?
				"  OPTIONAL { ?range rdfs:label ?rlabel } } \n"+
				"  } \n"+
				"LIMIT 500";
// TODO: just the first resource type is considered right now			
			var query = queryPattern.replace(/\[types\]/g, resourceTypes[0]);
			rhz.sparqlRDF(query, function(out){ rhizomik.SemanticForms.addPropertyCallBack(out, resource, rhz); });
		},
		addPropertyCallBack: function(response, resource, rhz)
		{
			var XML = new rhizomik.XMLFactory();
			var doc = XML.createXMLDocFromText(response);
			var properties = processResults(doc);
			
			var lastRow = YAHOO.util.Dom.get(resource);
			var newRow = document.createElement("tr");
			var newCell1 = document.createElement("td");
			var newCell2 = document.createElement("td");
			newCell1.innerHTML = "<div><input name='newProperty' type='text'/><div></div></div>";
			newCell2.innerHTML = "<input disabled='true' type='text'/>";
			newRow.appendChild(newCell1);
			newRow.appendChild(newCell2);
			YAHOO.util.Dom.insertBefore(newRow, lastRow);
			var ac = YAHOO.util.Dom.getFirstChild(newCell1);
			var ac_input = YAHOO.util.Dom.getFirstChild(ac);
			var ac_container = YAHOO.util.Dom.getLastChild(ac);

			properties = properties.concat(genericProperties);
			var propertiesDS = new YAHOO.util.LocalDataSource(properties);
			propertiesDS.responseSchema = {fields : ["label","uri","range","rlabel"]};
			var autocomplete = new YAHOO.widget.AutoComplete(ac_input, ac_container, propertiesDS);
			autocomplete.resultTypeList = false;
			autocomplete.maxResultsDisplayed = 20;
			autocomplete.minQueryLength = 2;
			autocomplete.queryDelay = 0.5;
			autocomplete.typeAhead = true;
			autocomplete.formatResult = function(oResultData, sQuery, sResultMatch) {
			   	var sKey = sResultMatch;
			   	var sKeyRemainder = sKey.substr(sQuery.length);
			   	var aMarkup = ["<div class='ac-label'>",
			      sResultMatch,
			      "</div><div class='ac-range'>",
			      oResultData.rlabel,
			      "</div><div class='ac-uri'>",
			      oResultData.uri,
			      "</div>"];
			  	return (aMarkup.join(""));
			};
			
		    var myHandler = function(sType, aArgs) {
	    	    var myAC = aArgs[0]; // reference back to the AC instance
		        var elLI = aArgs[1]; // reference to the selected LI element
		        var oData = aArgs[2]; // object literal of selected item's result data
		    	
		        myAC.getInputEl().title = oData.uri;
		        
				var newCell1 = YAHOO.util.Dom.getAncestorByTagName(myAC.getInputEl(), "td");
				var newCell2 = YAHOO.util.Dom.getNextSibling(newCell1);
				
				if (oData.range == null) oData.range = "http://www.w3.org/2000/01/rdf-schema#Resource";

				if (oData.range == "http://www.w3.org/2000/01/rdf-schema#Literal")
					newCell2.innerHTML = "<input class='literal' name='"+oData.uri+"' type='text'/>" +
						"<select name='lang'><option></option><option value='en'>en</option><option value='es'>es</option></select>";
				else if (oData.range.indexOf("http://www.w3.org/2001/XMLSchema#")>=0)
					newCell2.innerHTML = "<input class='literal' name='"+oData.uri+"' type='text'/>" +
					"<select name='datatype'><option value='"+oData.range+"'>"+
					oData.rlabel+"</option></select>";
				else
				{
					newCell2.innerHTML = "<div><input type='text'/><input type='hidden' class='object' name='"+oData.uri+"'/><div></div></div>";
					var acElem = YAHOO.util.Dom.getFirstChild(newCell2);
					var resourceAC = rhizomik.SemanticForms.resourceTypeAutocomplete(acElem, oData.range);
					resourceAC.getInputEl().focus();
				}
		    };
		    autocomplete.itemSelectEvent.subscribe(myHandler);
		    autocomplete.getInputEl().focus();
		},
		
		// Create autocomplete for elemId based on range for propertyURI
		propertyValueAutocomplete: function (elemId, domainTypes, propertyURI)
		{
			var autocompleteElem = YAHOO.util.Dom.get(elemId);
			var queryPattern = 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"+
				"SELECT DISTINCT ?range WHERE { \n"+ 
				"{ <[propertyURI]> rdfs:range ?range } \n"+
				"UNION { ?r rdf:type owl:Restriction; owl:onProperty <[propertyURI]>. ?t rdfs:subClassOf ?r. \n"+
				"  OPTIONAL { ?r owl:allValuesFrom ?range } \n"+
				"  OPTIONAL { ?r owl:someValuesFrom ?range } \n"+
// TODO: owl:hasValue?
				"  FILTER (?t = <[domainType]>) } } \n"+
				"LIMIT 5";
			var query = queryPattern.replace(/\[propertyURI\]/g, propertyURI);
// TODO: Just the first domain type is considered
			query = query.replace(/\[domainType\]/g, domainTypes[0]);
			rhz.sparqlRDF(query, function(out){ 
				rhizomik.SemanticForms.propertyValueAutocompleteCallBack(out, autocompleteElem); });
		},
		propertyValueAutocompleteCallBack: function (response, autocompleteElem)
		{
			var XML = new rhizomik.XMLFactory();
			var doc = XML.createXMLDocFromText(response);
			var ranges = processResults(doc);
			var range = "http://www.w3.org/2000/01/rdf-schema#Resource";
// TODO: just the first range is considered right now
			if (ranges.length > 0 && ranges[0].range)
				range = ranges[0].range;
			if (range=="http://www.w3.org/2002/07/owl#Thing" && ranges.length > 1 && ranges[1].range)
				range = ranges[1].range;
			rhizomik.SemanticForms.resourceTypeAutocomplete(autocompleteElem, range);
		},
		
		// Create autocomplete at elem for resources of a given type and optionally a value label
		resourceTypeAutocomplete: function (elem, resourceType)
		{
			var ac_input = YAHOO.util.Dom.getFirstChild(elem);
			var ac_container = YAHOO.util.Dom.getLastChild(elem);
			
			var resourcesDS = new YAHOO.util.XHRDataSource(rhz.getBaseURL());
			resourcesDS.connMgr.initHeader('Accept', 'application/rdf+xml', true);
			resourcesDS.responseType = YAHOO.util.XHRDataSource.TYPE_XML;
			resourcesDS.parseXMLData = function ( oRequest , oFullResponse ) {
				var rsArray = processResults(oFullResponse);
				return { results: rsArray, error: false};
			};
			resourcesDS.responseSchema = {fields : ["label","uri"]};
			
			var autocomplete = new YAHOO.widget.AutoComplete(ac_input, ac_container, resourcesDS, 
				{resourceType: resourceType});
			autocomplete.resultTypeList = false;
			autocomplete.maxResultsDisplayed = 20;
			autocomplete.minQueryLength = 2;
			autocomplete.queryDelay = 0.5;
			autocomplete.typeAhead = false;
			autocomplete.generateRequest = function(sQuery) {
				var queryPattern = 
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
					"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"+
					"SELECT DISTINCT ?uri ?label WHERE { \n"+ 
					"   ?uri rdf:type <[type]> .\n"+
					"   OPTIONAL { ?uri rdfs:label ?label } \n"+
					"   FILTER (REGEX(?label, '( |^)[query].*','i') || REGEX(STR(?uri), '(#|/)[query].*','i')) }";
				var queryPatternResource = // Query pattern when no info about rdf:type, i.e. a rdfs:Resource
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
					"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"+
					"SELECT DISTINCT ?uri ?label WHERE { \n"+ 
					"   { ?uri rdf:type [] .\n"+
					"   FILTER (REGEX(STR(?uri), '(#|/)[query].*','i')) } \n"+
					"   UNION \n"+
					"   { ?uri rdfs:label ?label .\n"+
					"   FILTER (REGEX(?label, '( |^)[query].*','i')) } }";
				
				var resourceType = this.resourceType;
				var query = "";
				if (resourceType != "http://www.w3.org/2000/01/rdf-schema#Resource")
				{
					query = queryPattern.replace(/\[query\]/g, sQuery);
					query = query.replace(/\[type\]/g, resourceType);
				}
				else
					query = queryPatternResource.replace(/\[query\]/g, sQuery);
				
			    return "?query="+encodeURIComponent(query);
			};
			autocomplete.formatResult = function(oResultData, sQuery, sResultMatch) {
			   	var sKey = sResultMatch;
			   	var sKeyRemainder = sKey.substr(sQuery.length);
			   	var aMarkup = ["<div class='ac-label'>",
			      sResultMatch,
			      "</div><div class='ac-uri'>",
			      oResultData.uri,
			      "</div>"];
			  	return (aMarkup.join(""));
			};
			var itemSelectEventHandler = function(sType, aArgs) {
	    	    var myAC = aArgs[0]; // reference back to the AC instance
		        var elLI = aArgs[1]; // reference to the selected LI element
		        var oData = aArgs[2]; // object literal of selected item's result data
		        var ac_hidden = YAHOO.util.Dom.getNextSibling(myAC.getInputEl());
		        ac_hidden.value = oData.uri;
	        	myAC.getInputEl().className="yui-ac-input"; // In case input contained invalid URI and marked with class "input-error"
		        myAC.getInputEl().title = oData.uri; //Set title for tooltip
		    };
		    autocomplete.itemSelectEvent.subscribe(itemSelectEventHandler);
		    
		    var unmatchedItemSelectEventHandler = function(sType, aArgs) {
	    	    var myAC = aArgs[0]; // reference back to the AC instance
		        var ac_hidden = YAHOO.util.Dom.getNextSibling(myAC.getInputEl());
		        var s = myAC.getInputEl().value;
		        if (rhizomik.Utils.isURI(s))
		        {
		        	ac_hidden.value = myAC.getInputEl().value;
		        	myAC.getInputEl().className="yui-ac-input";
			        myAC.getInputEl().title = s; //Set title for tooltip
		        }
		        else
		        {
		        	ac_hidden.value = "";
		        	myAC.getInputEl().className="input-error";
			        myAC.getInputEl().title = "Invalid URI: "+s; //Set title for tooltip
		        }
		    };
		    autocomplete.unmatchedItemSelectEvent.subscribe(unmatchedItemSelectEventHandler);
		    
		    return autocomplete;
		},

		// Generate a SPARQL query from a query form
		formToSPARQL: function (form)
		{
		    var wheres = "\nWHERE ";
		    var filters = "\nFILTER (";
		    var first = true;
		    for (i=0; i<form.elements.length; i++)
		    {
				if (form.elements[i].type=='text' && form.elements[i].value!="")
				{
				    if (first)
				    {
				    	wheres += '{\n';
				    	first=false;
				    }
				    else
				    { 
				    	wheres += '.\n '; 
				    	filters += '  &&  '; 
				    }
			
				    if (isURI(form.elements[i].value))
				    {
				        wheres += '?r   <'+form.elements[i].name+'>   ?x'+i;
				        filters += '?x'+i+' = <'+form.elements[i].value+'>';
				    }    
				    else
				    {
				    	if (form.elements[i].value.indexOf('*')>=0 || form.elements[i].value.indexOf('?')>=0)
				    	{
							x=form.elements[i].value.replace("*",".*");
							y=x.replace("?",".");
						    wheres += '?r   <'+form.elements[i].name+'>   ?x'+i;
						    filters += 'regex (?x'+i+', "'+y+'","i")';
						}
				    	else
				    	{
				    		wheres += '?r   <'+form.elements[i].name+'>   ?x'+i;
				    		//filters += '?x'+i+' = "'+form.elements[i].value+'"';
				    		filters += 'regex (?x'+i+', "'+form.elements[i].value+'","i")';
				    	}        
				    }
				}
				else if (form.elements[i].type=='select-one' && 
					 form.elements[i].options[form.elements[i].selectedIndex].value!="")
				{
				    if (first)
				    {
				     wheres += '{\n';
				     first=false;
				    }
				    else
				    { wheres += '.\n '; 
				      filters += '  &&  '; 
				    }		
				    
				    var selectedValue = form.elements[i].options[form.elements[i].selectedIndex].value;
				    if (isURI(selectedValue))
				    {	
				        wheres += '?r   <'+form.elements[i].name+'>   ?x'+i;
				        filters += '?x'+i+' = <'+selectedValue+'>';
				    }     
				    else
				    {
						 wheres += '?r   <'+form.elements[i].name+'>   ?x'+i;
				         filters += '?x'+i+' = "'+selectedValue+'"';
				    }
				}
		    }
		    var query = "DESCRIBE ?r "+wheres+'.'+filters+')\n}';
		    return query;			
		}
    };
}();

