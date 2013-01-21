<%@page import="net.rhizomik.rhizomer.autoia.manager.MenuManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS"%>

<div id="sitemap">

    <h1 style="text-align:center; margin-bottom:0px;">SITE MAP</h1>

    <div style="text-align:center; margin-top:0px; font-size:14px;">
        <a href="?mode=summary">Summarized</a> |
        <a href="?mode=original">Original</a> |
        <a href="?mode=full">Full</a>
    </div>

    <%

        String mode = request.getParameter("mode");

        if(mode==null || mode.equals("summary")){

    %>

        <%=menu.printAsSitemap(request,2, "compact")%>

    <%

        }

        else if(mode.equals("original")){

    %>

        <%=originalMenu.printAsSitemap(request,2, "compact")%>

    <%

        }

        else if(mode.equals("full")){

    %>
         <%=originalMenu.printAsSitemap(request,10,mode)%>

    <%

        }

    %>



</div>