<%@page import="net.rhizomik.rhizomer.autoia.manager.MenuManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS, net.rhizomik.rhizomer.autoia.classes.HierarchyMenu, net.rhizomik.rhizomer.autoia.classes.MenuConfig"%>

<link href="<%=request.getContextPath()%>/style/treemap.css" type="text/css" rel="stylesheet" />

<script type="text/javascript" src="<%=request.getContextPath()%>/script/vis/d3.v2.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/script/vis/jit.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/script/vis/vis.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/script/vis/treemap.js"></script>


    <%

        MenuManager manager = MenuManager.getInstance(config);

        String path = config.getServletContext().getRealPath("WEB-INF");
        String menuFile = (String) config.getServletContext().getAttribute("menuFile");

    %>


    <script type="text/javascript">

        var hierarchy = <%= manager.getJSON(path+"/"+menuFile)%>;

        $j(document).ready(function() {
            //countNumMaxInstances(hierarchy);
            totalArea = countInstances(hierarchy);
            hierarchy.data.$area = totalArea;
            hierarchy.data.instances = totalArea;
            treatNode(hierarchy);
            countChilds(hierarchy);
            countSubclassesInstances(hierarchy);
            initTreeMap(hierarchy);
        });

    </script>

<div id="status" style="width:100%; height:20px; text-align:center; font-size:14px; margin-bottom:20px;">

</div>
<div id="vis"  class="treemap">
</div>