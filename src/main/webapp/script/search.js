YUI().use('autocomplete', 'autocomplete-highlighters', 'datasource-local', 'datasource-arrayschema', 'json', function (Y) {

    var data = [
            {name:"abc",id:123,extra:"foo"},
            {name:"def",id:456,extra:"bar"},
            {name:"ghi",id:789,extra:"baz"}
        ];

    var ds = new Y.DataSource.Local({source:data});

    ds.plug(Y.Plugin.DataSourceArraySchema, {
        schema: {
            resultFields: ["name","id"]
        }
    });


    Y.one('#search_input').plug(Y.Plugin.AutoComplete, {
        resultHighlighter: 'phraseMatch',
        source: ds,
        resultTextLocator: function (result) {
            return result.name + ': ' + result.id;
        }


    });

});
