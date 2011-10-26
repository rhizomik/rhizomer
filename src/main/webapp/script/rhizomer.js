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
	var waitImage = "<img id='waitImage' src='"+baseURL+"/images/black-loader.gif'/>";
	// Number of results per page
	var step = 10;
	
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
	};
	// XMLHTTPRequest GET
	function get(url, callback, contentype)
	{
		if (!contentype) contentype = "application/rdf+xml";
		var oCallback = {
				success: function(o) { callback(o.responseText); },
				failure: function(o) { onFailure(o); }
			};
		YAHOO.util.Connect.setDefaultPostHeader(false);
		YAHOO.util.Connect.initHeader("Accept", contentype);
		YAHOO.util.Connect.asyncRequest("GET", url, oCallback);
	};
	// XMLHTTPRequest PUT
	function put(url, uri, content, contenttype, callback)
	{
		var oCallback = {
				success: function(o) { callback(o.responseText); },
				failure: function(o) { onFailure(o); }
			};
		YAHOO.util.Connect.setDefaultPostHeader(false);
		YAHOO.util.Connect.initHeader('Content-Type', contenttype);
		YAHOO.util.Connect.asyncRequest('PUT', url+'?uri='+uri, oCallback, content);
	};
	// XMLHTTPRequest POST
	function post(url, content, contenttype, callback)
	{
		var oCallback = {
				success: function(o) { callback(o.responseText); },
				failure: function(o) { onFailure(o); }
			};
		YAHOO.util.Connect.setDefaultPostHeader(false);
		YAHOO.util.Connect.initHeader('Content-Type', contenttype);
		YAHOO.util.Connect.asyncRequest('POST', url, oCallback, content);
	} ;
	// XMLHTTPRequest DELETE
	function del(url, uri, callback)
	{
		var oCallback = {
				success: function(o) { callback(o.responseText); },
				failure: function(o) { onFailure(o); }
			};
		YAHOO.util.Connect.asyncRequest('DELETE', url+'?uri='+uri, oCallback);
	};
	
	// By default, on successful query add response to history
	function defaultOnResponse(query, response)
	{
		dhtmlHistory.add("history"+hex_md5(query), {query: query, response: response});
	};
	// Send query and add result to history
	function query(expression, callback, contentype)
	{
		//Escape '#' and '&'
		expression = expression.replace(/#/g, "%23").replace(/&/g, "%26");
		get(base + "?query="+expression, 
			function(out) {defaultOnResponse(expression, out); callback(out);}, contentype);
	};
	// Send query but do not add it to history
	function queryNoHistory(expression, callback, contentype)
	{
		//Escape '#' and '&'
		expression = expression.replace(/#/g, "%23").replace(/&/g, "%26");
		get(base + "?query="+expression, 
			function(out) {callback(out);}, contentype);
	};
	// Handle history change (back and forward) and reloads.
	// When there is not history data, issue the default query
	function handleHistoryChange(newLocation, historyData)
	{
		if (historyData)
		{
			last = historyData;
			transform.rdf2html(historyData.response, target, last.query);
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
				query(last.query, function(out) {transform.rdf2html(out, target, last.query);});
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
		queryNoHistory(encodeURIComponent(sparqlQuery),
			function(out) {callback(out);});
	};
	// Perform SPARQL query and return JSON to the provided callback function
	self.sparqlJSON = function(sparqlQuery, callback)
	{
		queryNoHistory(encodeURIComponent(sparqlQuery),
			function(out) {callback(out);}, "application/json");
	};
	// Perform SPARQL query and return HTML in the specified target DOM element.
	// Intended for rendering RDF in different parts of the page, do not keep history
	self.sparqlHTML = function(sparqlQuery, target)
	{
		queryNoHistory(encodeURIComponent(sparqlQuery),
			function(out) {transform.rdf2html(out, target, sparqlQuery);});
	};
	// Perform SPARQL query and show results as HTML in the target DOM element
	self.sparql = function(sparqlQuery)
	{
		self.showMessage("<p>Querying...</p>\n"+waitImage);
		query(encodeURIComponent(sparqlQuery),
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
		
		self.showMessage("<p>List resources...</p>\n"+waitImage);
		
		query("CONSTRUCT {?r a ?type; <http://www.w3.org/2000/01/rdf-schema#label> ?l; \n"+
			  "              <http://www.w3.org/2000/01/rdf-schema#comment> ?c. \n"+
			  "           ?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "WHERE { ?r a ?type \n" +
			  "        OPTIONAL {?r <http://www.w3.org/2000/01/rdf-schema#label> ?l} \n"+
			  "        OPTIONAL {?r <http://www.w3.org/2000/01/rdf-schema#comment> ?c} \n"+
			  "        OPTIONAL {?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "        OPTIONAL {?r a ?type2. ?type2 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?type. \n" +
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
		query = query.replace(/"/g, "&quot;").replace(/'/g, "&apos;");
		var newOffset = parseInt(offset)+step;
		var moreDivHTML = "<a href=\"#\" onclick=\"javascript:rhz."+listFunction+"('"+query+"', "+newOffset+"); return false;\">More...</a>";

		var moreDiv = document.createElement("div");
		moreDiv.setAttribute("class", "moreResults");
		moreDiv.innerHTML = moreDivHTML;
		target.appendChild(moreDiv);
	};
	// Like listResources but without keeping history
	self.listResourcesNoHistory = function(sparqlSelectQuery, offset)
	{
		if (mode=="describe")
			self.describeResourcesNoHistory(sparqlSelectQuery, offset);
		else
			self.constructResourcesNoHistory(sparqlSelectQuery, offset);
	};
	// Like listResources but without keeping history
	self.constructResourcesNoHistory = function(sparqlSelectQuery, offset)
	{
		if (!offset) offset = "0";
		
		self.showMessage("<p>List resources...</p>\n"+waitImage);
		
		queryNoHistory("CONSTRUCT {?r a ?type; <http://www.w3.org/2000/01/rdf-schema#label> ?l; \n"+
			  "              <http://www.w3.org/2000/01/rdf-schema#comment> ?c. \n"+
			  "           ?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "WHERE { ?r a ?type \n" +
			  "        OPTIONAL {?r <http://www.w3.org/2000/01/rdf-schema#label> ?l} \n"+
			  "        OPTIONAL {?r <http://www.w3.org/2000/01/rdf-schema#comment> ?c} \n"+
			  "        OPTIONAL {?type <http://www.w3.org/2000/01/rdf-schema#label> ?lt} \n"+
			  "        OPTIONAL {?r a ?type2. ?type2 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?type. \n" +
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
		
		query = "SELECT ?r WHERE {?r ?p <"+escape(uri)+">}";
		self.listResources(query, offset);
	};
	// SPARQL DESCRIBE query for the input URI
	self.describeResource = function(uri)
	{
		self.showMessage("<p>Describe "+uri+"</p>\n"+waitImage);
		query("DESCRIBE <"+escape(uri)+">",
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); transform.rdf2html(out, target);});
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// SPARQL DESCRIBE query for all resources selected by the input query, like SELECT ?r WHERE...
	self.describeResources = function(sparqlSelectQuery, offset)
	{		
		if (!offset) offset = "0";
		
		self.showMessage("<p>Describe resources...</p>\n"+waitImage);
		  
		query("DESCRIBE ?r \n"+
				  "WHERE { ?r a ?type \n" +
				  "        { "+sparqlSelectQuery+" LIMIT "+step+" OFFSET "+offset+" } } ",
			function(out) {self.showMessage("<p>Showing...</p>\n"+waitImage); 
			               transform.rdf2html(out, target); 
			               self.listMore("describeResources", sparqlSelectQuery, offset); });
		contentTabs.set('activeIndex', 0); // Show the first tab, where data is loaded
	};
	// Like describeResources but without keeping history
	self.describeResourcesNoHistory = function(sparqlSelectQuery, offset)
	{		
		if (!offset) offset = "0";
		
		self.showMessage("<p>Describe resources...</p>\n"+waitImage);
		  
		queryNoHistory("DESCRIBE ?r \n"+
				  "WHERE { ?r a ?type \n" +
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

		query = "SELECT ?r WHERE {?r a <"+escape(typeURI)+">}";
		self.describeResources(query, offset);
	};
	// SPARQL DESCRIBE query for all resources connected to the input URI
	self.describeReferrers = function(uri, offset)
	{
		if (!offset) offset = "0";
		
		query = "SELECT ?r WHERE {?r ?p <"+escape(uri)+">}";
		self.describeResources(query, offset);
	};
	// Edit the RDF description for the input URI
	self.editResourceDescription = function(uri)
	{
		queryNoHistory("DESCRIBE <"+escape(uri)+">",
			function(out) {transform.rdf2form(out, target, "edit");});
	};
	// New RDF description using that for the input URI as source
	self.newResourceDescription = function(uri)
	{
		queryNoHistory("DESCRIBE <"+escape(uri)+">",
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
	
	return self;
};
