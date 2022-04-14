package com.eka.middleware.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.eka.middleware.service.ServiceUtils;

public class SQL {
public static List<Map<String,Object>> DQL(String sqlCode, List<Map<String,Object>> sqlParameters,Connection myCon ) throws Exception{
	List<Map<String,Object>> outputDocList=new ArrayList<Map<String,Object>>();
	
	if(sqlParameters!=null && sqlParameters.size()>0) {
		for (Map<String, Object> map : sqlParameters) {
			String query=sqlCode;
			
			for (String k : map.keySet()) {
				String v=map.get(k)+"";
				query=query.replaceAll(Pattern.quote("{"+k+"}"), v);
			}
			PreparedStatement myStmt = myCon.prepareStatement(query);
			ResultSet myRs = myStmt.executeQuery();
			List<Map<String,Object>> docList=resultSetToList(myRs);
			if(docList!=null && docList.size()>0)
				outputDocList.addAll(docList);
		}
	}else {
		PreparedStatement myStmt = myCon.prepareStatement(sqlCode);
		ResultSet myRs = myStmt.executeQuery();
		List<Map<String,Object>> docList=resultSetToList(myRs);
		if(docList!=null && docList.size()>0)
			outputDocList.addAll(docList);
	}
	return outputDocList;
}


public static int DML(String sqlCode, List<Map<String,Object>> sqlParameters,Connection myCon ) throws Exception{
	int rows=0;	
	if(sqlParameters!=null && sqlParameters.size()>0) {
		for (Map<String, Object> map : sqlParameters) {
			String query=sqlCode;
			for (String k : map.keySet()) {
				String v=map.get(k)+"";
				query=query.replaceAll(Pattern.quote("{"+k+"}"), v);
			}
			PreparedStatement myStmt = myCon.prepareStatement(query);
			rows += myStmt.executeUpdate();
		}
	}else {
		PreparedStatement myStmt = myCon.prepareStatement(sqlCode);
		rows = myStmt.executeUpdate();
	}
	return rows;
}

public static String[] DML_RGKs(String sqlCode, List<Map<String,Object>> sqlParameters,Connection myCon) throws Exception{
	String ids="";	
	if(sqlParameters!=null && sqlParameters.size()>0) {
		for (Map<String, Object> map : sqlParameters) {
			String query=sqlCode;
			for (String k : map.keySet()) {
				String v=map.get(k)+"";
				query=query.replaceAll(Pattern.quote("{"+k+"}"), v);
			}
			PreparedStatement myStmt = myCon.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
			if(myStmt.executeUpdate()>0) {
				ResultSet keys=myStmt.getGeneratedKeys();
				if(keys.next())
					ids+=keys.getObject(1)+",";
				else
					ids+="null,";
			}
			
			//rows += myStmt.executeUpdate();
		}
	}else {
		PreparedStatement myStmt = myCon.prepareStatement(sqlCode);
		if(myStmt.executeUpdate()>0) {
			ResultSet keys=myStmt.getGeneratedKeys();
			if(keys.next())
				ids=keys.getObject(1)+",";
			else
				ids="null";
		}
	}
	ids=(ids+"_").replace(",_", "");
	return ids.split(",");
}

public static Boolean DDL(String sqlCode, List<Map<String,Object>> sqlParameters,Connection myCon) throws Exception{
	List<Map<String,Object>> outputDocList=new ArrayList<Map<String,Object>>();
	Boolean isSuccessful=false;
	if(sqlParameters!=null && sqlParameters.size()>0) {
		for (Map<String, Object> map : sqlParameters) {
			String query=sqlCode;
			
			for (String k : map.keySet()) {
				String v=map.get(k)+"";
				query=query.replaceAll(Pattern.quote("{"+k+"}"), v);
			}
			PreparedStatement myStmt = myCon.prepareStatement(query);
			isSuccessful = myStmt.execute();
		}
	}else {
		PreparedStatement myStmt = myCon.prepareStatement(sqlCode);
		isSuccessful = myStmt.execute();
	}
	return isSuccessful;
}


public static Connection startTransaction(String jdbcConnection) throws Exception {
	Connection myCon = getConnection(jdbcConnection);	
	myCon.setAutoCommit(false);
	return myCon;
}

public static void commitTransaction(Connection myCon) throws Exception {
	myCon.commit();
}

public static void rollbackTransaction(Connection myCon) throws Exception {
	myCon.rollback();
}

public static Connection getConnection(String jdbcConnection) throws Exception {
	Properties jdbcProperties=new Properties();
	String pPath=ServiceUtils.getPackagesPath();
	String connectionPropFile=pPath+"packages"+jdbcConnection+".jdbc";
	jdbcProperties.load(new FileInputStream(new File(connectionPropFile)));
	
	String connectionUrl=jdbcProperties.getProperty("url");
	String driver=jdbcProperties.getProperty("driver");
	String username=jdbcProperties.getProperty("username");
	String password=jdbcProperties.getProperty("password");
	
	Class.forName(driver);
    
	Connection myCon = null;

	if(username!=null)
		myCon=DriverManager.getConnection(connectionUrl,username,password);
	else
		myCon=DriverManager.getConnection(connectionUrl);
	
	return myCon;
}


private static List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
    ResultSetMetaData md = rs.getMetaData();
    int columns = md.getColumnCount();
    List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    while (rs.next()){
        Map<String, Object> row = new HashMap<String, Object>(columns);
        for(int i = 1; i <= columns; ++i){
            row.put(md.getColumnName(i), rs.getObject(i));
        }
        rows.add(row);
    }
    return rows;
}



}
