<%@page import="net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS, net.rhizomik.rhizomer.autoia.classes.HierarchyMenu, net.rhizomik.rhizomer.autoia.classes.MenuConfig, net.rhizomik.rhizomer.service.ServiceManager, net.rhizomik.rhizomer.service.Service"%>

<div id="content">

<div id="contentTabs" class="yui-navset">
    <ul class="yui-nav">
        <li class="selected"><a href="#tab1"><em>HTML</em></a></li>
    </ul>            
    <div class="yui-content">
        <div>
	        <div id="wikiactions">[<a href="<%=request.getContextPath()%>/html<%=request.getPathInfo()%>?edit">edit</a> | 
								   <!-- a href="${url}?delete" -->delete<!-- /a --> | 
    							   <!-- a href="${url}?history" -->history<!-- /a -->]
			</div>
			<%= request.getAttribute("pagecontent") %>
		</div>
    </div>
</div>
