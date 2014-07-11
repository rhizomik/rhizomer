
<%@page import="javax.xml.transform.stream.StreamResult"%>
<%@page import="javax.xml.transform.stream.StreamSource"%>
<%@page import="javax.xml.transform.Transformer"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="javax.xml.transform.TransformerFactory"%>
<%@page import="java.io.StringWriter"%>
<%@page import="com.hp.hpl.jena.rdf.model.*"%>
<%@page import="java.net.URL" %>
<%@page import="com.hp.hpl.jena.util.FileUtils" %>
<%@page import="java.net.HttpURLConnection" %>
<%@page import="java.io.StringReader" %>
<%@page import="java.net.MalformedURLException" %>
<%@page import="java.io.IOException" %>

<%@page pageEncoding="UTF-8"%>
<%@page contentType="text/html;charset=UTF-8"%>

<%
    class StreamSourceFromRequest
    {
        private HttpServletRequest request;
        private StreamSourceFromRequest(HttpServletRequest request)
        {
            this.request = request;
        }

        StreamSource getStreamSource() throws IOException
        {
            StreamSource source;

            // Check if input RDF comes through an attribute or form parameter
            String rdf = (String)request.getSession(false).getAttribute("rdf");
            if (rdf == null)
                rdf = request.getParameter("rdf");
            // If attribute or parameter...
            if (rdf != null)
            {
                // First try to interpret the rdf parameter/attribute as URL to load RDF from
                try
                {
                    URL rdfURL = new URL(rdf);
                    HttpURLConnection urlConn = (HttpURLConnection)rdfURL.openConnection();
                    urlConn.setRequestProperty("Accept", "application/rdf+xml, text/plain;q=0.5, text/rdf+n3;q=0.6");
                    source = new StreamSource(urlConn.getInputStream());
                }
                // If not an URL, load RDF from rdf parameter/attribute
                catch (MalformedURLException e)
                {
                    source = new StreamSource(new StringReader(rdf));
                }
            }
            // If there is no rdf parameter/attribute, read RDF from request input stream
            else
                source = new StreamSource(request.getInputStream());
            return source;
        }
    }

	request.setCharacterEncoding("UTF-8");

	StringWriter result = new StringWriter();
	
	TransformerFactory factory = TransformerFactory.newInstance();

	String jspPath = request.getPathInfo();
	String folderPath = jspPath.substring(0,jspPath.lastIndexOf('/'));
	String realPath = getServletConfig().getServletContext().getRealPath(folderPath);
    FileInputStream license2text = new FileInputStream(realPath + "/license2text.xsl");
	Transformer transformer = factory.newTransformer(new StreamSource(license2text));

    StreamSourceFromRequest getSource = new StreamSourceFromRequest(request);
	StreamSource inStream = getSource.getStreamSource();

	StreamResult outStream = new StreamResult(result);
	
	transformer.setParameter("base", request.getContextPath()+folderPath+"/");

	transformer.transform(inStream, outStream);
%>
<%= result.toString() %>
