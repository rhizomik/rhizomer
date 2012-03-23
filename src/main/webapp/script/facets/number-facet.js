 facet.NumberFacet = function(property){
	var that = new facet.Facet(property);
	var min;
	var max;
	var numValues;
	var numBins;
	var range;
	var begin;
	var end;
	that.mode = "list";
	that.histogramStatus = false;
	that.bins = new Array();
	that.histogram;
	that.from;
	that.to;
	
	that.render = function (target){
		that.renderBase(target);
		that.renderNumber(that.getLabel()+"_div");
		that.renderValueList(that.getLabel()+"_div");
		that.renderEnd(target);
	};
	
	that.makeSPARQL = function (varCount){
		var query;
		if(that.mode == "range"){
			query = "?r <"+that.getUri()+"> ?o . FILTER(?o>="+that.from+" && ?o<="+that.to+") .";
		}
		else{
			query = "?r <"+that.getUri()+"> ?var"+varCount+ " FILTER(";
	    	for(value in that.getSelectedValues()){
	    		query+="str(?var"+varCount+")=\""+addSlashes(value)+"\" ||";
	    	}
	    	query = query.substring(0,query.length-2);
	    	query += ") ."
			/*query = "?r <"+that.getUri()+"> \""+that.getSelectedValue()+"\"^^<"+encodeURIComponent(that.getRange())+"> . ";*/
			
		}
		return query;
	};
	
	that.renderNumber = function (target){
		/*
		var html ="<div id=\""+that.getLabel()+"_bar\" class=\"visualizations\">" +
				"<div style=\"display:none\"><a href=\"#\" onclick=\"fm.defaultView('"+that.getUri()+"'); return false;\">Default View</a></div>" +		
				"<div><a href=\"#\" onclick=\"fm.histogramView('"+that.getUri()+"'); return false;\">Histogram View</a></div>" +
				"</div>";
		*/
		/*html += "<div class=\"facet_form\">";
		html += "<input style=\"width:50px;\" type=\"text\" id=\""+that.getLabel()+"_min\" title=\"from...\" value=\"from...\"/>";
		html += "<input style=\"width:50px;\" type=\"text\" id=\""+that.getLabel()+"_max\" title=\"to...\" value=\"to...\"/>";
		html += "<input style=\"width: 28px;\" type=\"submit\" value=\"&gt;\" onclick=\"javascript:filterMinMaxProperty('"+that.getUri()+"');\">";
		html += "</div>";*/
		//$j("#"+target).append(html);
	};
	
	that.makeHistogram = function (){
		$j("#"+that.getLabel()+"_values").hide();
		if(that.histogramStatus){
			$j("#"+that.getLabel()+"_histogram").show();
			$j("#"+that.getLabel()+"_bar > div").toggle();
		}
		else{
			var html = "<div id=\""+that.getLabel()+"_loading\"><p>Loading...</p><img class='waitImage' src=\"images/black-loader.gif\"/></div>";
			$j("#"+that.getLabel()+"_div").append(html);
			var query = "SELECT (min(?o) as ?min) (max(?o) as ?max) (count(?o) as ?total) "+
				"WHERE { "+
				"?r a <"+facetURI+"> ; <"+that.getUri()+"> ?o . "+fm.makeRestrictions()+
				"}";
			
			rhz.sparqlRDF(query, that.processMaxMin);
		}
		
	};
	
	that.defaultView = function (){
		$j("#"+that.getLabel()+"_histogram").hide();
		$j("#"+that.getLabel()+"_values").show();
		$j("#"+that.getLabel()+"_bar > div").toggle();	
		that.mode = "list";
	};
	
	that.processMaxMin = function(resultsXMLDoc){
		var xmlobject = (new DOMParser()).parseFromString(resultsXMLDoc, "text/xml"); //revisar
		var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";
		var solution = xmlobject.getElementsByTagNameNS(rsNS,"solution")[0];
		var results = {};
		var bindings = solution.getElementsByTagNameNS(rsNS,"binding");
		for (var i = 0; i < bindings.length; i++) 
		{
			var variable = bindings[i].getElementsByTagNameNS(rsNS,"variable")[0].textContent;
			var value = bindings[i].getElementsByTagNameNS(rsNS,"value")[0].textContent;
			results[variable] = value;
		}
		that.min = parseInt(results["min"]);
		that.max = parseInt(results["max"]);
		that.numValues = parseInt(results["total"]);
		that.numBins = Math.round(1 + (Math.log(that.numValues)/Math.log(2)));
		that.range = Math.round((that.max - that.min)/that.numBins);  //Arrodonir
		that.begin = that.min;
		that.end = that.min+that.range;
		that.getBins();
	};
	
	that.getBins = function(){
		query = "SELECT (COUNT(?r) AS ?total) WHERE{"+
			"?r a <"+facetURI+"> ; <"+that.getUri()+"> ?o . "+fm.makeRestrictions()+
			" FILTER(?o >= "+that.begin+" && ?o <= "+that.end+")" +
			"}";
		if(that.begin<=that.max){
			that.begin = that.end+1;
			that.end = that.end + that.range;			
			rhz.sparqlRDF(query, that.processBins);
		}
		else{
			that.renderHistogram();
		}
	};
	
	that.processBins = function(resultsXMLDoc){
		var xmlobject = (new DOMParser()).parseFromString(resultsXMLDoc, "text/xml"); //revisar
		var rsNS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";
		var solution = xmlobject.getElementsByTagNameNS(rsNS,"solution")[0];
		var results = {};
		var bindings = solution.getElementsByTagNameNS(rsNS,"binding");
		for (var i = 0; i < bindings.length; i++) 
		{
			var variable = bindings[i].getElementsByTagNameNS(rsNS,"variable")[0].textContent;
			var value = bindings[i].getElementsByTagNameNS(rsNS,"value")[0].textContent;
			results[variable] = value;
		}

		that.bins.push(results["total"]);
		that.getBins();
	};
	
	that.renderHistogram = function(){
		$j("#"+that.getLabel()+"_loading").remove();
		var html = "<div id='"+that.getLabel()+"_histogram'>";
		html += "<div id='"+that.getLabel()+"_histogramwidget'></div>";
		html += "<input type='hidden' id='"+that.getLabel()+"_from'>";
		html += "<input type='hidden' id='"+that.getLabel()+"_to'>";
		html += "<div class=\"more\"><a href=\"#\" id='"+that.getLabel()+"_hbutton'>Apply filter</a></div></div>";
		$j("#"+that.getLabel()+"_div").append(html);
		data = { baseBins : that.bins, bins : that.bins, min : that.min, max: that.max, step: that.getStep(), from : that.min};
		that.from = that.min;
		that.to = that.max;
		config = {mode:"range"};
		that.histogram = new RangeFacet(that, that.getLabel(),$j("#"+that.getLabel()+"_histogramwidget"), config, null);
		that.histogram.updateState(data);
		that.histogramStatus = true;
		$j("#"+that.getLabel()+"_bar > div").toggle();
		$j("#"+that.getLabel()+"_hbutton").click(function (){
			that.setActive(true);
			that.mode = "range";
			fm.reloadFacets();
		});
	};
	
	that.getStep = function(){
		step = 1;
		diff = that.max - that.min;
		if(diff > 10){
			while(step*100 < diff)
				step = step*10;
		}
		else{
			while (step*100 > diff)
				step = step/10;
		}
		return step;
	};
	
	
	return that;
};
