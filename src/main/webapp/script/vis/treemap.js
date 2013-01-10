var labelType, useGradients, nativeTextSupport, animate;
var tm;

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
        titleHeight: 15,
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
                if(node){ tm.enter(node);
                    showStatus(node);
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
                tm.out();
                //clearStatus();
                showStatus(tm.clickedNode);
                //console.log(tm.clickedNode);
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
                //html += "Subclasses: " + node.children.length;
                /*
                 if(data.image) {
                 html += "<img src=\""+ data.image +"\" class=\"album\" />";
                 }
                 */
                tip.innerHTML =  html;
            }
        },
        //Add the name of the node in the correponding label
        //This method is called once, on label creation.
        onCreateLabel: function(domElement, node){
            domElement.innerHTML = node.name;
            /*
            if(node.id == "root")
                domElement.innerHTML = node.name + " - " + node.data.instances;
            else
                domElement.innerHTML = node.name + "<br/>" + node.data.instances;
            */
            //var label = document.createElement('span');
            //label.innerHTML = node.name;
            //domElement.innerHTML = label;

            /*
             if(node.id != "root"){
             if(tm.clickedNode){
             if(tm.clickedNode.id != node.id)
             domElement.innerHTML += "<br/>" + node.data.playcount;
             }
             else
             domElement.innerHTML += "<br/>" + node.data.playcount;
             }
             */

            var style = domElement.style;
            style.display = '';
            style.color = '#000000';
            /*style.fontSize = '11px';*/

            style.border = '1px solid transparent';
            domElement.onmouseover = function() {
                style.border = '1px solid #9FD4FF';
            };
            domElement.onmouseout = function() {
                style.border = '1px solid transparent';
            };

        },
        /*
         onPlaceLabel: function(domElement, node){
         var style = domElement.style,
         width = node.getData('width'),
         height = node.getData('height');
         //alert(node.id + " " +height);
         style.display = '';
         //style.width = width + 'px';
         style.height = height + 'px';
         }
         */
    });
    tm.loadJSON(json);
    tm.refresh();
}