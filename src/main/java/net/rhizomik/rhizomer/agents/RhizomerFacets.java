package net.rhizomik.rhizomer.agents;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import net.rhizomik.rhizomer.autoia.classes.FacetProperties;
import net.rhizomik.rhizomer.autoia.classes.FacetValue;
import net.rhizomik.rhizomer.autoia.classes.HierarchyMenu;
import net.rhizomik.rhizomer.autoia.classes.HierarchyNode;
import net.rhizomik.rhizomer.autoia.manager.FacetManager;

public class RhizomerFacets extends HttpServlet {

    private FacetManager createFacetManager() throws ServletException {
        try {
            String filePath = getFilePath();
            return new FacetManager(filePath);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
            throw new ServletException(e);
		} catch (SQLException e) {
			e.printStackTrace();
            throw new ServletException(e);
		}
    }

    private String getFilePath() {
        ServletConfig config = getServletConfig();
        String filePath = config.getServletContext().getRealPath("WEB-INF");

        String dataSetId = "";
        if (config.getServletContext().getInitParameter("db_graph")!=null)
            dataSetId = config.getServletContext().getInitParameter("db_graph");
        else if (config.getServletContext().getInitParameter("db_url")!=null)
            dataSetId = config.getServletContext().getInitParameter("db_url");
        else if (config.getServletContext().getInitParameter("file_name")!=null)
            dataSetId = config.getServletContext().getInitParameter("file_name");

        int facetHash = dataSetId.hashCode();
        filePath += "/facets-"+facetHash+".db";

        System.out.println(filePath);
        return filePath;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        FacetManager fm = createFacetManager();
        response.setCharacterEncoding("UTF-8");

        String mode = request.getParameter("mode");
        String facetURI = request.getParameter("facetURI");

        HttpSession session = request.getSession(true);

        HierarchyMenu menu = (HierarchyMenu) session.getAttribute("originalMenu");
        HierarchyNode node = menu.getByUri(facetURI);
        int numInstances = node.getNumInstances();

        FacetProperties properties;

        try {
            if ("facets".equals(mode)) {
                ArrayList<String> omitProperties = new ArrayList<String>();
                //omitProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
                omitProperties.add("http://www.w3.org/2000/01/rdf-schema#subClassOf");
                omitProperties.add("http://www.w3.org/2002/07/owl#intersectionOf");
                /*
                omitProperties.add("http://xmlns.com/foaf/0.1/page");
                omitProperties.add("http://data.linkedmdb.org/resource/movie/filmid");
                omitProperties.add("http://data.linkedmdb.org/resource/movie/performance");
                omitProperties.add("http://data.linkedmdb.org/resource/movie/film_cut");
                 */
                properties = fm.getProperties(facetURI, omitProperties);
            } else if ("charts".equals(mode)) {
                properties = fm.getNumericProperties(facetURI);
            } else {
                throw new ServletException("Bad mode: " + mode);
            }
            
            StringBuffer propertiesOut = properties.printJSON();
            StringBuffer output = new StringBuffer();
            output.append("{\"numInstances\" : " + numInstances + ", \"properties\": " + propertiesOut + "}");
            PrintWriter out = response.getWriter();
            out.println(output);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}