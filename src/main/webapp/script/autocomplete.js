 /*
 * Rhizomer AJAX
 *
 * Author: http://rhizomik.net/~roberto
 * Author: Juanma Giménez
 * 
 */

/****************************************************************************
 * Rhizomer AutoComplete Singleton
 ****************************************************************************/


YUI().use('autocomplete', 'autocomplete-highlighters', 'autocomplete-filters', 'autocomplete-list', 'node', 'event', 'event-valuechange', 'event-key', 'datasource', 'json', function (Y) 
{

rhizomik.AutoComplete = function()
	{					
		/**
		 * Private Attributes
		 */
				
		/**
		 * Private Methods
		 */
		function createResultObject(label, type, uri)	//crea un objeto con estructura de resultado AC
    	{
            var newResult = {};
				newResult.text = label;
            newResult.raw = {"uri":{"type":"uri", "value": uri},
			                "label":{"type":"literal", "value": label}};
            newResult.display = "<div class='ac-label'>" +				      
            					label +
                                "</div><div class='ac-range'>" +
      				            type +
            	      			"</div><div class='ac-uri'>" +
      		                   	uri +
            	      			"</div>";
            return newResult;	      				
        };
		
		/**
		 * Public Methods
		 */
	    return {
		    generateACInput: function(textInput, dataSource)
            {
                var ac = new Y.AutoComplete({				//creamos una instancia autocompete y le especificamos
					  inputNode			: textInput,					//algunos atributos de configuración
					  source			: dataSource,
  					  render 			: true,
					  resultFilters		: "startsWith",
					  resultHighlighter	: "startsWith"
				});
				ac.set("maxResults", 20);
				ac.set("minQueryLength", 1);
				ac.set("queryDelay", 0.5);
				ac.set("activateFirstItem", true);
				ac.set("scrollIntoView", true);
				ac.set("resultTextLocator", function(result){
                       						     rhizomik.SemanticForms.addMissingFields(result);
					                             return result.label.value;
                              					});
				return ac; 
            },
            
            createRemoteDataSource: function()
            {
                var baseURL = rhz.getBaseURL();
				var dataSource = new Y.DataSource.IO({source: baseURL});	//creamos una fuente de datos remota
				Y.io.header('accept', 'application/json');
				return dataSource;
            },
            
            createExternalDataSource: function(baseURL)
            {
            	var dataSource = new Y.DataSource.Get({source: baseURL});	//creamos una fuente de datos remota
				Y.io.header('accept', 'application/json');
				return dataSource;
            },
            
            defineJSONFields: function(dataSource, fields)
            {
            	dataSource.plug({fn: Y.Plugin.DataSourceJSONSchema, cfg: {		//le indicamos donde encontrar los resultados y los campos que nos interesan
				    schema: {
				        resultListLocator	: "results.bindings",
				        resultFields		: fields
						}
				}});          
	    	},
            
            setQueryPattern: function(ac, queryPattern, types)	//asigna a AC el patrón de consulta especificado
            {												//con la restricción de tipo indicada			
            	ac.set("requestTemplate", function(sQuery) 
						{
                			var query = queryPattern(types, sQuery);
                			return query;
						});
            },
            
            changeDataSource: function(ac, ds, queryPattern, type, typeLabel)
            {			//cambia la fuente de datos asignada a AC por la especificada
            		rhizomik.AutoComplete.setQueryPattern(ac, queryPattern, type);
            		rhizomik.AutoComplete.formatResults (ac, typeLabel);
            		ac.set("source", ds);
					ac.sendRequest();
			},
			
			formatResults: function(ac, typeLabel)
            {			//define el formato para los elementos de la lista de resultados de AC
            	var typeLabel_rangeLabel = typeLabel;  //si el tipo está definido, se trata con recursos del repositorio local
            	ac.set("resultFormatter", function(query, resultsArray) 
        				{                            
        					return Y.Array.map(resultsArray, function(result)			
        					{																	
        						var resultTemplate = "<div class='ac-label'>" +					      
        				   	      				"{label}" +
        					     				"</div><div class='ac-range'>" +
        					      				"{type_range}" +
        					      				"</div><div class='ac-uri'>" +
        					      				"{uri}" +
        					      				"</div>";
        					    if (!typeLabel && result.raw.rlabel)	//si tiene rangeLabel, se trata con propiedades
        					    	typeLabel_rangeLabel = result.raw.rlabel.value;
        					    else if (!typeLabel && result.raw.tlabel)	//si tiene typeLabel, se trata con recursos de un repositorio externo
       					    		typeLabel_rangeLabel = result.raw.tlabel.value;
       					    	
        					    return Y.Lang.sub(resultTemplate, {
        					    	label      	: result.highlighted,
        					    	type_range	: typeLabel_rangeLabel,
        					    	uri	        : result.raw.uri.value
        					    }); 				
        					});
        				});	
	    	},
	    	insertTopResults: function(results, uri, label, dbpedia)	//genera dos objetos resultado y los inserta en primer y segundo lugar del array de resultados
	        {
	    		var listElem1 = createResultObject(label, "New Resource", uri);                    						
	        	if (!dbpedia)						
	        		var listElem2 = createResultObject(label, "Look up in DBpedia", "http://dbpedia.org");	
	        	else								
	        		var listElem2 = createResultObject(label, "Look up in Local Server", "http://rhizomik.net");	
	        	results.unshift(listElem2);
	        	results.unshift(listElem1);
	        },
	        
	        processSelectedResult: function(input, selectedResult)			
	         {	
	        	 var hiddenInput = input.next();									
	        	 hiddenInput.set('value', selectedResult.uri.value);
	        	 input.set('title', selectedResult.uri.value);		//Set title for tooltip		   	
	         },	
			
	         processSelectedFirstResult: function(input, type, typeLabel)
	         {	
	        	 var selectedValue = input.get('value');
	        	 if (rhizomik.Utils.isURI(selectedValue))				//Si lo que ha escrito el usuario es una uri, ya tenemos nuevo recurso
	        	 {	
	        		 var hiddenInput = input.next();
	        		 hiddenInput.set('value', selectedValue);
	        		 input.set('title', selectedValue);		//Set title for tooltip
		       	}
		    	else									//Sino creamos un formulario para definir el nuevo recurso
		    		rhizomik.SemanticForms.createNewForm(input, type, typeLabel);
	         }
	    };
    }();
});	
