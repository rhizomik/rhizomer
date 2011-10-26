<%@page pageEncoding="UTF-8"%>
<%@page contentType="text/html;charset=UTF-8"%>

<%@page import="com.hp.hpl.jena.rdf.model.*"%>
<%@page import="com.hp.hpl.jena.rdf.model.impl.*"%>
<%@page import="com.hp.hpl.jena.query.*"%>
<%@page import="com.hp.hpl.jena.vocabulary.*"%>
<%@page import="com.hp.hpl.jena.query.ResultSet"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.net.URL"%>
<%@page import="java.io.BufferedInputStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.InputStream"%>
<% 
	request.setCharacterEncoding("UTF-8");
	
	final Property TRANSCRIPT = new PropertyImpl("http://www.w3.org/ns/ma-ont#hasAudioDescription");
	final Property COPY = new PropertyImpl("http://rhizomik.net/ontologies/2009/09/copyrightonto.owl#hasInstance");
    
   	String audioUrl = "";
	String audioTranscript = "";	
	
	Model model = ModelFactory.createMemModelMaker().createDefaultModel();
	try { model.read(request.getInputStream(), "RDF/XML"); }
	catch (Exception e) {}
	
	ResIterator it = model.listResourcesWithProperty(COPY, null);
	if (it.hasNext())
	{
		Resource r = it.nextResource();
		audioUrl = r.getProperty(COPY).getObject().toString(); 
		Statement s = r.getProperty(TRANSCRIPT);
		if (s!=null)
		{
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			BufferedInputStream bin = null;
			try
        	{
				URL audioTranscriptURL = new URL(s.getObject().toString());
				bin = new BufferedInputStream(audioTranscriptURL.openStream(), 8192);
				byte[] b = new byte[8192];
				int l;
				while((l=bin.read(b))>0)
				{
					bout.write(b, 0, l);
				}
				bout.close();
			}
	        catch(Exception e)
	        {
	        	audioTranscript = e.toString();
	        }
	        audioTranscript = bout.toString("UTF-8");
	        
		}
	}	
%>
<div>
    <br/>
    <br/>
    <object data="<%=request.getContextPath()%>/services/player/player.swf?soundFile=<%=audioUrl%>" type="application/x-shockwave-flash" pluginspage="http://www.macromedia.com/go/getflashplayer" height="24" width="290">
        <param value="<%=request.getContextPath()%>/services/player/player.swf?soundFile=<%=audioUrl%>" name="movie" />
        <param value="high" name="quality" />
        <param value="true" name="menu" />
        <param value="transparent" name="wmode" />
    </object>
    <div style="">
        <%= audioTranscript%>
    </div>
</div>
