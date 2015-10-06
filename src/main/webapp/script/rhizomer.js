/*
 * Rhizomer AJAX
 *
 * Author: http://rhizomik.net/~roberto
 */
 
var rhizomik;
if (rhizomik && (typeof rhizomik != "object" || rhizomik.NAME))
    throw new Error("Namespace 'rhizomik' already exists");
rhizomik = {};
rhizomik.NAME = "Rhizomik";
rhizomik.VERSION = 1.0;

/****************************************************************************
 * Rhizomer Class
 ****************************************************************************/
rhizomik.Rhizomer = function(baseURL, targetElem, defaultQuery)
{
	var self = this;

	/**
	 * Private Attributes
	 */
	// Browse mode: describe (full resource description) or construct (resource description summary)
	var browseMode = "describe";
	// Base URL for Ajax connections and styles
	var base = baseURL || "http://localhost:8080/rhizomer";
	// Target element for result HTML
	var target = targetElem || document.getElementById("metadata");
	// XSL transformation engine
	var transform = new rhizomik.Transform(base);
	// Wait image
	var waitImage = "<img class='waitImage' src='"+baseURL+"/images/black-loader.gif'/>";
	// Number of results per page
	var step = 15;
	
	// Last history step
	var last = null;
	
	initialiseHistory(defaultQuery);
	
	/**
	 * Private Methods
	 */
	// Default XMLHTTPRequest on failure function
	function onFailure(o)
	{
		mySimpleDialog = new YAHOO.widget.SimpleDialog("dlg", { 
			width: "20em", 
			fixedcenter:true,
			modal:true,
		    visible:false,
			draggable:false });
		mySimpleDialog.setHeader("Error "+o.statusText);
		mySimpleDialog.setBody(o.responseText);
		mySimpleDialog.cfg.setProperty("icon",YAHOO.widget.SimpleDialog.ICON_WARN);
        mySimpleDialog.render(document.body);
        mySimpleDialog.show();
	};
	// XMLHTTPRequest GET
	function get(url, callback, contenttype)
	{
		if (!contenttype) contenttype = "application/rdf+xml";

        YUI().use('io-base', function (Y) {
            var cfg = {
                method: 'GET',
                headers: {'Accept': contenttype},
                on: {
                    success: function(tid, response) { callback(response.responseText); },
                    failure: function(tid, response) { onFailure(response); }
                } };
            Y.io(url, cfg);
        });
	};
	// XMLHTTPRequest PUT
	function put(url, uri, content, contenttype, callback)
	{
        YUI().use('io-base', function (Y) {
            var cfg = {
                method: 'PUT',
                data: content,
                headers: {'Content-Type': contenttype},
                on: {
                    success: function(tid, response) { callback(response.responseText); },
                    failure: function(tid, response) { onFailure(response); }
                } };
            Y.io(url+'?uri='+uri, cfg);
        });
	};
	// XMLHTTPRequest POST
	function post(url, content, contenttype, callback)
	{
        YUI().use('io-base', function (Y) {
            var cfg = {
                method: 'POST',
                data: content,
                headers: {'Content-Type': contenttype},
                on: {
                    success: function(tid, response) { callback(response.responseText); },
                    failure: function(tid, response) { onFailure(response); }
                } };
            Y.io(url, cfg);
        });
	};
	// XMLHTTPRequest DELETE
	function del(url, uri, callback)
	{
        YUI().use('io-base', function (Y) {
            var cfg = {
                method: 'DELETE',
                on: {
                    success: function(tid, response) { callback(response.responseText); },
                    failure: function(tid, response) { onFailure(response); }
                } };
            Y.io(url+'?uri='+uri, cfg);
        });
	};
	
	// By default, on successful query add response to history
	function defaultOnResponse(query, response)
	{
		dhtmlHistory.add("history"+hex_md5(query), {type: 'resources', query: query, response: response});
	};
	// Send query and add result to history
	function queryHistory(expression, callback, contentype)
	{
		//Escape '#' and '&'
		//expression = expression.replace(/#/g, "%23").replace(/&/g, "%26");
		get(base + "?query="+encodeURIComponent(expression),
			function(out) {defaultOnResponse(expression, out); callback(out);}, contentype);
	};
	// Send query but do not add it to history
	function queryNoHistory(expression, callback, contentype)
	{
		//Escape '#' and '&'
		//expression = expression.replace(/#/g, "%23").replace(/&/g, "%26");
		get(base + "?query="+encodeURIComponent(expression),
			function(out) {callback(out);}, contentype);
	};
	// Handle history change (back and forward) and reloads.
	// When there is not history data, issue the default query
	function handleHistoryChange(newLocation, historyData)
	{
		if (historyData)
		{
			last = historyData;

            if(historyData.type=="resources")
			    transform.rdf2html(historyData.response, target, last.query);
            else{
                facetBrowser.loadHistory(historyData.parameters);
            }
		}
		else if (defaultQuery != null) // Issue default query
		{
			get(base + "?query="+defaultQuery,
				function(out) {transform.rdf2html(out, target, defaultQuery);});
		}
	};
	// Initialise history, then detect and manage first load or reload
	function initialiseHistory(defaultQuery)
	{
		dhtmlHistory.initialize();
		dhtmlHistory.addListener(handleHistoryChange);

		// Issue default query for first load
		if (dhtmlHistory.isFirstLoad() && defaultQuery != null)
		{
			get(base + "?query="+defaultQuery,
				function(out) {transform.rdf2html(out, target, defaultQuery);});
		}
		else // It is a reload, reissue the last query
		{
			if (last!=null)
				queryHistory(last.query, function(out) {transform.rdf2html(out, target, last.query);});
		}
	};
		
	/**
	 * Public Methods
	 */
	// Show message in the target element area
	self.getBaseURL = function()
	{
		return base;
	};
	// Show message in the target element area
	self.showMessage = function(message)
	{
		target.innerHTML = message;
	};
	// Perform SPARQL query and return RDF/XML to the provided callback function
	self.sparqlRDF = function(sparqlQuery, callback)
	{
		queryNoHistory(sparqlQuery,
			function(out) {callback(out);});
	};
	// Perform SPARQL query and return JSON to the provided callback function
	self.sparqlJSON = function(sparqlQuery, callback)
	{
		queryNoHistory(sparqlQuery,
			function(out) {callback(out);}, "application/json");
	};
	// Perform SPARQL query and return HTML in the specified target DOM element.
	// Intended for rendering RDF in different parts of the page, do not keep history
	self.sparqlHTML = function(sparqlQuery, target)
	{
		queryNoHistory(sparqlQuery,
			function(out) {transform.rdf2html(out, target, sparqlQuery);});
	};
	// Perform SPARQL query and show results as HTML in the target DOM element
	self.sparql = function(sparqlQuery)
	{
		self.showMessage("<p>Querying...</p>\n"+waitImage);
		queryHistory(sparqlQuery,
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); transform.rdf2html(out, target, sparqlQuery);});
	};
	// List descriptions (full or summary) for resources selected by the input query, like SELECT ?r WHERE...
	self.listResources = function(sparqlSelectQuery, offset)
	{
		if (browseMode=="describe")
			self.describeResources(sparqlSelectQuery, offset);
		else
			self.constructResources(sparqlSelectQuery, offset);
	};
	// List the basic information (labels and types) for resources selected by the input query, like SELECT ?r WHERE...
	self.constructResources = function(sparqlSelectQuery, offset)
	{
		if (!offset) offset = "0";
        if(facetBrowser == null)
            var selectVar = "r1";
        else
		    var selectVar = facetBrowser.getActiveManager().getVariable();


		self.showMessage("<p>List resources...</p>\n"+waitImage);
		
		queryHistory(
              "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
              "CONSTRUCT {?"+selectVar+" a ?type; <http://www.w3.org/2000/01/rdf-schema#label> ?l; \n"+
			  "              <http://www.w3.org/2000/01/rdf-schema#comment> ?c. \n"+
			  "           ?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "WHERE { ?"+selectVar+" a ?type \n" +
			  "        OPTIONAL {?"+selectVar+" <http://www.w3.org/2000/01/rdf-schema#label> ?l} \n"+
			  "        OPTIONAL {?"+selectVar+" <http://www.w3.org/2000/01/rdf-schema#comment> ?c} \n"+
			  "        OPTIONAL {?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "        OPTIONAL {?"+selectVar+" a ?type2. ?type2 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?type. \n" +
			  "                  FILTER (?type!=?type2 && !isBlank(?type2) )} \n"+
			  "        { "+sparqlSelectQuery+" LIMIT "+step+" OFFSET "+offset+" } \n"+
			  "          FILTER( !bound(?type2) ) }",
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); 
			               transform.rdf2html(out, target); 
			               self.listMore("listResources", sparqlSelectQuery, offset); });
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// Generates "More..." link to get more results using the given list function
	self.listMore = function(listFunction, query, offset)
	{
            if(facetBrowser != null){
                var numResults = facetBrowser.getNumResults();
            }
            else
                var numResults = 100; /*Obtain results from query*/
            var numPages = parseInt(numResults / step);
            var currentPage = offset/step + 1;
            if(numResults%step>0)
                numPages++;
            query = query.replace(/"/g, "&quot;").replace(/'/g, "&apos;");
            var newOffset = parseInt(offset)+step;
            var html = "";
            if(currentPage>1){
                var peviousOffset = parseInt(offset)-step;
                html += "<a href=\"#\" onclick=\"javascript:rhz."+listFunction+"('"+addSlashes(query)+"', "+peviousOffset+"); return false;\">Previous...</a>";
            }
            html += "&nbsp;&nbspPage "+currentPage+" of "+numPages + "&nbsp;&nbsp";

            /*var moreDivHTML = previousDivHTML + "<a href=\"#\" onclick=\"javascript:rhz."+listFunction+"('"+query+"', "+newOffset+"); return false;\">Next...</a>";*/
            if(numPages>currentPage)
                html += "<a href=\"#\" onclick=\"javascript:rhz."+listFunction+"('"+addSlashes(query)+"', "+newOffset+"); return false;\">Next...</a>";

            var moreDiv = document.createElement("div");
            moreDiv.setAttribute("class", "moreResults");
            /*moreDiv.innerHTML = moreDivHTML;*/
            moreDiv.innerHTML = html;
            target.appendChild(moreDiv);

	};
	// Like listResources but without keeping history
	self.listResourcesNoHistory = function(sparqlSelectQuery, offset)
	{
		if (browseMode=="describe")
			self.describeResourcesNoHistory(sparqlSelectQuery, offset);
		else
			self.constructResourcesNoHistory(sparqlSelectQuery, offset);
	};
	// Like listResources but without keeping history
	self.constructResourcesNoHistory = function(sparqlSelectQuery, offset)
	{
		if (!offset) offset = "0";
        if(facetBrowser == null)
            var selectVar = "r1";
        else
            var selectVar = facetBrowser.getActiveManager().getVariable();
		self.showMessage("<p>List resources...</p>\n"+waitImage);
		
		queryNoHistory(
              "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
              "CONSTRUCT {?"+selectVar+" a ?type; <http://www.w3.org/2000/01/rdf-schema#label> ?l; \n"+
			  "              <http://www.w3.org/2000/01/rdf-schema#comment> ?c. \n"+
			  "           ?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "WHERE { ?"+selectVar+" a ?type \n" +
			  "        OPTIONAL {?"+selectVar+" <http://www.w3.org/2000/01/rdf-schema#label> ?l} \n"+
			  "        OPTIONAL {?"+selectVar+" <http://www.w3.org/2000/01/rdf-schema#comment> ?c} \n"+
			  "        OPTIONAL {?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "        OPTIONAL {?"+selectVar+" a ?type2. ?type2 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?type. \n" +
			  "                  FILTER (?type!=?type2 && !isBlank(?type2) )} \n"+
			  "        { "+sparqlSelectQuery+" LIMIT "+step+" OFFSET "+offset+" } \n"+
			  "          FILTER(!isBlank(?type) && !bound(?type2)) }",
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); 
			               transform.rdf2html(out, target); 
			               self.listMore("listResourcesNoHistory", sparqlSelectQuery, offset); });
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// List the basic information (labels and types) for all resources whose type is the input URI
	self.listResourcesOfType = function(typeURI, offset)
	{
		if (!offset) offset = "0";

		query = "SELECT ?r WHERE {?r a <"+escape(typeURI)+">}";
		self.listResources(query, offset);
	};
	// SPARQL DESCRIBE query for all resources connected to the input URI
	self.listReferrers = function(uri, offset)
	{
		if (!offset) offset = "0";
		
		query = "SELECT DISTINCT ?r WHERE { ?r ?p <"+uri+"> . }";
		self.listResources(query, offset);
	};
	// SPARQL DESCRIBE query for the input URI
	self.describeResource = function(uri)
	{
		self.showMessage("<p>Describe "+uri+"</p>\n"+waitImage);
		queryHistory("DESCRIBE <"+uri+">",
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); transform.rdf2html(out, target);
                self.makeInverseProperties();
                self.makeImages();
            });
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// SPARQL DESCRIBE query for all resources selected by the input query, like SELECT ?r WHERE...
	self.describeResources = function(sparqlSelectQuery, offset)
	{		
		if (!offset) offset = "0";

		self.showMessage("<p>Describe resources...</p>\n"+waitImage);

        if(facetBrowser == null)
            var selectVar = "r1";
        else
            var selectVar = facetBrowser.getActiveManager().getVariable();

		var describeQuery =
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "DESCRIBE ?"+selectVar+" WHERE { ?"+selectVar+" a ?type \n { " +
			sparqlSelectQuery + " LIMIT " + step + " OFFSET " + offset + " } } ";
		
		queryHistory(describeQuery,
			  function(out) 
			  {		self.showMessage("<p>Showing...</p>\n"+waitImage); 
			        transform.rdf2html(out, target);
                    self.makeInverseProperties();
                    self.makeImages();
			        self.listMore("describeResources", sparqlSelectQuery, offset); }
			 );
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// Like describeResources but without keeping history
	self.describeResourcesNoHistory = function(sparqlSelectQuery, offset)
	{		
		if (!offset) offset = "0";
		
		self.showMessage("<p>Describe resources...</p>\n"+waitImage);
        if(facetBrowser == null)
            var selectVar = "r1";
        else
            var selectVar = facetBrowser.getActiveManager().getVariable();
		queryNoHistory(
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"+
            "DESCRIBE ?"+selectVar+" \n"+
	        "WHERE { ?"+selectVar+" a ?type \n" +
		    "        { "+sparqlSelectQuery+" LIMIT "+step+" OFFSET "+offset+" } } ",
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); 
			               transform.rdf2html(out, target); 
			               self.listMore("describeResourcesNoHistory", sparqlSelectQuery, offset); });
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// SPARQL DESCRIBE query for all resources whose type is the input URI
	self.describeResourcesOfType = function(typeURI, offset)
	{
		if (!offset) offset = "0";

		query = "SELECT ?r WHERE {?r a <"+typeURI+">}";
		self.describeResources(query, offset);
	};
	// SPARQL DESCRIBE query for all resources connected to the input URI
	self.describeReferrers = function(uri, offset)
	{
		if (!offset) offset = "0";
		
		query = "SELECT ?r WHERE {?r ?p <"+uri+">}";
		self.describeResources(query, offset);
	};
	// Edit the RDF description for the input URI
	self.editResourceDescription = function(uri)
	{
		queryNoHistory("DESCRIBE <"+uri+">",
			function(out) {transform.rdf2form(out, target, "edit");});
	};
	// New RDF description using that for the input URI as source
	self.newResourceDescription = function(uri)
	{
		queryNoHistory("DESCRIBE <"+uri+">",
			function(out) 
			{
				transform.rdf2form(out, target, "new");
				var sourceURI = document.editform.elements[0].value;
				var uriNS = rhizomik.Utils.uriNS(sourceURI);
				document.editform.elements[0].value = uriNS+'...';
				document.editform.elements[0].focus();
				document.editform.elements[0].select();
			});
	};	
	// Put new data for the input URI in different formats (application/rdf+xml, application/n-triples or application/n3)
	self.putRDF = function(uri, metadata, format)
	{
		self.showMessage("<p>Sending...</p>\n"+waitImage);
		put(base, escape(uri), metadata, format,
			function(out) {window.location.reload();});
	};
	// Post new data in different formats (application/rdf+xml, application/n-triples or application/n3)
	self.postRDF = function(metadata, format)
	{
		self.showMessage("<p>Sending...</p>\n"+waitImage); 
		post(base, metadata, format,
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); transform.rdf2html(out, target);});
	};
	// Delete the RDF description for the input URI
	self.deleteResourceDescription = function(uri)
	{
		del(base, escape(uri),
			function(out) {window.location.reload();});
	};
	// Show content in tab
	self.showTab = function(tabName, rawContent, delegate)
	{
		var tabs = contentTabs.get('tabs'); // The rhizomik.contentTabs object is defined in WEB-INF/jsp/wikipage-view.jsp
		var tabPosition = tabs.length; //if new tab place last
		for(i=0; i<tabs.length; i++)
		{
			//Check if tab for service already exists and replace it
			if (tabs[i].get('label') == tabName)
			{
				tabPosition = i;
				contentTabs.removeTab(tabs[i]);
			}
		}
        var tabContent = delegate ? 'Loading service ....' : rawContent;
        var tab = new YAHOO.widget.Tab({label: tabName, content:tabContent, active: true});
        contentTabs.addTab(tab, tabPosition);
        if (delegate) {
        	YAHOO.plugin.Dispatcher.process(tab, rawContent, {action:'tabview'});
        }
	};
	// Call service on resources descriptions, the list of resources is separated by spaces
	self.callServiceOnResource = function(service, endpoint, resourceURIs)
	{
		var resources = resourceURIs.split(" ");
		
		queryNoHistory("DESCRIBE <"+resources.join("> <")+">",
			function(description) 
			{
				post(
					//base+"/serviceproxy"+"?endpoint="+
					base+endpoint, description, 'application/rdf+xml', 
					function(out) { self.showTab(service, out, true); });
			});
		window.scroll(0,0); 
	};
	// Get the specified facets
	self.getFacets = function(facets, callback)
	{
		var queryString = rhizomik.Utils.toQueryString(facets);
		post(base+"/facetProperties.jsp?"+queryString, "", "",
			function(out) {callback(out);});
	};
        // Get numeric valued properties of selected type

    self.getNumericProperties = function(facetURI, callback)
    {
        var queryString = rhizomik.Utils.toQueryString({facetURI: facetURI, mode: "charts" });
        post(base+"/facetProperties.jsp?"+queryString, "", "", callback);
    }

    self.makeInverseProperties = function(){

        if (!$j(".description").length)
            return;

        var query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"+
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "SELECT DISTINCT ?r ?uri ?c ?labelc ?labelr ?p ?labelp \n"+
            "WHERE{ ?r ?p ?uri . ?r a ?c \n";
        query += ". FILTER(?c!=<http://www.w3.org/2002/07/owl#Thing>) ."+
            "FILTER(LANG(?labelc)='en' || LANG(?labelc)='') ."+
            "FILTER(LANG(?labelp)='en' || LANG(?labelp)='') ."+
            "FILTER(LANG(?labelr)='en' || LANG(?labelr)='') .";

        $j(".description").each(function(index){
            var uri = $j(this).children("table").attr("about").toString();
            if(index==0)
                query += "FILTER(?uri=<"+uri+">";
            else
                query += " || ?uri=<"+uri+">";
        });

        query += ") OPTIONAL{?r rdfs:label ?labelr} OPTIONAL{?c rdfs:label ?labelc} OPTIONAL{?p rdfs:label ?labelp} \n" +
            "} ORDER BY ?p";

        /*console.log(query);*/

        rhz.sparqlJSON(query, function(out){
            var inverseProperties = {};
            data = JSON.parse(out);
            for(i=0; i<data.results.bindings.length; i++){
                var uri = data.results.bindings[i].uri.value;
                var r = data.results.bindings[i].r.value;
                var c = data.results.bindings[i].c.value;
                var p = data.results.bindings[i].p.value;
                if(data.results.bindings[i].labelc)
                    var labelc = data.results.bindings[i].labelc.value;
                else
                    var labelc = makeLabel(c)
                if(data.results.bindings[i].labelr)
                    var labelr = data.results.bindings[i].labelr.value;
                else
                    var labelr = makeLabel(r)
                if(data.results.bindings[i].labelp)
                    var labelp = data.results.bindings[i].labelp.value;
                else
                    var labelp = makeLabel(p)

                var key = c+p;
                if(uri in inverseProperties){
                    if(key in inverseProperties[uri]){
                        inverseProperties[uri][key]['values'].push({r:r,labelr:labelr});
                    }
                    else{
                        obj = {c : c, p : p, labelc : labelc, labelp : labelp, values : [{r:r, labelr : labelr}]};
                        inverseProperties[uri][key] = obj;
                    }
                }
                else{
                    inverseProperties[uri] = {};
                    obj = {c : c, p : p, labelc : labelc, labelp : labelp, values : [{r:r, labelr : labelr}]};
                    inverseProperties[uri][key] = obj;
                }
            }

            $j(".description").each(function(index){
                var uri = $j(this).children("table").attr("about").toString();
                var label = $j(this).children("table").children("tbody").children("tr").children("th").children("a")[0].text;
                html = "";
                $j.each(inverseProperties[uri],function(i, property)
                {
                    var link = {type : property.c,
                        filters : [
                            {property : property.p,
                                value : uri,
                                label : label
                            }
                        ]};
                    link = encodeURIComponent(JSON.stringify(link))
                    html += "<tr><td><a href=\"facets.jsp?q="+uri+"#"+link+"\">Is "+property.labelp+" of "+property.labelc+"</a></td>";
                    html += "<td>";
                    $j.each(property.values,function(i, value)
                    {
                        html+="<div class='property-object'>"+value.labelr+"</div><div class='connector'>,</div>";
                    });
                    html += "<a style='font-size:12px; font-weight:bold;' href=\"facets.jsp?q="+uri+"#"+link+"\">See related "+property.labelc+"s</a></td></tr>";
                });
                $j(this).children("table").append(html);
            });

        });
    };

    self.makeImages = function(){
        $j(".description").each(function(index){
            var uri = $j(this).children("table").attr("about").toString();
            var element = this;
            pictureUri = self.getPictureUri(this, "http://dbpedia.org/ontology/thumbnail");
            if(!pictureUri)
                pictureUri = self.getPictureUri(this, "http://xmlns.com/foaf/0.1/depiction");

            if(pictureUri){
                $j.ajax({
                    type: 'HEAD',
                    url: pictureUri,
                    success: function(){
                        $j(element).children("table").css("width","80%").css("padding-right","25px");
                        html = "<div style=\"float:right; padding-right:10px; padding-top: 10px; width:18%\"><img style=\"max-width:150px;\" src=\""+pictureUri+"\"></img></div>";
                        $j(element).prepend(html);
                    },
                    error: function() {
                        pictureUri = pictureUri.replace("/commons/","/en/");
                        $j(element).children("table").css("width","80%").css("padding-right","25px");
                        html = "<div style=\"float:right; padding-right:10px; padding-top: 10px; width:18%\"><img style=\"max-width:150px;\" src=\""+pictureUri+"\"></img></div>";
                        $j(element).prepend(html);
                    }
                });
            }
            else{
                pictureUri = self.getPictureUri(this, "http://dbpedia.org/property/hasPhotoCollection");
                if(pictureUri){
                    requestUri = "http://localhost:8080/proxy.jsp?url="+encodeURIComponent(pictureUri+"?format=rdf");
                    /*console.log(requestUri);*/
                    $j.ajax({
                        type: 'GET',
                        url: requestUri,
                        success: function(data){
                            elements = data.getElementsByTagName("rdf:Description");
                            console.log(elements);
                        },
                        error: function(jqXHR, exception){
                            if (jqXHR.status === 0) {
                                console.log('Not connect.\n Verify Network.');
                            } else if (jqXHR.status == 404) {
                                alert('Requested page not found. [404]');
                            } else if (jqXHR.status == 500) {
                                alert('Internal Server Error [500].');
                            } else if (exception === 'parsererror') {
                                alert('Requested JSON parse failed.');
                            } else if (exception === 'timeout') {
                                alert('Time out error.');
                            } else if (exception === 'abort') {
                                alert('Ajax request aborted.');
                            } else {
                                alert('Uncaught Error.\n' + jqXHR.responseText);
                            }
                        }
                    });
                }
            }



        });
    };

    self.getPictureUri = function(element, property){
        selector = property.replace(/[!"#$%&'()*+,.\/:;<=>?@\[\\\]^`{|}~]/g, '\\$&');
        var picture_td = $j(element).children("table").find("tr."+selector).children("td")[1];
        var picture_uri = $j($j(picture_td).children("div").children("a")[0]).attr("resource");
        return picture_uri;
    };


	
	return self;
};
