<%@page import="net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS, net.rhizomik.rhizomer.autoia.classes.HierarchyMenu, net.rhizomik.rhizomer.autoia.classes.MenuConfig, net.rhizomik.rhizomer.service.ServiceManager, net.rhizomik.rhizomer.service.Service, net.rhizomik.rhizomer.autoia.manager.FacetManager"%>
<%

    String parameters = request.getParameter("p");

%>

    <script src="<%=request.getContextPath()%>/script/jquery.blockUI.js"></script>


    <link href="<%=request.getContextPath()%>/style/facets.css" type="text/css" rel="stylesheet" />
    <link href="<%=request.getContextPath()%>/style/breadcrumbs.css" type="text/css" rel="stylesheet" />
    <link href="<%=request.getContextPath()%>/style/popover.css" type="text/css" rel="stylesheet" />
    <link href="<%=request.getContextPath()%>/style/charts.css" type="text/css" rel="stylesheet" />
	<!--link href="<%=request.getContextPath()%>/style/jquery-ui-1.10.3.css" type="text/css" rel="stylesheet" /-->
    <script src="<%=request.getContextPath()%>/script/facets/popover.js"></script>
	<script src="<%=request.getContextPath()%>/script/util/dom.js"></script>
	<script src="<%=request.getContextPath()%>/script/util/namespaces.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/DefaultNSPrefixes.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/facet-browser.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/facet-manager.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/facet.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/facet-value.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/number-facet.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/string-facet.js"></script>
	<script src="<%=request.getContextPath()%>/script/facets/inverse-facet.js"></script>
	<script src="<%=request.getContextPath()%>/script/charts/charts.js"></script>

<script type="text/javascript">
//<![CDATA[
	var activeURI = null;
	var fm = null;
	var facetBrowser = null;
    var rhz = null;
	YAHOO.util.Event.addListener(window, "load", startRhizomer);

	function startRhizomer()
	{
		// The default get metadata for current URI (and its HTML representation)
		var location = window.document.location;
		var current = location.protocol+"//"+location.host+location.pathname;
		var alternativeQuery = null;

		// Define alternative query when search not specified, get RSS items
		if (location.search=="")
			alternativeQuery = "DESCRIBE ?r WHERE {?r a <"+facetURI+">} LIMIT 25";

		// Define Rhizomer endpoint
		var endpoint = '<%=request.getScheme()+"://"+request.getServerName()+
							":"+request.getServerPort()+request.getContextPath()%>';

		rhz = new rhizomik.Rhizomer(endpoint,
			document.getElementById("metadata"), alternativeQuery);

        var parameters = JSON.parse('<%=parameters%>');
		facetBrowser = new facet.FacetBrowser(parameters);
		facetBrowser.loadFacets();
    }

//]]>
</script>

<!--div id="subclasses"></div-->

<div id="content" style="margin-left:0px;">

    <div id="content-left">
        <div id="facets">
        </div>
    </div>

<div id="content-right">
        <div id="active-connections"></div>
        <div id="connections">
        </div>
</div>

    <div id="content-center">
        <div id="breadcrumbs" style="margin-bottom:10px;"></div>
        <div id="contentTabs" class="yui-navset" style="position:static;">
            <ul class="yui-nav">
                <li class="selected"><a href="#tab1"><em>Data</em></a></li>
            </ul>
            <div class="yui-content">
                <div>
                    <div id="context">
                        <!--div id="breadcrumbs"></div-->
                        <div id="active_facets">
                        </div>
                        <div id="sort">
                        </div>
                    </div>
                    <div style="margin-bottom:10px;"></div>
                    <div id="metadata">
                        Retrieving data...
                    </div>
                    <div id="metadatafoot">
                        <%
                            String requestURL = request.getRequestURL().toString();
                        %>
                        <a href="http://www.w3.org/2007/08/pyRdfa/extract?uri=<%=requestURL%>">
                            <img src="<%=request.getContextPath()%>/images/sw-rdfa-grey.png" alt="XHTML+RDFa" width="80" height="15"/>
                        </a>
                        <img src="<%=request.getContextPath()%>/images/sw-sparql-grey.png" alt="SPARQL" width="80" height="15"/>
                        <a href="<%=request.getContextPath()%>/copyright">
                            <img src="<%=request.getContextPath()%>/images/cc-some_rights.png" alt="Creative Commons some rights reserved" width="80" height="15"/>
                        </a>
                    </div>
                </div>
            </div>
        </div>

    </div>

