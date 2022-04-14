package com.eka.middleware.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.flow.FlowUtils;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;

public class SqlResolver {
	public static void execute(DataPipeline dp,JsonObject mainSqlJsonObject) throws Exception{

		JsonValue sqlInputs = mainSqlJsonObject.asJsonObject().get("input");
		JsonValue sqlOutput = mainSqlJsonObject.asJsonObject().get("output");
		String sqlCode = mainSqlJsonObject.getString("sql");
		Boolean validationRequired=false;//mainSqlJsonObject.getBoolean("enableServiceDocumentValidation");
		if(validationRequired)
			FlowUtils.validateDocuments(dp, sqlInputs);
		
		byte[] decodedBytes = Base64.getDecoder().decode(sqlCode);
		sqlCode = new String(decodedBytes);
		Properties sqlProperties=(Properties)dp.get("sqlLocalProperties");	
		
		List<Map<String,Object>> sqlParameters=dp.getAsList("inputDocList");
		
		Boolean isTxn=dp.getAsBoolean("isTxn");
		if(isTxn==null)
			isTxn=false;
		
		Object txConn=dp.get("txConn");
		
		String type="DQL";
		if(sqlProperties.get("type")!=null) {
			type=(String) sqlProperties.get("type");
			type=type.toUpperCase();
		}else
			throw new Exception("SQL type not specified in the configuration");
        
		Connection myCon = null;
		
		//DQL and DDL are non-transactional
		if(txConn!=null && "DML".equals(type) && isTxn==true) {
			myCon=(Connection)txConn;
			//isTxn=true;
		}else {
			myCon=SQL.getConnection(sqlProperties.getProperty("JDBC"));
			isTxn=false;
		}
		
		List<Map<String,Object>> outputDocList=null;
		try {
			
			switch (type) {
			case "DQL":
				outputDocList=SQL.DQL(sqlCode, sqlParameters, myCon);
				dp.put("outputDocList", outputDocList);
				dp.put("rows", outputDocList.size());
				dp.put("success", true);
				break;
			case "DDL":
				Boolean isSuccessful=SQL.DDL(sqlCode, sqlParameters, myCon);
				dp.put("success", isSuccessful);
				break;
			case "DML":
				int rows=SQL.DML(sqlCode, sqlParameters, myCon);
				dp.put("rows", rows);
				dp.put("success", true);
				break;
			case "DML_RGK":
				String keys[]=SQL.DML_RGKs(sqlCode, sqlParameters, myCon);
				dp.put("rows", keys.length);
				dp.put("keys", keys);
				dp.put("success", true);
				break;
			default:
				break;
			}
		}catch(Exception e) {
			throw e;
		} finally {
			if(!isTxn)
				myCon.close();
		}

		JsonArray flowOutputArray=sqlOutput.asJsonArray();
		
		if(flowOutputArray.isEmpty())
			flowOutputArray=null;
		Map<String, Object> outPutData=null;
		
		if(flowOutputArray!=null) {
			outPutData=new HashMap<>();
			for (JsonValue jsonValue : flowOutputArray) {
				String key=jsonValue.asJsonObject().getString("text");
				Object val=dp.get(key);
				if(val!=null)
					outPutData.put(key, dp.get(key));
			}
		}
		//if(dp.get("*multiPart")!=null)
			//outPutData.put("*multiPart", dp.get("*multiPart"));
		dp.clear();
		if(outPutData!=null)
			dp.putAll(outPutData);
		if(validationRequired)
			FlowUtils.validateDocuments(dp, sqlOutput);
	}
	
	
}
