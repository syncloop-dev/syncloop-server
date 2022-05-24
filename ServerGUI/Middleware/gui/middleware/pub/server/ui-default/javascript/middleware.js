function getUrlParam(name) {
    var results = new RegExp('[\?&]' + name + '=([^&#]*)')
                      .exec(window.location.search);

    return (results !== null) ? results[1] || 0 : false;
}
function syncRestRequest(url, method, payload, contentType, dataType){
	var response = {};
	var status=200;
	if(contentType==null)
		contentType="application/json";
	if(dataType==null)
		dataType="application/json";
	$.ajax({
        url: url, // url where to submit the request
        type : method, // type of action POST || GET
        contentType: contentType,
        dataType : dataType, // data type
        data : payload, // post data || get data
        async: false,
        success : function(result) {
        	
            // you can see the result from the console
            // tab of the developer tools
        	response=result;
        	//alert(result);
            console.log(result);
        },
        error: function(errormessage) {
        	status=errormessage.status;
        	if(errormessage.status==200)
        		response= errormessage.responseText;
        	else{
        	console.log(errormessage.status);
            response= errormessage.responseText;
        	//alert(errormessage.responseText);
        	}
        }
    })
    return {"status":status,"payload":response};
}
function removeIcons(jsonObject){
jQuery.each(jsonObject, function(i, val) {
  				if(val.icon){
                  val.icon=null;
                }
  				if(val.children){
                	removeIcons(val.children);
                }
		  });
}
function asyncRestRequest(url, payload, method, callBack){
	$.ajax({
        url: url, // url where to submit the request
        type : method, // type of action POST || GET
        contentType: 'application/json',
        dataType : 'json', // data type
        data : payload, // post data || get data
        success : function(result) {
        	callBack(result);
            console.log(result);
        },
        error: function(errormessage) {
        	alert(errormessage.responseText);
        }
    })
}

function deleteAllCookies() {
    var cookies = document.cookie.split(";");

    for (var i = 0; i < cookies.length; i++) {
        var cookie = cookies[i];
        var eqPos = cookie.indexOf("=");
        var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;
        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT";
    }
}

function getJstreeFromSchema(jsonSchema){
	
						var datapipeline=JSON.parse(jsonSchema).properties;
						var data=toJson(datapipeline,null);
						var jsonJstreeObj=data[0];
						var jsonObj=data[1];
						//console.log("JSTREE******************************");
						//console.log(JSON.stringify(jsonJstreeObj));
						//console.log("JSON Payload******************************");
						//console.log(jsonObj);
						return jsonJstreeObj;
					}
					
function toJson(jsonSchemaObj,isArray){
						var jsonJstreeObj=[];
						var jsonObj={};
						if(isArray==true){
							jsonJstreeObj=[];
							
						}
                        var mapperThis=this;
						//console.log("Schema ******************************");
						//console.log(jsonSchemaObj);
						$.each(jsonSchemaObj, function(propName, propVal) {
							//console.log(propName, propVal);
							var jjtOBJ={};
							var jobj={};
							  jjtOBJ.text=propName;
							  jjtOBJ.children={};
							  if(propVal.type=="object"){
								  jjtOBJ.type="document";
							  	let jsonOA=toJson(propVal.properties);
							  	jjtOBJ.children=(jsonOA[0]);
							  	jsonObj[propName]=jsonOA[1];
							  }
							  else if(propVal.type=="array" && (propVal.items.type=="object" || propVal.items.type=="array")){
								  jjtOBJ.type="documentList";
								let jsonOA=mapperThis.toJson(propVal.items.properties,null);
								jjtOBJ.children=(jsonOA[0]);
								jsonObj[propName]=jsonOA[1];
							  }
							  else{// pending to add condition when its value array
								  jjtOBJ.type=propVal.type;
							  	if(propVal.type=="array"){
							  		jsonObj[propName]=[];
                                    if(!propVal.items.type)
                                      propVal.items.type="string"
							  		jjtOBJ.type=propVal.items.type+"List";
							  		switch(propVal.items.type){
							  			case "string":
										  jsonObj[propName].push("");
										  break;
							  			case "integer":
										  jsonObj[propName].push(0);
										  break;
							  			case "number":
											  jsonObj[propName].push(0.0);
											  break;
										default:
										  jsonObj[propName].push("");
							  		}
							  	}else{
							  		switch(propVal.type){
								  		case "string":  
									  		jsonObj[propName] = "";
									  		break;
								  		case "integer":
										  	jsonObj[propName]= 0;
										  	break;
								  		case "number":
										  	jsonObj[propName]= 0.0;
										  	break;
							  		}
							  	}
								}
								jsonJstreeObj.push(jjtOBJ); 
						});
						return [jsonJstreeObj,jsonObj];
					}

function copyToClipboard(textToCopy) {
    // navigator clipboard api needs a secure context (https)
    if (navigator.clipboard && window.isSecureContext) {
        // navigator clipboard api method'
        return navigator.clipboard.writeText(textToCopy);
    } else {
        // text area method
        let textArea = document.createElement("textarea");
        textArea.value = textToCopy;
        // make the textarea out of viewport
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        textArea.style.top = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        return new Promise((res, rej) => {
            // here the magic happens
            document.execCommand('copy') ? res() : rej();
            textArea.remove();
        });
    }
}
function IOSchemaMenu(node, id)
{
  var tree = $(id).jstree(true);
    var items = {
        renameItem: {
            label: "Rename",
            action: function (e) {
              tree.edit(node); 
            }
        },
        deleteItem: {
            label: "Delete",
            action: function (e) { 
            	tree.delete_node(node);
            },
            "separator_after": true
        },
        copyItem: {
            label: "Copy",
            action: function (e) { 
              $(node).addClass("copy");
              tree.copy(node) 
            }
        },
        cutItem: {
            label: "Cut",
            action: function (e) { 
              $(node).addClass("cut");
              tree.cut(node);
            }
        },
        pasteItem: {
            label: "Paste",
            action: function (e) { 
              $(node).addClass("paste");
              tree.paste(node); 
            }
        },
        copyXPath: {
            label: "copyXPath",
            action: function (e) { 
              var nodePath=tree.get_path(node, '/');
              copyToClipboard(nodePath);
            }
        },
        properties:{
          	"seperator_before": false,
            "seperator_after": false,
            "label": "Properties",
            action: function (node) {
              console.log(node);
                  	openForm(id,node);
          }}
    };
      
      if (node.type === 'document' || node.type === 'documentList') {
        items.createItem={
            label: "New",
            action: false,
          	"submenu": {
                                    "Document": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Document",
                                        action: function (node) {
                                          	createSchema("document",tree);
                                        }
                                    },
                                    "String": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "String",
                                        action: function (node) {
                                          createSchema("string",tree);
                                        }
                                    },
              						"Integer": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Integer",
                                        action: function (node) {
                                          createSchema("integer",tree);
                                        }
                                    },
              						"Number": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Number",
                                        action: function (node) {
                                          createSchema("number",tree);
                                        }
                                    },
              						"Date": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Date",
                                        action: function (node) {
                                          createSchema("date",tree);
                                        }
                                    },
              						"Boolean": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Boolean",
                                        action: function (node) {
                                          createSchema("boolean",tree);
                                        }
                                    },
              						"Byte": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Byte",
                                        action: function (node) {
                                          createSchema("byte",tree);
                                        }
                                    },
              						"Object": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Object",
                                        action: function (node) {
                                          createSchema("javaObject",tree);
                                        }
                                    }
                                }
          //function (node) { return { createItem: this.create(node) }; }
        };
    } 
  return items;
}

function createSchemaJstree(id) {
	var to = false;
	$('search_q').keyup(function() {
		if (to) {
			clearTimeout(to);
		}
		to = setTimeout(function() {
			var v = $('search_q').val();
			$(id).jstree(true).search(v);
		}, 250);
	});
	
	var ref=$(id)
	.jstree(
			{
				"core" : {
					"animation" : 0,
					"check_callback" : true,
					'force_text' : true,
					"themes" : {
						"stripes" : true,
						"responsive": false,
						"dots" : true
					},
					
					'data' : []
				},
				"types" : {
					"default" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/doc.png",
						"valid_children" : [
							"default","document", "string","documentList","stringList","javaObjectList","javaObject","integer","integerList","number","numberList","date","dateList","boolean","booleanList","byte","byteList"]
					},
					"document" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/doc.png",
						"valid_children" : [
							"default","document", "string","documentList","stringList","javaObjectList","javaObject","integer","integerList","number","numberList","date","dateList","boolean","booleanList","byte","byteList"]
					},"documentList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/docList.png",
						"valid_children" : [
							"default","document", "string","documentList","stringList","javaObjectList","javaObject","integer","integerList","number","numberList","date","dateList","boolean","booleanList","byte","byteList"]
					},
					"string" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/text.png",
						"valid_children" : []
					},"stringList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/textArr.png",
						"valid_children" : []
					},
					"javaObject" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/javaObject.png",
						"valid_children" : []
					},"javaObjectList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/javaObjectArr.png",
						"valid_children" : []
					},
					"integer" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/integer.png",
						"valid_children" : []
					},"integerList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/integerArr.png",
						"valid_children" : []
					},"number" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/number.png",
						"valid_children" : []
					},"numberList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/numberArr.png",
						"valid_children" : []
					},"date" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/date.png",
						"valid_children" : []
					},"dateList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/dateArr.png",
						"valid_children" : []
					},"boolean" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/boolean.png",
						"valid_children" : []
					},"booleanList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/booleanArr.png",
						"valid_children" : []
					},"byte" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/byte.png",
						"valid_children" : []
					},"byteList" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/byteArr.png",
						"valid_children" : []
					}
				},
				"plugins" : [ "unique",
						"contextmenu", "dnd",
						"search", "state", "types","wholerow"
						 ],
                "contextmenu": {"items":function(node){return IOSchemaMenu(node,id);}}
			}).on('open_node.jstree',
						function(e, data) {	//console.log(data.node.id);
                }).on('close_node.jstree',
						function(e, data) {
				}).on('click',
						function(e, data) {
							//console.log($(id).jstree(true));
							//selectMapping(id,e.target);
							currentSelectedSchemaJStreeID=id;
						});
 return $(id).jstree(true);
}


function flowDesignerMenu(node, id)
{
  var tree = $(id).jstree(true);
    var items = {
        deleteItem: {
            label: "Delete",
            action: function (e) { 
              	//var nd=tree.get_node(node.id);
            	tree.delete_node(node);
            },
            "separator_after": true
        },
        copyItem: {
            label: "Copy",
            action: function (e) { 
              $(node).addClass("copy");
              tree.copy(node) 
            }
        },
        cutItem: {
            label: "Cut",
            action: function (e) { 
              $(node).addClass("cut");
              tree.cut(node);
            }
        },
        pasteItem: {
            label: "Paste",
            action: function (e) { 
              $(node).addClass("paste");
              tree.paste(node); 
            }
        }
    };
  
  if(node.type === 'invoke'){
    items.select= {
            label: "Select",
            action: function(node){
              openSelectServiceModalDialog(id,node);
            } 
    };
  }else if(node.type === 'switch'){
        	items.createItem={
            label: "Add",
            action: false,
          	"submenu": {
                                    "Sequence": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "CASE",
                                        action: function (node) {
                                            createSchema("sequence",tree);
                                        }
                                    }
            }};
      }else if (!(node.type === 'map' || node.type === 'invoke' || node.type === 'try-catch')) {
        items.createItem={
            label: "Add",
            action: false,
          	"submenu": {
                                    "Sequence": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Sequence",
                                        action: function (node) {
                                            createSchema("sequence",tree);
                                        }
                                    },
                                    "Switch": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Switch",
                                        action: function (node) {
                                            createSchema("switch",tree);
                                        }
                                    },
              						"TCF-Block": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "TCF-Block",
                                        action: function (node) {
                                          	createSchema("try-catch",tree);
                                        }
                                    },
              						"Loop": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Loop",
                                        action: function (node) {
                                          	createSchema("loop",tree);
                                        }
                                    },
              						"Repeat": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Repeat",
                                        action: function (node) {
                                          	createSchema("repeat",tree);
                                        }
                                    },
              						"Map": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Map",
                                        action: function (node) {
                                          	createSchema("map",tree);
                                        }
                                    },
              						"Invoke": {
                                        "seperator_before": false,
                                        "seperator_after": false,
                                        "label": "Invoke",
                                        action: function (node) {
                                          	createSchema("invoke",tree);
                                        }
                                    }
                                }
          //function (node) { return { createItem: this.create(node) }; }
        };
    } 
  
  items.properties={
          	"seperator_before": false,
            "seperator_after": false,
            "label": "Properties",
            action: function (node) {
              console.log(node);
                  	openFlowElementProperties(id,node);
          }};
  return items;
}



function createFlowJstree(id) {
	var to = false;
	$('search_q').keyup(function() {
		if (to) {
			clearTimeout(to);
		}
		to = setTimeout(function() {
			var v = $('search_q').val();
			$(id).jstree(true).search(v);
		}, 250);
	});
	
	var ref=$(id)
	.jstree(
			{
				"core" : {
					"animation" : 0,
					"check_callback" : true,
					'force_text' : true,
					"themes" : {
						"stripes" : true,
						"responsive": false,
						"dots" : true
					},
					
					'data' : []
				},
				"types" : {
					"default" : {
						"icon" : "/files/gui/middleware/server/ui/icons/sequence.png",
						"valid_children" : [
							"default","sequence", "switch","loop","repeat","try-catch","map","invoke"]
					},
                    "sequence" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/sequence.png",
						"valid_children" : [
							"default","sequence", "switch","loop","repeat","try-catch","map","invoke"]
					},
                    "switch" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/switch.png",
						"valid_children" : [
							"default","sequence", "switch","loop","repeat","try-catch","map","invoke"]
					},
                    "loop" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/loop.png",
						"valid_children" : [
							"default","sequence", "switch","loop","repeat","try-catch","map","invoke"]
					},
                    "repeat" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/repeat.png",
						"valid_children" : [
							"default","sequence", "switch","loop","repeat","try-catch","map","invoke"]
					},
                    "try-catch" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/try-catch.png",
						"valid_children" : [
							"default","sequence", "switch","loop","repeat","try-catch","map","invoke"]
					},
                    "map" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/map.png",
						"valid_children" : []
					},
                    "invoke" : {
						"icon" : "/files/gui/middleware/pub/server/ui-default/icons/invoke.png",
						"valid_children" : []
					}
				},
				"plugins" : [
						"contextmenu", "dnd",
						"search", "state", "types","wholerow"
						 ],
                "contextmenu": {"items":function(node){return flowDesignerMenu(node,id);}}
			}).on('open_node.jstree',
						function(e, data) {	//console.log(data.node.id);
                }).on('close_node.jstree',
						function(e, data) {
				}).on('click',
						function(e, data) {
							//console.log($(id).jstree(true));
							//selectMapping(id,e.target);
							//alert(id);
							currentSelectedFlowDesignerJStreeID=id;
						});
 return $(id).jstree(true);
}

function deleteArtifact(filePath){
  var references=syncRestRequest("/execute/packages.middleware.pub.service.findReferences.main?serviceFqn="+(filePath).split(".")[0],"GET");
  var list=JSON.parse(references.payload).list;
  if (confirm("Service references:-\n\n"+JSON.stringify(list)+"\n\nAre you sure you want to delete ('"+filePath+"') ?")) {
    filePath="files/"+filePath;
    var response=syncRestRequest("/"+filePath, "DELETE");
    if(response.status==200){
       localStorage.setItem(filePath, "");
       var error=JSON.parse(response.payload).error;
       if(error){
       	alert(error);
       }else
       	location.reload();
    }else
       alert(JSON.parse(response.payload).error);
    
  }
}

function packagesContextMenu(node, id)
{
  var tree = $(id).jstree(true);
  var sel = tree.get_selected()[0];
  var dest=tree.get_path(sel, '/');
    var items = {};
  if(node.type!="properties"){
  items.renameItem={
            label: "Rename",
            action: function (e) { 
            	var sel = tree.get_selected();
              	tree.edit(sel);
            }
        };
  
  }
  
  
        items.deleteItem= {
            label: "Delete",
            action: function (e) { deleteArtifact(dest+"."+node.type); },
            "separator_after": true
        };
  
      if(dest.startsWith("gui")){
          if (node.type === 'gui-app'){
            items.export= {
                label: "Export",
                action: function (node) { $(node).addClass("copy"); return { copyItem: tree.copy(node) }; }
            }
                  items.import={
                                "seperator_before": false,
				                "seperator_after": false,
                                "label": "Import",
                                action: function (node) {
                                 var url='/upload/zip?dest='+dest;
                                 uploadFile(url,"file",".zip");
                                }
        		}
      	}else
        if (node.type === 'folder'){
            items.export= {
                label: "Export",
                action: function (node) {
                  	var url='/upload/jar?dest='+dest;
                    uploadFile(url,"file",".jar");
                }
            }
          items.import= {
                label: "Import",
                action: function (node) {
                  	var url='/upload/jar?dest='+dest;
                    uploadFile(url,"file",".html,.js,.css,images/*");
                }
            }
        }
      }else if (node.type === 'package') {
        items.export= {
            label: "Export",
            action: function (node) { $(node).addClass("copy"); return { copyItem: tree.copy(node) }; }
        }
          items.import={
              label: "Import",
              action: false,
              submenu: {}
            //function (node) { return { createItem: this.create(node) }; }
          };
              items.import.submenu.jar={
                                          "seperator_before": false,
                                          "seperator_after": false,
                                          "label": "Jar File",
                                           action: function (node) {
                                            	var url='/upload/jar?dest='+dest;
                                                uploadFile(url,"file",".jar");
                                           }
                                      };
        	  items.import.submenu.service={
                                          "seperator_before": false,
                                          "seperator_after": false,
                                          "label": "Service",
                                           action: function (node) {
                                            	var url='/upload/service?dest='+dest;
                                                uploadFile(url,"file",".service");
                                           }
                                       };
      }else if (node.type === 'root') {
        items.export= {
            label: "Export",
            action: function (node) { 
              var cid=sel;
              console.log(sel);
              var elemId=document.getElementById(cid+"_checkbox");
              if(elemId==null)
                alert("Please select Tools>Build first");
              else{
                //var includeAllDependencies=confirm("Do you want to export the build with all the dependencies?");
                //exportBuild('includeDependencies','includeGlobalProperties','includeLocalProperties','includeEndpoint','buildNameInput')
               	var includeDependencies=$("#includeDependencies").prop("checked");//prompt("Please create your build name:", "myBuild");
                if(includeDependencies)
                  includeDependencies=true;
                else
                  includeDependencies=false;
                
                var includeGlobalProperties=$("#includeGlobalProperties").prop("checked");
                if(includeGlobalProperties)
                  includeGlobalProperties=true;
                else
                  includeGlobalProperties=false;
                
                var includeLocalProperties=$("#includeLocalProperties").prop("checked");
                if(includeLocalProperties)
                  includeLocalProperties=true;
                else
                  includeLocalProperties=false;
                
                var includeEndpoint=$("#includeEndpoint").prop("checked");
                if(includeEndpoint)
                  includeEndpoint=true;
                else
                  includeEndpoint=false;
                
                var buildName=$("#buildNameInput").val();
                if(buildName!=null && buildName.trim().length>0){
                  	var data =tree.get_json('#', {'flat': true});
                    var selected=[];
                    var counter=0;
                    for(var index in data) {
                        var map=data[index];
                        var elemNode=$("#"+map.id);
                        var elemChecked=elemNode.attr('checked');
                        
                        //alert(JSON.stringify(map)+" : "+elemChecked);
                      	if(elemChecked){
                          //alert(JSON.stringify(map));
                          var elemTreeNode=tree.get_node(map.id);
                          //alert(elemTreeNode);
                          var nodePath=tree.get_path(elemTreeNode, '/');
                          //alert(nodePath);
                          var artifact={"type":"","asset":""};
                          artifact.type=map.type;
                          artifact.asset=nodePath;
                          selected.push(artifact);
                          //selected[counter++].nodePath;
                        }
					} 
                  	var content=JSON.stringify(selected);
                   // alert(content);
                    var qp="buildName="+buildName+"&includeDependencies="+includeDependencies+"&includeGlobalProperties="+includeGlobalProperties
                    +"&includeLocalProperties="+includeLocalProperties+"&includeEndpoints="+includeEndpoint;
                	var response=syncRestRequest("/build?"+qp, "POST", content,"application/json","application/json");
                    if(response.status==200){
                        var jObj=JSON.parse(response.payload);
                        if(jObj.msg=="Success"){
                          alert(jObj.msg);
                          var element = document.createElement('a');
                          element.setAttribute('href',JSON.parse(response.payload).url);
                          element.setAttribute('target', "_blank");
                          document.body.appendChild(element);
                          element.click();
                        }else
                          alert(jObj.msg);
                    }
                }
              }
              //window.open('http://192.168.2.133:8182/files/gui/middleware/pub/server/ui-default/workspace/web/export.html','popUpWindow','height=720,width=400,left=100,top=100,location=0,directories=0,resizable=yes,scrollbars=yes,toolbar=no,menubar=no,location=no,directories=no, status=no'); 
            }
        }
        	  items.import={
                                          "label": "Import",
                                           action: function (node) {
                                            	var url='/upload/packages?dest='+dest;
                                                uploadFile(url,"file",".zip");
                                           }
                                       };
      }else if (node.type === 'folder'){
            items.export= {
                label: "Export",
                action: function (node) {
                  	var url='/upload/jar?dest='+dest;
                    uploadFile(url,"file",".jar");
                }
            }
          items.import= {
                label: "Import",
                action: function (node) {
                  	var url='/upload/jar?dest='+dest;
                    uploadFile(url,"file",".service,.map,.flow");
                }
            }
        }
        
  return items;
}

function uploadFile(url,key,commaSepFileExts){
 //alert(src);
  if(key==null)
    key="file";
  if(commaSepFileExts==null)
    commaSepFileExts="all";
 var fileUploadElement='<input type="file" id="middlewareFile" name="file" style="display:none" accept="'+commaSepFileExts+'"/>';
  var file=document.getElementById("middlewareFile");
  //alert(file);
  if(file==null){
  	$("body").append(fileUploadElement);
  }
  $("#middlewareFile").change(function(){
  var fd = new FormData();
                var files = $('#middlewareFile')[0].files[0];
                fd.append(key, files);
       
                $.ajax({
                    url: url,
                    type: 'post',
                    data: fd,
                    contentType: false,
                    processData: false,
                    success: function(response){
                        if(response != 0){
                           alert(JSON.stringify(response));
                        }
                        else{
                            alert(JSON.stringify(response));
                        }
                      $("#middlewareFile").remove();
                    },
                });
  });
  document.getElementById("middlewareFile").click();
}

function createPackageJstree(id) {
						var to = false;
						$(id + '_q').keyup(function() {
							if (to) {
								clearTimeout(to);
							}
							to = setTimeout(function() {
								var v = $(id + '_q').val();
								$(id).jstree(true).search(v);
							}, 250);
						});
						
						var ref=$(id)
								.jstree(
										{
											"core" : {
												"animation" : 0,
												"check_callback" : true,
												'force_text' : true,
												"themes" : {
													"stripes" : true,
													"responsive": false,
													"dots" : true
												},
												
												    'data' : []
											},
											"types" : {
                                              "#" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/filesystem/unknown.png",
													"valid_children" : [
														"root","ui-root"]
												},
                                                "default" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/filesystem/unknown.png",
													"valid_children" : [
														"package","gui-app"]
												},
												"root" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/myPackage.png",
													"valid_children" : [
														"package"]
												},"ui-root" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/ui.png",
													"valid_children" : [
														"gui-app"]
												},"package" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/myPackages.png",
													"valid_children" : [
														"folder"]
												},"gui-app" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/gui-app.png",
													"valid_children" : [
														"folder"]
												},
												"folder" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/folder.png",
													"valid_children" : [
														"folder", "service","package","flow","map","html","js","css","jar","jdbc","sql","properties"]
												},"service" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/filesystem/cog.png",
													"valid_children" : []
												},"map" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/filesystem/arrow_switch.png",
													"valid_children" : []
												},"doc" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/doc.png",
													"valid_children" : []
												},"properties" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/properties.png",
													"valid_children" : []
												},"html" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/html.png",
													"valid_children" : []
												},"js" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/js.png",
													"valid_children" : []
												},"css" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/css.png",
													"valid_children" : []
												},"flow" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/flow.png",
													"valid_children" : []
												},"jar" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/jar.png",
													"valid_children" : []
												},"jdbc" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/jdbc.png",
													"valid_children" : []
												},"sql" : {
													"icon" : "/files/gui/middleware/pub/server/ui-default/icons/sql.png",
													"valid_children" : []
												}
											},
											"plugins" : [ "unique","contextmenu",
													"search", "state", "types","wholerow"
													 ],
                "contextmenu": {"items":function(node){return packagesContextMenu(node,id);}}
										});
return $(id).jstree(true);					
}


function toggleJSTreeCheckBox(elemId,jsTreeID,checked){
  var jstreeRef=$(jsTreeID).jstree(true);
  var id=elemId.replace("_checkbox","");
  var node=$("#"+id);
  $("#"+elemId).remove();
  var nodeAnchor=$("#"+id+"_anchor");
  var jsTreeNode = jstreeRef.get_node(id);
//  console.log(jsTreeNode);
//  console.log(jsTreeNode.parent);
  var checkedBox="<span id='"+id+"_checkbox' class='jstree-anchor'><input type='checkbox' checked onclick=toggleJSTreeCheckBox('"+id+"_checkbox','"+jsTreeID+"') /></span>" ;
  var uncheckedBox="<span id='"+id+"_checkbox' class='jstree-anchor'><input type='checkbox' onclick=toggleJSTreeCheckBox('"+id+"_checkbox','"+jsTreeID+"') /></span>" ;
  var elemChecked=node.attr('checked');
  if(checked!=null){
    elemChecked=checked;
    //alert(elemChecked);
  }
  if(elemChecked){
    for(var i=0;i<jsTreeNode.children_d.length;i++){
    	toggleJSTreeCheckBox(jsTreeNode.children_d[i]+"_checkbox",jsTreeID,true);}
    //for(var i=0;i<jsTreeNode.parents.length;i++)
    //	toggleJSTreeCheckBox(jsTreeNode.parents+"_checkbox",jsTreeID,true);
  	node.attr('checked',false);
    nodeAnchor.before(uncheckedBox);
  }else{
    for(var i=0;i<jsTreeNode.children_d.length;i++){
    	toggleJSTreeCheckBox(jsTreeNode.children_d[i]+"_checkbox",jsTreeID,false);}
    //for(var i=0;i<jsTreeNode.parents.length;i++)
    //	toggleJSTreeCheckBox(jsTreeNode.parents+"_checkbox",jsTreeID);
    node.attr('checked',true);
    nodeAnchor.before(checkedBox);
  }
  
 }
function addCheckBoxOnJSTree(jsTreeID) {
        //$("#"+data.node.id).prop('title', data.node.text);
  		//alert(e.target.id);
  $(jsTreeID).jstree("open_all");
  var jstreeRef=$(jsTreeID).jstree(true);
  var v =jstreeRef.get_json('#', {'flat': true});
for (i = 0; i < v.length && i < v.length; i++) {
      var cid = v[i].id;
      var node=$("#"+cid);
      var nodeAnchor=$("#"+cid+"_anchor");
  	  var elemChecked=node.attr('checked');
      var elemId=document.getElementById(cid+"_checkbox");
  	  if(elemId==null)
      		nodeAnchor.before("<span id='"+cid+"_checkbox' class='jstree-anchor'><input type='checkbox' onclick=toggleJSTreeCheckBox('"+cid+"_checkbox','"+jsTreeID+"') /></span>" ); 
  	  else
        $("#"+cid+"_checkbox").remove();
  }
}

var currentSelectedSchemaJStreeID=null;
function createSchema(type,ref,selected,text) {
						//alert(type);
						//console.log($(currentSelectedSchemaJStreeID));
						//var ref = $(currentSelectedSchemaJStreeID).jstree(true), 
						var sel = ref.get_selected();
  						
                        if(selected)
                          sel[0]=selected;
                        if(!text)
                          text = type.toUpperCase();
						if (!sel.length)
                          sel="#";
                      	else
                        /*{
							sel=ref.create_node("#", {
                                "text" : text,
								"type" : type
							});
							ref.edit(sel);	
							return;
						} else {*/
                            sel = sel[0];
  							var selNode=ref.get_node(sel);
  							var selNodeParent=ref.get_node(selNode.parent);
  							//alert(selNode.text+", "+selNodeParent.text+", "+type);
  							if(selNode.text=="config" && selNodeParent.text=="dependency" && type=="properties"){
                              text="package";
                            }else
  							if(type=="properties"){
                              return;
                            }
                            if(type=="try-catch"){
                              text = "TCF-Block";
                          	}else if(type=="sequence" && selNode.type=="switch"){// && sel.parent.type=="switch"){
                            	//console.log();
                              text="CASE";
                            }
							
							sel = ref.create_node(sel, {
                                "text" : text,
								"type" : type
							});
                            if (sel){
                              if(type=="try-catch"){
                                createSchema("sequence",ref,sel,"TRY");
                                createSchema("sequence",ref,sel,"CATCH");
                                createSchema("sequence",ref,sel,"FINALLY");
                              }else if(type == "sequence"){
                              }else if(!text){
                                  ref.edit(sel);
                              }
                            }
                          return sel;
						//}
					}
currentSelectedJSONObject={};
					function changeCurrentNodeType(value){
						var node=currentSelectedJSONObject.node;
						
						if(value==true)
							currentSelectedJSONObject.ref.set_type(node,currentSelectedJSONObject.nodeType+'List');
						else
							currentSelectedJSONObject.ref.set_type(node,currentSelectedJSONObject.nodeType);
						//currentSelectedNode.
					}
					function openConfigurationProprties(){
						//alert("iuiojj");
						var modal = document.getElementById("configurePropertiesModelDialog");
						var span = document.getElementById("closeConfigurePropertiesModelDialog");
						span.onclick = function() {
							var alias=$("#serviceAliasValue").val();
							if(alias.trim().length>1){
								//alert("loadFile: "+loadFile);
                                var urlLoadFile=loadFile.trim()+"$";
								var packageName=("/"+urlLoadFile).replace("/files/","alias?fqn=").replace(".service$",".main").replace(".flow$",".main").replace(".sql$",".main");
								//var packageName=("/"+loadFile).replace("/files/","alias?fqn=").replace(".service",".main").replace(".flow",".main");
								
								packageName=packageName.split("/").join(".");
								//alert("packageName: "+packageName);
								var urlPath="/"+packageName;
								//alert("urlPath: "+urlPath);
								urlPath=urlPath.replace("/files","/alias");
								//alert("urlPath: "+urlPath);
								var method=$("#serviceHTTPMethodValue").val();
								//var alias=$("#serviceAliasValue").val();
								//alert("urlEncoded: "+urlPath+"&alias="+encodeURI(method+alias));
                                if(!alias.includes(packageName)){
								var response=syncRestRequest(urlPath+"&alias="+encodeURI(method+alias), "POST", "");
                                  if(response.status!=200){
                                    alert(response.payload.msg);
                                    return;
                                  }//else
                                     // alert(response.payload.msg);
                                }
							}
							
							var properties=$("#servicePropertiesFile").val();
							if(properties!=null && properties.trim().length>0){
								var urlLoadFile=loadFile.trim()+"$";
								var propertyPath=urlLoadFile.replace(".service$",".properties").replace(".flow$",".properties").replace(".sql$",".properties");
                                //alert(propertyPath);
                              //var propertyPath=loadFile.replace(".service",".properties").replace(".flow",".properties");//).replace("/files","alias?fqn=packages").replace(".service",".main");
								var propFileName=(propertyPath.split("/").join(".")).replace("files.","");
								var propURLPath=("/files/packages/"+propertyPath.split("/")[2]+"/dependency/config/"+propFileName);
								//alert(propURLPath);
								var responseProps=syncRestRequest(propURLPath, "POST", properties, "application/text", "application/text");
								if(responseProps.status!=200){
									alert(JSON.stringify(responseProps));
									return;
								}
							}
							
							modal.style.display = "none";
							
						}
						modal.style.display = "block";
						var urlLoadFile=loadFile.trim()+"$";
						var packageName=("/"+urlLoadFile).replace("/files/","alias?fqn=").replace(".service$",".main").replace(".flow$",".main").replace(".sql$",".main");
						//var packageName=("/"+loadFile).replace("/files/","alias?fqn=").replace(".service",".main").replace(".flow",".main");
						packageName=packageName.split("/").join(".");
						//alert(packageName);
                        var urlPath="/"+packageName;
						//urlPath=urlPath.replace("/files","/alias");
						var response=syncRestRequest(urlPath, "GET", "");
						if(response.status==200){
							var fullAlias=JSON.parse(response.payload).alias;
                          	var alias=null;
                            var method="GET";
                            if(fullAlias){
                              method=fullAlias.split("/")[0];
                              alias=("#"+fullAlias).replace("#"+method,"");
                            }
                            if(alias==null || alias.trim().length==0){
                              alias="/execute/"+packageName.replace("alias?fqn=","");
                              method="GET";
                            }
							$("#serviceAliasValue").val(alias);
							$("#serviceHTTPMethodValue").val(method);
							//alert(JSON.parse(response.payload).alias);
						}else
							alert(JSON.stringify(response));
						//alert(JSON.stringify(response));
                        var urlLoadFile=loadFile.trim()+"$";
						var propertyPath=urlLoadFile.replace(".service$",".properties").replace(".flow$",".properties").replace(".sql$",".properties");
						//var propertyPath=loadFile.replace(".service",".properties").replace(".flow",".properties");//).replace("/files","alias?fqn=packages").replace(".service",".main");
						//alert("propFilePath: "+propertyPath);
						var propFileName=(propertyPath.split("/").join(".")).replace("files.","");
						//alert("propFileName: "+propFileName);
						var propURLPath=("/files/packages/"+propertyPath.split("/")[2]+"/dependency/config/"+propFileName);
						//alert("propURLPath: "+propURLPath);
						var responseProps=syncRestRequest(propURLPath, "GET", "");
						if(responseProps.status==200)
							$("#servicePropertiesFile").val(responseProps.payload);
						else
							alert(JSON.stringify(responseProps));
					}

function openBuildConfigurationForm(){
  var modal = document.getElementById("exportBuildModelDialog");
  var span = document.getElementById("closeExportBuildModelDialog");
  var selectArtifactsButton = document.getElementById("selectArtifactsButton");
  modal.style.display = "block";
  span.onclick = function() {
	  modal.style.display = "none";
	  //$(".elementProperty").css("display","none");
  }
  
  selectArtifactsButton.onclick = function() {
      addCheckBoxOnJSTree('#packageManagerJsTree');
	  modal.style.display = "none";
  }
}
					function openForm(jsTreeId,sel){
					// Get the modal
					var modal = document.getElementById("elementPropertyModalDialog");
					
					
					var ref = $(jsTreeId).jstree(true); 
					var sel = ref.get_selected();
					if(!sel.length){
						alert("Select an element first");
						return;
					}else if(sel.length>1){
						alert("Select only one element");
						return;
					}
					
					// Get the button that opens the modal
					//var btn = document.getElementById("myBtn");
					sel = ref.get_selected()[0];
					currentNodePath=ref.get_path(sel, '/');
					currentSelectedJSONObject.node=sel;
                    currentSelectedJSONObject.selNode=ref.get_node(sel);
					currentSelectedJSONObject.jsTreeId=jsTreeId;
					currentSelectedJSONObject.ref=ref;
					currentSelectedJSONObject.nodeType=ref.get_type(sel);
					if(currentSelectedJSONObject.nodeType=="default")
						currentSelectedJSONObject.nodeType="document";
					if(currentSelectedJSONObject.nodeType.endsWith("List"))
						$("#isArray").prop('checked', true);
					else
						$("#isArray").prop('checked', false);
					var elemType=currentSelectedJSONObject.nodeType.replace("List","");
					$("#elementType").prop("value",elemType);
					showProperties(elemType);
					//ref.set_type(sel,'docList');
					// Get the <span> element that closes the modal
					var span = document.getElementById("closeNodeProperties");
					$("#currentNodePath").html(currentNodePath);
					// When the user clicks on the button, open the modal
					
					  modal.style.display = "block";
					

					// When the user clicks on <span> (x), close the modal
					span.onclick = function() {
					  modal.style.display = "none";
					  $(".elementProperty").css("display","none");
					}
					}
					
					function showProperties(elementType){
                        var curDataNode=currentSelectedJSONObject.selNode;
                        for(var key in curDataNode.data) {
                          var value=curDataNode.data[key];
                          if(key.endsWith('Description') || key.endsWith('regex'))
                        	value=atob(value);
                          //alert(key+":"+value);
                          if(key=="isRequiredField" && value==true){
                            //alert(key+":"+value);
                            //alert($("#"+key));
                            $("#"+key).prop( "checked", true );
                          }
                          else
     					  	$("#"+key).val(value);
						} 
						$(".elementProperty").css("display","none");
						$("#"+elementType+"Properties").css("display","block");
						if(currentSelectedJSONObject.nodeType==elementType)
							return;
						currentSelectedJSONObject.nodeType=elementType;
						changeCurrentNodeType($("#isArray").prop('checked'));
					}
					function updateDataField(field,value){
                      var curDataNode=currentSelectedJSONObject.selNode;
                      if(curDataNode.data==null)
                        curDataNode.data={};
                      if(field.endsWith('Description') || field.endsWith('regex'))
                        value=btoa(value);
                      curDataNode.data[field]=value;
                    }

 function addDblclickClickListener(id,callback){
   $(id).dblclick(function(event) {

              callback();
        
  });
 }

function setUnsavedChanges(fileURL,markStar){
 		var tokenize=fileURL.split("/");
  		var name=tokenize[tokenize.length-1].split(".")[0];
  		if(markStar!=false)
        	localStorage.setItem(fileURL,name+"*");
  		else{
          localStorage.setItem(fileURL,name);
        }
}
