<body onload="window.location = '<%=request.getHeader("referer")%>'">
<p>You have now logged in, <a href="<%=request.getHeader("referer")%>">return</a>.</p>
</body>