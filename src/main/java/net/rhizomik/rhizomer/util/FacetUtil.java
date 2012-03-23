package net.rhizomik.rhizomer.util;

import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FacetUtil {
	
	public static String makeLabel(String uri){
		String label = null;
		String[] uriSplitted = uri.split("#");
		if(uriSplitted.length>1)
			label = uriSplitted[uriSplitted.length-1];
		else{
			uriSplitted = uri.split("/");
			if(uriSplitted.length>1)
				label = uriSplitted[uriSplitted.length-1];
			else
				label = uri;							
		}
		return capitalizeString(label.replace("_", " "));
	}
	
	public static String makeInverseLabel(String uri, String inverseClassUri){
		String uriLabel = makeLabel(uri);
		String inverseLabel = makeLabel(inverseClassUri);
		return "Is "+uriLabel+" of "+inverseLabel;
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
	
	public static String capitalizeString(String string) {
		  char[] chars = string.toLowerCase().toCharArray();
		  boolean found = false;
		  for (int i = 0; i < chars.length; i++) {
		    if (!found && Character.isLetter(chars[i])) {
		      chars[i] = Character.toUpperCase(chars[i]);
		      found = true;
		    } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
		      found = false;
		    }
		  }
		  return String.valueOf(chars);
	}

}
