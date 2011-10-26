package net.rhizomik.rhizomer.util;

import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FacetUtil {
	
	public static String makeLabel(String uri){
		String[] uriSplitted = uri.split("#");
		if(uriSplitted.length>1)
			return uriSplitted[uriSplitted.length-1];
		else{
			uriSplitted = uri.split("/");
			if(uriSplitted.length>1)
				return uriSplitted[uriSplitted.length-1];
			else
				return uri;							
		}
	}
	
	public static String makeQueryString(Map<String, String[]> parameters, String omitParameter) throws UnsupportedEncodingException{
		String ret = new String("");
		for(String parameter : parameters.keySet()){
			if(!parameter.equals(omitParameter)){
				if(!ret.equals(""))
					ret = ret + "&";
				ret = ret + URLEncoder.encode(parameter,"UTF-8") + "=" + URLEncoder.encode(parameters.get(parameter)[0],"UTF-8");
			}
		}
		return ret;
	}

}
