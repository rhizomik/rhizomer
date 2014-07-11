package net.rhizomik.rhizomer.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import net.rhizomik.rhizomer.service.ServiceManager;

/**
 * Serve the HTML representation of the resource descriptions.
 * 
 * @author  : http://rhizomik.net/~roberto
 */

@SuppressWarnings("serial")
public class RhizomerHTML extends HttpServlet
{
	private RhizomerRDF store = null;
	private RDF2HTMLTransformer transformer = null;
        
    public RhizomerHTML() 
    {
        super();
    }
        
    public void init(ServletConfig config) throws ServletException
    {   
        super.init(config);
        store = RhizomerRDF.instance();
        try 
        {
			transformer = new RDF2HTMLTransformer(getServletContext().getRealPath("/"));
		} 
        catch (Exception e) 
        {
			throw new ServletException(e);
		}
    }
    
    public void destroy()
    {
    }
    
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
    	
    	response.setBufferSize(8192);
    	response.setCharacterEncoding("UTF-8");
    	response.setContentType("text/xhtml");
    	
    	// Get the metadata associated with the requested URL
    	String rdf = store.getMetadata(request);
        ServiceManager serviceManager = new ServiceManager(request.getSession(false));
        rdf = serviceManager.addServices(rdf,request);

		//Generate HTML views for the metadata and content, taking into account the preferred language
		request.setAttribute("metadata", transformer.rdf2html(rdf, request));
		
		try
		{
			request.setAttribute("pagecontent", readHTMLContent(request));
			RequestDispatcher d = null;
			if (request.getParameter("edit")!=null)
				d = getServletConfig().getServletContext().getNamedDispatcher("edit.jsp");
			else
				d = getServletConfig().getServletContext().getNamedDispatcher("view.jsp");
            d.forward(request, response);
		}
		catch(FileNotFoundException e)
		{
			request.setAttribute("pagecontent", "");
			RequestDispatcher d = null;
			if (request.getParameter("edit")!=null)
				d = getServletConfig().getServletContext().getNamedDispatcher("edit.jsp");
			else
				d = getServletConfig().getServletContext().getNamedDispatcher("new.jsp");
            d.forward(request, response);
		}
	}

    /**  POST new HTML content for a new or updated HTML resource
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
    	RequestDispatcher d = getServletConfig().getServletContext().getNamedDispatcher("save");
        d.forward(request, response);
	}

	/** DELETE HTML resource, delete the corresponding file
	 *  TODO: implement and maintain history, keep old version + timestamp
	 */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		/*response.setBufferSize(8192);
    	response.setCharacterEncoding("UTF-8");
    	RequestDispatcher d = getServletConfig().getServletContext().getNamedDispatcher("delete");
        d.forward(request, response);*/
	}
    
	
	
	private String readHTMLContent(HttpServletRequest request) throws FileNotFoundException 
	{
        //String filename = request.getPathTranslated()+ File.separator + "index.html";
        String filename = this.getServletContext().getRealPath("/html"+request.getPathInfo()+"/index.html");
        
        StringBuffer b = new StringBuffer();
        BufferedReader in = null;        
        FileReader fr = new FileReader(filename);
        
        try
        {
        	in =  new BufferedReader(fr);
	        String l = null;
	        while ((l=in.readLine()) != null)
	        {
	            b.append(l);
	            b.append(System.getProperty("line.separator"));
	        }
        }
        catch(IOException e)
        {
        	b.append(e);
        }
        finally 
        { try { in.close(); } catch(IOException e2){} }

        return b.toString();
    }
}
