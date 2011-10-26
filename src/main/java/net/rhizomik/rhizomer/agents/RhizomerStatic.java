package net.rhizomik.rhizomer.agents;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author  : http://rhizomik.net/~roberto
 */
public class RhizomerStatic extends HttpServlet
{
	private static final long serialVersionUID = 1L;
        
    public RhizomerStatic() 
    {
        super();
    }
        
    public void init(ServletConfig servletConfig) throws ServletException
    {   
        super.init(servletConfig);
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
    				throws ServletException, IOException
	{
    	performTask(request, response);
	}
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
					throws ServletException, IOException
	{
    	performTask(request, response);
	}
    
	/**
     *  Serve the content of an static resource, a file like /../*.*
     *  
     */
    public void performTask(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException
    {   	
    	RequestDispatcher d = null;
    	if (request.getPathInfo().endsWith(".jsp"))
    		d = getServletConfig().getServletContext().getNamedDispatcher("jsp");
    	else
    		d = getServletConfig().getServletContext().getNamedDispatcher("default");
    	d.forward(request, response);
    }

	protected static boolean isStaticResource(String path)
	{
		String token = null;
		if (path!=null)
		{
			StringTokenizer st = new StringTokenizer(path, "/");
			while (st.hasMoreTokens())
				token = st.nextToken();
		}
		return token!=null?token.indexOf('.')>0:false;
	}
}
