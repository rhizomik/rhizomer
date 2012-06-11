/*
 * Rhizomer AJAX
 *
 * Author: http://rhizomik.net/~roberto
 * Author: Juanma Giménez
 * 
 */

// TODO: convert from singleton to prototype initialised with rhizomer object and the SPARQLEndpoints to 
// autocomplete from.

/****************************************************************************
 * Rhizomer SemanticForms Singleton
 ****************************************************************************/


YUI().use('autocomplete', 'autocomplete-highlighters', 'autocomplete-filters', 'autocomplete-list', 'node', 'event', 'event-valuechange', 'event-key', 'datasource', 'json', function (Y) 
{
	rhizomik.SemanticForms = function()
	{					
		/**
		 * Private Attributes
		 */
		var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";
		
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
		
		function processRDFResults(resultsXMLDoc)
		{
			var solutions = resultsXMLDoc.getElementsByTagNameNS(rsNS,"solution");
			var results=[];					
			for(var i=0; i<solutions.length; i++)
			{
				var result = processSolution(solutions[i]);
				rhizomik.SemanticForms.addMissingFields(result);
				results[i] = result;
			}
			return results;
		};
		
		function processRDFSolution(solutionElem)
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
		
		function processJSONResults(jsonStrResults)
		{	
			var jsonData = Y.JSON.parse(jsonStrResults);	
			var resultsArray = jsonData.results.bindings;	
			for(var i=0; i<resultsArray.length; i++)		
				rhizomik.SemanticForms.addMissingFields(resultsArray[i]);
			jsonData.results.bindings = resultsArray; 
            return objectToJSONString(jsonData);            													
    	};
		
		function setFieldAttributes(newField, knownField)
		{	    
		    newField.type = "literal";
			newField.value = rhizomik.Utils.uriLocalname(knownField.value);
		};
		
		function objectToJSONString(jsonObject)
        {                  			                        
			var jsonString = Y.JSON.stringify(jsonObject, null, 3);	 
			jsonString = jsonString.replace(/\\"/g,'"');
			jsonString = jsonString.replace(/"\[/g,'[');         	//El método del YUI llena la cadena de barras de escape
			jsonString = jsonString.replace(/\]"/g,']');	        //y comillas innecesarias, las eliminamos
			return jsonString;
		};	
		
		function tempLocalUri (type, label)
        {
            return "http://rhizomik.net/" +
                    type.toLowerCase().replace(/ */g,"") + "/" + 
                    label.toLowerCase().replace(/ */g,""); 
        };
		
		function getResourceTypeLabel(typeLabel, type)
        {
            var typeName; 
            if (typeLabel)						
            	typeName = typeLabel;
            else if (type)			
            	typeName = rhizomik.Utils.uriLocalname(type);
            else 					
            {
                typeName = "Resource";
                type = "http://www.w3.org/2000/01/rdf-schema#Resource";
            }
            return typeName;			   
        };
    
        function newPropertyRow(element)
		{
            var lastRow = Y.one('[id=' + element + ']');			//recuperamos el nodo que contiene el enlace + a addProperty
			var newRow = Y.Node.create("<tr></tr>");
			var cell1 = Y.Node.create("<td></td>");
			var cell2 = Y.Node.create("<td></td>");
			cell1.setContent("<div><input name='newProperty' type='text'/></div>");	//entrada para la nueva propiedad que funcionará con autocomplete
			cell2.setContent("<input disabled='true' type='text'/>");				//entrada deshabilitada hasta la selección de una propiedad, entonces se habilitará para colocar el valor de la propiedad
			newRow.appendChild(cell1);
			newRow.appendChild(cell2);
			lastRow.insert(newRow, 'before');
			return newRow;
		};
		
        function createRow(type, propLabel, propUri, label, uri)	//crea una fila con una etiqueta y una entrada de texto 
        {
            var row = Y.Node.create("<tr></tr>");
            var labelCell = createLinkedLabel(propLabel, propUri); 
            if(type === "literal")
            	var valueCell = createLiteralInput(label, propUri);            
            else if (type === "resource")
                var valueCell = createACInput(label, propUri, uri);            
            row.appendChild(labelCell);
            row.appendChild(valueCell);
            return row;
        };

        function createLinkedLabel(label, uri)		//crea una etiqueta enlazada a la dirección especificada
        {
            var cell = Y.Node.create("<td></td>"); 
            cell.setContent('<a class="describe" ' +
            				'href="?query=DESCRIBE%20<' + uri + '>"' +
            				'onclick="javascript:' +
            				'rhz.describeResource(' + "'" + uri + "'" + ');' +
            				'return false;"' +
            				'title="Describe '+ uri +'">' + label + '</a>');
            return cell;
         };
         
         function createLiteralInput(value, name, cell)	//crea una entrada para literales 
         {												//sobre cell si se ha especificado,
              if (!cell)								// o nueva en caso contrario
            	  cell = Y.Node.create("<td></td>");  
              cell.setContent('<input type="text" value="' + value +
                                '" name="' + name + '" class="literal">' +
                                '<select name="lang"><option></option>' +
                                '<option value="en">en</option>' +
                                '<option value="es">es</option></select>');                
            return cell;
         }; 
          
         function createXMLSchemaInput(name, type, typeLabel, cell)
         {															//crea una entrada para tipos XMLSchema		
        	 if(!cell)												//sobre cell si se ha especificado,
        		 cell = Y.Node.create("<td></td>");					//o nueva en caso contrario
        	 cell.setContent("<input class='literal'" +
        			 		"name='" + name + "' type='text'/>" +
        			 		"<select name='datatype'>" +
        			 		"<option value='" + type + "'>" + typeLabel + "</option>" +
							"</select>");
        	 return cell;
         };
            
         function createACInput(value, name, title, cell)
         {													//crea una entrada para asignarle AutoComplete		
        	 if (!cell)										//sobre cell si se ha especificado,
        		 cell = Y.Node.create("<td></td>");  		//o nueva en caso contrario
        	 cell.setContent('<div><input type="text"' +
        			 			'value= "' + value + '" title= "' + title + '"' +
        			 			'autocomplete="off"/>' +
        			 			'<input class="object" type="hidden"' +
        			 			'name="' + name + '" value="' + title + '"/>' +
        			 			'</div>');
        	 return cell;
         };
       
         function createLink(id, link)					//crea una enlace
         {
        	var row = Y.Node.create("<tr></tr>");			
   	       	var cell = Y.Node.create("<td></td>");
   	       	row.set('id', id);		
   	       	row.appendChild(cell);
   	       	cell.setAttribute('colspan', '2');
   	       	cell.setContent('<a href="' + link + '">+</a>');
   	       	return row;       
         };
         
         function entitleValueInput(result, valueInput)		//Habilita la entrada para el valor de la propiedad seleccionada
         {
        	 if (result.range.value === null)				
        	 	result.range.value = "http://www.w3.org/2000/01/rdf-schema#Resource";
        	 if (result.range.value === "http://www.w3.org/2000/01/rdf-schema#Literal") 	
        	 	createLiteralInput("", result.uri.value, valueInput);
        	 else if (result.range.value.indexOf("http://www.w3.org/2001/XMLSchema#")>=0)	
        	 	createXMLSchemaInput(result.uri.value, result.range.value, result.rlabel.value, valueInput);
        	 else		//Si el rango no es Literal ni DataSchema, creamos un AutoComplete sobre la entrada deshabilitada
        	 {	
        		 createACInput("", result.uri.value, "", valueInput);
        		 var acElem = valueInput.one('*');				//firstChild de valueInput (<div></div>)
        		 var resourceAC = rhizomik.SemanticForms.resourceTypeAutocomplete(acElem, result.range.value, result.rlabel.value);
        		 acElem.one('*').focus();		
        	 }
         };
	    	
         function backspaceHandler (ac, restriction, dbpedia)	//resetea el nivel de restricción de tipo para consultas a DBpedia		
         {										
        	 if (dbpedia)
        	 {
        		 restriction.state = true;
        		 rhizomik.AutoComplete.setQueryPattern(ac, rhizomik.SemanticForms.dbpQuery, restriction.condition);
        	 }
         };	
		        
         function uriFieldValueHandler(uriCell, typeCell, labelCell)
         {		//Actualiza el campo URI cuando se modifica alguno de los demás campos del formulario
        	 var uriInput = uriCell.one('*');
        	 var labelInput = labelCell.one('*');
        	 var typeInput = typeCell.one('*');
        	 labelInput.on('valueChange', function (e){
        		 			updateUriField(uriInput, typeInput, labelInput);
        	 			});
        	 typeInput.on('valueChange', function (e){
        		 			updateUriField(uriInput, typeInput, labelInput);
                        });
         };
           
         function updateUriField(uriInput, typeInput, labelInput)
         {
        	 var updatedType = typeInput.get('value');	       		   
        	 var updatedLabel = labelInput.get('value');
        	 uriInput.set('value', tempLocalUri(updatedType, updatedLabel));
         };
         
         function levelDownTypeRestriction(ac, restriction)	//rebaja un nivel de restricción de tipo
        {	    			
        	if (restriction.typeUriQuery.state === true)		
        	{								//se asigna restricción de typeLabel
        		restriction.typeUriQuery.state = false;
        		restriction.typeLabelQuery.state = true;
        		rhizomik.AutoComplete.setQueryPattern(ac, rhizomik.SemanticForms.dbpQuery, restriction.typeLabelQuery.condition);
        		ac.sendRequest();					
        	}
        	else if (restriction.typeLabelQuery.state === true) 
        	{								//se elimina la restricción de tipo
        		restriction.typeLabelQuery.state = false;
        		rhizomik.AutoComplete.setQueryPattern(ac, rhizomik.SemanticForms.dbpQuery, "");
        		ac.sendRequest();					
        	}
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
					else if (form.elements[i].type=='select-one' && form.elements[i].options[form.elements[i].selectedIndex].value!="")
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
			},
		
        	addMissingFields: function(result)
            {	
                if (!result.label && result.uri)
                {	
                    result.label = {};
				    setFieldAttributes(result.label, result.uri);
                }	
                if (!result.tlabel && result.type)
                {
                    result.tlabel = {};
			        setFieldAttributes(result.tlabel, result.type);
                }
                if (!result.rlabel && result.range)
                {
                    result.rlabel = {};
                    setFieldAttributes(result.rlabel, result.range);	
                }	
            },
            
            propertiesQuery: function(resourceTypes)
	         {	
	         	var queryPattern = 
						"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+		//Consulta de propiedades específicas + prop. genéricas
						"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+			//para el tipo indicado
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
						"UNION { ?uri rdf:type ?t. \n"+
						"  OPTIONAL { ?uri rdfs:label ?label } \n"+
						"  OPTIONAL { ?uri rdfs:domain ?d } \n"+
						"  OPTIONAL { ?uri rdfs:range ?range } \n"+
						"  OPTIONAL { ?range rdfs:label ?rlabel } \n"+
						"  FILTER ( (?d=rdfs:Resource || !bound(?d)) && \n"+
						"           (?t = rdf:Property || ?t = owl:DatatypeProperty || ?t=owl:ObjectProperty || ?t=owl:AnnotationProperty) )} \n"+
						"  } \n"+
						"LIMIT 500";
	         	// TODO: just the first resource type is considered right now			
					var query = queryPattern.replace(/\[types\]/g, resourceTypes[0]);
					return '?query=' + encodeURIComponent(query);
			 },
	         
	        localQuery: function(resourceType, sQuery) 	
			{	//consulta para obtener los recursos del tipo indicado(en tal caso), cuyo nombre comience por la cadena especificada
		    	if(resourceType !== "http://www.w3.org/2000/01/rdf-schema#Resource")
		    	{
				    var queryPattern = 
					   "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
					   "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
					   "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"+
					   "SELECT DISTINCT ?uri ?label WHERE { \n"+ 
					   "   ?uri rdf:type <[type]> .\n"+
					   "   OPTIONAL { ?uri rdfs:label ?label } \n"+
					   "   FILTER (REGEX(?label, '( |^)[query].*','i') || REGEX(STR(?uri), '(#|/)[query].*','i')) }";
				    var query = queryPattern.replace(/\[query\]/g, sQuery);
					query = query.replace(/\[type\]/g, resourceType);
		    	}
	            else
	            {
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
		           	var query = queryPatternResource.replace(/\[query\]/g, sQuery);
				}
				return '?query=' + encodeURIComponent(query);
			},

	        dbpQuery: function(typeCondition, sQuery)
			{		//consulta para lanzar a DBpedia con una condición intercambiable
				var query =	"SELECT DISTINCT ?uri ?label ?type \n" +
							"WHERE { \n" +
							"?uri rdf:type ?type; rdfs:label ?label. \n" +
							"FILTER (" + typeCondition +
                           "(bif:contains(?label, '" + '"' + sQuery + '*"' + "') && lang(?label)='en' ))}\n"+ 
//							"(regex(?label, '( |^)" + sQuery + ".*','i')))}\n" +
							"LIMIT 500";
				return '&query=' + encodeURIComponent(query) + '&format=json&timeout=5000'; 
			},

				          
            // Show popup with recommended properties depending on the types of the resource
			addProperty: function(resource, resourceTypes)
			{
				var newRow = newPropertyRow(resource);
				var newCell1 = newRow.one('*');                 //firstChild de newRow (<td></td>)
				var ac = newCell1.one('*');						//firstChild de newCell1 (<div></div>)
				var acInput = ac.one('*');						//firstChild de ac (<input type='text'/>)
				acInput.focus();
						
				var propertiesDS = rhizomik.AutoComplete.createRemoteDataSource();
				rhizomik.AutoComplete.defineJSONFields(propertiesDS, ["label", "uri", "range", "rlabel"]);
				
				var autocomplete = rhizomik.AutoComplete.generateACInput(acInput, propertiesDS);
				rhizomik.AutoComplete.setQueryPattern (autocomplete, rhizomik.SemanticForms.propertiesQuery, resourceTypes);
				rhizomik.AutoComplete.formatResults (autocomplete);
												
			    var itemSelectEventHandler = function(e) 
			    {
			    	var newCell2 = newCell1.next();
		    	    acInput.set('title', e.result.raw.uri.value);	//Set title for tooltip 
			        entitleValueInput(e.result.raw, newCell2);		//Se habilita la entrada para el valor 		
				};
			    autocomplete.on("select", itemSelectEventHandler);
			},
			
			// Create autocomplete for elemId based on range for propertyURI
			propertyValueAutocomplete: function (elemId, domainTypes, propertyURI)
			{	
				var autocompleteElem = Y.one('[id=' + elemId + ']');
				if(autocompleteElem === null)
					autocompleteElem = elemId; 
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
				rhz.sparqlJSON(query, function(out){ rhizomik.SemanticForms.propertyValueAutocompleteCallBack(out, autocompleteElem); });
			},
			propertyValueAutocompleteCallBack: function (response, autocompleteElem)
			{
				var ranges = processJSONResults(response);
				var jsonRanges = Y.JSON.parse(ranges);				
				var rangesArray = jsonRanges.results.bindings;		
				var range = "http://www.w3.org/2000/01/rdf-schema#Resource";
				var rangeLabel = "Resource";
	// TODO: just the first range is considered right now
				if (rangesArray.length > 0 && rangesArray[0].range)
				{
					range = rangesArray[0].range.value;
					if(rangesArray[0].rlabel)
						rangeLabel = rangesArray[0].rlabel.value;
				}	
				if (range === "http://www.w3.org/2002/07/owl#Thing" && rangesArray.length > 1 && rangesArray[1].range)
				{
					range = rangesArray[1].range.value;
					if(rangesArray[1].rlabel)
						rangeLabel = rangesArray[1].rlabel.value;
				}	
				rhizomik.SemanticForms.resourceTypeAutocomplete(autocompleteElem, range, rangeLabel);
			},
			
			// Create autocomplete at elem for resources of a given type and optionally a value label
			resourceTypeAutocomplete: function (acElem, resourceType, resourceTypeLabel)
			{
				var resourcesDS = rhizomik.AutoComplete.createRemoteDataSource();
				rhizomik.AutoComplete.defineJSONFields(resourcesDS, ["label", "uri", "type"]);	
				
				var dbpediaDS = rhizomik.AutoComplete.createExternalDataSource("http://omediadis.udl.cat:8890/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org");
				rhizomik.AutoComplete.defineJSONFields(dbpediaDS, ["label", "uri", "type"]);
				var dbpedia = false;				//booleano para saber si estamos buscando en el servidor local o en DBpedia
				
				var acInput = acElem.one('*');		//firstChild de acElem (<input type=text/>)
				var autocomplete = rhizomik.AutoComplete.generateACInput(acInput, resourcesDS);
				rhizomik.AutoComplete.setQueryPattern (autocomplete, rhizomik.SemanticForms.localQuery, resourceType);
				rhizomik.AutoComplete.formatResults (autocomplete, resourceTypeLabel);
				//objeto para gestionar los tres niveles de restricción de tipo en consultas a DBpedia (restricción de uri del tipo, del label del tipo, y sin restricción)
				var typeRestriction = {	typeUriQuery:{condition : "(?type = <" + resourceType + ">)&&\n",
														state: true},
										typeLabelQuery:{condition:"(regex(?type, '" + resourceTypeLabel + "', 'i'))&&\n" ,
														state:false}
									};
				var responseHandler = function(e){			

					var results = e.results.length > 0 || e.data === undefined;
					if(dbpedia && !results)			//si no se obtienen resultados en una consulta a DBpedia, se baja un nivel de restricción
						levelDownTypeRestriction(autocomplete, typeRestriction);
					var tempLabel = acInput.get('value');
					var tempUri = tempLocalUri(resourceTypeLabel, tempLabel);
					rhizomik.AutoComplete.insertTopResults(e.results, tempUri, tempLabel, dbpedia);  //se insertan los dos primeros resultados para crear un nuevo recurso, y cambiar la fuente de datos de AC
				};
				autocomplete.on("results", responseHandler);
				acInput.on("key", function(e)		//al borrar algun caracter, se reinician los niveles de restricción de tipo
								{
									backspaceHandler(autocomplete, 
											typeRestriction.typeUriQuery, dbpedia);
								},'down:8,127');			
			
				var itemSelectEventHandler = function(e) {
					
					if(e.result.display.indexOf("New Resource") !== -1)			
						rhizomik.AutoComplete.processSelectedFirstResult (acInput, resourceType, resourceTypeLabel);
				    else if(e.result.display.indexOf("Look up in ") !== -1) 
					{											
						if(!dbpedia)
						{	
							rhizomik.AutoComplete.changeDataSource(autocomplete, dbpediaDS, rhizomik.SemanticForms.dbpQuery, typeRestriction.typeUriQuery.condition);
							typeRestriction.typeUriQuery.state = true;
							dbpedia = true;
						}
						else
						{
							rhizomik.AutoComplete.changeDataSource(autocomplete, resourcesDS, rhizomik.SemanticForms.localQuery, resourceType, resourceTypeLabel);
							dbpedia = false;
						}	
					}
					else		//Si se ha seleccionado cualquier elemento que no sea el primero/segundo de la lista
						rhizomik.AutoComplete.processSelectedResult(acInput, e.result.raw);
			    };
			    autocomplete.on("select", itemSelectEventHandler);
			    return autocomplete;
			},
			
			createNewForm: function (acInput, resourceType, resourceTypeLabel)
			{
				var typeName = getResourceTypeLabel(resourceTypeLabel, resourceType);
	       		var tempUri = tempLocalUri(typeName, acInput.get('value'));
				var uriField = acInput.ancestor("td");		//newCell2	     		   
			   	uriField.setContent('<input type="text" style="text-align:center" value=' + tempUri +
                                     ' name="http://www.w3.org/1999/02/22-rdf-syntax-ns#about" >');
	       		var labelField = createRow("literal", "label",
                                            "http://www.w3.org/2000/01/rdf-schema#label",
                                            acInput.get('value'));
			    var typeField = createRow("resource", "type",
                                            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                                            typeName, resourceType);
			    var addPropertyLink = createLink(tempUri,
   		      	                         "javascript:rhizomik.SemanticForms.addProperty('" + tempUri +
     		                             "', new Array('" + resourceType + "'))");
			    var table = Y.Node.create("<table></table>");
	       	   	uriField.appendChild(table);  	
	       	   	table.appendChild(labelField);
	       	   	table.appendChild(typeField);
	       	   	table.appendChild(addPropertyLink);
	       	   
	       	   	var typeValueCell = typeField.one('*').next();														
	       	   	var typeValueAC = typeValueCell.one('*');		
	       	   	rhizomik.SemanticForms.propertyValueAutocomplete(typeValueAC, resourceType,
	       	   									"http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	       	   	var labelValueCell = labelField.one('*').next();
	       	   	uriFieldValueHandler(uriField, typeValueAC, labelValueCell);	//mantiene actualizado el valor del campo URI
			}
		};
	}();
});
