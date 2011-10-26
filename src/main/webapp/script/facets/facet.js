facet.Facet = function(property)
{
	var self = this;

	/**
	 * Private Attributes
	 */	
	var id = hex_md5(property.uri);
	var uri = property.uri;
	var label = property.label;
	var range = property.range;
	var valueList = {};
	var selectedValues = {};
	var numValues = 0;
	var numSelectedValues = 0;
	var selected = false;
	var opened = false;
	
	self.getId = function(){
		return id;
	};

	self.getLabel = function(){
		return label;
	};
	
	self.getUri = function(){
		return uri;
	};
	
	self.getSelectedValues = function(){
		return selectedValues;
	};	
	
	self.isActive = function(){
		if(numSelectedValues>0)
			return true;
		else 
			return false;
	};
	
	self.setSelected = function(value){
		selected = value;
	};
	
	self.isSelected = function(){
		return selected;
	}
	
	self.getCurrentValues = function(){
		return numValues;
	}
	
	self.isOpened = function(){
		return opened;
	};	
	
	self.resetFacet = function(){
		numValues = 0;
		selected = false;
		valueList = {};
		$j("#"+id+"_ul").empty();
	}	
		
	self.renderBase = function(target){
		var html = "<div id=\""+id+"_facet\">";
		html += "<div id=\""+id+"_title\" class=\"facet_title\" onclick=\"fm.toggleFacet('"+id+"'); return false;\">";
		html +="<h4>" + label + "</h4>";
		html +="</div>";
		html +="<div id=\""+id+"_loading\"></div>";
		html +="<div class=\"facet_options\" id=\""+id+"_div\"></div>";
		$j("#"+target).append(html);
	};
	
	self.renderValueList = function(target){
		var html = "<div id=\""+id+"_values\"><ul id=\""+id+"_ul\" class=\"values\"></ul>";
		html += "<div class=\"more\"><a id=\""+id+"_more\" href=\"#\" >more values</a></div>";
		html+="</div>"
		$j("#"+target).append(html);
		$j("#"+id+"_more").click(function (){
			self.getMoreValues();
		});		
	};	
	
	self.renderEnd = function(target){
		$j("#"+target).append("</div><div class=\"facet_sep\"></div>");	
	};	
	
	self.toggleFacet = function(){
		if(opened){
			opened = false;
			$j("#"+id+"_div").hide();
			$j("#"+id+"_loading").empty().hide();
		}
		else{
			opened = true;
			if(numValues==0)
				self.getMoreValues();
			else
				$j("#"+id+"_div").show();	
		}
	};
	
	self.toggleValue = function(value){
		valueId = hex_md5(value);
		if(selectedValues[value]){
			delete selectedValues[value];
			numSelectedValues--;
			$j("#"+valueId).removeClass("selected_item");
			$j("#"+valueId).addClass("item");			
			return false;
		}
		else{
			selectedValues[value] = true;
			numSelectedValues++;
			$j("#"+valueId).removeClass("item");
			$j("#"+valueId).addClass("selected_item");
			return valueList[value];
		}	
	};	
	
	self.reloadValues = function(restrictions){
		$j("#"+id+"_div").css('display','none');
		$j("#"+id+"_loading").append("<img src=\"images/black-loader.gif\"/>");
		$j("#"+id+"_loading").show();			
		self.resetFacet();
		query ="PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + 
		"SELECT ?o (COUNT(?o) AS ?n) ?label "+
		"WHERE {"+
		"	?r a <"+facetURI+"> . "+
		"   ?r <"+uri+"> ?o ."+
		"   FILTER(?o!=\"\" && !isBlank(?o)) ."+
		" OPTIONAL{ ?o rdfs:label ?label " +
		"  FILTER(LANG(?label)='en' || LANG(?label)='')} ."+
		restrictions+
		" } GROUP BY ?o ?label ORDER BY DESC(?n) LIMIT 5";
		rhz.sparqlJSON(query, self.processMoreValues);
	}	
	
	self.getMoreValues = function(){
		$j("#"+id+"_div").css('display','none');
		$j("#"+id+"_loading").append("<img src=\"images/black-loader.gif\"/>");
		$j("#"+id+"_loading").show();		
		query ="PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
			   "SELECT ?o (COUNT(?o) AS ?n) ?label "+
		       "WHERE { "+
		            "?r a <"+facetURI+"> . "+
		            "?r <"+uri+"> ?o . "+
		    		"   FILTER(?o!=\"\" && !isBlank(?o)) ."+
		    		"OPTIONAL{ ?o rdfs:label ?label . " +
		    		"FILTER(LANG(?label)='en' || LANG(?label)='')} " +
		    		fm.makeRestrictions(uri)+
		    		" } GROUP BY ?o ?label ORDER BY DESC(?n) LIMIT 5 OFFSET "+self.getCurrentValues();
		rhz.sparqlJSON(query,self.processMoreValues);
	};
	
	
	self.processMoreValues = function(output){
		data = output.evalJSON();
		if(data.results.bindings.length > 0){
			$j("#"+id+"_facet").show();
			$j.each(data.results.bindings, function(i, option){
				if(i<5){
					if(option.label)
						tlabel = option.label.value;
					else
						tlabel = makeLabel(option.o.value);
					self.addValueToList(option.o.value, tlabel, option.n.value);
				}
			});
			$j("#"+id+"_loading").empty();
			$j("#"+id+"_loading").hide();
			$j("#"+id+"_div").show();
			$j("#"+id+"_values div.loading").remove();
			if(data.results.bindings.length >= 5)
				$j("#"+id+"_more").show();
			else
				$j("#"+id+"_more").hide();
		}
		else{
			$j("#"+id+"_loading").empty();
			$j("#"+id+"_loading").append("<div>This facet has no possible values</div>");
			/*$j("#"+id+"_facet").hide();*/
		}
	};	
	
	self.addValueToList = function(value, vlabel, instances){
		valueList[value] = new FacetValue(value, vlabel, instances); 
		if(selectedValues[value])
			cls = "selected_item";
		else
			cls = "item";
		html = "<li class=\""+cls+"\" id=\""+hex_md5(value)+"\" onclick=\"javascript:fm.filterProperty('"+uri+"','"+value+"'); return false;\">";
		html += "<div class='item_text'>"+vlabel+" ("+instances+")</div></li>";
		$j("#"+id+"_ul").append(html);
		numValues++;
	};	
	
	return self;
};