/**
 * The RhizomerDescribeHandlerFactory class creates RhizomerDescribeHandlers.
 * 
 * @author: http://rhizomik.net/~roberto
 */

package net.rhizomik.rhizomer.store.jena;

import com.hp.hpl.jena.sparql.core.describe.DescribeHandler;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerFactory;

public class RhizomerDescribeHandlerFactory implements DescribeHandlerFactory {

	private boolean includeLabels = true;
	
	public RhizomerDescribeHandlerFactory(boolean includeLabels) 
	{
		super();
		this.includeLabels = includeLabels;
	}

	public DescribeHandler create() 
	{
		RhizomerDescribeHandler handler = new RhizomerDescribeHandler(includeLabels);
		return handler;
	}

}
