YUI().use("charts", "json", "node", function (Y) {


    CHARTS = (function () {

        "use strict";

        // The module depends on three global objects.
        // - facetURI
        // - fm
        // - rhz
        // THis is the single entry point for the module and expects rhz object has
        // been initialized (that's why it can't be added to the explicit dependencies
        // of the module).
        // Needs #contentTabs to be present in the Dom tree.
        Y.on("domready", function () {
            createTab();
        });

        // Coordinates the changes on the parameters affecting the chart and tries to
        // minimize calls to the server.
        var mediator = {
            restrictions: undefined,
            restrictionsChanged: false,
            facetURI: undefined,
            facetURIChanged: false,
            formData: {},
            formDataJSON: undefined,
            formDataChanged: true,
            urisToLabels: {},
            setRestrictions: function (newRestrictions) {
                this.restrictionsChanged = this.restrictions !== newRestrictions;
                this.restrictions = newRestrictions;
            },
            setFacetUri: function (newFacetURI) {
                this.facetURIChanged = this.facetURI !== newFacetURI;
                this.facetURI = newFacetURI;
            },
            setFormData: function (newFormData) {
                var newFormDataJSON = Y.JSON.stringify(newFormData);
                this.formDataChanged = this.formDataJSON !== newFormDataJSON;
                this.formData = newFormData;
                this.formDataJSON = newFormDataJSON;
            },
            clearChartIfNeeded: function () {
                if (this.restrictionsChanged) {
                    clearChart();
                }
            },
            loadPropertiesIfNeeded: function () {
                var that = this;
                if (this.facetURIChanged) {
                    loadProperties(function (properties) {
                        that.urisToLabels = {};
                        properties.forEach(function (v) {
                            that.urisToLabels[v.uri] = v.label;
                        });
                    });
                }
            },
            resetFormIfNeeded: function (newFacetURI) {
                this.setFacetUri(newFacetURI);
                this.loadPropertiesIfNeeded();
            },
            doChartIfNeeded: function (newFormData, newRestrictions) {
                this.setRestrictions(newRestrictions);
                this.setFormData(newFormData);
                if (this.restrictionsChanged || this.formDataChanged) {
                    this.resetFlags();
                    queryForValues(function (chartData) {
                        clearChart();
                        showChartYUI3(chartData);
                    });
                }
            },
            resetFlags: function () {
                this.facetURIChanged = false;
                this.restrictionsChanged = false;
                this.formDataChanged = false;
            }
        };

        function loadProperties(callback) {
            rhz.getNumericProperties(mediator.facetURI, function(output) {
                var response = Y.JSON.parse(output);
                setOptions(response.properties);
                callback(response.properties);
            });
        }

        // HTML page manipulations

        function createTab() {
            var tabs = new YAHOO.widget.TabView("contentTabs"),
                chartTab = new YAHOO.widget.Tab({
                    label: "Charts",
                    content: '<div id="chart-form">' +
                             '  <form id="chart-parameters" action="" method="get">' +
                             '    <fieldset>' +
                             '    <legend>Chart configuration</legend>' +
                             '    <label for="property-selector">Variables to plot: </label>' +
                             '    <select id="property-selector" multiple="multiple" name="vars"></select>' +
                             '    <label for="maxResults">Max results: </label>' +
                             '    <input type="text" name="maxResults" value="10"/>' +
                             '    <button type="button" onclick="CHARTS.processForm(this.form);">Make chart</button>' +
                             '    </fieldset>' +
                             '  </form>' +
                             '</div>' +
                             '<div id="chart-viewer"></div>'
                });
            tabs.addTab(chartTab);
            chartTab.addListener("activeChange", function (e) {
                if (e.newValue) {
                    mediator.resetFormIfNeeded(activeURI);
                }
            });
        }

        function setOptions(variables) {
            Y.one("#property-selector").setContent("");
            variables.forEach(function (v) {
                Y.one("#property-selector")
                    .append("<option value=\"" + v.uri + "\">" + v.label + "</option>");
            });
        }

        function clearChart() {
            Y.one("#chart-viewer").setContent("");
        }

        // SPARQL query for values

        function makeSelectQueryPart() {
            return "?r1 ?label " + mediator.formData.properties.map(function (p, i) {
                return "?value" + i;
            }).join(" ");
        }

        function makeValuesQueryPart() {
            return mediator.formData.properties.map(function (p, i) {
                return " ?r1 <" + p + "> ?value" + i + " .";
            }).join("");
        }

        function makeFilterValuesQueryPart() {
            return " FILTER(" +
                mediator.formData.properties.map(function (p, i) {
                    return "?value" + i + "!=\"\" && !isBlank(?value" + i + ")";
                }).join(" && ") +
                ") . ";
        }

        function makeTranslatorValuesToProperties() {
            var translator = {};
            mediator.formData.properties.forEach(function (p, i) {
                translator["value" + i] = mediator.urisToLabels[p];
            });
            return translator;
        }

        function makeQuery() {
            var variables = makeSelectQueryPart(),
                values = makeValuesQueryPart(),
                filterBlanks = makeFilterValuesQueryPart();
            return " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                " SELECT " + variables +
                " WHERE {?r1 a <" + mediator.facetURI + "> . " +
                         values + filterBlanks +
                "        OPTIONAL{ ?r1 rdfs:label ?label " +
                "                  FILTER(LANG(?label)='en' || LANG(?label)='')} ." +
                         mediator.restrictions +
                " } ORDER BY ?label LIMIT " + mediator.formData.maxResults;
        }

        function lastPartOfURI(uri) {
            var regex = /[#\/]([^#\/]*)$/,
                match = regex.exec(uri);
            return match ? match[1] : uri;
        }

        function transformToObject(obj, keys, translation) {
            var item = {}, label, uri;
            keys.forEach(function (key) {
                if (obj[key] != undefined) {
                    if (key === "label") {
                        label = obj[key].value;
                    } else if (key === "r1") {
                        uri = obj[key].value;
                    } else {
                        item[translation[key]] = obj[key].value;
                    }
                }
            });
            item.label = label || lastPartOfURI(uri);
            return item;
        }

        function queryForValues(callback) {
            var query = makeQuery(mediator);
            rhz.sparqlJSON(query, function (output) {
                var data = Y.JSON.parse(output),
                    translator = makeTranslatorValuesToProperties(mediator.formData.properties),
                    chartData = data.results.bindings.map(function (obj) {
                        return transformToObject(obj, data.head.vars, translator);
                    });
                callback(chartData);
            });
        }

        // YUI3 Charts

        function showChartYUI3(chartData) {
            var myTooltip = {
                    styles: {
                        backgroundColor: "#333",
                        color: "#eee",
                        borderColor: "#fff",
                        textAlign: "center"
                    },
                    markerLabelFunction: function (categoryItem, valueItem, itemIndex, series, seriesIndex) {
                        return categoryItem.value + '<br>' + valueItem.displayName + ': ' + valueItem.value;
                    }
                },
                mychart = new Y.Chart({
                    dataProvider: chartData,
                    type: "column",
                    categoryKey: "label",
                    render: "#chart-viewer",
                    tooltip: myTooltip
                });
        }

        return {
            // The only method the module exports is the function to process the
            // form.
            processForm: function (form) {
                var options = Array.prototype.slice.call(form.vars.options),
                    selected = options.filter(function (o) {
                        return o.selected;
                    }).map(function (o) {
                        return o.value;
                    });
                mediator.doChartIfNeeded({
                    properties: selected,
                    maxResults: form.maxResults.value
                }, facetBrowser.makeRestrictions());
            }
        };

    }());
});
