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

        %><a class="active" href="#<%=c%>"><%=c%></a> | <%

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
        if(node.getNumInstances()>0){
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
            <li><a class="fold" id="<%=node.getUri().hashCode()%>"><%= node.getLabel()%> (<%=node.getNumInstances()%>)</a>
            <div id="<%=node.getUri().hashCode()%>_div" style="display:none;">
            <%
            if(node.getParent()!=null){
            %>
                <span style="font-weight:bold;"><%=node.getLabel()%></span><br/>
                Parent: <%=node.getParent().getLabel()%><br/>
            <%
            }
            %>
            <%
            if(node.hasChilds()){
            %>
                Subclasses:
            <%
                for(HierarchyNode child : node.getChilds()){
            %>
                <%=child.getLabel()%>,
            <%
                }
            }
            %>
            </div>
            </li>
            <%
                previousLetter=letter;
        }
    }

    %>
        </ul>
        <div class="top">
            <a href="#top">&uarr; Top</a>
        </div>
        </div>


    </div>

    <div id="tooltip"></div>

    <script>
        YUI().use('overlay', 'event', 'widget-anim', function (Y) {

            var waitingToShow = false;
            var tooltip = new Y.Overlay({
                srcNode: "#tooltip",
                visible: false
            }).plug(Y.Plugin.WidgetAnim);
            tooltip.anim.get('animHide').set('duration', 0.01);
            tooltip.anim.get('animShow').set('duration', 0.15);
            tooltip.render();

            // handler that positions and shows the tooltip
            var onMousemove = function (e) {
                var i;
                if (tooltip.get('visible') === false) {
                    // while it's still hidden, move the tooltip adjacent to the cursor
                    Y.one('#tooltip').setStyle('opacity', '0');
                    tooltip.move([(e.pageX + 10), (e.pageY + 20)]);
                }
                if (waitingToShow === false) {
                    // wait half a second, then show tooltip
                    setTimeout(function(){
                        Y.one('#tooltip').setStyle('opacity', '1');
                        tooltip.show();
                    }, 500);

                    // while waiting to show tooltip, don't let other
                    // mousemoves try to show tooltip too.
                    waitingToShow = true;
                    div = e.currentTarget.one('div');
                    id = div.generateID();
                    d = $j("#"+id);
                    tooltip.setStdModContent('body', d.html());
                }
            }

            // handler that hides the tooltip
            var onMouseleave = function (e) {

                // this check prevents hiding the tooltip
                // when the cursor moves over the tooltip itself
                if ((e.relatedTarget) && (e.relatedTarget.hasClass('yui3-widget-bd') === false)) {
                    tooltip.hide();
                    waitingToShow = false;
                }
            }

            //Y.delegate('mousemove', onMousemove, '.lists', 'li');
            //Y.delegate('mouseleave', onMouseleave, '.lists', 'li');
            Y.delegate('mouseover', onMousemove, '#siteindex', 'li');
            Y.delegate('mouseleave', onMouseleave, '#siteindex', 'li');
            Y.one('#tooltip').on('mouseleave', onMouseleave);

        });
    </script>

</div>