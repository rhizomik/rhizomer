package net.rhizomik.rhizomer.autoia.classes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HierarchyNode implements Comparable{

		private static final Logger log = Logger.getLogger(HierarchyNode.class.getName());

		public static String ALPHABETICAL = "alphabetical";
		public static String INSTANCES = "instances";
	
		protected String uri;
		protected List<String> aliases;
		protected int numInstances;
		protected List<HierarchyNode> childs;
		protected HierarchyNode parent;
		protected HierarchyNode lastParent;
		private boolean abstractNode;
		protected String label; 
		
		
		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
		
		public HierarchyNode(String uri) {
			this.uri = uri;
			this.childs = new ArrayList<HierarchyNode>();
			this.numInstances = 0;
			this.setAbstractNode(false);
		    makeLabel();
		}
		
		public HierarchyNode(String uri, Boolean abstractNode) {
			this.uri = uri;
			this.childs = new ArrayList<HierarchyNode>();
			this.numInstances = 0;
			this.setAbstractNode(abstractNode);
		    makeLabel();
		}
		
		public boolean hasChilds(){
			if (this.getChilds().size()>0)
				return true;
			else
				return false;
		}
		
		private void makeLabelOther(){
			if(this.uri.equals("#Other"))
				this.label = "Other";
			else{
				String[] uriSplitted = this.uri.split("#");
				if(uriSplitted.length>2)
					this.label = "Other " + uriSplitted[uriSplitted.length-1];
				else{
					uriSplitted = this.uri.split("/");
					if(uriSplitted.length>1)
						this.label = "Other " + uriSplitted[uriSplitted.length-1];
					else
						this.label = "Other " + this.uri;
				}
			}
			if (this.label == "Other Other")
				this.label = "Others";	
		}
		
		private void makeLabel(){
			if(this.isAbstractNode())
				makeLabelOther();
			else{
				String[] uriSplitted = this.uri.split("#");
				if(uriSplitted.length>1)
					this.label = uriSplitted[uriSplitted.length-1];
				else{
					uriSplitted = this.uri.split("/");
					if(uriSplitted.length>1)
						this.label = uriSplitted[uriSplitted.length-1];
					else
						this.label = this.uri;							
				}
			}
		}
		
		public HierarchyNode(HierarchyNode other){
			this.uri = other.uri;
			this.numInstances = other.numInstances;
			this.aliases = other.aliases;
			this.childs = new ArrayList<HierarchyNode>();
			this.setAbstractNode(other.isAbstractNode());
			this.label = other.label;
		}
		
		public void addAlias(String alias){
			if(this.aliases == null){
				this.aliases = new ArrayList<String>();
			}
			aliases.add(alias);
		}
		
		public List<String> getAliases() {
			return aliases;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}
		
		public void setParent(HierarchyNode parent){
			this.parent = parent;
		}
		
		public void setAbstractNode(boolean abstractNode){
			this.abstractNode = abstractNode;
		}
		
		public int getOwnedInstances() {
			return this.numInstances;
		}

		public int getNumInstances() {
			int total = this.numInstances;
			for(HierarchyNode n : childs){
				total += n.getNumInstances();
			}
			return total;
		}

		public HierarchyNode getLastParent() {
			return lastParent;
		}

		public void setLastParent(HierarchyNode lastParent) {
			this.lastParent = lastParent;
		}

		public void setNumInstances(int num) {
			this.numInstances = num;
		}
		
		public void addChild(HierarchyNode node){	
			// Check if node to be added as child already parent of the current node to avoid cycles in the hierarchy
			if (this.getParent()!=null && this.getParent().getLabel().equals(node.getLabel()))
	            log.log(Level.INFO, "Avoiding cycle in the hierarchy, "+this.getLabel()+" already child of "+node.getLabel());
			else
			{
				node.setParent(this);
				this.childs.add(node);
			}
		}
				
		public HierarchyNode getParent() {
			return parent;
		}

		public void addChildBeforeNode(HierarchyNode node, HierarchyNode before){
			node.setParent(this);
			this.childs.add(this.childs.indexOf(before),node);
		}
		
		public void deleteChild(HierarchyNode node){
			this.childs.remove(node);
		}
		
		public List<HierarchyNode> getChilds(){
			return this.childs;
		}
		
		/*public boolean skipNode(){
			if(this.getNumInstances()==0)
				return true;
			else if(this.getOwnedInstances()==0 && this.getChilds().size()==1)
				return true;
			else
				return false;
		}
		*/


		public boolean isAbstractNode() {
			return abstractNode;
		}
		
		public void print(int levels, int tab) {
			/*
			if(this.getOwnedInstances()==0 && this.getChilds().size()==1){
				this.getChilds().get(0).print(levels, tab);
			}
			else{
			*/
				for(int i=1;i<tab;i++)
					System.out.print("\t");
				System.out.println(label+" = "+this.getNumInstances());
				if(levels>1){
					for(HierarchyNode n : childs){
						n.print(levels-1, tab+1);
					}		
				}
			//}
			
		}
		
		public void printAsUl(HttpServletRequest req, int levels, StringBuffer out) 
		{			
			//TODO: IMPORTANT! Make link to facets.jsp independent from where the app is installed!
			//String link = req.getContextPath()+"/facets.jsp?uri="+this.uri.replace("#", "%23");
			String link = req.getContextPath()+"/facets.jsp?q=SELECT ?r1 WHERE{?r1 a <"+this.uri.replace("#", "%23")+">}";
			String query = "SELECT ?r WHERE { ?r ";
			
			String label = this.label;
			int pos;
			if ((pos = label.indexOf('@')) > 0)
				label = label.substring(0, pos);
			
			if (levels>1 && this.getChilds().size()>0)
			{
				if (this.uri.indexOf("#Other")>=0){
                    System.out.println(this.uri + " - " + this.label);
					out.append("<a class=\"yui3-menu-label\" href=\"#"+label+"-options\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");
                }
				else
				{
					out.append("<span class=\"yui3-menu-label\">");
					out.append("<a href=\""+link+"\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");
					out.append("<a href=\"#"+label+"-options\"></a>");
					out.append("</span>");
				}
				
				out.append("<div id=\""+label+"-options\" class=\"yui3-menu\">\n" + 
						   "	<div class=\"yui3-menu-content\">\n" + 
						   "		<ul>");
				for(HierarchyNode n : this.getChilds())
				{
					out.append("<li class=\"yui3-menuitem\">");
					n.printAsUl(req, levels-1, out);
					out.append("</li>");
				}
				out.append("		</ul>\n" + 
						   "	</div>\n" + 
						   "</div>");
			}
			else
				out.append("<a class=\"yui3-menuitem-content\" href=\""+link+"\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");
		}

		public void printAsUl(HttpServletRequest req, int levels, StringBuffer out, String property) 
		{
			String link = req.getContextPath()+"/show/?query=DESCRIBE ?r WHERE { ?r ";
			String query = "SELECT ?r WHERE { ?r ";
			
			link = link + "<"+property+"> <"+this.uri.replace("#", "%23")+"> }";
			query = query + "<"+property+"> <"+this.uri.replace("#", "%23")+"> }";
			
			String onclick = "javascript:rhz.listResources('"+query+"'); return false;";
			
			String label = this.label;
			int pos;
			if ((pos = label.indexOf('@')) > 0)
				label = label.substring(0, pos);
			
			if (levels>1 && this.getChilds().size()>0)
			{
				if (this.uri.indexOf("#Other")>=0)
					out.append("<a class=\"yui3-menu-label\" href=\"#"+label+"-options\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");
				else
				{
					out.append("<span class=\"yui3-menu-label\">");
					out.append("<a onclick=\""+onclick+"\" href=\""+link+"\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");
					out.append("<a href=\"#"+label+"-options\"></a>");
					out.append("</span>");
				}
				
				out.append("<div id=\""+label+"-options\" class=\"yui3-menu\">\n" + 
						   "	<div class=\"yui3-menu-content\">\n" + 
						   "		<ul>");
				for(HierarchyNode n : this.getChilds())
				{
					out.append("<li class=\"yui3-menuitem\">");
					n.printAsUl(req, levels-1, out, property);
					out.append("</li>");
				}
				out.append("		</ul>\n" + 
						   "	</div>\n" + 
						   "</div>");
			}
			else
				out.append("<a class=\"yui3-menuitem-content\" onclick=\""+onclick+"\" href=\""+link+"\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");
		}
		
		public int compareTo(Object o) { 
			HierarchyNode other = (HierarchyNode) o;
			if(other.isAbstractNode()) //Revisar
				return -1;
			else
				return this.label.compareToIgnoreCase(other.label);
		}
		
		
		protected void sort(String sort, int levels){
			
			Comparator c = new Comparator(){
				public int compare(Object o1, Object o2) {
					HierarchyNode node = (HierarchyNode) o1;
					HierarchyNode other = (HierarchyNode) o2;
					if(other.isAbstractNode())
						return -1;
					if(node.getNumInstances()>other.getNumInstances())
						return -1;
					else
						return 1;
				}
			};
			if(sort.equals(this.ALPHABETICAL))
				Collections.sort(this.childs);
			else
				Collections.sort(this.childs, c);
			if(levels>=1){
				for(HierarchyNode child : this.childs)
					child.sort(sort, levels-1);
			}			
		}
		
		public int countChildsWithInstances(){
			int total = 0;
			for(HierarchyNode child : this.childs){
				if(child.getNumInstances()>0)
					total++;
			}
			return total;
		}
		
		public HierarchyNode getChildWithInstances(){
			for(HierarchyNode child : this.childs){
				if(child.getNumInstances()>0)
					return child;
			}
			return null;
		}

        public void printAsSitemap(HttpServletRequest req, int levels, StringBuffer out)
    {
        //TODO: IMPORTANT! Make link to facets.jsp independent from where the app is installed!
        //String link = req.getContextPath()+"/facets.jsp?uri="+this.uri.replace("#", "%23");
        String link = req.getContextPath()+"/facets.jsp?q=SELECT ?r1 WHERE{?r1 a <"+this.uri.replace("#", "%23")+">}";
        String query = "SELECT ?r WHERE { ?r ";

        String label = this.label;
        int pos;
        if ((pos = label.indexOf('@')) > 0)
            label = label.substring(0, pos);

        if (levels>=1)
        {
            out.append("<a id=\""+this.getUri()+"\" class=\"sitemap-root\" href=\""+link+"\">"+label+" <span class=\"menu_instances\">("+this.getNumInstances()+")</span></a>");

            if(this.hasChilds()){
                out.append("<ul class=\"sub\">");
                java.util.Collections.sort(this.getChilds());
                for(HierarchyNode n : this.getChilds())
                {
                    out.append("<li class=\"inline\">");
                    String clink = req.getContextPath()+"/facets.jsp?q=SELECT ?r1 WHERE{?r1 a <"+n.uri.replace("#", "%23")+">}";
                    String clabel = n.label;
                    int cpos;
                    if ((cpos = clabel.indexOf('@')) > 0)
                        clabel = clabel.substring(0, cpos);
                    out.append("<a id=\""+n.getUri()+"\" class=\"sitemap\" href=\""+clink+"\">"+clabel+" <span class=\"menu_instances\">("+n.getNumInstances()+")</span></a>, ");
                    //n.printAsUl(req, levels-1, out);
                    out.append("</li>");
                }
                out.append("</ul>");
            }
        }

    }

    public StringBuffer printAsJSON(){
        String uri = this.getUri();
        int instances = this.getNumInstances();
        String label = this.getLabel();

        StringBuffer out = new StringBuffer();

        out.append("\n{ data :{\"instances\": "+instances+", \"$area\": "+instances+"}, \"id\":\""+uri+"\", \"name\":\""+label.replace("'","\'")+"\"," +

                "\"children\":[");

        for(HierarchyNode child : this.getChilds()){
            out.append(child.printAsJSON());
        }

        out.append("]},");

        return out;
    }

}