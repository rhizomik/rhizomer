YUI().use('autocomplete', 'autocomplete-highlighters', 'datasource', 'datasource-local', 'datasource-arrayschema', 'json', function (Y) {

    var formatterTemplate = '{label} a {class}';
    var formatterTemplate2 = '{label} collection ({instances})';

    function formatter(query, results){
        return Y.Array.map(results, function (result) {
            if(result.raw.c){
                return Y.Lang.sub(formatterTemplate, {
                    label       : result.highlighted,
                    class        : makeLabel(result.raw.c)
                });
            }
            else{
                return Y.Lang.sub(formatterTemplate2, {
                    label       : result.highlighted,
                    instances        : result.raw.instances
                });
            }
        });
    }

    var ac = new Y.AutoComplete({
        inputNode			: "#search_input",
        source			: "/autoCompleteSearch.jsp?q={query}",
        render 			: true,
        resultFilters: ['charMatch', 'wordMatch'],
        resultHighlighter	: "phraseMatch",
        resultListLocator: 'results',
        resultTextLocator: 'label',
        resultFormatter: formatter,
        queryDelay: 500,
        /*requestTemplate: function (query) {
            return encodeURI(query);
        },*/
        on : {
            select : function(e) {
                var link = (e.result.raw.link);
                self.location=link;
            }
        }
    });

    ac.set("maxResults", 15);
    ac.set("minQueryLength", 3);
    ac.set("activateFirstItem", false);
    ac.set("scrollIntoView", true);

    $j("#search_input").focus(function() {
        if($j("#search_input").val()=="Quick search...")
            $j("#search_input").val("");
    });

    $j("#search_input").blur(function() {
        if($j("#search_input").val()=="")
            $j("#search_input").val("Quick search...");
    });


    /*var obj = {type : "http://data.linkedmdb.org/resource/movie/film",
        focus : {uri : "http://data.linkedmdb.org/resource/film/62", label: 'Airplane!'}
    };
    */

/*
  var obj = {type : "http://data.linkedmdb.org/resource/movie/film",
        filters : [
            {property : "http://data.linkedmdb.org/resource/movie/actor",
                value : "http://data.linkedmdb.org/resource/actor/29679"
                }
        ]};
*/
    /*
    var obj = {type : "http://data.linkedmdb.org/resource/movie/film"};
    console.log(JSON.stringify(obj));
    var uriparam = (encodeURIComponent(JSON.stringify(obj)));

    console.log(uriparam);
    */
    /*
    var obj2 = decodeURIComponent(uriparam);

    console.log(obj2);
    */


});


