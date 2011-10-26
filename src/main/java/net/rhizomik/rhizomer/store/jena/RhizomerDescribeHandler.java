/**
 * The RhizomerDescribeHandler class builds up the subgraphs used for visualisation 
 * and manipulation. Implements the Concise Bounded Description approach plus all available
 * labels for the involved resources.
 * 
 * @author: http://rhizomik.net/~roberto
 */
package net.rhizomik.rhizomer.store.jena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.sparql.core.describe.DescribeHandler;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerFactory;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class RhizomerDescribeHandler implements DescribeHandler
{    
    public Model acc;
    boolean labels = true;
    
    public RhizomerDescribeHandler()
    {
    	super();
    }
    
    public RhizomerDescribeHandler(boolean includeLabels)
    {
    	super();
    	this.labels = includeLabels;
    }    
    
	public void start(Model model, Context ctxt)
	{
		this.acc = model;
	}
    
	public void describe(Resource r)
	{
		List visited = new ArrayList();
    	visited.add(r);
    	
    	Selector s = new SimpleSelector(r, null, (RDFNode)null);
    	StmtIterator sIter = null;
    	Model model = r.getModel();
    	
    	if (model instanceof InfModel)
			sIter = ((InfModel)model).getRawModel().listStatements(s);
    	else
    		sIter = model.listStatements(s);
    	
        for (; sIter.hasNext(); )
        {
            Statement stmt = sIter.nextStatement();
            acc.add(stmt);
            if (labels)
        		acc.add(stmt.getPredicate().listProperties(RDFS.label));
            
            closure(stmt.getObject(), visited);
        }
	}
	
	public void finish()
	{	}
    
    private void closure(RDFNode n, Collection visited)
    {
    	if (!(n instanceof Resource))
            return;
        Resource r = (Resource)n;
    	if (visited.contains(r))
    		return;
        if (!r.isAnon()) 
        {
        	//Stop if not anonymous but add all available labels for r if requested
        	if (labels)
        		acc.add(r.listProperties(RDFS.label));
        	return;
        }
    	
    	visited.add(r);
    	StmtIterator sIter = r.listProperties() ;
        for (; sIter.hasNext(); )
        {
            Statement stmt = sIter.nextStatement();
            acc.add(stmt);
            if (labels)
        		acc.add(stmt.getPredicate().listProperties(RDFS.label));
            
            closure(stmt.getObject(), visited);
        }
    }
}
