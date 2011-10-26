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

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import net.rhizomik.rhizomer.autoia.classes.FacetProperties;
import net.rhizomik.rhizomer.autoia.classes.FacetValue;
import net.rhizomik.rhizomer.autoia.manager.FacetManager;

public class RhizomerFacets extends HttpServlet {

	private ServletConfig config;
	private FacetManager fm;
	private String filePath;
	
	public void init(ServletConfig config) throws ServletException
    {   
        super.init(config);
        this.config = config;
		String path = config.getServletContext().getRealPath("WEB-INF");
		
		String datasetId = "";
		if (config.getServletContext().getInitParameter("db_graph")!=null)
			datasetId = config.getServletContext().getInitParameter("db_graph");
		else if (config.getServletContext().getInitParameter("db_url")!=null)
			datasetId = config.getServletContext().getInitParameter("db_url");
		else if (config.getServletContext().getInitParameter("file_name")!=null)
			datasetId = config.getServletContext().getInitParameter("file_name");
		
		int facetHash = datasetId.hashCode();
		String file = "facets-"+facetHash+".db";
		
		filePath = path+="/"+file;
		System.out.println(filePath);

    }
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		try {
			this.fm = new FacetManager(filePath);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
        
		String mode = request.getParameter("mode");
		String facetURI = request.getParameter("facetURI");

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

		FacetManager fm;
		try {
			fm = new FacetManager(filePath);
			FacetProperties properties = fm.getProperties(facetURI, omitProperties);
			StringBuffer propertiesOut = new StringBuffer();
			propertiesOut = properties.printJSON();    		    
			StringBuffer output = new StringBuffer();
			output.append("{\"properties\": "+propertiesOut+ "}");
			PrintWriter out = response.getWriter();
			out.println(output);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
