<%@page import="net.rhizomik.rhizomer.autoia.manager.MenuManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS"%>

<link href="<%=request.getContextPath()%>/style/treemap.css" type="text/css" rel="stylesheet" />

<div id="sitemap">

    <h1>SITEMAP</h1>

    <%
        MenuManager manager = MenuManager.getInstance(config);

        HierarchyManagerSPARQL hm = manager.getManager();

    %>

    <%=hm.getHierarchyMenu().printAsSitemap(request,2)%>


    </ul>
</div>