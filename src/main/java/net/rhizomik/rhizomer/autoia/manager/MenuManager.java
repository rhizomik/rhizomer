package net.rhizomik.rhizomer.autoia.manager;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.rhizomik.rhizomer.autoia.classes.HierarchyMenu;
import net.rhizomik.rhizomer.autoia.classes.MenuConfig;
import net.rhizomik.rhizomer.util.FacetUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MenuManager {


    private static MenuManager instance =  null;
    private ServletConfig config;
    private HierarchyManagerSPARQL manager = null;
    private HierarchyManagerSKOS skos = null;

    public static MenuManager getInstance(ServletConfig config) {
        if(instance == null)
            instance = new MenuManager(config);
        return instance;
    }

    public HierarchyManagerSPARQL getManager(){
        return manager;
    }

    public HierarchyManagerSKOS getSKOSManager(){
        return skos;
    }

    private MenuManager(ServletConfig config) {
        this.config = config;
        try {
            getMenu();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getMenu() throws Exception{

		String path = config.getServletContext().getRealPath("WEB-INF");
		
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

        if(store.equals("xml")){
            File f = new File(path+"/menu-"+menuHash+".xml");
            manager = new HierarchyManagerSPARQL();
            if(!f.exists()){
                manager.readModel();
                manager.writeXMLFile(path+"/menu-"+menuHash+".xml");
            }
            else
                manager.readXML(path+"/menu-"+menuHash+".xml");
        }
        else if(store.equals("void")){
            File fvoid = new File(path+"/"+menuHash+".void");
            manager = new HierarchyManagerSPARQL();
            if(!fvoid.exists()){
                manager.readXML(path+"/menu-"+menuHash+".xml");
                manager.writeVoid(path+"/"+menuHash+".void");
            }
            else
               manager.readVoid(path+"/"+menuHash+".void");
        }

        /*

        config.getServletContext().setAttribute("voidFile", menuHash+".void");
        config.getServletContext().setAttribute("menuFile", "menu-"+menuHash+".xml");

        File fvoid = new File(path+"/"+menuHash+".void");
        if(!fvoid .exists()){
            HierarchyManagerSPARQL manager = new HierarchyManagerSPARQL();
            manager.readModel();
            manager.writeXMLFile(path+"/menu-"+menuHash+".xml");
        }

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

		*/
	}

    public String getJSON(String path) throws ParserConfigurationException, IOException, SAXException {

        File file = new File(path);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeLst = doc.getElementsByTagName("branch");
        StringBuffer out = new StringBuffer();

        out.append("{\"id\": \"root\", \"name\": \"Classes\", \"$area\": 0, \"data\": {\"instances\": 0, \"size\": 0}, \"children\": [");

        for (int s = 0; s < nodeLst.getLength(); s++) {
            Node xmlNode = nodeLst.item(s);
            String uri = xmlNode.getAttributes().getNamedItem("uri").getTextContent();
            int instances = Integer.parseInt(xmlNode.getAttributes().getNamedItem("instances").getTextContent());
            String label = xmlNode.getAttributes().getNamedItem("label").getTextContent();

            //String label = FacetUtil.makeLabel(uri);

            out.append("\n{ data :{\"instances\": "+instances+", \"$area\": "+instances+"}, \"id\": \""+uri+"\" ,  \"name\":\""+label.replace("'","\'")+"\"," +

                    "\"children\":[");

            out.append(getChildNodes(xmlNode));

            out.append("]},");

        }


        out.append("]}");

        System.out.println(out);

        return out.toString();
    }

    private static StringBuffer getChildNodes(Node xmlNode){
        StringBuffer out = new StringBuffer();
        NodeList childNodes = xmlNode.getChildNodes();

        for(int i=0; i<childNodes.getLength(); i++){
            Node childNode = childNodes.item(i);
            if(childNode.getNodeName().equals("node")){
                out.append(getXMLNode(childNode));
            }
        }

        return out;
    }


    private static StringBuffer getXMLNode(Node xmlNode){
        String uri = xmlNode.getAttributes().getNamedItem("uri").getTextContent();
        int instances = Integer.parseInt(xmlNode.getAttributes().getNamedItem("instances").getTextContent());
        String label = xmlNode.getAttributes().getNamedItem("label").getTextContent();
        label = FacetUtil.makeLabel(label);

        StringBuffer out = new StringBuffer();

        out.append("\n{ data :{\"instances\": "+instances+", \"$area\": "+instances+"}, \"id\":\""+uri+"\", \"name\":\""+label.replace("'","\'")+"\"," +

                "\"children\":[");

        out.append(getChildNodes(xmlNode));

        out.append("]},");

        return out;

    };


}