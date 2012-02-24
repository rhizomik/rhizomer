/*
 * Rhizomer AJAX
 *
 * Author: http://rhizomik.net/~roberto
 */

/****************************************************************************
 * Rhizomer Transform Class
 ****************************************************************************/

rhizomik.Transform = function(baseURL)
{
	var self = this;
	
	/**
	 * Private Attributes
	 */	
	// Base URL
	var base = baseURL || "http://localhost:8080/rhizomer";
	// RDF to HTML XSL transformation
	var xslRDF2HTML = "/style/rdf2html.js.xsl";
	// XML Document for the RDF to HTML XSL transformation
	var xslDocRDF2HTML;
	// XSLTProcessor for the RDF2HTML transformation
	var rdf2htmlTransformer;
	// RDF to HTML Form XSL transformation
	var xslRDF2FORM = "/style/rdf2form.xsl";
	// XML Document	for the RDF to HTML Form XSL transformation
	var xslDocRDF2FORM;
	// XSLTProcessor for the RDF2FORM transformation
	var rdf2formTransformer;
	// XML Factory
	var XML = new rhizomik.XMLFactory();
	
	/**
	 * Private Methods
	 */	
	// Load the XML documents for the XSL Transformations
	function loadTransformations()
	{
		xslDocRDF2HTML = XML.createXMLDocFromURL(base+xslRDF2HTML);
		xslDocRDF2FORM = XML.createXMLDocFromURL(base+xslRDF2FORM);

		if (typeof XSLTProcessor != "undefined") 
		{
			rdf2formTransformer = new XSLTProcessor();
			rdf2formTransformer.importStylesheet(xslDocRDF2FORM);
			rdf2htmlTransformer = new XSLTProcessor();
			rdf2htmlTransformer.importStylesheet(xslDocRDF2HTML);
		}
        else if(window.ActiveXObject) {
            var xsltDocRDF2HTML = new ActiveXObject("Msxml2.XSLTemplate");
            var xsltDocRDF2FORM = new ActiveXObject("Msxml2.XSLTemplate");

            var xsltDocRDF2HTMLFree  = new ActiveXObject("Msxml2.FreeThreadedDOMDocument");
            var xsltDocRDF2FORMFree = new ActiveXObject("Msxml2.FreeThreadedDOMDocument");
            xsltDocRDF2HTMLFree.loadXML(xslDocRDF2HTML.xml);
            xsltDocRDF2FORMFree.loadXML(xslDocRDF2FORM.xml);

            xsltDocRDF2HTML.stylesheet = xsltDocRDF2HTMLFree;
            xsltDocRDF2FORM.stylesheet = xsltDocRDF2FORMFree;

            rdf2htmlTransformer = xsltDocRDF2HTML.createProcessor();
            rdf2formTransformer = xsltDocRDF2FORM.createProcessor();
        }
	};
	
	/**
	 * Public Methods
	 */	
	// Perform a transformation based on the RDF2HTML XSL
	self.rdf2html = function(response, targetElem, query)
	{
		var language = null;
		var direction = null;
		var orderProperty = null;
		
		if (query!=null)
		{
			if (query.indexOf("ORDER BY")>0)
			{
				var orderVariable = null;
				if (query.indexOf("DESC(")>0)
				{
					direction = "descending";
					var matchs = /ORDER\s+BY\s+DESC\(\?(\S+)\)/i.exec(query);
					orderVariable = matchs!=null?matchs[1]:null;
				}
				else if (query.indexOf("ASC(")>0)
				{
					direction = "ascending";
					var matchs = /ORDER\s+BY\s+ASC\(\?(\S+)\)/i.exec(query);
					orderVariable = matchs!=null?matchs[1]:null;
				}
				else
				{
					direction = "ascending";
					var matchs = /ORDER\s+BY\s+\?(\S+)/i.exec(query);
					orderVariable = matchs!=null?matchs[1]:null;
				}
				if (orderVariable!=null)
				{
					var regex = new RegExp("<(\\S+)>\\s+\\?"+orderVariable,"i");
					var matchs = regex.exec(query);
					orderProperty = matchs!=null?matchs[1]:null;
				}
			}
		}
		
		if (!xslDocRDF2HTML)
			loadTransformations();
		
		if (typeof XSLTProcessor != "undefined") 
		{
			try
			{
				var inputXML = new DOMParser().parseFromString(response, 'text/xml');
				
				rdf2htmlTransformer.clearParameters();
				rdf2htmlTransformer.setParameter(null, "mode", "rhizomer");
				rdf2htmlTransformer.setParameter(null, "logo", "false");
				if (language!=null) rdf2htmlTransformer.setParameter(null, "language", language);
	            if (direction!=null) rdf2htmlTransformer.setParameter(null, "direction", direction);
	            if (orderProperty!=null) rdf2htmlTransformer.setParameter(null, "order", orderProperty);
				var node = rdf2htmlTransformer.transformToFragment(inputXML, document);
				targetElem.innerHTML = "";
				targetElem.appendChild(node);	
			}
			catch(e){ targetElem.innerHTML = e.toString(); }
		}
		else
		{
            //TODO: add parameters for XSLT transformation...

            if(window.ActiveXObject) {
                var inputXML = XML.createXMLDocFromText(response);
                rdf2htmlTransformer.input = inputXML;
                rdf2htmlTransformer.transform();
                targetElem.innerHTML = rdf2htmlTransformer.output;

                return true;
            }
		}
		window.scroll(0,0);
    };
    
    // Perform a transformation based on the RDF2FORM XSL
	self.rdf2form = function(response, targetElem, action)
	{
		if (!xslDocRDF2FORM)
			loadTransformations();
	
		if (typeof XSLTProcessor != "undefined") 
		{
			try
			{
				var inputXML = new DOMParser().parseFromString(response, 'text/xml');
				
				rdf2formTransformer.clearParameters();
				rdf2formTransformer.setParameter(null, "action", action);
				var node = rdf2formTransformer.transformToFragment(inputXML, window.document);
				targetElem.innerHTML = "";
				targetElem.appendChild(node);
			}
			catch(e){ targetElem.innerHTML = e.toString(); }
		}
		else
		{
			try
			{
//TODO: add parameters for XSLT transformation...
				var inputXML = XML.createXMLDocFromText(response);
				targetElem.innerHTML = inputXML.transformNode(xslDocRDF2FORM);
			}
			catch(e){ targetElem.innerHTML = e.toString(); }
		}
		window.scroll(0,0);
    };
    return self;
};

/****************************************************************************
 * XML Factory Class
 ****************************************************************************/
rhizomik.XMLFactory = function()
{
	var self = this;
	
	/**
	 * Private Attributes
	 */	
	var MSXML = getMSXMLVersion();
	
	/**
	 * Private Methods
	 */	
	// Detect MSXML version for IExplorer
	function getMSXMLVersion() 
	{
		if (window.ActiveXObject)
	    {
			var aVersions = // ["MSXML2.DOMDocument.5.0", "MSXML2.DOMDocument.4.0","MSXML2.DOMDocument.3.0",
	    					[ "Msxml2.DOMDocument","Microsoft.DOMDocument"];
			for (var i = 0; i < aVersions.length; i++)
			{
	        	try 
	        	{
	            	var oXmlHttp = new ActiveXObject(aVersions[i]);
	            	return aVersions[i];
	        	} catch(e){}
	      	}
	    }
	};
	
	/**
	 * Public Methods
	 */	
	// Browser independent creation of an XML Document from the input URL
	self.createXMLDoc = function()
	{
		var xmlDoc;
		
		if (document.implementation && document.implementation.createDocument)
			xmlDoc = document.implementation.createDocument("", "", null);
		else
			xmlDoc = new ActiveXObject(MSXML);
		
		return xmlDoc;
	};
	
	self.createXMLHTTPRequest = function () 
	{
		   try { return new XMLHttpRequest(); } catch(e) {}
		   try { return new ActiveXObject("Msxml2.XMLHTTP"); } catch (e) {}
		   try { return new ActiveXObject("Microsoft.XMLHTTP"); } catch (e) {}
		   throw new Error("XMLHttpRequest not supported");
		   return null;
	};
	
	self.createXMLDocFromURLSync = function(url)
	{
		var xmlDoc = self.createXMLDoc();
		
		xmlDoc.async = false;
		if (!xmlDoc.load(url))
			 throw new Error("Parse error for:\n"+url);
		
		return xmlDoc;
	};
	
	self.createXMLDocFromURL = function(url, doc)
	{
		var request = self.createXMLHTTPRequest();
		request.open("GET", url, false); // false indicates this call is synchronous
		request.send(null);
		return request.responseXML;
	};
	
	self.createXMLDocFromText = function(text)
	{
		var xmlDoc;

		if (typeof XSLTProcessor != "undefined") 
		{
			xmlDoc = new DOMParser().parseFromString(text, 'text/xml');
		}
		else 
		{
			xmlDoc = new ActiveXObject("Msxml2.DOMDocument");
			xmlDoc.async = false;
			if (!xmlDoc.loadXML(text))
				throw new Error("Parse error for:\n"+text);
		}
		return xmlDoc;
	};
	    
    return self;
};


/*XMLDocument.prototype.xml = function()
{
	return (new XMLSerializer()).serializeToString(this);
};*/


/****************************************************************************
 * Rhizomer Utils Singleton
 ****************************************************************************/
rhizomik.Utils = function()
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
/*  function isURI(uri)
	{
    	var regexUri = /^(\[a-z0-9+.-\]+):(?:\/\/(?:((?:\[a-z0-9-._~!$&'()*+,;=:\]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\d*))?(\/(?:\[a-z0-9-._~!$&'()*+,;=:@\/\]|%[0-9A-F]{2})*)?|(\/?(?:\[a-z0-9-._~!$&'()*+,;=:@\]|%\[0-9A-F\]{2})+(?:\[a-z0-9-._~!$&'()*+,;=:@\/\]|%\[0-9A-F\]{2})*)?)(?:\?((?:\[a-z0-9-._~!$&'()*+,;=:\/?@\]|%\[0-9A-F\]{2})*))?(?:#((?:\[a-z0-9-._~!$&'()*+,;=:\/?@]|%[0-9A-F]{2})*))?$/i;
	    return (uri.match(regexUri));
	};
*/	
	/**
	 * Public Methods
	 */
	return {
		isURI: function (s) {
			var regexURI = "^([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\\d*))?(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?|(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?)(?:\\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?(?:#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?$";
			var re = new RegExp(regexURI, "i");
			return (re.test(s));
		},
	    uriNS: function (sURI) {
			var ns = sURI;
			if (sURI.indexOf('#') > 0)
				ns = sURI.slice(0, sURI.lastIndexOf('#')+1);
			else if (sURI.charAt(sURI.length-1)=='/')
			{
				var sURINoEndSlash = sURI.slice(0, sURI.length-1);
				ns = sURINoEndSlash.slice(0, sURINoEndSlash.lastIndexOf('/')+1);
			}
			else
				ns = sURI.slice(0, sURI.lastIndexOf('/')+1);
			return ns;
		},
		
		uriLocalname: function (sURI) {
			var justSlashsURI = sURI.replace("#", "/");
			if (justSlashsURI.charAt(justSlashsURI.length-1)=='/')
				justSlashsURI = justSlashsURI.slice(0, justSlashsURI.length-1);
			return justSlashsURI.slice(justSlashsURI.lastIndexOf('/') + 1);
		},
		
		toQueryString: function(o) {
		    if(typeof o !== 'object') {
		        return false;
		    }
		    var _p, _qs = [];
		    for(_p in o) {
		        _qs.push(encodeURIComponent(_p) + '=' + encodeURIComponent(o[_p]));
		    }
		    return _qs.join('&');
		}
    };
}();