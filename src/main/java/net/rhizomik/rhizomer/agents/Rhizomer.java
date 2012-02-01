package net.rhizomik.rhizomer.agents;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.rhizomik.rhizomer.autoia.generator.FacetGenerator;
import net.rhizomik.rhizomer.autoia.manager.MenuManager;
import net.rhizomik.rhizomer.service.ServiceManager;

/**
 * Rhizomer servlet responsible for capturing all request (except /html/* ones) and
 * performing content negotiation. If the request is for application/rdf+xml, RDF metadata
 * for the requested resource is served. Otherwise, a 303 redirect to /html/* is performed.
 * 
 * Requests include GET (LinkedData and SPARQL queries), PUT (replace metadata for resource), 
 * POST (add new metadata) and DELETE (delete all metadata for resource).
 * 
 * @author  : http://rhizomik.net/~roberto
 */

public class Rhizomer extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private RhizomerRDF store = null;
	private RDF2HTMLTransformer transformer = null;
        
    public Rhizomer() 
    {
        super();	
    }
        
    public void init(ServletConfig config) throws ServletException
    {   
        super.init(config);
        try
        {
        	store = RhizomerRDF.instance();
        	store.addStore(config);
			transformer = new RDF2HTMLTransformer(getServletContext().getRealPath("/"));
        	MenuManager menuMng = new MenuManager();
        	menuMng.getMenu(config);
        	FacetGenerator fg = new FacetGenerator(config);
        }
        catch(Exception e)
        { throw new ServletException(e); }
    }
    
    public void destroy()
    {
    	store.destroy();
    }
    
	/**
     *  REST implementation: GET resource representation depending on content
     *  negotiation (RDF or HTML). Representations include also the result of 
     *  SPARQL queries that can be posed through the "query" parameter
     *  
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException
    {
    	String ua = request.getHeader("User-Agent").toLowerCase();
    	if(ua.matches(".*(android.+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino).*")||ua.substring(0,4).matches("1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|e\\-|e\\/|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(di|rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|xda(\\-|2|g)|yas\\-|your|zeto|zte\\-")){
    		RequestDispatcher d = getServletContext().getNamedDispatcher("RhizomerMobile");
            d.forward(request, response);
            //return;
    	}
    	else{
    		
    	
    	// If the requested resource is a file (i.e. *.*), forward to RhizomerStatic
    	if (RhizomerStatic.isStaticResource(request.getPathInfo()))
    	{
    		RequestDispatcher d = getServletContext().getNamedDispatcher("RhizomerStatic");
            d.forward(request, response);
            return;
    	}
    	
    	// Content negotiation
    	String accept = request.getHeader("Accept");
    	String requestedWith = request.getHeader("X-Requested-With");
    	
    	if (accept!=null && accept.indexOf("application/json")>=0)
    	{
    		String json = store.getMetadataJSON(request);
	    	response.setBufferSize(8192);
	    	response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.print(json);
			out.close();
    	}
    	// If the request is for RDF/XML, return metadata and done
    	// TODO: take into account the Q parameter in the Accept header
    	else if (accept!=null && accept.indexOf("application/rdf+xml")>=0)
    	{
        	String rdf = store.getMetadata(request);
                ServiceManager serviceManager = new ServiceManager();
                rdf = serviceManager.addServices(rdf,request);
        	response.setBufferSize(8192);
        	response.setCharacterEncoding("UTF-8");
    		response.setContentType("application/rdf+xml");
    		PrintWriter out = response.getWriter();
    		out.print(rdf);
    		out.close();
    	} 
    	// The request is for HTML but from AJAX, send just the HTML snippet 
    	// for the requested metadata without the rest of the HTML page template
    	else if (requestedWith!=null && requestedWith.equals("XMLHttpRequest"))
    	{
    		String rdf = store.getMetadata(request);
                ServiceManager serviceManager = new ServiceManager();
                rdf = serviceManager.addServices(rdf,request);
        	response.setBufferSize(8192);
        	response.setCharacterEncoding("UTF-8");
        	response.setContentType("text/xhtml");
    		PrintWriter out = response.getWriter();
    		out.print(transformer.rdf2html(rdf, request));
    		out.close();
    	}
    	// Else, 303 redirect to /html/* which will be catch by RhizomerHTML
    	else
    	{
    		URL requestURL = new URL(request.getRequestURL().toString());
    		String base = requestURL.getProtocol()+"://"+requestURL.getHost()+(requestURL.getPort()>0?":"+requestURL.getPort():"");
    		String destination = base+request.getContextPath()+"/html"+request.getPathInfo();
    		if (request.getQueryString() != null)
    			destination += "?"+request.getQueryString();
    		response.setStatus(HttpServletResponse.SC_SEE_OTHER);
    		response.setHeader("Location", destination);
    	}
    	}
    }

	/** REST PUT implementation: replace the metadata for the given URI
	 *  PUT URI content-type RDF (content) --> removeRDF(CBD(URI)), storeRDF(content)
	 *  TODO: keep record of previous metadata (history) and contributor
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String uri = request.getParameter("uri");
		if (uri!=null)
		{
			try { store.remove(new URI(uri)); }
			catch (URISyntaxException e){} 
		}
		
    	response.setBufferSize(8192);
    	response.setCharacterEncoding("UTF-8");
    	// If the request if for RDF
    	if (request.getContentType().indexOf("application/rdf+xml")>=0 ||
       		request.getContentType().indexOf("application/n-triples")>=0 ||
       		request.getContentType().indexOf("application/n3")>=0)
    	{
    		String storedData = store.store(request.getInputStream(), request.getContentType());
			response.setContentType("application/rdf+xml");
		    PrintWriter out = response.getWriter();
		    out.print(storedData);
		    out.close();
		} 
	}

    /** REST POST implementation: insert additional metadata
     *  POST URL content-type RDF (content) --> storeRDF(content)
     *  TODO: keep record of the contributor
     */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
    	// If the requested resource is a file (i.e. *.*), forward to RhizomerStatic
    	if (RhizomerStatic.isStaticResource(request.getPathInfo()))
    	{
    		RequestDispatcher d = getServletContext().getNamedDispatcher("RhizomerStatic");
            d.forward(request, response);
            return;
    	}
    	
		response.setBufferSize(8192);
    	response.setCharacterEncoding("UTF-8");
    	// If the posted data is RDF
    	if (request.getContentType().indexOf("application/rdf+xml")>=0 ||
    		request.getContentType().indexOf("application/n-triples")>=0 ||
    		request.getContentType().indexOf("application/n3")>=0)
    	{
    		String storedData = "";
    		InputStream input = request.getInputStream();
    		
    		if (request.getContentLength() < 256) // Try to parse as URL
    		{
    			StringBuffer out = new StringBuffer();
    		    byte[] b = new byte[256];
    		    for (int n; (n = request.getInputStream().read(b)) != -1;)
    		        out.append(new String(b, 0, n));
    		    try
    		    {
    		    	URL url = new URL(out.toString());
    		    	input = url.openStream();
    		    }
    		    catch(MalformedURLException e)
    		    {}
    		}
    		
    		storedData = store.store(input, request.getContentType());
    		
			response.setContentType("application/rdf+xml");
	    	PrintWriter out = response.getWriter();
	    	out.print(storedData);
	    	out.close();
    	}
	}

	/** REST DELETE implementation: remove all metadata describing the resource,
	 *  i.e. its Concise Bounded Description (CBD)
	 *  DEL URL --> removeRDF(URL)
	 *  TODO: maintain history, store old version + timestamp?
	 */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String uri = request.getParameter("uri");
		try
		{
			store.remove(new URI(uri));
			String rdf = store.query("DESCRIBE <"+uri+">");
			response.setBufferSize(8192);
	    	response.setCharacterEncoding("UTF-8");
			response.setContentType("application/rdf+xml");
	    	PrintWriter out = response.getWriter();
	    	out.print(rdf);
	    	out.close();
		} 
		catch (URISyntaxException e)
		{
			throw new ServletException(e.toString());
		}
	}
}
