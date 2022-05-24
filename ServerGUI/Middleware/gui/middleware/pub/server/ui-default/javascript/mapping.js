class mapper{
  
	lines = [];
	createList=[];
	dropList=[];
	isMouseDown=false;
	startConnectionId=null;
	endConnectionId=null;
	selectedLines=[];
	loopCounter=0;
	transformers=[];
    node=null;
  	isEnabled=false;
  	isAllowed=false;
    inJSTreeID=null;
    outJSTreeID=null;
    funcInJSTreeID=null;
    funcOutJSTreeID=null;
	inputSchema={
							  "$schema": "https://json-schema.org/draft/2020-12/schema",
							  "$id": "https://example.com/product.schema.json",
							  "title": "Product",
							  "description": "A product from Acme's catalog",
							  "type": "object",
							  "properties": {
							    "productId": {
							      "description": "The unique identifier for a product",
							      "type": "string"
							    },
							    "productName": {
							      "description": "Name of the product",
							      "type": "string"
							    },
							    "price": {
							      "description": "The price of the product",
							      "type": "number"
							    },
							    "tags": {
							      "description": "Tags for the product",
							      "type": "array",
							      "items": {
							        "type": "string"
							      }
							    },
							    "dimensions": {
							      "type": "array",
							      "items": {
							    	    "type": "object",
							    	    "properties": {
							    	      "loc": {
							    	        "type": "string"
							    	      },
							    	      "toll": {
							    	        "type": "string"
							    	      },
							    	      "message": {
							    	        "type": "string"
							    	      },
							    	      "extras" : {
							    	    	  "type":"array",
							    	    	  "items":{
							    	    		  "type":"object",
							    	    		  "properties": {
												        "length": {
												          "type": "integer"
												        },
												        "width": {
												          "type": "integer"
												        },
												        "height": {
												          "type": "integer"
												        }
												      }
							    	    	  }
							    	      }
							    	    }
							    	  }
							    }
							  }
							};
constructor(inJSTreeID,outJSTreeID,funcInJSTreeID,funcOutJSTreeID){
  this.inJSTreeID=inJSTreeID;
  this.outJSTreeID=outJSTreeID;
  this.funcInJSTreeID=funcInJSTreeID;
  this.funcOutJSTreeID=funcOutJSTreeID;
  this.enableMappingOnJstree("#launching_arrow_jsTree");
  this.enableMappingOnJstree("#landing_arrow_jsTree");
  this.enableMappingOnJstree("#launching_arrow_jsTree_function");
  this.enableMappingOnJstree("#landing_arrow_jsTree_function");
  let mapperThis=this;
  	$('body').mousemove(function(e){
                        let newLine = $('#newLine');
						if(mapperThis.isMouseDown){
							if(newLine!=null){
								newLine.attr({'x2':e.clientX-2,'y2':e.clientY-2});
							}
						}else{
							if(newLine!=null)
							newLine.remove();
						}
					});
  $('body').mouseup(function(e){
  	let newLine = $('#newLine');
	if(newLine!=null){
		newLine.remove();
    }
  });
  }
  
  clean(){
  	this.transformers=[];
    let mapperThis=this;
	this.lines.forEach(function(lineMap){
      if(lineMap.line && lineMap.line!=""){
      	lineMap.line.remove();
        lineMap.line=0;
      }
    });
    this.lines = [];
	this.createList=[];
	this.dropList=[];
	this.isMouseDown=false;
	this.startConnectionId=null;
	this.endConnectionId=null;
	this.selectedLines=[];
	this.loopCounter=0;
	this.transformers=[];
    this.node=null;
    $(".dropperTag").remove();
    $(".changeTag").remove();
    $(".valueTag").remove();
  }
  
  drop(){
    let ref = $(this.outJSTreeID).jstree(true);
  	var sel = ref.get_selected()[0];
    let mapperThis=this;
    if(sel){
      let node=ref.get_node(sel);
      let nodePath=ref.get_path(node, '/');
      let dropPath={};
      let typePath=this.getNodeTypePath(this.outJSTreeID,nodePath);
      dropPath.path=nodePath;
      dropPath.typePath=typePath;
      let isDropped=false;
      if(!this.dropList)
        this.dropList=[];
      
      for(let i=0;i<this.dropList.length;i++) {
		if(nodePath==this.dropList[i].path){
        	isDropped=true;
          	break;
        }
      }
      	if(!isDropped){
          this.dropList.push(dropPath);
          this.reMap();
          
      }
  	}
    
  }
  
  deleteSelectedFromDropList(){
  if(!this.dropList)
    return null;
  let ref = $(this.outJSTreeID).jstree(true);
  var sel = ref.get_selected()[0];
  let deleted=false;
  if(sel){
    let node=ref.get_node(sel);
    let nodePath=ref.get_path(node, '/');    
	for(let i=0;i<this.dropList.length;i++) {
		if(nodePath==this.dropList[i].path){
			//this.dropList[i].remove();
			delete this.dropList[i];
			this.dropList[i]=null;
          	deleted=true;
            break;
		}
	}
	for(let i = 0; i < this.dropList.length; i++){ 
	    if ( this.dropList[i] == null) { 
	     	this.dropList.splice(i, 1); 
	        i--; 
	   	}
    }
  }
  if(deleted){
	$(".dropperTag").remove();
    this.reMap();
  }
  return deleted;
}  
  setValue(jsTreeRef,value,evaluate){
  	var sel = jsTreeRef.get_selected()[0];
    let mapperThis=this;
    if(!this.createList)
      this.createList=[];
    if(sel){
      let node=jsTreeRef.get_node(sel);
      let nodePath=jsTreeRef.get_path(node, '/');
      let deleted=false;
      let createValue=this.getValue(jsTreeRef);
      let isAvailable=false;
      if(createValue!=null)
        isAvailable=true;
      else
        createValue={};
        
      createValue.path=nodePath;
      createValue.value=value;
      //alert(createValue.value);
      createValue.evaluate=evaluate;
      //console.log(jsTreeRef);
      //console.log(jsTreeRef.element[0].id);
      //console.log(jsTreeRef.element.0.id);
      createValue.typePath=this.getNodeTypePath("#"+jsTreeRef.element[0].id,nodePath);
      if(!isAvailable)
      	this.createList.push(createValue);
      this.reMap();
//      $(this.outJSTreeID).on('scroll',function(e, data) {setTimeout(function(){mapperThis.reMap();},100)});
    }
  }

getValue(jsTreeRef){
  if(!this.createList)
    return null;
  var sel = jsTreeRef.get_selected()[0];
  if(sel){
    let node=jsTreeRef.get_node(sel);
    let nodePath=jsTreeRef.get_path(node, '/');
    for(let i=0;i<this.createList.length;i++) {
        if(nodePath==this.createList[i].path)
          return this.createList[i];
    }
  }
  return null;
}
  
deleteSelectedFromCreateList(){
  if(!this.createList)
    return null;
  let jsTreeRef = $(this.outJSTreeID).jstree(true);
  var sel = jsTreeRef.get_selected()[0];
  let deleted=false;
  if(sel){
    let node=jsTreeRef.get_node(sel);
    let nodePath=jsTreeRef.get_path(node, '/');    
	for(let i=0;i<this.createList.length;i++) {
		if(nodePath==this.createList[i].path){
			//this.dropList[i].remove();
			delete this.createList[i];
			this.createList[i]=null;
          	deleted=true;
            break;
		}
	}
	for(let i = 0; i < this.createList.length; i++){ 
	    if ( this.createList[i] == null) { 
	     	this.createList.splice(i, 1); 
	        i--; 
	   	}
    }
  }
  if(deleted){
	$(".valueTag").remove();
    this.reMap();
  }
  return deleted;
}
  
  init(node){
    this.isAllowed=false;
    this.isEnabled=false;
    this.node=node;
    
    if(!this.node.data)
      this.node.data={};
    
    if(!this.node.data.transformers)
      this.node.data.transformers=[];
    
    this.transformers=JSON.parse(JSON.stringify(this.node.data.transformers));
    //console.log("-----------------------------------------------------------------------------------");
    //console.log(this.transformers);
    //console.log(this.node.data);
    
    if(this.node.data.dropList){
    	this.dropList=JSON.parse(JSON.stringify(this.node.data.dropList));
    }
    if(this.node.data.createList){
      //console.log(this.node.data.createList);
    	this.createList=JSON.parse(JSON.stringify(this.node.data.createList));
      //console.log(this.createList);
    }
    if(this.node.data.lines){
    	this.lines=JSON.parse(JSON.stringify(this.node.data.lines));
    }
    //console.log(this.dropList);
    this.reMap();
  }
					
					getLineMap(inputPath,outputPath){
						//lineMap={};
						for (const lm of this.lines) {
							  if(lm.inputPath==inputPath && lm.outputPath==outputPath)
								 return lm; 
							}
						return null;
					}
					
					getLineMapOp(inputPath,op){
						//lineMap={};
						for (const lm of this.lines) {
							  if(lm.inputPath==inputPath && lm.op==op)
								 return lm; 
							}
						return null;
					}
					
  					getLineMapsByInputParent(inputPath){
						let lineMaps=[];
                        //console.log(this.lines);
						for (const lm of this.lines) {
							  if(lm!=null && lm.inputPath.startsWith(inputPath) && lm.op!=null)
								 lineMaps.push(lm); 
							}
						return lineMaps;
					}
  
					getLineMapByLoop_Id(loop_Id){
						//lineMap={};
						for (const lm of this.lines) {
							  if(lm.loop_Id==loop_Id)
								 return lm; 
							}
						return null;
					}
					
					getLineMapsByFollowers(loop_Id){
						let lineMaps=[];
						for (const lm of this.lines) {
							  if(lm.follow==loop_Id)
								  lineMaps.push(lm); 
							}
						return lineMaps;
					}
					
					getLineMaps(inputPath,outputPath){
						let lineMaps=[];
						this.selectedLines=[];
                        //console.log(this.lines);
						for (const lm of this.lines) {
                          	if(!lm.line){
                            	lm.line=0;
                              	this.reConnectMap(lm.line);
                            }
                          	//console.log(lm);
                          	if(lm.line!=0)
								lm.line.color='rgba(30, 130, 250, 0.5)';
							$("#deleteMap").attr("disabled",true);
							$("#mapProperties").attr("disabled",true);
							  if((inputPath==null || lm.inputPath==(inputPath)) && (outputPath==null || lm.outputPath==(outputPath)))
								  lineMaps.push(lm); 
							}
						return lineMaps;
					}
					
					
					
					selectMapping(jsTreeId,node){
						let instance=$(jsTreeId).jstree(true);
						let nodePath=instance.get_path(node, '/');
						////console.log("nodePath");
						////console.log(nodePath);
						let lineMaps=[];
						lineMaps=this.getLineMaps(nodePath,null);
						//console.log(lineMaps);
						if(lineMaps!=null && lineMaps.length>0){
							if(lineMaps[0].inpJsTree!=jsTreeId)
								lineMaps=this.getLineMaps(null,nodePath);
						}else {
							lineMaps=this.getLineMaps(null,nodePath);
							if(lineMaps==null || lineMaps.length==0 || lineMaps[0].outpJsTree!=jsTreeId)
								return;
						}
						
						////console.log("Again:");
						////console.log(lineMaps);
						if(lineMaps!=null && lineMaps.length>0){
							this.selectedLines=lineMaps;
							let inpJsTree_instance = $(lineMaps[0].inpJsTree).jstree(true);
							let outpJstree_instance = $(lineMaps[0].outpJsTree).jstree(true);
							inpJsTree_instance.deselect_all(true);
							outpJstree_instance.deselect_all(true);
							let counter=0;
                            let mapperThis=this;
							lineMaps.forEach(function(lineMap){
							//console.log("iterating over lineMaps");
							//inpJsTree_instance = $(lineMap.inpJsTree).jstree(true);
							//outpJstree_instance = $(lineMap.outpJsTree).jstree(true);
							let inputNode=mapperThis.getNodeByPath(lineMap.inpJsTree,lineMap.inputPath);
							////console.log("inputNode");
							////console.log(inputNode);
							////console.log(lineMap.outpJsTree);
							////console.log(lineMap.outputPath);
							let outputNode=mapperThis.getNodeByPath(lineMap.outpJsTree,lineMap.outputPath);
							////console.log("outputNode");
							////console.log(outputNode);
							inpJsTree_instance.select_node(inputNode);
							outpJstree_instance.select_node(outputNode);
							lineMap.line.color='rgba(210, 0, 0, 1)';
							$("#deleteMap").attr("disabled",false);
							if(counter==0)
								$("#mapProperties").attr("disabled",false);
							else
								$("#mapProperties").attr("disabled",true);
							counter++;
						});
						}
						
					}
					
					
					
					deleteSelectedLines(){
                      let mapperThis=this;
						$("#deleteMap").attr("disabled",true);
							mapperThis.selectedLines.forEach(function(selectedLine){
								for(let i=0;i<mapperThis.lines.length;i++) {
									if(selectedLine==mapperThis.lines[i]){
                                        if(mapperThis.lines[i].line!=null && mapperThis.lines[i].line!=0 && mapperThis.lines[i].line!="")
											mapperThis.lines[i].line.remove();
                                        if(mapperThis.lines[i].dashedLine==true){
                                        	let lineMaps=mapperThis.getLineMapsByInputParent(mapperThis.lines[i].inputPath);
                                            let loop_Id=mapperThis.lines[i].loop_Id;
                                            //console.log(lineMaps);
                                            lineMaps.forEach(function(lineMap){
                                                //console.log(lineMap.INPath)
                                            	//console.log(loop_Id);
                                                lineMap.INPath=lineMap.INPath.replace("{"+loop_Id+"}","0");
                                                lineMap.OUTPath=lineMap.OUTPath.replace("{"+loop_Id+"}","0")
                                            });
                                        }
										delete mapperThis.lines[i];
										mapperThis.lines[i]=null;
									}
								}
							});
							
							for(let i = 0; i < mapperThis.lines.length; i++){ 
                                
						        if ( mapperThis.lines[i] == null) { 
						        	mapperThis.lines.splice(i, 1); 
						            i--; 
						        }
						    }
						this.refresh();
					}
					
					reMap(){
						
                        let mapperThis=this;
						$(".dropperTag").remove();
                      	$(".changeTag").remove();
                      	let bp={};
                        let instance = $(mapperThis.outJSTreeID).jstree(true);
                        let transformer=null;
                        
                        this.lines.forEach(function(lineMap,index){
                          //console.log(index);
                          //console.log(mapperThis.transformers);
                          //console.log(mapperThis.transformers[index]);
                          if(mapperThis.transformers.length>index)
                        	transformer=mapperThis.transformers[index];
                          mapperThis.reConnectMap(lineMap,transformer);
                        });
                        this.transformers=[];
                        this.dropList.forEach(function(dropPath){
                          mapperThis.createNode(mapperThis.outJSTreeID,dropPath.path,dropPath.typePath);
                          let node=mapperThis.getTaggableNode(mapperThis.outJSTreeID,dropPath.path);
                          if(node){
                          		let nodeAnchor=$("#"+node.id+"_anchor");
                          		let path=instance.get_path(node, '/');
                          		if(path==dropPath.path)
                            		nodeAnchor.before("<span class='glyphicon glyphicon-thumbs-down dropperTag' style='width:0px;overflow:visible;left:-16px;top:2px'></span>");
                            	else if(!bp[path]){
                                  	bp[path]="true";
                                  	nodeAnchor.before("<span class='glyphicon glyphicon-pushpin changeTag' style='width:15px;overflow:visible;'></span>");
                                }
                          }
                        });
                        this.createList.forEach(function(createPath){
                          mapperThis.createNode(mapperThis.outJSTreeID,createPath.path,createPath.typePath);
                          let node=mapperThis.getTaggableNode(mapperThis.outJSTreeID,createPath.path);
                          if(node){
                          		let nodeAnchor=$("#"+node.id+"_anchor");
                          		let path=instance.get_path(node, '/');
                          		if(path==createPath.path)
                            		nodeAnchor.before("<span class='glyphicon glyphicon-pencil valueTag' style='width:0px;overflow:visible;left:-16px;top:2px'></span>");
                            	else if(!bp[path]){
                                  	bp[path]="true";
                                  	nodeAnchor.before("<span class='glyphicon glyphicon-pushpin changeTag' style='width:15px;overflow:visible;'></span>");
                                }
                          }
                        });
                      this.refresh();
					}
					showJP(){
						reMap();
						alert(JSON.stringify(this.transformers));
						}
					getNodeByPath(jstreeID,path){
						let pathNode=null;
						let instance = $(jstreeID).jstree(
								true);
						$(jstreeID).jstree('get_json', null, { 'flat': true }).forEach(function(node){
							if(pathNode!=null)
								return;
							let np = instance.get_path(
									node, '/');
							////console.log("Searching node: "+np+" == "+path);
							if(np==path)
								pathNode=node;
						});
						////console.log("Path node found:"+pathNode);
						return pathNode;
					}
				
  refresh(){
    this.node.data.lines=null;
    let mapperThis=this;
  	let tempJSONObj=JSON.parse(JSON.stringify(this.lines));
    this.node.data.transformers=[];
    this.transformers=[];
    tempJSONObj.forEach(function(DatalineMap){
        let transformer=mapperThis.generateJSONPatch(DatalineMap);
      
        let inpJsTree=DatalineMap.inpJsTree;
      	let inputPath=DatalineMap.inputPath;
      	transformer.inTypePath=mapperThis.getNodeTypePath(inpJsTree,inputPath);
      	DatalineMap.inTypePath=transformer.inTypePath;
      
      	let outpJsTree=DatalineMap.outpJsTree;
		let outputPath = DatalineMap.outputPath;
      	//alert(outpJsTree+","+outputPath);
      	transformer.outTypePath=mapperThis.getNodeTypePath(outpJsTree,outputPath);
      	DatalineMap.outTypePath=transformer.outTypePath;
      	//alert(transformer.outTypePath);
		mapperThis.transformers.push(transformer);
        mapperThis.node.data.transformers.push(transformer);
        DatalineMap.line=0;
      
    }); 
    this.node.data.lines=tempJSONObj;
    if(this.dropList){
    	this.node.data.dropList=JSON.parse(JSON.stringify(this.dropList));
    }
    if(this.createList){
    	this.node.data.createList=JSON.parse(JSON.stringify(this.createList));
    }
  }
  
  
  
					generateJSONPatch(lineMap){
						let transformer={};
						transformer.op=lineMap.op;
                        var indexes=lineMap.INPath.match(/#\d+/g);
                        if(indexes!=null){
                            var from=null;
                            for(var i=0;i<indexes.length;i++){
                                var index=indexes[i];
                                if(from==null)
                                  from=lineMap.INPath;
                                var val=index.replace("#","");
                                from=from.replace("/"+index+"/","/"+val+"/");
                            }
                          transformer.from="/"+from;
                        }else
							transformer.from="/"+lineMap.INPath;
                        indexes=lineMap.OUTPath.match(/#\d+/g);
                        if(indexes!=null){
                            var to=null;
                            for(var i=0;i<indexes.length;i++){
                                var index=indexes[i];
                                if(to==null)
                                  to=lineMap.OUTPath;
                                var val=index.replace("#","");
                                to=to.replace("/"+index+"/","/"+val+"/");
                            }
                          transformer.to="/"+to;
                        }else
							transformer.to="/"+lineMap.OUTPath;
						if(lineMap.loop_Id!=null)
							transformer.loop_Id=lineMap.loop_Id;
						if(lineMap.follow!=null)
							transformer.follow=lineMap.follow;
						if(lineMap.grouped==true)
							transformer.group=true;
						if(lineMap.sort==true)
							transformer.sort=true;
                        if(lineMap.condition!=null)
                      		transformer.condition=lineMap.condition;
                        if(lineMap.applyFunction!=null)
                        	transformer.applyFunction=lineMap.applyFunction;
                        if(lineMap.jsFunction!=null)
                        	transformer.jsFunction=lineMap.jsFunction;
                        if(lineMap.jsFunctionSig!=null)
                        	transformer.jsFunctionSig=lineMap.jsFunctionSig;
						return transformer;
					}

getTaggableNode(jsTreeId,nodePath){
  let node=this.getNodeByPath(jsTreeId,nodePath);
  let inpJsTree_instance = $(jsTreeId).jstree(true);
  let pos=$('#'+node.id).position();
  while(pos==null){
      node=inpJsTree_instance.get_node(node.parent);
      pos=$('#'+node.id).position();
  }
  //alert(JSON.stringify(node));
  let inPathNode=node;
  let inpJsTreeRef=$(jsTreeId);
  let inPathNode_anchor=$('#'+inPathNode.id+"_anchor");
  let isOverflowBottom=false;
  if(inPathNode_anchor.offset().top>(inpJsTreeRef.offset().top+inpJsTreeRef.height()-20))
      isOverflowBottom=true;
  let isInVisible=(!isOverflowBottom && (inpJsTreeRef.offset().top<=(inPathNode_anchor.offset().top+2)));//$(inpJsTree).height()+40>inPathNode_anchor.offset().top && inPathNode_anchor.offset().top>30;
  if(isInVisible)
    return node;
  return null;
}

					reConnectMap(lineMap,transformer){
						if(lineMap==null || lineMap.line==null)
							return;
						if(lineMap.line!=null && lineMap.line!=0){
                          //console.log(lineMap.line);
                          lineMap.line.remove();
                        }
						lineMap.line=null;
						//return;
						let inpJsTree=lineMap.inpJsTree;
						let outpJstree=lineMap.outpJsTree;

							let node=this.getNodeByPath(inpJsTree,lineMap.inputPath);
                      		if(!node){
                              if(lineMap.inTypePath)
                            	this.createNode(inpJsTree,lineMap.inputPath,lineMap.inTypePath);
                              
                              	node=this.getNodeByPath(inpJsTree,lineMap.inputPath);
                              	//if(!this.selectedLines)
                                this.selectedLines=[];//
                                if(node==null){
                                  this.selectedLines.push(lineMap);
                                  this.deleteSelectedLines();
                                  return;
                                }
                            }
                      		//console.log("**************************");
                            //console.log(JSON.stringify(node));
							let inpJsTree_instance = $(inpJsTree).jstree(true);
							//isOpen=inpJsTree_instance.is_open(node);
							let pos=$('#'+node.id).position();
							while(pos==null){
								node=inpJsTree_instance.get_node(node.parent);
								pos=$('#'+node.id).position();
							}
							let inPathNode=node;
                      		let inpJsTreeRef=$(inpJsTree);
							let inPathNode_anchor=$('#'+inPathNode.id+"_anchor");
                      		let isInputOverflowBottom=false;
							if(inPathNode_anchor.offset().top>(inpJsTreeRef.offset().top+inpJsTreeRef.height()-20))
								isInputOverflowBottom=true;
							let isInVisible=(!isInputOverflowBottom && (inpJsTreeRef.offset().top<=(inPathNode_anchor.offset().top+2)));//$(inpJsTree).height()+40>inPathNode_anchor.offset().top && inPathNode_anchor.offset().top>30;
                            //console.log(outpJstree);
                      		//console.log(lineMap);
							node=this.getNodeByPath(outpJstree,lineMap.outputPath);
                      		
                      		if(!node){
                              if(lineMap.outTypePath)
                            	this.createNode(outpJstree,lineMap.outputPath,lineMap.outTypePath);
                              	node=this.getNodeByPath(outpJstree,lineMap.outputPath);
                              	//if(!this.selectedLines)
                                this.selectedLines=[];//
                                if(node==null){
                                  this.selectedLines.push(lineMap);
                                  this.deleteSelectedLines();
                                  return;
                                }
                            }
							let outpJstree_instance = $(outpJstree).jstree(true);
							pos=$('#'+node.id).position();
							while(pos==null){
								node=outpJstree_instance.get_node(node.parent);
								pos=$('#'+node.id).position();
							}
							
							let outPathNode=node;
                      		let outJstTreeRef=$(outpJstree);
							let outPathNode_anchor=$('#'+outPathNode.id+"_anchor");
                      		let isOutputOverflowBottom=false;
							if(outPathNode_anchor.offset().top>(outJstTreeRef.offset().top+outJstTreeRef.height()-20))
								isOutputOverflowBottom=true;
							let isOutVisible=(!isOutputOverflowBottom && (outJstTreeRef.offset().top<=(outPathNode_anchor.offset().top+2)));//$(outpJstree).height()+40>outPathNode_anchor.offset().top && outPathNode_anchor.offset().top>30;
							let jsFunction=null;
                            let lineColour='30, 130, 250';
                            //if(mappingIndex){
                                //console.log("mappingIndex: "+(mappingIndex-1));
                                //console.log(this.transformers);
                            	//let transformer=this.transformers[mappingIndex-1];
                                //console.log("transformer: "+transformer);
                      		  let mappingCondition=null;
                      		  let mappingApplyFunction=null;
                              if(transformer){
                                jsFunction=transformer.jsFunction;
                              	mappingCondition=transformer.condition;
                                mappingApplyFunction=transformer.applyFunction
                              }
                              if((jsFunction!=null && jsFunction.trim().length>0) || (mappingCondition!=null && mappingCondition.trim().length>0) || (mappingApplyFunction!=null && mappingApplyFunction.trim().length>0))
                                lineColour='12, 187, 82';
                            //}
							let line=null;
							if(isInVisible && isOutVisible){
								line = new LeaderLine(document.getElementById(inPathNode.id+"_anchor"),document.getElementById(outPathNode.id+"_anchor"),{dash: lineMap.dashedLine});
								line.color = 'rgba('+lineColour+', 0.8)';
								line.size = 2;
							}else if(isInVisible){
								if(isOutputOverflowBottom){
									line = new LeaderLine(document.getElementById(inPathNode.id+"_anchor"),document.getElementById(outJstTreeRef.attr('id')+"_bp"),{dash: lineMap.dashedLine});
									line.setOptions({endSocket: 'top'});
								}else{
									line = new LeaderLine(document.getElementById(inPathNode.id+"_anchor"),document.getElementById(outJstTreeRef.attr('id')+"_tp"),{dash:lineMap.dashedLine});
									line.setOptions({endSocket: 'bottom'});
								}	
								line.color = 'rgba('+lineColour+', 0.8)';
								line.size = 2;
									line.setOptions({ // element-1, element-2
										  gradient: {
										    startColor: 'rgba('+lineColour+', 0.8)',
										    endColor: 'rgba('+lineColour+', .2)'
										  }
										});
							}else if(isOutVisible){
								if(isInputOverflowBottom){
									line = new LeaderLine(document.getElementById(inpJsTreeRef.attr('id')+"_bp"),document.getElementById(outPathNode.id+"_anchor"),{dash: lineMap.dashedLine});
									line.setOptions({startSocket: 'top'});
								}else{
                                  	//console.log(inpJsTreeRef);
									line = new LeaderLine(document.getElementById(inpJsTreeRef.attr('id')+"_tp"),document.getElementById(outPathNode.id+"_anchor"),{dash: lineMap.dashedLine});
									line.setOptions({startSocket: 'bottom'});
								}	
								line.color = 'rgba('+lineColour+', 0.8)';
								line.size = 2;
									line.setOptions({ // element-1, element-2
										  gradient: {
										    startColor: 'rgba('+lineColour+', .2)',
										    endColor: 'rgba('+lineColour+', 0.8)'
										  }
										});
							}else{
								lineMap.line=0;
								return;
							}
								lineMap.line=line;
					}
					
					enableMappingOnJstree(id) {
                        let mapperThis=this;
						$(id).on(
										'open_node.jstree',
										function(e, data) {
											//console.log(data.node.id);
											setTimeout(function(){
											mapperThis.reMap();},100);
											
										}).on('close_node.jstree',
											function(e, data) {
											//console.log(data.node.id);
											setTimeout(function(){
											mapperThis.reMap();},100);
										}).on('click',
											function(e, data) {
											//console.log(e.target);
                          					setTimeout(function(){
											//mapperThis.reMap();
                                            mapperThis.selectMapping(id,e.target);
                                            },100);
											
										}).on('scroll',
											function(e, data) {
											setTimeout(function(){
											mapperThis.reMap();},100);
										}).on('mousedown',
											function(e, data) {
                          					
											if(!mapperThis.isMouseDown){
												mapperThis.isMouseDown=true;
                                              
											let newLine = document.createElementNS('http://www.w3.org/2000/svg','line');
											newLine.setAttribute('id','newLine');
											$("#map").append(newLine);
											newLine = $('#newLine');
                                            //console.log(mapperThis.isMouseDown+" "+JSON.stringify({'x1':e.clientX,'y1':e.clientY,'x2':e.clientX,'y2':e.clientY,'style':'stroke:rgb(0,0,0);stroke-width:1;'}));
											newLine.attr({'x1':e.clientX,'y1':e.clientY,'x2':e.clientX,'y2':e.clientY,'style':'stroke:rgb(0,0,0);stroke-width:1;'});
											let instance = $(id).jstree(true);
											instance.deselect_all(true);
											instance.select_node(e.target);
                                            if(!$(id).jstree("get_selected",true)[0]){
                                            	if(newLine!=null)
													newLine.remove();
                                              	mapperThis.isMouseDown=false;
                                            }
                                              
											mapperThis.startConnectionId=id;
											}
										
										}).on('mouseup',
											function(e, data) {
											let newLine = $('#newLine');
											if(newLine!=null)
												newLine.remove();
											if(mapperThis.isMouseDown){
												mapperThis.isMouseDown=false;
												if(id!=mapperThis.startConnectionId){
												mapperThis.endConnectionId=id;
												setTimeout(function(){
													if(mapperThis.startConnectionId!=null && mapperThis.endConnectionId!=null){
														mapperThis.connect(mapperThis.startConnectionId,mapperThis.endConnectionId);
														mapperThis.startConnectionId=null;
														mapperThis.endConnectionId=null;
													}
													},100);
												}
											}
										}).on('mouseover',
											function(e, data) {
											if(mapperThis.isMouseDown){
												if(id!=mapperThis.startConnectionId)
												setTimeout(function(){
													let instance = $(id).jstree(true);
													instance.deselect_all(true);
													instance.select_node(e.target);
													},100);
											}
										});
                      /*.on('refresh.jstree',
											function(e, data) {
											//console.log(e.target);
                          					if(mapperThis.isEnabled){
                                              if(mapperThis.isAllowed)
                                                  setTimeout(function(){ mapperThis.init(mapperThis.node);},250);
                                              mapperThis.isAllowed=true;
                                            }
										});*/
					}
//----------------------------Main mapping logic---------------------------------
//Connect function maps and connect the lines. Its used only when we drag drop the connting lines.
connect(inpJsTree,outpJstree,IPNode,OPNode,loop_Id) {
	if(!inpJsTree.startsWith("#launching_arrow") || !outpJstree.startsWith("#landing_arrow"))
		return;
	let inpJsTree_instance = $(inpJsTree).jstree(true);
	let outpJstree_instance = $(outpJstree).jstree(true);
	let inPathNode=IPNode;
	let outPathNode=OPNode;
	if(inPathNode==null)
		inPathNode=$(inpJsTree).jstree("get_selected",true)[0];
	if(outPathNode==null)
		outPathNode=$(outpJstree).jstree("get_selected",true)[0];

	let inType=inpJsTree_instance.get_type(inPathNode);
	let outType=outpJstree_instance.get_type(outPathNode);
	if(inType!=outType){
		alert("Invalid Mapping request");
		return;
    }
    

	let lastInputPathSelected=inpJsTree_instance.get_path(inPathNode, '/');
	let lastOutputPathSelected=outpJstree_instance.get_path(outPathNode, '/');
	let lm=this.getLineMap(lastInputPathSelected,lastOutputPathSelected);
	if(lm!=null)
		return;
  	if(inType.startsWith("document") && outType.startsWith("document") && loop_Id==null){
        let childConnectionLines=this.getLineMapsByInputParent(lastInputPathSelected);
        if(childConnectionLines!=null && childConnectionLines.length>0){
    		let confirmBox=confirm("Solid mapping of document will replace the entire child structure of target path with source.\nAll the child mappings will be removed If you click 'OK'.\nPlease confirm.");
        	if(confirmBox){
            	this.selectedLines=childConnectionLines;
              	this.deleteSelectedLines();
            }else
              return;
        }
    }
	let lineMap=this.getLineMap(lastInputPathSelected);
	if(lineMap==null)
		lineMap={};
	let dashedLine=false;
	if(loop_Id!=null){
		lineMap.loop_Id=loop_Id;
		dashedLine=true;
	}
	lineMap.inputPath=lastInputPathSelected;
	lineMap.outputPath=lastOutputPathSelected;
	lineMap.inpJsTree=inpJsTree;
	lineMap.outpJsTree=outpJstree;
	lineMap.INPath=lineMap.inputPath;
	lineMap.OUTPath=lineMap.outputPath;
	let newMap=null;

	newMap=this.maplistElementAndGetNewPathObject(lineMap);
    if(newMap=="error")
      return null;
	if(newMap!=null){
		lineMap.INPath=newMap.INPath;
        lineMap.OUTPath=newMap.OUTPath;
        lineMap.follow=newMap.follow;
        lineMap.op=newMap.op;
        if(newMap.grouped==true)
            lineMap.grouped=true;
	}

	if(inType==outType && outType=="documentList" && loop_Id!=null){
		lineMap.INPath+="/#{"+loop_Id+"}";
		lineMap.OUTPath+="/#{"+loop_Id+"}";
	}
	
	let line = new LeaderLine(document.getElementById(inPathNode.id+"_anchor"),document.getElementById(outPathNode.id+"_anchor"), {dash: dashedLine});
	line.color = 'rgba(30, 130, 250, 0.5)';
	line.size = 2;

	lineMap.line=line;
	lineMap.dashedLine=dashedLine;
	//console.log(line);
	if(lineMap.op==null){
		if(lineMap.loop_Id!=null)
		lineMap.op="loop";
		else if(lineMap.follow!=null)
			lineMap.op="add";
			else
				lineMap.op="copy";
	}
  	lineMap.inType=inType;
  	lineMap.outType=outType;
	this.lines.push(lineMap);
    this.refresh();
	//alert("connecting "+lineMap.INPath+" to "+lineMap.OUTPath);
	return lineMap;
}
//-------------Hash function. Its mostly used to generate loop ids.----------------------------
hashCode(s) {
  var h = 0, l = s.length, i = 0;
  if ( l > 0 )
    while (i < l)
      h = (h << 5) - h + s.charCodeAt(i++) | 0;
  return h;
};
//----------------This function ths used to map looping structures.--------------------------------------
  maplistElementAndGetNewPathObject(lineMap){
   
	let inputPath=lineMap.inputPath;
	let outputPath=lineMap.outputPath;
				
	let lm=this.getLineMap(inputPath,outputPath);
	if(lm!=null)
		return null;
			
	let newMap={};
	newMap.INPath=lineMap.inputPath;
   	newMap.OUTPath=lineMap.outputPath;
   
	let inpJsTree=lineMap.inpJsTree;
	let inpJsTree_instance = $(inpJsTree).jstree(true);	
	let nodeI=this.getNodeByPath(inpJsTree,inputPath);
    let inputConnectPath=inputPath.split("/");
	let inputParentPath=inputConnectPath[0];
    //nodeI=this.getNodeByPath(inpJsTree,inputParentPath);
    let isInputDocList=inpJsTree_instance.get_type(nodeI)=="documentList";
    let inParent=null;
   
	let outpJsTree=lineMap.outpJsTree;
	let outpJsTree_instance = $(outpJsTree).jstree(true);
	let nodeO=this.getNodeByPath(outpJsTree,outputPath);
    let outputConnectPath=outputPath.split("/");
    let outputParentPath=outputConnectPath[0];
    //nodeO=this.getNodeByPath(outpJsTree,outputParentPath);
    let isOutputDocList=outpJsTree_instance.get_type(nodeO)=="documentList";
    let outParent=null;
    if(lineMap.loop_Id!=null || lineMap.follow!=null){//isOutputDocList==isInputDocList && isInputDocList==true){
    	return null;
    }
   let indexI=1;
   let indexO=1;
   nodeO=this.getNodeByPath(outpJsTree,outputParentPath);
   isOutputDocList=outpJsTree_instance.get_type(nodeO)=="documentList";
   nodeI=this.getNodeByPath(inpJsTree,inputParentPath);
   isInputDocList=inpJsTree_instance.get_type(nodeI)=="documentList";
   let prevCMap=null;
   let lastLoopId=null;
   while(inputParentPath!=inputPath || outputParentPath!=outputPath){
          
     while(!isInputDocList && inputParentPath!=inputPath){
       inputParentPath=inputParentPath+"/"+inputConnectPath[indexI++];
       nodeI=this.getNodeByPath(inpJsTree,inputParentPath);
       isInputDocList=inpJsTree_instance.get_type(nodeI)=="documentList";
       console.log("isInputDocList: "+isInputDocList+", Path: "+inputParentPath+", indexI: "+indexI);
     }
     
     while(!isOutputDocList && outputParentPath!=outputPath){
       outputParentPath=outputParentPath+"/"+outputConnectPath[indexO++];
       nodeO=this.getNodeByPath(outpJsTree,outputParentPath);
       isOutputDocList=outpJsTree_instance.get_type(nodeO)=="documentList";
       console.log("isOutputDocList: "+isOutputDocList+", Path: "+outputParentPath+", indexO: "+indexO);
     }
     
     if(isOutputDocList==isInputDocList && isInputDocList==true){
     	let loop_Id="loop_id_"+this.hashCode(inpJsTree_instance.get_path(nodeI, '_'));
       	console.log(loop_Id);
        let lineTemp=this.getLineMaps(inputParentPath,outputParentPath);
        if(lineTemp!=null && lineTemp.length>0 && lineTemp[0].dashedLine!=true){
         alert("Remove solid mapping from '"+inputParentPath+" to "+outputParentPath);
         return "error";
        }
        let newCMap=null;
        if(inputParentPath==inputPath || outputParentPath==outputPath){
          	newCMap=null;
            loop_Id=null;
        }else
          	newCMap=this.connect(inpJsTree,outpJsTree,this.getNodeByPath(inpJsTree,inputParentPath),this.getNodeByPath(outpJsTree,outputParentPath),loop_Id);
       	if(newCMap!=null && newCMap.INPath!=null){
          if(prevCMap!=null){             
          	newCMap.OUTPath=newCMap.OUTPath.replace(prevCMap.OUTPath.split("#{"+lastLoopId+"}")[0],prevCMap.OUTPath+"/");
            newCMap.INPath=newCMap.INPath.replace(prevCMap.INPath.split("#{"+lastLoopId+"}")[0],prevCMap.INPath+"/");
          }
          newMap.OUTPath=newCMap.OUTPath+(outputPath.replace(outputParentPath,""));
          newMap.INPath=newCMap.INPath+(inputPath.replace(inputParentPath,""));//+outputParentPath+/*"/#{"+loop_Id+"}";/*/newMap.INPath.split(inputParentPath)[1];
          newMap.follow=loop_Id;
        }else{
          let outPP=outputParentPath;
          let inPP=inputParentPath;
          let loop_Id_temp=loop_Id;
          if(lastLoopId!=null){
            if(prevCMap!=null)
            	lineTemp=this.getLineMaps(prevCMap.inputPath,prevCMap.outputPath);
            else
                lineTemp=null;
            if(lineTemp!=null && lineTemp.length>0 && lineTemp[0].dashedLine!=true){
              outPP=outPP.replace(newMap.OUTPath.split("#{"+lastLoopId+"}")[0],"#0/");
              inPP=inPP.replace(newMap.INPath.split("#{"+lastLoopId+"}")[0],"#0/");
            }else{
              outPP=outPP.replace(newMap.OUTPath.split("#{"+lastLoopId+"}")[0],"#{"+lastLoopId+"}/");
              inPP=inPP.replace(newMap.INPath.split("#{"+lastLoopId+"}")[0],"#{"+lastLoopId+"}/");
            }
          }
          if(loop_Id!=null){
            newMap.OUTPath=newMap.OUTPath.replace(outPP,outPP+"/#{"+loop_Id+"}");
            newMap.INPath=newMap.INPath.replace(inPP,inPP+"/#{"+loop_Id+"}");//+outputParentPath+/*"/#{"+loop_Id+"}";/*/newMap.INPath.split(inputParentPath)[1];
            newMap.follow=loop_Id;
          }
        }
        prevCMap=newCMap;
        lastLoopId=loop_Id;
     }else if(isInputDocList){
     	//inputParentPath=inpJsTree_instance.get_path(nodeI, '/');
        if(newMap.INPath=="")
          newMap.INPath=inputParentPath;
        let tempPath="";
        let i=0;
        for(i=indexI;i<inputConnectPath.length;i++)
         tempPath+="/"+inputConnectPath[i];
        newMap.INPath=(newMap.INPath).replace(tempPath,"/#0"+tempPath);//+"/#0"+(inputPath.replace(inputParentPath,""));
     }else if(isOutputDocList){
       //outputParentPath=outpJsTree_instance.get_path(nodeO, '/');
       if(newMap.OUTPath=="")
         newMap.OUTPath=outputParentPath;
       let tempPath="";
       let o=0;
       for(o=indexO;o<outputConnectPath.length;o++)
         tempPath+="/"+outputConnectPath[o];
       //console.log("/#0"+tempPath);
       //console.log(newMap.OUTPath);
       newMap.OUTPath=(newMap.OUTPath).replace(tempPath,"/#0"+tempPath);//(outputPath.replace(outputParentPath,""));
     }
     isInputDocList=false;
     isOutputDocList=false;
     //console.log(newMap);
   }
   //console.log(newMap);
   return newMap;
 }
  getNodeTypePath(jsTreeId,targetPath){
   // alert("2");
   let typePath="";
   let path="";
   let pathArr=targetPath.split("/");
   let ref = $(jsTreeId).jstree(true);
   for(let i=0;i<pathArr.length;i++){
     //alert(pathArr[i]);
      if(i==0)
      	path=pathArr[i];
      else
        path+="/"+pathArr[i];
      //alert(path);
      let node=this.getNodeByPath(jsTreeId,path);
      if(i==0)
      	typePath=ref.get_type(node);
      else
        typePath+="/"+ref.get_type(node);
    } 
    return typePath;
  }
  createNode(jsTreeId,targetPath,typePath){
	let pathArr=targetPath.split("/");
    let pathTypeArr=typePath.split("/");
    let ref = $(jsTreeId).jstree(true);
    let lastNode="#";
    let path="";
    for(let i=0;i<pathArr.length;i++){
      if(i==0)
      	path=pathArr[i];
      else
        path+="/"+pathArr[i];
      let node=this.getNodeByPath(jsTreeId,path);
      if(!node){
      	lastNode=createSchema(pathTypeArr[i],ref,lastNode,pathArr[i]);
      }else
      	lastNode=node;
    }
  }
  
  getPathListFromJSTreeSchema(schemaJSTreeID,droppedList){
    let schemaJSTreeRef=$(schemaJSTreeID).jstree(true);
  	let nodes=schemaJSTreeRef.get_json('#', {flat:true});
    let schemaPaths=[];
    let mapperThis=this;
    jQuery.each(nodes, function(i, node) {
      let schemaPath={};
      schemaPath.path=schemaJSTreeRef.get_path(node, '/');
     // let pathHash=mapperThis.hashCode(schemaPath.path);
      let droppedPath=false;
      var dropPath=mapperThis.hashCode(schemaPath.path);
        if(!droppedList[dropPath]){
          var dropped=mapperThis.findInDropList(droppedList,schemaPath.path);
          if(dropped==true){
            droppedList[dropPath]={};
            droppedList[dropPath].path=schemaPath.path;
            droppedList[dropPath].dropped=true;
          }
        }
      if(droppedList!=null && droppedList[dropPath]!=null)
      	droppedPath=droppedList[dropPath].dropped;
      if(!droppedPath){
      	schemaPath.typePath=mapperThis.getNodeTypePath(schemaJSTreeID,schemaPath.path);
        schemaPaths.push(schemaPath);
      }
    });
    return schemaPaths;
  }
  
findInDropList(dropList,path){
  var dropped=false;
  //console.log("Chcking path: "+path);
  for (var dropPath in dropList) {
  	var map=dropList[dropPath];
    //console.log(path+" startsWith "+map.path);
    if(path.startsWith(map.path)){
    //   console.log("Dropped path: "+map.path);
       //   dropped=true;
          return true;
    }
  }
  return false;
}
  
}