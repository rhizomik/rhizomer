package net.rhizomik.wiki;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// import net.rhizomik.wiki.util.HistoryManager;

/**
 * @author  : http://rhizomik.net/~roberto
 */
public class Delete extends HttpServlet {
    boolean DEBUG = true;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String namespace = (String) request.getAttribute("namespace");
        String pagename = (String) request.getAttribute("pagename");
        String button = request.getParameter("button");

        if ("ok".equals(button)) {
//            HistoryManager.deleteWikiPage(request, response);
        }

        if (request.getParameter("backurl") != null) {
            response.sendRedirect(request.getParameter("backurl"));
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
