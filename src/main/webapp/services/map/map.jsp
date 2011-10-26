<script>var key_google = "ABQIAAAASq3AuJIpvOnNgZujr6uwpRT4jtj0wBQwejnU1B7AitQLzcoysBR5wZ_N6GdPzKNXHr5IryKdzG3M6w";</script>
<%@page import="java.util.HashMap"%>
<%@page import="com.hp.hpl.jena.rdf.model.*"%>
<%@page import="com.hp.hpl.jena.rdf.model.impl.*"%>
<%@page import="com.hp.hpl.jena.query.*"%>
<%@page import="com.hp.hpl.jena.vocabulary.*"%>
<%@page import="com.hp.hpl.jena.query.ResultSet"%>
<%
    response.setCharacterEncoding("UTF-8");

    final String queryString =
    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> \n"+
    "PREFIX georss: <http://www.georss.org/georss#>		   	\n"+
    "PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>       \n"+
    "SELECT DISTINCT ?r 									\n"+
    "WHERE { { ?r geo:lat ?lat; geo:long ?long. }			\n"+
    "  UNION { ?r vcard:geo ?geo. ?geo vcard:latitude ?lat; vcard:longitude ?long. } \n"+
    "  UNION { ?r georss:point ?point. } }				    \n";

    class GeoPoint
    {
    	public String id;
		public String lat;
    	public String lon;
    	public String title;
    	public String html;

    	final Property LAT = new PropertyImpl("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
    	final Property LON = new PropertyImpl("http://www.w3.org/2003/01/geo/wgs84_pos#long");
    	final Property VFN = new PropertyImpl("http://www.w3.org/2006/vcard/ns#fn");
    	final Property VGEO = new PropertyImpl("http://www.w3.org/2006/vcard/ns#geo");
    	final Property VLAT = new PropertyImpl("http://www.w3.org/2006/vcard/ns#latitude");
    	final Property VLON = new PropertyImpl("http://www.w3.org/2006/vcard/ns#longitude");
    	final Property POINT = new PropertyImpl("http://www.georss.org/georss#point");

    	public GeoPoint(Resource r)
    	{
    		if (r.hasProperty(LAT))
    		{
    	    	lat = r.getProperty(LAT).getString();
    	    	lon = r.getProperty(LON).getString();
    	    }
    		else if (r.hasProperty(VGEO))
    		{
    			Resource geo = r.getPropertyResourceValue(VGEO);
    	    	lat = geo.getProperty(VLAT).getString();
    	    	lon = geo.getProperty(VLON).getString();
    	    }
        	else
        	{
        		String point = r.getProperty(POINT).getString();
        		String[] coor = point.split(" ");
        		if (coor.length == 2)
    	    	{
    	    		lat = coor[0];
    	    		lon = coor[1];
    	    	}
    	    }
    	    if (r.hasProperty(DC.title))
    	    	title = r.getProperty(DC.title).getString().replace("'", "&acute;");
    	    else if (r.hasProperty(RDFS.label))
    	    	title = r.getProperty(RDFS.label).getString().replace("'", "&acute;");
    	    else if (r.hasProperty(VFN))
    	    	title = r.getProperty(VFN).getString().replace("'", "&acute;");
    	    else
    	    	title = r.getURI();
    	    html = "<div><p>"+title+"</p></div>";
    	}
    }

	// Get list of previously drawn events from session, or create it
    HashMap<String,GeoPoint> points = new HashMap<String,GeoPoint>();
    if (session.getAttribute("GoogleMapPoints") != null)
        points = (HashMap<String,GeoPoint>)session.getAttribute("GoogleMapPoints");

	Model model = ModelFactory.createMemModelMaker().createDefaultModel();
	try { model.read(request.getInputStream(), "RDF/XML"); }
	catch (Exception e) {}

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
	        if (points.containsKey(r.toString()))
        		points.remove(r.toString());
        	else
        		points.put(r.toString(), new GeoPoint(r));
	    }
  	} finally { qexec.close(); }

    session.setAttribute("GoogleMapPoints", points);

    // Center map, currently just get first point as center
    String centerLat = "0";
    String centerLon = "0";
    String zoomLevel = "2";
    if (points.values().iterator().hasNext())
    {
    	GeoPoint first = points.values().iterator().next();
    	centerLat = first.lat;
    	centerLon = first.lon;
        zoomLevel = "8";
    }
%>

<script type="text/javascript">
//<![CDATA[

function loadAPI()
{
    var script = document.createElement("script");
    script.src = "http://www.google.com/jsapi?key="+key_google+"&amp;callback=loadMaps";
    script.type = "text/javascript";
    document.getElementsByTagName("head")[0].appendChild(script);
}

function loadMaps()
{
    //AJAX API is loaded successfully. Now lets load the maps api
    google.load("maps", "3",  {"callback" : onLoadMap,other_params:"sensor=false"});
}

var map = null;
function onLoadMap(){
    // Creating an option object for the map
    var options = {
      zoom: <%=zoomLevel%>,
      center: new google.maps.LatLng(<%=centerLat%>, <%=centerLon%>),
      mapTypeControl: true,
      mapTypeControlOptions: {style: google.maps.MapTypeControlStyle.DROPDOWN_MENU},
      navigationControl: true,
      navigationControlOptions: {style: google.maps.NavigationControlStyle.SMALL},
      mapTypeId: google.maps.MapTypeId.ROADMAP
    }
    // Initializing the map
    var map = new google.maps.Map(document.getElementById('map'), options);
    // Creating markers
    var infowindow = new google.maps.InfoWindow();
    <%
        int i = 0;
        for(GeoPoint gp : points.values())
        {
    %>
            var marker = new google.maps.Marker({
              position: new google.maps.LatLng(<%=gp.lat%>,<%=gp.lon%>),
              map: map,
              title: '<%=gp.title%>'
            });
            google.maps.event.addListener(marker, 'click', function() {
            	infowindow.setContent(this.title);
            	infowindow.open(map, this);
            });
    <%      i++;
        }
    %>
}
//]]>
</script>
<div id="map" name="map" style="width:100%; height: 400px"></div>
<script type="text/javascript">
    loadAPI();
</script>