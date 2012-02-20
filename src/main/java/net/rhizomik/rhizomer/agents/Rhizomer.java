package net.rhizomik.rhizomer.agents;

import net.rhizomik.rhizomer.autoia.generator.FacetGenerator;
import net.rhizomik.rhizomer.autoia.manager.MenuManager;
import net.rhizomik.rhizomer.service.ServiceManager;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
