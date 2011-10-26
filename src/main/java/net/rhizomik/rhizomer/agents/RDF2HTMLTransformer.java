package net.rhizomik.rhizomer.agents;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.rhizomik.rhizomer.util.URIResolverImpl;

import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementAssign;
import com.hp.hpl.jena.sparql.syntax.ElementBind;
import com.hp.hpl.jena.sparql.syntax.ElementDataset;
import com.hp.hpl.jena.sparql.syntax.ElementExists;
import com.hp.hpl.jena.sparql.syntax.ElementFetch;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementMinus;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementNotExists;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementService;
import com.hp.hpl.jena.sparql.syntax.ElementSubQuery;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;

public class RDF2HTMLTransformer
{
	private Transformer transformer = null;

	public RDF2HTMLTransformer(String base) throws FileNotFoundException, TransformerConfigurationException
	{
        URIResolver resolver = new URIResolverImpl(base);
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.setURIResolver(resolver);
        FileInputStream rdf2htmlFile = new FileInputStream(base + "style/rdf2html.xsl");
		transformer = factory.newTransformer(new StreamSource(rdf2htmlFile));
	}
	
	public String rdf2html(String source, HttpServletRequest request) 
	{
		String direction = null;
		String orderProperty = null;
		String language = (String) request.getSession().getAttribute("language");
		
		if (request.getParameter("query") != null)
    	{
    		String queryString = request.getParameter("query");
    		Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
    		if (query.hasOrderBy())
    		{
    			List<SortCondition> orders = query.getOrderBy();
    			direction = orders.get(0).getDirection()==-2?"ascending":"descending";
    			VisitOrderProperty visit = new VisitOrderProperty(orders.get(0).getExpression().getVarName());
    			query.getQueryPattern().visit(visit);
    			orderProperty = visit.getOrderProperty();
    		}
    	}
		StringWriter result = new StringWriter();
		try
		{
			StreamSource inStream = new StreamSource(new StringReader(source));
			StreamResult outStream = new StreamResult(result);
			transformer.clearParameters();
			if (language!=null) transformer.setParameter("language", language);
            transformer.setParameter("mode", "rhizomer");
            transformer.setParameter("logo", "false");
            if (direction!=null) transformer.setParameter("direction", direction);
            if (orderProperty!=null) transformer.setParameter("order", orderProperty);
			transformer.transform(inStream, outStream);
		}
		catch (TransformerConfigurationException e) {} 
		catch (TransformerException e) {}
		return result.toString();
	}

	private class VisitOrderProperty implements ElementVisitor
	{
		String orderProperty = "";
		String varName = "";
		
		public VisitOrderProperty (String varName)
		{
			this.varName = varName;
		}
		
		public String getOrderProperty()
		{
			return orderProperty;
		}
		
		public void visit(ElementTriplesBlock block) 
		{
			List<Triple> triples = block.getPattern().getList();
			for (Triple triple : triples) 
			{
				if (triple.getObject().isVariable())
				{
					String tripleVarName = ((Node_Variable)triple.getObject()).getName();
					if (tripleVarName.equals(varName))
						orderProperty = triple.getPredicate().toString();
				}
			}
		}
		public void visit(ElementPathBlock block)
		{
			List<TriplePath> triples = block.getPattern().getList();
			for (TriplePath triple : triples) 
			{
				if (triple.getObject().isVariable())
				{
					String tripleVarName = ((Node_Variable)triple.getObject()).getName();
					if (tripleVarName.equals(varName))
						orderProperty = triple.getPredicate().toString();
				}
			}
		}
		public void visit(ElementFilter arg0) {}
		public void visit(ElementAssign arg0) {}
		public void visit(ElementUnion arg0) {}
		public void visit(ElementOptional arg0) {}
		public void visit(ElementGroup group)
		{
			List<Element> groupElems = group.getElements();
			for (Element element : groupElems) {
				element.visit(this);
			}
		}
		public void visit(ElementDataset arg0) {}
		public void visit(ElementNamedGraph arg0) {}
		public void visit(ElementExists arg0) {}
		public void visit(ElementNotExists arg0) {}
		public void visit(ElementMinus arg0) {}
		public void visit(ElementService arg0) {}
		public void visit(ElementFetch arg0) {}
		public void visit(ElementSubQuery arg0) {}
		public void visit(ElementBind arg0) {}	
	}
}
