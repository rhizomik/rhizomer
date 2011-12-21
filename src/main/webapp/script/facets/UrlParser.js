facet.UrlParser = function(query){
	var self = this;
	
	/**  
	 * Private attributes
	 */
	
	var reg_select = new RegExp("^SELECT\\s+\\?(\\S+)");
	var reg_where = new RegExp("WHERE\\s*{(.*)\\s*}");
	var reg_class = new RegExp("\\?(\\S+)\\s+a\\s+<(\\S+)>");
	var reg_restriction = new RegExp("\\?(\\S+)\\s+(\\S+)\\s+(\\S+)");
	var reg_filter = new RegExp("FILTER\\s*\\((.*)\\)\\s*$");
	var reg_str = new RegExp("str\\(\\?\\S+\\)\\s*=\\s*\"(\\S+)\"");
	
	var variable;
	var activeUri;
	var restrictions;
	
	self.getVariable = function(){
		return self.variable;
	};
	
	self.getActiveUri = function(){
		return self.activeUri;
	};
	
	self.getRestrictions = function(){
		return self.restrictions;
	};
	
	self.parse = function(){
			self.restrictions = new Array();
			tmp = query.match(reg_select);
			self.variable = tmp[1];
			tmp = query.match(reg_where)[1].split(' .');
			for(var i=0; i<tmp.length; i++){
				self.restrictions[i] = new Array();			
				tmp2 = tmp[i].match(reg_class);
				if(tmp2 && tmp2[1]==self.variable){
					self.activeUri = tmp2[2];		
				}
				else{				
					tmp2 = tmp[i].match(reg_restriction);
					if(tmp2){
						self.restrictions[i][0] = tmp2[1];				
						self.restrictions[i][1] = tmp2[2];
						self.restrictions[i][2] = tmp2[3];
						tmp3 = tmp[i].match(reg_filter);
						if(tmp3){
							self.restrictions[i][3] = new Array();
							tmp4 = tmp3[1].split(' || ');
							for(var x=0;x<tmp4.length;x++){
								tmp5 = tmp4[x].match(reg_str);
								self.restrictions[i][3][x]=tmp5[1];
							}
						}
					}
				}
			}
	};
}