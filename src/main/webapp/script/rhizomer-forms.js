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


YUI().use('autocomplete', 'autocomplete-highlighters', 'autocomplete-filters', 'autocomplete-list', 'node','event', 'datasource', 'json', function (Y)
{
    rhizomik.SemanticForms = function()
    {

        /**
         * Private Attributes
         */
        var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";

        // Load the list of generic properties, applicable to any resource, just when the edit
        // form for RDF is triggered (rdf2form.xsl) using the getGenericProperties() method
        var genericProperties = "";

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

        function processJSONResults(JSONStrResults)
        {
            var JSONData = Y.JSON.parse(JSONStrResults);	//Convierte la string en un objeto JSON

            var resultsArray = JSONData.results.bindings;	//Localizamos los resultados y le añadimos los
            //campos label y rlabel a cada resultado que no
            for(var i=0; i<resultsArray.length; i++)		//disponga de ellos
            {
                var result = resultsArray[i];
                if (!result.label && result.uri)
                {
                    result.label = {};
                    result.label.type = "literal";
                    result.label.value = rhizomik.Utils.uriLocalname(result.uri.value);
                }
                if (result.range && !result.rlabel)
                {
                    result.rlabel = {};
                    result.rlabel.type = "literal";
                    result.rlabel.value = rhizomik.Utils.uriLocalname(result.range.value);

                }
                resultsArray[i] = result;
                JSONData.results.bindings[i] = resultsArray[i];
            }														//Volvemos a convertirlo en string para que pueda
            JSONStrResults = Y.JSON.stringify(JSONData, null, 3);	//ser tratado más adelante
            JSONStrResults = JSONStrResults.replace(/\\"/g,'"');
            JSONStrResults = JSONStrResults.replace(/"\[/g,'[');	//El método del YUI te llena la cadena de barras de escape
            JSONStrResults = JSONStrResults.replace(/\]"/g,']');	//y comillas innecesarias, las eliminamos

            //alert("JSON String: " + JSONStrResults);

            return JSONStrResults;
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

                rhz.sparqlJSON(genericPropertiesQuery, function(out){ rhizomik.SemanticForms.getGenericPropertiesCallBack(out); });
            },
            getGenericPropertiesCallBack: function(response)
            {
                genericProperties = processJSONResults(response);
            },

            // Show popup with recommended properties depending on the types of the resource
            addProperty: function(resource, resourceTypes)
            {
                var lastRow = Y.one('[id=' + resource + ']');			//recuperamos el nodo que contiene el enlace + a addProperty
                var newRow = Y.Node.create("<tr></tr>");
                var newCell1 = Y.Node.create("<td></td>");
                var newCell2 = Y.Node.create("<td></td>");
                newCell1.setContent("<div><input name='newProperty' type='text'/></div>");	//entrada para la nueva propiedad que funcionará con autocomplete
                newCell2.setContent("<input disabled='true' type='text'/>");				//entrada deshabilitada hasta la selección de una propiedad, entonces se habilitará para colocar el valor de la propiedad
                newRow.appendChild(newCell1);
                newRow.appendChild(newCell2);
                lastRow.insert(newRow, 'before');
                var ac = newCell1.one('*');						//firstChild de newCell1 (<div></div>)
                var ac_input = ac.one('*');						//firstChild de ac (<input type='text'/>)
                ac_input.focus();

                var properties = rhz.getBaseURL();
                var propertiesDS = new Y.DataSource.IO({source: properties});	//creamos una fuente de datos remota
                Y.io.header('accept', 'application/json');
                propertiesDS.plug({fn: Y.Plugin.DataSourceJSONSchema, cfg: {		//le indicamos donde encontrar los resultados y los campos que nos interesan
                    schema: {
                        resultListLocator	: "results.bindings",
                        resultFields		: ["label", "uri", "range", "rlabel"]
                    }
                }});

                var autocomplete = new Y.AutoComplete({				//creamos una instancia autocompete y le especificamos
                    inputNode			: ac_input,					//algunos atributos de configuración
                    render 			: true,
                    source			: propertiesDS,
                    resultFilters		: "phraseMatch",
                    resultHighlighter	: "phraseMatch"
                });

                //autocomplete.resultTypeList = false;
                autocomplete.set("maxResults", 50);
                autocomplete.set("minQueryLength", 1);
                autocomplete.set("queryDelay", 0.5);
                autocomplete.set("activateFirstItem", true);
                autocomplete.set("scrollIntoView", true);
                autocomplete.set("resultTextLocator", function(result)
                {
                    if (!result.label && result.uri)				//añadir label a los recursos que no tengan
                    {
                        result.label = {};
                        result.label.type = "literal";
                        result.label.value = rhizomik.Utils.uriLocalname(result.uri.value);
                    }
                    if (!result.rlabel && result.range)				//añadir rlabel a los recursos que no tengan
                    {
                        result.rlabel = {};
                        result.rlabel.type = "literal";
                        result.rlabel.value = rhizomik.Utils.uriLocalname(result.range.value);
                    }
                    return result.label.value;

                });
                autocomplete.set("requestTemplate", function(sQuery)
                {
                    var queryPattern =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+				//Consulta de propiedades específicas + prop. genéricas
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
                    return "?query=" + encodeURIComponent(query);
                });
                autocomplete.set("resultFormatter", function(query, resultsArray)
                {
                    return Y.Array.map(resultsArray, function(result)			//modificamos el formato de los resultados
                    {															//representados en la lista para que se muestre
                        var resultTemplate = "<div class='ac-label'>" +			//el label, rlabel y la uri
                            "{label}" +
                            "</div><div class='ac-range'>" +
                            "{rangeLabel}" +
                            "</div><div class='ac-uri'>" +
                            "{uri}" +
                            "</div>";
                        var resultJSON = result.raw;
                        return Y.Lang.sub(resultTemplate, {
                            label		: result.highlighted,
                            rangeLabel	: resultJSON.rlabel.value,
                            uri			: resultJSON.uri.value
                        });
                    });
                });

                var myHandler = function(e)
                {
                    var result = e.result.raw;

                    ac_input.set('title', result.uri.value);		//Asignamos la uri de la propiedad seleccionada
                    //como title de la entrada
                    //var newCell1 = ac_input.ancestor('td');
                    //var newCell2 = newCell1.next();

                    if (result.range.value == null)				//Si la propiedad no tiene rango, le asignamos el de recurso
                    {
                        result.range.value = "http://www.w3.org/2000/01/rdf-schema#Resource";
                    }

                    if (result.range.value == "http://www.w3.org/2000/01/rdf-schema#Literal") 		//Si la propiedad tiene rango de Literal, creamos una entrada tipo literal sobre la entrada deshabilitada
                    {
                        newCell2.setContent("<input class='literal' name='" + result.uri.value + "' type='text'/>" +
                            "<select name='lang'><option></option><option value='en'>en</option><option value='es'>es</option></select>");
                    }
                    else if (result.range.value.indexOf("http://www.w3.org/2001/XMLSchema#")>=0)	//Si la propiedad tiene rango de DataSchema, creamos una entrada tipo literal sobre la entrada deshabilitada
                    {
                        newCell2.setContent("<input class='literal' name='" + result.uri.value + "' type='text'/>" +
                            "<select name='datatype'><option value='" + result.range.value + "'>" +	result.rlabel.value + "</option></select>");
                    }
                    else																			//Si el rango no es Literal ni DataSchema, creamos un autocomplete sobre la entrada deshabilitada
                    {
                        newCell2.setContent("<div><input type='text'/><input type='hidden' class='object' name='" + result.uri.value + "'/></div>");
                        var acElem = newCell2.one('*');				//firstChild de newCell2 (<div></div>)
                        var resourceAC = rhizomik.SemanticForms.resourceTypeAutocomplete(acElem, result.range.value, result.rlabel.value);
                        acElem.one('*').focus();
                    }
                };
                autocomplete.on("select", myHandler);
                //ac_input.focus();
            },

            // Create autocomplete for elemId based on range for propertyURI
            propertyValueAutocomplete: function (elemId, domainTypes, propertyURI)
            {
                var autocompleteElem = Y.one('[id=' + elemId + ']');
                if(autocompleteElem === null){ autocompleteElem = elemId; }
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
                var JSONRanges = Y.JSON.parse(ranges);				//Convierte la string en un objeto JSON
                var rangeArray = JSONRanges.results.bindings;		//Array de resultados
                var range = "http://www.w3.org/2000/01/rdf-schema#Resource";
                var rangeLabel = null;
                // TODO: just the first range is considered right now
                if (rangeArray.length > 0 && rangeArray[0].range)
                {
                    range = rangeArray[0].range.value;
                    if(rangeArray[0].rlabel) { rangeLabel = rangeArray[0].rlabel.value; }
                }
                if (range=="http://www.w3.org/2002/07/owl#Thing" && rangeArray.length > 1 && rangeArray[1].range)
                {
                    range = rangeArray[1].range.value;
                    if(rangeArray[1].rlabel) { rangeLabel = rangeArray[1].rlabel.value; }
                }
                rhizomik.SemanticForms.resourceTypeAutocomplete(autocompleteElem, range, rangeLabel);
            },

            // Create autocomplete at elem for resources of a given type and optionally a value label
            resourceTypeAutocomplete: function (elem, resourceType, rlabel)
            {
                var ac_input = elem.one('*');				//firstChild de elem
                var baseURL = rhz.getBaseURL();				//URL del servidor local
                var resourcesDS = new Y.DataSource.IO({		//base de datos de nuestro servidor
                    source: baseURL
                });
                Y.io.header('accept', 'application/json');

                resourcesDS.plug({fn: Y.Plugin.DataSourceJSONSchema, cfg: {		//le indicamos dónde encontrar los resultados y los campos que nos interesan
                    schema: {
                        resultListLocator	: "results.bindings",
                        resultFields		: ["label", "uri"]
                    }
                }});

                var dbpediaURL = "http://lod.openlinksw.com/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org";	//URL para acceder al sevidor DBpedia a través de lod.openlinksw.com
                var dbpediaDS = new Y.DataSource.Get({			//base de datos externa (DBpedia)
                    source: dbpediaURL
                });
                var dbpedia = false;				//booleano para saber si estamos buscando en el servidor local o en DBpedia

                dbpediaDS.plug({fn: Y.Plugin.DataSourceJSONSchema, cfg: {		//le indicamos dónde encontrar los resultados y los campos que nos interesan
                    schema: {
                        resultListLocator	: "results.bindings",
                        resultFields		: ["label", "uri"]
                    }
                }});


                var autocomplete = new Y.AutoComplete({				//creamos una instancia autocompete y le especificamos
                    inputNode			: ac_input,					//algunos atributos de configuración
                    render 			: true,
                    source			: resourcesDS,
                    resultFilters		: "startsWith",
                    resultHighlighter	: "startsWith",
                    resultTextLocator	: function (result){
                        if (!result.label && result.uri)				//añadir label a los recursos que no tengan
                        {
                            result.label = {};
                            result.label.type = "literal";
                            result.label.value = rhizomik.Utils.uriLocalname(result.uri.value);
                        }
                        return result.label.value;
                    }
                });

                //autocomplete.resultTypeList = false;
                autocomplete.set("maxResults", 50);
                autocomplete.set("minQueryLength", 1);
                autocomplete.set("queryDelay", 0.5);
                autocomplete.set("activateFirstItem", false);
                autocomplete.set("scrollIntoView", true);
                autocomplete.set("requestTemplate", localQuery);
                function localQuery(sQuery) 	//devuelve la consulta codificada para el servidor local
                {
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

                    var query = "";
                    if (resourceType != "http://www.w3.org/2000/01/rdf-schema#Resource")
                    {
                        query = queryPattern.replace(/\[query\]/g, sQuery);
                        query = query.replace(/\[type\]/g, resourceType);
                    }
                    else
                    {
                        query = queryPatternResource.replace(/\[query\]/g, sQuery);
                    }
                    return "?query=" + encodeURIComponent(query);
                }

                autocomplete.set("resultFormatter", function(query, resultsArray)
                {
                    return Y.Array.map(resultsArray, function(result)			//modificamos el formato de los resultados
                    {															//representados en la lista para que se muestre
                        var resultTemplate = "<div class='ac-label'>" +			//el label y la uri
                            "{label}" +
                            "</div><div class='ac-uri'>" +
                            "{uri}" +
                            "</div>";
                        var resultJSON = result.raw;
                        return Y.Lang.sub(resultTemplate, {
                            label		: result.highlighted,
                            uri			: resultJSON.uri.value
                        });
                    });
                });
                var responseHandler = function(e){			//Cuando recibamos la respuesta, creamos un objeto
                    //resultado New Resource y lo colocamos el primero de
                    var newResource = {};					//la lista del contenedor
                    var tempUri = "http://rhizomik.net/" + rlabel.toLowerCase() + "/" + ac_input.get('value').toLowerCase().replace(/ */g,"");
                    newResource.raw = {"uri":{"type":"uri", "value": tempUri},
                        "label":{"type":"literal", "value": ac_input.get('value')}};

                    var newRTemplate = "<div class='ac-label'>" +
                        "{label}" +
                        "</div><div class='ac-range'>" +
                        "New Resource" +
                        "</div><div class='ac-uri'>" +
                        "{uri}" +
                        "</div>";
                    var newRDisplay = Y.Lang.sub(newRTemplate,{
                        label	: ac_input.get('value'),
                        uri		: tempUri
                    });
                    newResource.display = newRDisplay;
                    //Creamos un objeto resultado para poder realizar la consulta en
                    var dbpediaLookUp = {};					//DBPedia y lo colocamos el segundo de la lista del contenedor
                    dbpediaLookUp.raw = {"label":{"type":"literal", "value": ac_input.get('value')}};
                    if (!dbpedia)						//Si estamos buscando en el servidor local,
                    {									//ofrecemos la posibilidad de buscar en DBpedia
                        dbpediaLookUp.display = "<div class='ac-label'>" +
                            ac_input.get('value') +
                            "</div><div class='ac-range'>" +
                            "Look up in DBpedia" +
                            "</div><div class='ac-uri'>" +
                            "Look up in DBpedia" +
                            "</div>";
                    }
                    else								//Si estamos buscando en DBpedia, ofrecemos la
                    {									//posibilidad de buscar en el servidor local de nuevo
                        dbpediaLookUp.display = "<div class='ac-label'>" +
                            ac_input.get('value') +
                            "</div><div class='ac-range'>" +
                            "Look up in Local Server" +
                            "</div><div class='ac-uri'>" +
                            "Look up in Local Server" +
                            "</div>";
                    }
                    e.results.unshift(dbpediaLookUp);
                    e.results.unshift(newResource);
                };
                autocomplete.on("results", responseHandler);

                var itemSelectEventHandler = function(e) {

                    var ac_hidden = ac_input.next();	//input type=hidden
                    var s = ac_input.get('value');		//valor de la entrada autocompete
                    var aux = e.result.display;			//entrada que ha seleccionado el ususario (cadena con HTML)

                    if(aux.indexOf("New Resource") !== -1)			//Si se ha seleccionado la primera opción (New Resource)
                    {
                        if (rhizomik.Utils.isURI(s))				//Si lo que ha escrito el usuario es una uri, ya tenemos nuevo recurso
                        {
                            ac_hidden.set('value', s);
                            ac_input.set('title', s);				//Set title for tooltip
                            //ac_input.set('className', "yui-ac-input");
                        }
                        else									//Sino creamos un formulario para el nuevo recurso
                        {
                            rhizomik.SemanticForms.createNewForm(ac_input, resourceType, rlabel);
                        }
                    }
                    else if(aux.indexOf("Look up in DBpedia") !== -1 || aux.indexOf("Look up in Local Server")!== -1)
                    {											//Si se ha seleccionado la segunda opción (Look up in DBpedia/Local Server)
                        if (!dbpedia)							//Si estamos buscando en el servidor local quiere decir que se ha seleccionado la opción de buscar en DBpedia
                        {
                            dbpedia = true;
                            autocomplete.set("requestTemplate", function(sQuery)	//definimos un patrón para las consultas a DBpedia
                            {
                                var queryPattern = 	"SELECT DISTINCT ?uri ?label \n" +
                                    "WHERE { \n" +
                                    "?uri rdf:type []; rdfs:label ?label. \n" +
                                    "FILTER (bif:contains(?label, '" + '"' + sQuery + '*"' + "') && lang(?label)='en' )}";

                                return '&query=' + encodeURIComponent(queryPattern) + '&format=json&timeout=5000';
                            });
                            autocomplete.set("source", dbpediaDS);				//especificamos la nueva fuente de datos (DBpedia)
                            autocomplete.sendRequest();						//realizamos una consulta con el contenido actual de la entrada AC,
                            //ya que a partir de ahora la consultas se realizan en DBpedia pero
                            //la última fue realizada en el servidor local únicamente

                            /*dbpediaDS.sendRequest({
                             request: '&query=SELECT+DISTINCT+%3Furi+%3Flabel+%0D%0AWHERE+{+%0D%0A+++%3Furi+rdf%3Atype+[]%3B+rdfs%3Alabel+%3Flabel.%0D%0A+++FILTER+(bif%3Acontains(%3Flabel%2C+'+"'"+'"molleruss*"'+"'"+')+%26%26+lang(%3Flabel)%3D"en"+)%0D%0A}&format=json&timeout=5000',
                             callback: {
                             success: function(e){
                             var cadena = Y.JSON.stringify(e.response, null, 3);
                             cadena = cadena.replace(/\\"/g,'"');
                             cadena = cadena.replace(/"\[/g,'[');	//El método del YUI te llena la cadena de barras de escape
                             cadena = cadena.replace(/\]"/g,']');	//y comillas innecesarias, las eliminamos
                             alert(cadena);
                             },
                             failure: function(e){
                             alert(e.error.message);
                             }
                             }
                             });*/
                        }
                        else							//si estamos buscando en DBpedia significa que se ha seleccionado la opción de buscar en el servidor local
                        {
                            dbpedia = false;
                            autocomplete.set("requestTemplate", localQuery);
                            autocomplete.set("source", resourcesDS);
                            autocomplete.sendRequest();				//realizamos una consulta con el contenido actual de la entrada AC, ya que
                            //a partir de ahora la consultas se realizan en el servidor local pero la
                            //última fue realizada DBpedia únicamente
                        }
                    }
                    else											//Si se ha seleccionado cualquier elemento que no sea el primero/segundo de la lista
                    {
                        var result = e.result.raw;						// object literal of selected item's result data
                        ac_input.set('title', result.uri.value);		//Set title for tooltip
                        ac_hidden.set('value', result.uri.value);
                        //ac_input.set('className', 'yui-ac-input');		// In case input contained invalid URI and marked with class "input-error"
                    }
                };
                autocomplete.on("select", itemSelectEventHandler);
                return autocomplete;
            },

            createNewForm: function (ac_input, resourceType, rlabel)
            {

                var myInput = ac_input;
                var newUri = myInput.ancestor("td");	//newCell2
                var myType;

                if (rlabel)						//Si tiene range label, éste será el tipo
                {
                    myType = rlabel;
                    if (!resourceType)
                    {
                        resourceType = "http://www.w3.org/2000/01/rdf-schema#Resource";
                    }
                }
                else if (resourceType)			//Si no tiene rlabel, obtenemos el tipo del rango, que es
                {								//una uri pasada como argumento a través de resourceType
                    if (resourceType.indexOf("#") !== -1 )
                    {
                        myType = resourceType.slice(resourceType.lastIndexOf("#") + 1, resourceType.length);
                    }
                    else
                    {
                        myType = resourceType.slice(resourceType.lastIndexOf("/") + 1, resourceType.length);
                        if (myType == "")
                        {
                            myType = resourceType.slice(resourceType.lastIndexOf("/", resourceType.length - 2) + 1, resourceType.length - 1);
                        }
                    }
                }
                else 						//Si no tiene range label ni range, es un recurso
                {
                    myType = "Resource";
                    resourceType = "http://www.w3.org/2000/01/rdf-schema#Resource";
                }

                //Creamos la uri de la nueva propiedad que será del tipo: http://rhizomik.net/[type]/[label]
                var myUri = "http://rhizomik.net/" + myType.toLowerCase().replace(/ */g,"") + "/" + myInput.get('value').toLowerCase().replace(/ */g,"");
                newUri.setContent('<input type="text" style="text-align:center" value=' + myUri + ' name="http://www.w3.org/1999/02/22-rdf-syntax-ns#about" >');

                var newTable = Y.Node.create("<table></table>");			//Creamos una tabla que contendrá las entradas
                newUri.appendChild(newTable);							//de label y type además de un icono + para
                //añadir nuevas propiedades
                var newLabel = Y.Node.create("<tr></tr>");
                var newType = Y.Node.create("<tr></tr>");

                var newLabelLabel = Y.Node.create("<td></td>");
                var newLabelValue = Y.Node.create("<td></td>");
                var newTypeLabel = Y.Node.create("<td></td>");
                var newTypeValue = Y.Node.create("<td></td>");

                newTable.appendChild(newLabel);
                newTable.appendChild(newType);

                newLabel.appendChild(newLabelLabel);
                newLabel.appendChild(newLabelValue);
                newType.appendChild(newTypeLabel);
                newType.appendChild(newTypeValue);

                newLabelLabel.setContent('<a class="describe" href="?query=DESCRIBE%20&lt;http://www.w3.org/2000/01/rdf-schema%23label&gt;" onclick="javascript:rhz.describeResource(' + "'http://www.w3.org/2000/01/rdf-schema#label'" + '); return false;" title="Describe http://www.w3.org/2000/01/rdf-schema#label">label</a>');
                newTypeLabel.setContent('<a class="describe" href="?query=DESCRIBE%20<http://www.w3.org/1999/02/22-rdf-syntax-ns%23type>" onclick="javascript:rhz.describeResource(' + "'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'" + '); return false;" title="Describe http://www.w3.org/1999/02/22-rdf-syntax-ns#type">type</a>');
                newLabelValue.setContent('<input type="text" value="' + myInput.get('value') + '" name="http://www.w3.org/2000/01/rdf-schema#label" class="literal"> <select name="lang"><option></option><option value="en">en</option><option value="es">es</option></select>');
                newTypeValue.setContent('<div><input type="text" value= "' + myType + '" title= "' + resourceType + '" autocomplete="off"/> <input class="object" type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#type" value="' + resourceType + '"/></div>');

                var newAddP = Y.Node.create("<tr></tr>");			//Creamos una celda con un enlace a addProperty
                var newAddProp = Y.Node.create("<td></td>");

                newAddP.set('id', myUri);			//Añadimos un atributo id que coincide con la
                //uri del recurso sobre el que queremos trabajar
                newAddProp.setAttribute('colspan', '2');

                newAddProp.setAttribute('style', 'text-align:left');

                newTable.appendChild(newAddP);
                newAddP.appendChild(newAddProp);
                newAddProp.setContent('<a href="javascript:rhizomik.SemanticForms.addProperty(' + "'" + myUri + "'" + ', new Array(' + "'" + resourceType + "'" + '))">+</a>');
                //Aplicamos un autocomplete en la entrada type
                var newTypeAutoComplete = newTypeValue.one('*');		//firstChild de newTypeValue
                rhizomik.SemanticForms.propertyValueAutocomplete(newTypeAutoComplete, resourceType, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
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
            }
        };

    }();
});
