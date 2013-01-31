<%@page import="net.rhizomik.rhizomer.autoia.manager.MenuManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS"%>

<script type="text/javascript" src="<%=request.getContextPath()%>/script/jquery.treeview.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/script/jquery.hashchange.js"></script>


<div id="sitemap">

    <h1 style="text-align:center; margin-bottom:0px;">SITE MAP</h1>

    <div style="text-align:center; margin-top:0px; font-size:14px;">
        <a href="?mode=summary">Summarized</a> |
        <a href="?mode=full">Full</a>
    </div>

    <%

        String mode = request.getParameter("mode");

        if(mode==null || mode.equals("summary")){
    %>
        <%=menu.printAsSitemap(request, "compact")%>
    <%
        }
        else if(mode.equals("full")){
    %>
        <div id="sidetreecontrol">
          <a href="?#">Collapse All</a> | <a href="?#">Expand All</a>
        </div>

         <%=originalMenu.printAsSitemap(request,mode)%>
    <%
        }
    %>



</div>

<script type="text/javascript">
    $j(function() {
        $j("#tree").treeview({
            collapsed: true,
            animated: "medium",
            control:"#sidetreecontrol",
            persist: "location"
        });
        $j(window).hashchange( function(){
            $j(".sitemap-root").css("background-color", "white");
            var hash = window.location.hash;
            if(hash){
                $j(hash).css("background-color", "#fff2a8");
            }
        });
        $j(window).hashchange();
    });
</script>