facet.NumberFacet = function(property, fm, typeUri){

    var that = new facet.Facet(property, fm, typeUri);
    var min;
    var max;
    var from;
    var to;

    that.setMin = function(value){
        min = value;
    };

    that.setMax = function(value){
        max = value;
    };

    that.getMin = function(){
        return min;
    };

    that.getMax = function(){
        return max;
    };

    that.setFrom = function(value){
        from = value;
    };

    that.setTo = function(value){
        to = value;
    };

    that.getFrom = function(){
        return from;
    };

    that.getTo = function(){
        return to;
    };

    that.isActive = function(){
        if(from!=min || to!=max)
            return true;
        else
            return false;
    };

    that.reset = function(){
        that.setFrom(that.getMin());
        $j("#"+that.getId()+"_slider").slider('values', 0, that.getMin());
        that.setTo(that.getMax());
        $j("#"+that.getId()+"_slider").slider('values', 1, that.getMax());
        facetBrowser.reloadFacets(false);
        facetBrowser.printBreadcrumbs();
    };

    that.printActive = function(){
        html = "<b>"+that.getLabel()+"</b> is ";
        html += from + " - " + to + "&nbsp;";
        html += "<a class=\"pointer\" onclick=\"javascript:facetBrowser.removeRangeProperty('"+that.getClassURI()+"','" +that.getId()+"'); return false;\"><img src='images/delete_blue.png'/></a>";
        return html;
    };

    that.render = function (target){
        that.renderBase(target);
        that.getEnds();
        /*that.renderNumber(that.getId()+"_facet");*/
        that.renderEnd(target);
    };

    that.renderNumber = function (target){
        html = "&nbsp;From&nbsp;<input type='text' value='"+min+"' style='width:50px;' id=\""+that.getId()+"_min\">";
        html += "&nbsp;To&nbsp;<input type='text' value='"+max+"' style='width:50px;' id=\""+that.getId()+"_max\">";
        html += "<div id=\""+that.getId()+"_slider\" style=\"text-align:center; margin-top:5px; width:90%;\">";
        $j("#"+target).append(html);

        $j("#"+that.getId()+"_slider" ).slider({ animate: "fast", max : max, min : min,
            range: true, values : [min,max],
            step : 0.01,
            stop: function( event, ui ) {
                facetBrowser.reloadFacets(false);
                facetBrowser.printBreadcrumbs();
            },
            slide: function( event, ui ) {
                from = ui.values[0];
                to = ui.values[1];
                $j("#"+that.getId()+"_min").val(ui.values[0]);
                $j("#"+that.getId()+"_max").val(ui.values[1]);
            }
        });

        $j("#"+that.getId()+"_min").blur(function() {
            value = parseFloat($j(this).val());
            if(!value>=that.getMin())
                value = that.getMin();
            that.setFrom(value);
            $j("#"+that.getId()+"_min").val(value);
            $j("#"+that.getId()+"_slider").slider('values', 0, that.getFrom());
            facetBrowser.reloadFacets(false);
            facetBrowser.printBreadcrumbs();
        });

        $j("#"+that.getId()+"_max").blur(function() {
            value = parseFloat($j(this).val());
            if(!value<=that.getMax())
                value = that.getMax();
            that.setTo(value);
            $j("#"+that.getId()+"_max").val(value);
            $j("#"+that.getId()+"_slider").slider('values', 1, that.getTo());
            facetBrowser.reloadFacets(false);
            facetBrowser.printBreadcrumbs();
        });
    }

    that.getEnds = function(){
        var query = "SELECT (min(?o) as ?min) (max(?o) as ?max) " +
            "WHERE {?r a <"+that.getClassURI()+"> ." +
            "?r <"+that.getUri()+"> ?o }";
        rhz.sparqlJSON(query, function(out){
            data = JSON.parse(out);
            min = parseFloat(data.results.bindings[0].min.value);
            max = parseFloat(data.results.bindings[0].max.value);
            from = min;
            to = max;
            /* Calcular step */
            that.renderNumber(that.getId()+"_facet");
        });
    };

    that.makeSPARQL = function (varCount, varName){
        if(that.isActive()){
            var query = "?"+varName+" <"+that.getUri()+"> ?"+varName+"var"+varCount+ " FILTER(";
            query += "?"+varName+"var"+varCount+">="+from+" && ?"+varName+"var"+varCount+"<="+to+") . ";
        }
        return query;
    };

    return that;
};
