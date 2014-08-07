<% request.getSession().invalidate(); %>
<body onload="window.location = '<%=request.getContextPath()%>/'">
<p>You have now logged out and can get back to the <a href="<%=request.getContextPath()%>/">home page</a>.</p>
</body>