<%@page import="net.rhizomik.rhizomer.autoia.manager.HierarchyManager, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSPARQL, net.rhizomik.rhizomer.autoia.manager.HierarchyManagerSKOS, net.rhizomik.rhizomer.autoia.classes.HierarchyMenu, net.rhizomik.rhizomer.autoia.classes.MenuConfig, net.rhizomik.rhizomer.service.ServiceManager, net.rhizomik.rhizomer.service.Service"%>
<div id="content">
<div id="wikicontent">
	<% 
		String editing = request.getContextPath()+"/html"+request.getPathInfo();
		if (request.isUserInRole("rhizomer"))
		{  %>
			<script type="text/javascript" src="<%=request.getContextPath()%>/fckeditor/fckeditor.js"></script>
			<script type="text/javascript">
			<!--
				YAHOO.util.Event.addListener(window, "load", startFCKEditor);

				function startFCKEditor()
				{
					var oFCKeditor = new FCKeditor('textcontent');
					oFCKeditor.Height = "400";
					oFCKeditor.BasePath = '<%=request.getContextPath()%>/fckeditor/';
					oFCKeditor.ReplaceTextarea();
				}
			//-->
			</script>
			<h1> Editing HTML content for <%=editing%></h1>
			<form action="<%=editing%>" method="post">
				<textarea id="textcontent" name="textcontent">
					<%= request.getAttribute("pagecontent") %>
				</textarea>
				<input type="button" value="Cancel" onclick="location.href='<%=editing%>'"/>
		    </form>
	<%	} else 
		{ %>
			<h1> Not authorised to edit: <%=editing%></h1>
			<p> Sorry, you should <a href="<%=request.getContextPath()%>/html/admin/login.jsp">login</a>
			 in order to edit this content. </p>
	<%  } %>
</div>