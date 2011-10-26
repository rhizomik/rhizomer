<%@page import="net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS, net.rhizomik.rhizomer.autoia.classes.HierarchyMenu, net.rhizomik.rhizomer.autoia.classes.MenuConfig, net.rhizomik.rhizomer.service.ServiceManager, net.rhizomik.rhizomer.service.Service"%>

<script type="text/javascript">

	YAHOO.util.Event.addListener(window, "load", resizeWhenNew);
	function resizeWhenNew(e)
	{ 
		YAHOO.util.Dom.setStyle("left","width","80%");
		var region = YAHOO.util.Dom.getRegion("left");
		var width = region.right - region.left;
		YAHOO.util.Dom.setStyle("content","margin-left",(width+27)+"px");
	}
</script>

<div id="content">

<div id="contentTabs" class="yui-navset">
    <ul class="yui-nav">
        <li class="selected"><a href="#tab1"><em>HTML</em></a></li>
    </ul>            
    <div class="yui-content">
        <div>
	        <div id="wikiactions">[<a href="<%=request.getContextPath()%><%=request.getPathInfo()%>?edit">edit</a> | 
								   <!-- a href="${url}?delete" -->delete<!-- /a --> | 
    							   <!-- a href="${url}?history" -->history<!-- /a -->]
			</div>
				<p> There isn't HTML content associated with this resource.</p>
		</div>
    </div>
</div>