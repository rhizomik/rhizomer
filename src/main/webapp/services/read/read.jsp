
<%@page import="javax.xml.transform.stream.StreamResult"%>
<%@page import="javax.xml.transform.stream.StreamSource"%>
<%@page import="javax.xml.transform.Transformer"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="javax.xml.transform.TransformerFactory"%>
<%@page import="java.io.StringWriter"%>

<%@page pageEncoding="UTF-8"%>
<%@page contentType="text/html;charset=UTF-8"%>

<% 
	request.setCharacterEncoding("UTF-8");

	StringWriter result = new StringWriter();
	
	TransformerFactory factory = TransformerFactory.newInstance();

	String jspPath = request.getPathInfo();
	String folderPath = jspPath.substring(0,jspPath.lastIndexOf('/'));
	String realPath = getServletConfig().getServletContext().getRealPath(folderPath);
    FileInputStream license2text = new FileInputStream(realPath + "/license2text.xsl");
	Transformer transformer = factory.newTransformer(new StreamSource(license2text));
	
	StreamSource inStream = new StreamSource(request.getInputStream());
	StreamResult outStream = new StreamResult(result);
	
	transformer.setParameter("base", request.getContextPath()+folderPath+"/");
	//if (language!=null) transformer.setParameter("language", language);

	transformer.transform(inStream, outStream);
%>
<%= result.toString() %>
