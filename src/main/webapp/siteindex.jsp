<%@page import="net.rhizomik.rhizomer.autoia.manager.MenuManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS"%>
<%@ page import="java.util.Map" %>
<%@ page import="net.rhizomik.rhizomer.autoia.classes.HierarchyNode" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>

<div id="siteindex">

    <h1 style="text-align:center;">SITE INDEX</h1>

    <h2 class="alphabet" style="text-align:center;">
    <a id="top"></a>

    <%
        /*
        MenuManager manager = MenuManager.getInstance(config);
        HierarchyManagerSPARQL hm = manager.getManager();
          */
    %>

    <%

    List<Character> letters = new ArrayList<Character>();
    for(char c : "abcdefghijklmnñopqrstuvwxyz".toUpperCase().toCharArray()) {
        letters.add(c);
    }
    /** TODO: Add numbers ans symbols **/

    List<String> initials = originalMenu.getInitials();

    for(Character c : letters){
        if(initials.contains(c.toString())){

        %><a style="text-decoration: underline;" href="#<%=c%>"><%=c%></a> | <%

        }
        else{
            %><%=c%> | <%
        }

    }

    %>

    </h2>

    <div id="siteindex_list" style="width: 50%; margin: 0px auto 0px auto;">

    <%

    Map<String, HierarchyNode> map = originalMenu.getMapNodes();

    List<HierarchyNode> nodes = new ArrayList<HierarchyNode>(map.values());
    java.util.Collections.sort(nodes);
    String previousLetter = null;

    for(HierarchyNode node : nodes){
        String link = request.getContextPath()+"/facets.jsp?q=SELECT ?r1 WHERE{?r1 a <"+node.getUri().replace("#", "%23")+">}";
        String label = node.getLabel();
        String letter = label.substring(0,1);
        if(previousLetter==null){
            %>
            <h3 id="<%=letter%>" class="siteindex_letter"><%=letter%></h3>
            <div class="siteindex_letter_list">
            <ul>
            <%
        }
        else if(!letter.equals(previousLetter)){
            %>
                </ul>
                <div class="top">
                    <a href="#top">&uarr; Top</a>
                </div>
                </div>
              <h3 id="<%=letter%>" class="siteindex_letter"><%=letter%></h3>
                <div class="siteindex_letter_list">
                <ul>
            <%
        }

    %>
        <li><a href="<%=link%>"><%= node.getLabel()%> (<%=node.getNumInstances()%>)</a></li>
    <%
    previousLetter=letter;
    }

    %>
        </ul>
        <div class="top">
            <a href="#top">&uarr; Top</a>
        </div>
        </div>


    </div>


</div>