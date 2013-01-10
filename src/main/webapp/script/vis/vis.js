var minArea = 1;
var other_num = 1;
var rest_num = 1;
var totalArea = 0;
var name;
var colors = d3.scale.category20();
var i = 0;
var max = 0;

/*
 var count = 0;
 for(i=0; i<json3.children.length; i++){
 count += json3.children[i].data.$area;
 }
 */

function showStatus(node){
    console.log(node);
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

function treatNode(node){
    var new_childs = new Array();
    var instanceCount = 0;
    var selected_nodes = new Array();
    for(var i=0 ;i<node.children.length; i++){
        if(node.children[i].data.$area > 0){
            if(node.children[i].data.$area < (node.data.$area*minArea/100)){
                instanceCount += node.children[i].data.$area;
                selected_nodes.push(node.children[i]);
            }
            else{
                new_childs.push(node.children[i]);
            }
            treatNode(node.children[i]);
        }
    }
    node.data.$color = generate_color();

    node.children = new_childs;
    if(selected_nodes.length > 0){
        var other = {data : {'$area':instanceCount, 'instances' : instanceCount, '$color' : generate_color()}, children : [], id : 'other'+other_num, name : 'Other '+name};
        other_num++;
        other.children = selected_nodes;
        node.children.push(other);
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
    return colors(i);
    //return Math.floor(Math.random()*16777215).toString(16);
    //return hsv_to_rgb(Math.random(), 0.5, 0.95);
}
