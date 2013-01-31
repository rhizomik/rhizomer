var labelType, useGradients, nativeTextSupport, animate;
var tm;
var clickedNode = null;
var treemapHistory = new Array();

(function() {
    var ua = navigator.userAgent,
        iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
        typeOfCanvas = typeof HTMLCanvasElement,
        nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
        textSupport = nativeCanvasSupport
            && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
    //I'm setting this based on the fact that ExCanvas provides text support for IE
    //and that as of today iPhone/iPad current text support is lame
    labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
    nativeTextSupport = labelType == 'Native';
    useGradients = nativeCanvasSupport;
    animate = !(iStuff || !nativeCanvasSupport);
})();

function initTreeMap(json){
//console.log("init TreeMap");
//console.log(json);
    //end
    //init TreeMap
    tm = new $jit.TM.Squarified({
        //where to inject the visualization
        injectInto: 'vis',
        //parent box title heights
        titleHeight: 20,
        //enable animations
        animate: false,
        constrained: false,
        levelsToShow: 2,
        //box offsets
        offset: 2,
        Label: {
            type: 'HTML',
            //size: 9,
            textBaseline: 'alphabetic',
            //family: 'Tahoma, Verdana, Arial'
        },
        Node: {
            CanvasStyles: {
                shadowBlur: 0,
                shadowColor: '#333333'
            }
        },
        //Attach left and right click events
        Events: {
            enable: true,
            onClick: function(node) {
                    /*
                    if(node.data.num_childs>0){
                        tm.enter(node);
                    }
                    else{
                        alert("Navigate to "+node.name);
                    }
                    */
                if(node){
                    if(tm.clickedNode == null || tm.clickedNode.id == "root"){
                        if(node.data.parent == "root"){
                            treemapHistory.push(node.name);
                        }
                        else if(node.data.parent == treemapHistory[treemapHistory.length-1]){
                            treemapHistory.push(node.name);
                        }
                        else{
                            treemapHistory.push(node.data.parent);
                            treemapHistory.push(node.name);
                        }
                    }
                    else{
                        if(node.data.parent == treemapHistory[treemapHistory.length-1]){
                            treemapHistory.push(node.name);
                        }
                        else{
                            treemapHistory.push(node.data.parent);
                            treemapHistory.push(node.name);
                        }
                    }

                    console.log(treemapHistory);
                    //console.log(tm.clickedNode);
                    /*
                    if(clickedNode==null)
                            ;
                    else if(node.parent == clickedNode.id)
                        alert("igual");
                    else
                        alert("not parent");
                    */
                    //console.log(node);
                    tm.enter(node);
                    clickedNode = node;
                    showStatus();
                    tm.refresh();
                }
                /*
                if(tm.leaf(node)){
                    console.log(node);
                    showStatus(node);
                }
                */
                //v.listResources(node.id);

            },
            onRightClick: function(node) {
                treemapHistory.pop();
                tm.out();
                tm.refresh();
                console.log(treemapHistory);
                showStatus();
            },
        },
        duration: 1000,
        //Enable tips
        Tips: {
            enable: true,
            //add positioning offsets
            offsetX: 20,
            offsetY: 20,
            //implement the onShow method to
            //add content to the tooltip when a node
            //is hovered
            onShow: function(tip, node, isLeaf, domElement) {
                var html = "<div style=\"font-weight:bold;\" class=\"tip-title\">" + node.name
                    + "</div><div class=\"tip-text\">";
                var data = node.data;
                if(data.instances) {
                    html += "Number of instances: " + data.instances;
                    html += "<br/>Subclasses: " + data.num_childs;
                }
                tip.innerHTML =  html;
            }
        },
        //Add the name of the node in the correponding label
        //This method is called once, on label creation.
        onCreateLabel: function(domElement, node){
            var style = domElement.style;
            style.display = '';
            style.color = '#000000';
            /*style.fontSize = '11px';*/

            style.border = '2px solid transparent';
            domElement.onmouseover = function() {
                style.border = '2px solid #ffffff';
            };
            domElement.onmouseout = function() {
                style.border = '2px solid transparent';
            };
        },
        onPlaceLabel: function (domElement, node) {
            if(node.id == "root" || (tm.clickedNode!=null && tm.clickedNode.id == node.id)){
                //domElement.style.backgroundColor="white";
                //domElement.style.height="15px";
                //domElement.style.display="none";
                /*domElement.innerHTML = "<div class=\"root\">"+node.name+"</div>";*/
                domElement.innerHTML = "";
            }
            else if(!tm.leaf(node)){
                var pt = (Math.round(node.data.$width/node.name.length));
                if(pt<6)
                    pt = 0;
                else if(pt>12)
                    pt = 12;

                domElement.innerHTML = "<div style=\"font-size:"+pt+"pt\" class=\"top\">"+node.name+"</div>";
                //else
                  //  domElement.innerHTML = "<div class=\"top\">"+node.name+"</div>";
            }

            else{
                if(node.data.$width == 0 || isNaN(node.data.$width))
                    var pt = 0;
                else{
                    var pt = (Math.round(node.data.$width/node.name.length));
                    var vpt = (Math.round(node.data.$height/node.name.length));
                    pt = Math.min(pt, vpt);

                    if(pt<6)
                        pt = 0;
                }
                domElement.innerHTML = "<div class=\"middle\" style=\"font-size:"+pt+"pt\">"+node.name+"</div>";
            }
        },
    });
    tm.loadJSON(json);
    tm.refresh();
    showStatus();
}