package net.rhizomik.rhizomer.autoia.manager;

import java.io.File;

import javax.servlet.ServletConfig;

import net.rhizomik.rhizomer.autoia.classes.MenuConfig;

public class MenuManager {
	
	public void getMenu(ServletConfig config) throws Exception{

		String path = config.getServletContext().getRealPath("WEB-INF");
		
		String datasetId = "";
		if (config.getServletContext().getInitParameter("db_graph")!=null)
			datasetId = config.getServletContext().getInitParameter("db_graph");
		else if (config.getServletContext().getInitParameter("db_url")!=null)
			datasetId = config.getServletContext().getInitParameter("db_url");
		else if (config.getServletContext().getInitParameter("file_name")!=null)
			datasetId = config.getServletContext().getInitParameter("file_name");
		
		int menuHash = datasetId.hashCode();
		config.getServletContext().setAttribute("menuFile", "menu-"+menuHash+".xml");
		File f = new File(path+"/menu-"+menuHash+".xml");
		if(!f.exists()){
			HierarchyManagerSPARQL manager = new HierarchyManagerSPARQL();
			manager.readModel();
			manager.writeXMLFile(path+"/menu-"+menuHash+".xml");
		}
		config.getServletContext().setAttribute("menuFileSKOS", "menu-"+menuHash+".skos.xml");
		File fskos = new File(path+"/menu-"+menuHash+".skos.xml");
		if(!fskos.exists()){
			HierarchyManagerSKOS manager = new HierarchyManagerSKOS();
			manager.readModel();
			manager.writeXMLFile(path+"/menu-"+menuHash+".skos.xml");
		}
	}

}
