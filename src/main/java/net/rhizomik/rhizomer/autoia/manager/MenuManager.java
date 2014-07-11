package net.rhizomik.rhizomer.autoia.manager;

import net.rhizomik.rhizomer.autoia.classes.MenuConfig;

import javax.servlet.ServletConfig;
import java.io.File;

public class MenuManager {


    private static MenuManager instance =  null;
    private ServletConfig config;
    private HierarchyManagerSPARQL manager = null;
    private HierarchyManagerSKOS skosManager = null;

    public static MenuManager getInstance(ServletConfig config) {
        if(instance == null)
            instance = new MenuManager(config);
        return instance;
    }

    public HierarchyManagerSPARQL getManager(){
        return manager;
    }

    public HierarchyManagerSKOS getSKOSManager(){
        return skosManager;
    }

    private MenuManager(ServletConfig config) {
        this.config = config;
        try {
            getMenu();
            //getSkosMenu();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getMenu() throws Exception{

		String path = config.getServletContext().getRealPath("/WEB-INF");
		
		String datasetId = "";
		if (config.getServletContext().getInitParameter("db_graph")!=null)
			datasetId = config.getServletContext().getInitParameter("db_graph");
		else if (config.getServletContext().getInitParameter("db_url")!=null)
			datasetId = config.getServletContext().getInitParameter("db_url");
		else if (config.getServletContext().getInitParameter("file_name")!=null)
			datasetId = config.getServletContext().getInitParameter("file_name");
		int menuHash = datasetId.hashCode();

        MenuConfig menuConfig = new MenuConfig(path+"/menuconfig.xml");
        String store = menuConfig.getStore();

        manager = new HierarchyManagerSPARQL();
        manager.setMenuConfig(menuConfig);
        if(store.equals("xml")){
            File f = new File(path+"/menu-"+menuHash+".xml");
            if(!f.exists()){
                manager.readModel();
                manager.writeXMLFile(path+"/menu-"+menuHash+".xml");
            }
            else
                manager.readXML(path+"/menu-"+menuHash+".xml");
        }
        else if(store.equals("void")){
            File fvoid = new File(path+"/menu-"+menuHash+".void");
            if(!fvoid.exists()){
                manager.readModel();
                //manager.readXML(path+"/menu-"+menuHash+".xml");
                manager.writeVoid(path+"/menu-"+menuHash+".void");
            }
            else
               manager.readVoid(path+"/menu-"+menuHash+".void");
        }
	}

    public void getSkosMenu() throws Exception{
        String path = config.getServletContext().getRealPath("WEB-INF");

        String datasetId = "";
        if (config.getServletContext().getInitParameter("db_graph")!=null)
            datasetId = config.getServletContext().getInitParameter("db_graph");
        else if (config.getServletContext().getInitParameter("db_url")!=null)
            datasetId = config.getServletContext().getInitParameter("db_url");
        else if (config.getServletContext().getInitParameter("file_name")!=null)
            datasetId = config.getServletContext().getInitParameter("file_name");
        int menuHash = datasetId.hashCode();

        MenuConfig menuConfig = new MenuConfig(path+"/menuconfig.skos.xml");
        config.getServletContext().setAttribute("menuFileSKOS", "menu-"+menuHash+".skos.xml");
        File fskos = new File(path+"/menu-"+menuHash+".skos.xml");
        skosManager = new HierarchyManagerSKOS();
        skosManager.setMenuConfig(menuConfig);
        if(!fskos.exists()){
            skosManager.readModel();
            skosManager.writeXMLFile(path+"/menu-"+menuHash+".skos.xml");
        }
        else
            skosManager.readXML(path+"/"+"menu-"+menuHash+".skos.xml");

    }

}