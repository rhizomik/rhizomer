var minArea = 1;
var other_num = 1;
var rest_num = 1;
var totalArea = 0;
var name;
var colors = d3.scale.category20c();
var colors2 = d3.scale.category20();
var i = 0;
var max = 0;

/*
 var count = 0;
 for(i=0; i<json3.children.length; i++){
 count += json3.children[i].data.$area;
 }
 */

function showStatus(node){
    var html = "<div style=\"font-weight:bold;\">"+node.name + ": " + node.data.instances+" instances</div>";
    html += "<div style=\"font-weight:bold;\"><a href=\"/facets.jsp?q=SELECT ?r1 WHERE{?r1 a <"+node.id+">}\">Browse resources</a> - <a href=\"javascript:backLevel();\">Back to previous level</a></div>";
    $j("#status").html(html);
}

function backLevel(){
    tm.out();
    showStatus(tm.clickedNode);
}

function clearStatus(){
    $j("#status").empty();
}

function countNumMaxInstances(node){
    for(var i=0; i<node.children.length; i++){
        instances = parseInt(node.children[i].data.$area);
        if(instances>max)
            max = instances;
    }
}

function countInstances(node){
    var count=0;
    for(var i=0; i<node.children.length; i++){
        instances = parseInt(node.children[i].data.$area);
        if(instances==0){
            instances = countInstances(node.children[i]);
            node.children[i].data.$area = instances;
            node.children[i].data.instances = instances;
        }
        count+=instances;
    }
    return count;
}

function propagateCount(node){
    var count=0;
    for(var i=0; i<node.children.length; i++){
        instances = parseInt(node.children[i].data.$area);
        if(instances==0){
            instances = propagateCount(node.children[i]);
            node.children[i].data.$area = instances;
            node.children[i].data.instances = instances;
        }
        count+=instances;
    }
    return count;
}

function treatHierarchy(hierarchy){
    hierarchy.data.$color = "#6e6e6e";
    for(var i=0 ;i<hierarchy.children.length; i++){
        treatNode(hierarchy.children[i])
    }
}

function treatNode(node){
    //var new_childs = new Array();
    //var instanceCount = 0;
    //var selected_nodes = new Array();
    var color = generate_color();
    var light_color = d3.lab(color).brighter().brighter();
    var dark_color = d3.lab(color).darker();
    console.log(node.children.length);
    //if(node.children.length>1)
      //  node.data.$color = "#6e6e6e";
    //else
    node.data.$color = color;
    var minSizeChild = getMinSizeChild(node);
    //console.log(node.id + " " + minSizeChild);
    //console.log(node.id + " - " +  node.data.$color);
    var color_scale = d3.scale.linear()
        .domain([minSizeChild,node.data.$area])  // min/max of data
        .range([light_color, dark_color])
        .interpolate(d3.cie.interpolateLch);

    for(var i=0 ;i<node.children.length; i++){
        /*
        if(node.children[i].data.$area > 0){
            if(node.children[i].data.$area < (node.data.$area*minArea/100)){
                instanceCount += node.children[i].data.$area;
                selected_nodes.push(node.children[i]);
            }
            else{
                new_childs.push(node.children[i]);
            }
        */
            treatChildNode(node.children[i], color_scale);
        //}
    }

    /*node.children = new_childs;
    if(selected_nodes.length > 0){
        var other = {data : {'$area':instanceCount, 'instances' : instanceCount, '$color' : generate_color()}, children : [], id : 'other'+other_num, name : 'Other '+name};
        other_num++;
        other.children = selected_nodes;
        node.children.push(other);
    }
    */
}

function getMinSizeChild(node){
    var minNode = node.data.$area;
    for(var i=0 ;i<node.children.length; i++){
        //if(node.children[i].length>0)
        var minChild = getMinSizeChild(node.children[i]);
        //else
        //    var minChild = node.data.$area;
        if(minNode == null)
            minNode = minChild;
        else if(minChild  < minNode)
            minNode = minChild;
    }
    return minNode;
}

function treatChildNode(node, color_scale){
    node.data.$color = color_scale(node.data.$area);
    //console.log(node.id + " - " + node.data.$color);
    for(var i=0 ;i<node.children.length; i++){
        treatChildNode(node.children[i], color_scale);
    }
}

function countChilds(node){
    node.data.num_childs = node.children.length;
    for(var i=0; i<node.children.length; i++)
        countChilds(node.children[i]);
}

function countSubclassesInstances(node){
    var count = 0;
    for(var i=0; i<node.children.length; i++)
        count += parseInt(node.children[i].data.instances);
    if(count != parseInt(node.data.instances) && node.children.length>0){
        var diff = parseInt(node.data.instances) - count;
        var rest = {data : {'$area': diff, 'instances': diff, '$color' : generate_color(), 'num_childs': 0}, children : [], id : 'rest'+rest_num, name : node.name};
        node.children.push(rest);
        rest_num++;
    }
    for(var i=0; i<node.children.length; i++)
        countSubclassesInstances(node.children[i]);
}


function generate_color(){
    i++;
    if(i>=20)
        var color = colors2(i);
    else
        var color = colors(i);
    if(color=="#7f7f7f" || color=="#c7c7c7")
        return generate_color();
    else
        return color;
}
