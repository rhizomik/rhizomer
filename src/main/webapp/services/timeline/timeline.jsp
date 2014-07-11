<%@page import="java.util.HashMap"%>
<%@page import="com.hp.hpl.jena.rdf.model.*"%>
<%@page import="com.hp.hpl.jena.rdf.model.impl.*"%>
<%@page import="com.hp.hpl.jena.query.*"%>
<%@page import="com.hp.hpl.jena.vocabulary.*"%>
<%@page import="com.hp.hpl.jena.query.ResultSet"%>
<%@ page import="java.net.URL" %>
<%@ page import="com.hp.hpl.jena.util.FileUtils" %>
<%@ page import="java.net.HttpURLConnection" %>
<%@ page import="java.io.StringReader" %>
<%@ page import="java.net.MalformedURLException" %>
<%@ page import="java.io.IOException" %>
<%
    response.setCharacterEncoding("UTF-8");

    final String queryString =
    "PREFIX ical: <http://www.w3.org/2002/12/cal/icaltzd#> 	\n"+
    "PREFIX dc: <http://purl.org/dc/elements/1.1/> 		   	\n"+
    "SELECT ?r 												\n"+
    "WHERE { { ?r ical:dtsart ?start; ical:dtend ?end. }	\n"+
    "		UNION { ?r dc:date ?date. } }					\n";

    class TimeLineEvent
    {
	    private static final long serialVersionUID = 1L;
    	public String id;
		public String start;
    	public String end;
    	public String title;
    	public String html;

    	final Property DTEND = new PropertyImpl("http://www.w3.org/2002/12/cal/icaltzd#dtend");
    	final Property DTSTART = new PropertyImpl("http://www.w3.org/2002/12/cal/icaltzd#dtstart");

    	public TimeLineEvent(Resource r)
    	{
    		if (r.hasProperty(DTSTART))
    		{
    	    	start = r.getProperty(DTSTART).getString();
    	    	end = r.getProperty(DTEND).getString();
    	    }
        	else
        	{
    	    	start = r.getProperty(DC.date).getString();
    	    	end = start;
    	    }
    	    if (r.hasProperty(DC.title))
    	    	title = r.getProperty(DC.title).getString().replace("'", "&acute;");
    	    else if (r.hasProperty(RDFS.label))
    	    	title = r.getProperty(RDFS.label).getString().replace("'", "&acute;");
    	    else
    	    	title = r.getURI();
    	    html = "<div></div>";
    	}
    }

    class RequestModelReader
    {
        private HttpServletRequest request;
        private RequestModelReader(HttpServletRequest request)
        {
            this.request = request;
        }

        Model readModelFromRequest() throws IOException
        {
            Model model = ModelFactory.createMemModelMaker().createDefaultModel();

            // Check if input RDF comes through an attribute or form parameter
            String rdf = (String) request.getSession(false).getAttribute("rdf");
            if (rdf == null)
                rdf = (String) request.getParameter("rdf");

            if (rdf != null)
            {
                // First try to interpret the rdf parameter/attribute as URL to load RDF from
                String format = null;
                try
                {
                    URL rdfURL = new URL(rdf);
                    format = FileUtils.guessLang(rdf);
                    HttpURLConnection urlConn = (HttpURLConnection)rdfURL.openConnection();
                    urlConn.setRequestProperty("Accept", "application/rdf+xml, text/plain;q=0.5, text/rdf+n3;q=0.6");
                    model.read(urlConn.getInputStream(), rdf, format);
                }
                // If not an URL, load RDF from rdf parameter/attribute
                catch (MalformedURLException e)
                {
                    model.read(new StringReader(rdf), "", format);
                }
            }
            // If there is no rdf parameter/attribute, read RDF from request input stream
            else
                model.read(request.getInputStream(), "RDF/XML");
            return model;
        }
    }

	// Get list of previously drawn events from session, or create it
    HashMap<String,TimeLineEvent> events = new HashMap<String,TimeLineEvent>();
    if (session.getAttribute("TimeLineEvents") != null)
        events = (HashMap<String,TimeLineEvent>)session.getAttribute("TimeLineEvents");

    RequestModelReader reader = new RequestModelReader(request);
	Model model = reader.readModelFromRequest();

	// Add events with DTSTART and DTEND or DATE, if already present remove
	Query query = QueryFactory.create(queryString);
  	QueryExecution qexec = QueryExecutionFactory.create(query, model);
  	try
  	{
    	ResultSet results = qexec.execSelect();
    	for (; results.hasNext();)
	    {
	    	QuerySolution soln = results.nextSolution();
	      	Resource r = soln.getResource("r");
	        if (events.containsKey(r.toString()))
        		events.remove(r.toString());
        	else
        		events.put(r.toString(), new TimeLineEvent(r));
	    }
  	} finally { qexec.close(); }

    session.setAttribute("TimeLineEvents", events);
%>

<script type="text/javascript">
    var tl;
    function onLoadTimeLine()
    {
        var eventSource = new Timeline.DefaultEventSource();
        <%  for(TimeLineEvent t : events.values()){ %>
        var events = {'dateTimeFormat': 'iso8601', 'events': [
                    	{
                        	'start': '<%=t.start%>',
                        	'end': '<%=t.end%>',
                        	'title': '<%=t.title%>',
                        	'description':'<%=t.html%>'
                    	}]
					 };
        try { eventSource.loadJSON(events, ''); }catch(err){}
        <% } %>

        var theme = Timeline.ClassicTheme.create();
        theme.event.label.width = 250;
        theme.event.bubble.width=320;
        theme.event.bubble.height=220;
        var bandInfos = [
          Timeline.createBandInfo({
              width: "20%",
              intervalUnit: Timeline.DateTime.DAY,
              intervalPixels: 100,
              eventSource: eventSource,
              theme:theme
          }),
          Timeline.createBandInfo({
              width: "50%",
              intervalUnit: Timeline.DateTime.MONTH,
              intervalPixels: 100,
              eventSource: eventSource,
              theme:theme
          }),
          Timeline.createBandInfo({
              width: "30%",
              intervalUnit: Timeline.DateTime.YEAR,
              intervalPixels: 200,
              eventSource: eventSource,
              theme:theme
          })
        ];
        bandInfos[1].syncWith = 0;
        bandInfos[2].syncWith = 0;
        bandInfos[1].highlight = true;
        bandInfos[2].highlight = true;
        tl = Timeline.create(document.getElementById("tline"), bandInfos);
        //Resize when all events have been loaded
        tl.finishedEventLoading()
     }

     var resizeTimerID = null;
     function onResize() {
         if (resizeTimerID == null) {
             resizeTimerID = window.setTimeout(function() {
                 resizeTimerID = null;
                 tl.layout();
             }, 500);
         }
     }
</script>
<div id="tline" style="height: 400px; border: 1px solid #aaa"><p style="text-align: center;">Loading...</p></div>

<script type="text/javascript">
	   var Timeline_API = "http://api.simile-widgets.org/timeline/2.3.1/timeline-api.js";
       var SimileAjax_urlPrefix = "http://api.simile-widgets.org/ajax/2.2.1/";
       var Exhibit_urlPrefix = "http://api.simile-widgets.org/exhibit/2.2.0/";
       var SimileAjax_onLoad = null;
       
       /* 
        *  You can optionally specify parameters:
        */
       var Exhibit_parameters = [
           {   name:  "views",
               value: "timeline"
           }
       ];
       
       window.setTimeout(function() {
           var head = document.getElementsByTagName("head")[0];
           
           var includeScript = function(url) {
               var script = document.createElement("script");
               script.type = "text/javascript";
               script.language = "JavaScript";
               script.src = url;
               head.appendChild(script);
           };
           
           var includeCSS = function(url) {
               var link = document.createElement("link");
               link.setAttribute("rel", "stylesheet");
               link.setAttribute("type", "text/css");
               link.setAttribute("href", url);
               head.appendChild(link);
           };
           
           SimileAjax_onLoad = function() {
               SimileAjax_onLoad = onLoadTimeLine;
               includeScript(Timeline_API);
           };
           
           includeScript(SimileAjax_urlPrefix + "simile-ajax-api.js");
       }, 100);
   </script>

