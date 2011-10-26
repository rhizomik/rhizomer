package net.rhizomik.wiki;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author  : http://rhizomik.net/~roberto
 */
public class Save extends HttpServlet 
{
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException 
    {
    	String filename = this.getServletContext().getRealPath("/html"+request.getPathInfo()+"/index.html");
    	String content = request.getParameter("textcontent");

        // TODO: manage the history of the file 
        //HistoryManager.manageHistory(filename, "index.html");

        try 
        {
            File f = new File(filename);
            f.getParentFile().mkdirs();
            f.createNewFile();
            BufferedWriter out;
            if (content != null) {
                out = new BufferedWriter(new FileWriter(f));
                out.write(content);
                out.flush();
                out.close();
            }                        
        } 
        catch (Exception e) { throw new ServletException(e); }
        
        response.sendRedirect(request.getContextPath()+"/html"+request.getPathInfo());
    }
}
