package net.rhizomik.rhizomer.agents;

import net.rhizomik.rhizomer.service.ServiceManager;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Serve the HTML representation of the resource descriptions.
 * 
 * @author  : http://rhizomik.net/~roberto
 */

@SuppressWarnings("serial")
public class RhizomerData extends HttpServlet
{
	private RhizomerRDF store = null;

    public RhizomerData()
    {
        super();
    }
        
    public void init(ServletConfig config) throws ServletException
    {   
        super.init(config);
        store = RhizomerRDF.instance();
    }
    
    public void destroy()
    {
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException
    {
        String accept = request.getHeader("Accept");

        // TODO: take into account the Q parameter in the Accept header
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
        else // application/rdf+xml
        {
            String rdf = store.getMetadata(request);
            response.setBufferSize(8192);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/rdf+xml");
            PrintWriter out = response.getWriter();
            out.print(rdf);
            out.close();
        }
    }

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
	}
}
