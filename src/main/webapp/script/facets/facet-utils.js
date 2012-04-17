/*
function addInputFocus(){

	$j('.facet_form input[type=text]').focus(function(){
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

function makeLabel(uri, capitalize){
    if (!uri || uri == "null")
        return "";
	if(capitalize == null)
		capitalize = true;
	var label;
	var uriSplitted = uri.split("#");
	if(uriSplitted.length>1)
		label = uriSplitted[uriSplitted.length-1];
	else{
		uriSplitted = uri.split("/");
		if(uriSplitted.length>1 && uri.charAt(uri.length-1)!='/')
			label = uriSplitted[uriSplitted.length-1];
		else if(uriSplitted.length>1)
			label = uriSplitted[uriSplitted.length-2];
		else
			label = uri;
	}
	if(capitalize)
		label = label.replace(/_/g," ").capitalize();
	return label;
}

function splitPrefix(uri){
	return uri.split(":");
}

String.prototype.capitalize = function(){
	return this.replace( /(^|\s)([a-z])/g , function(m,p1,p2){ return p1+p2.toUpperCase(); } );
};

function addSlashes(str) {
	str = str.replace(/\'/g,'\\\'');
	str = str.replace(/\"/g,'\\"');
	return str;
}

function replaceDot(str) {
	str = str.replace(/\./g,'\\\\.'); // escapar el "."
	return str;
}
