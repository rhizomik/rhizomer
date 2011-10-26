package net.rhizomik.rhizomer.store;

import java.util.Properties;

import javax.servlet.ServletConfig;

public class MetadataStoreFactory 
{
	public static MetadataStore getMetadataStore (ServletConfig config) throws Exception
	{
		Class storeClass = Class.forName(config.getServletContext().getInitParameter("store_class"));
        MetadataStore store = (MetadataStore)storeClass.newInstance();
        store.init(config);
        return store;
	}
	
	public static MetadataStore getMetadataStore (Properties props) throws Exception
	{
		Class storeClass = Class.forName(props.getProperty("store_class"));
        MetadataStore store = (MetadataStore)storeClass.newInstance();
        store.init(props);
        return store;
	}
}
