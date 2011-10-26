/*
function addInputFocus(){

	$j('.facet_form input[type=text]').focus(function(){
		alert("hola");
		$j(this).attr('value', '');
	});
}
function addInputBlur(){
	$j('.facet_form input[type=text]').blur(function(){
		if($j(this).attr('value') == ''){
			$j(this).attr('value', $j(this).attr('title'));
		}
	});
}
*/

function addToggle(){
	$j("div.facet_title").each(function(){
		$j(this).click(function(){
			$j(this).toggleClass("facet_title_show");			
		});	
	});
}

function makeLabel(uri){
	var uriSplitted = uri.split("#");
	if(uriSplitted.length>1)
		return uriSplitted[uriSplitted.length-1];
	else{
		uriSplitted = uri.split("/");
		if(uriSplitted.length>1 && uri.charAt(uri.length-1)!='/')
			return uriSplitted[uriSplitted.length-1];
		else if(uriSplitted.length>1)
			return uriSplitted[uriSplitted.length-2];
		else
			return uri;
	}	
}

function getPrefix(uri){
	label = makeLabel(uri);
	return uri.replace(label,"");
}


function loadFacets()
{	
	$j("#facets").html("<p style=\"font-weight:bold\">Loading filters...</p>");
	$j("#facets").append("<img id='waitImage' src=\"images/black-loader.gif\"/>");	
	/*rhz.listResources(fm.makeSPARQL());*/
	parameters = {};
	parameters["facetURI"] = facetURI;
	parameters["mode"] = "facets";

	rhz.getFacets(parameters, 
			function(output) 
			{
				var response = output.evalJSON();		
				html = "<div class='filter_by'>Filter by:</div>";
				html += "<div class='reset_facets'><a href=''>Reset filters</a></div>";
				
				$j("#facets").html(html);
				$j.each(response.properties, 
					function(i, property)
					{
						fm.addFacet(property);
					});
				fm.renderFacets("facets");
				addToggle();
				fm.setDefaultFilters();  
				fm.reloadFacets();
				fm.printActive();
			}
	);
}

function addSlashes(str) {
	str = str.replace(/\'/g,'\\\'');
	str = str.replace(/\"/g,'\\"');
	return str;
}

function replaceDot(str) {
	str = str.replace(/\./g,'\\\\.'); // escapar el "."
	return str;
}