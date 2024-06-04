package com.eka.middleware.adapter;

import com.eka.middleware.flow.FlowUtils;
import com.eka.middleware.pooling.DBCPDataSource;
import com.eka.middleware.service.*;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.sql.*;
import java.util.*;


public class SQL {
    //public static Logger LOGGER = LogManager.getLogger(SQL.class);
    public static List<Map<String, Object>> DQL(String sqlCode, List<Map<String, Object>> sqlParameters, Map<String,String> columnsType, Connection myCon, DataPipeline dp, boolean logQuery) throws Exception {
        List<Map<String, Object>> outputDocList = new ArrayList<>();
        boolean isColumnsTypeEmpty = columnsType.isEmpty();


        if (sqlParameters != null && sqlParameters.size() > 0) {

            String[] parameterNames = StringUtils.substringsBetween(sqlCode, "{", "}");
            String[] parameterNamesSquare = StringUtils.substringsBetween(sqlCode, "[", "]");
            //Map<String, String> columnsType = null;

            if ((parameterNames != null && parameterNames.length > 0) ||
                    (parameterNamesSquare != null && parameterNamesSquare.length > 0)) {
                //columnsType = new HashMap<>();
                Map<String, Object> firstMap = sqlParameters.get(0);
                if (parameterNames != null) {
                    for (String paramName : parameterNames) {
                        String valuePlaceholder = StringUtils.replace(("'{" + paramName + "}'"), "'", "");
                        if (firstMap.containsKey(paramName)) {
                            sqlCode = sqlCode.replace(valuePlaceholder, "?");
                            if(isColumnsTypeEmpty) {
                                columnsType.put(paramName, getColumnTypeFromDatabase(sqlCode, paramName, myCon.getMetaData()));
                            }
                        }
                    }
                    setStatementParameters(sqlCode, sqlParameters, myCon, outputDocList, parameterNames, columnsType);
                }

                if (parameterNamesSquare != null) {
                    for (String paramName : parameterNamesSquare) {
                        String valuePlaceholder = StringUtils.replace(("'[" + paramName + "]'"), "'", "");
                        if (firstMap.containsKey(paramName)) {
                            sqlCode = sqlCode.replace(valuePlaceholder, "?");
                            if (isColumnsTypeEmpty) {
                                columnsType.put(paramName, getColumnTypeForMySql(sqlCode, paramName, myCon.getMetaData()));
                            }
                        }
                    }
                    setStatementParameters(sqlCode, sqlParameters, myCon, outputDocList, parameterNamesSquare, columnsType);
                }
            }

            if (logQuery)
                dp.log(sqlCode);

        } else {
            sqlCode = removeUninitialized(sqlCode);
            if (logQuery)
                dp.log(sqlCode);

            try (PreparedStatement myStmt = myCon.prepareStatement(sqlCode)) {
                ResultSet myRs = myStmt.executeQuery();
                List<Map<String, Object>> docList = resultSetToList(myRs);
                if (docList != null && docList.size() > 0)
                    outputDocList.addAll(docList);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return outputDocList;
    }

    private static void setStatementParameters(String sqlCode, List<Map<String, Object>> sqlParameters, Connection myCon, List<Map<String, Object>> outputDocList, String[] parameterNames, Map<String, String> columnsType) {
        sqlCode = StringUtils.replace(sqlCode, "'", "");
        sqlCode = removeUninitialized(sqlCode);
        try (PreparedStatement myStmt = myCon.prepareStatement(sqlCode)) {
            for (Map<String, Object> map : sqlParameters) {
                int paramIndex = 1;
                for (String paramName : parameterNames) {
                    if (map.containsKey(paramName)) {
                        Object value = map.get(paramName);
                        String columnType = columnsType.get(paramName);

                        if (value == null) {
                            myStmt.setNull(paramIndex, Types.VARCHAR);
                        } else {
                            switch (columnType) {
                                case "INT":
                                case "INTEGER":
                                    myStmt.setInt(paramIndex, Integer.parseInt(value.toString()));
                                    break;
                                case "DOUBLE":
                                    myStmt.setDouble(paramIndex, Double.parseDouble(value.toString()));
                                    break;
                                case "VARCHAR":
                                case "STRING":
                                    myStmt.setString(paramIndex, value.toString());
                                    break;
                                case "BIT":
                                case "BOOLEAN":
                                    myStmt.setBoolean(paramIndex, Boolean.parseBoolean(value.toString()));
                                    break;
                                case "BLOB":
                                    myStmt.setBytes(paramIndex, (byte[]) value);
                                    break;
                                case "DATE":
                                    java.util.Date date = (java.util.Date) value;
                                    myStmt.setDate(paramIndex, new Date(date.getTime()));
                                    break;
                                default:
                                    myStmt.setObject(paramIndex, value);
                                    break;
                            }
                        }
                        paramIndex++;
                    }
                }

                ResultSet myRs = myStmt.executeQuery();
                List<Map<String, Object>> docList = resultSetToList(myRs);
                if (docList != null && docList.size() > 0)
                    outputDocList.addAll(docList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int DML(String sqlCode, List<Map<String, Object>> sqlParameters, Map<String, String> columnsType, Connection myCon, DataPipeline dp, boolean logQuery) throws Exception {
        int rows = 0;

        boolean isColumnsTypeEmpty = columnsType.isEmpty();

        if (sqlParameters != null && sqlParameters.size() > 0) {
            DatabaseMetaData databaseMetaData = myCon.getMetaData();
            Map<String, Object> mapObject = sqlParameters.get(0);
            //Map<String, String> columnsType = Maps.newHashMap();
            String query = sqlCode;
            String[] parameterNames = StringUtils.substringsBetween(sqlCode, "{", "}");

            if (parameterNames != null) {
                for (String paramName : parameterNames) {
                    String valuePlaceholder = StringUtils.replace(("'{" + paramName + "}'"), "'", "");
                    if (mapObject.containsKey(paramName)) {
                        query = query.replace(valuePlaceholder, "?");
                        if (isColumnsTypeEmpty) {
                            columnsType.put(paramName, getColumnTypeFromDatabase(query, paramName, databaseMetaData));
                        }
                    }
                }
            }
            query = StringUtils.replace(query, "'", "");
            query = removeUninitialized(query);
            if (logQuery)
                dp.log(query);


            try (PreparedStatement myStmt = myCon.prepareStatement(query)) {
                for (Map<String, Object> map : sqlParameters) {
                    int paramIndex = 1;
                    for (String paramName : parameterNames) {
                        if (map.containsKey(paramName)) {
                            Object value = map.get(paramName);
                            String columnName = paramName;
                            String columnType = columnsType.get(columnName);

                            if (value == null) {
                                myStmt.setNull(paramIndex, Types.VARCHAR);
                            } else {
                                switch (columnType) {
                                    case "INT":
                                    case "INTEGER":
                                        myStmt.setInt(paramIndex, Integer.parseInt(value.toString()));
                                        break;
                                    case "DOUBLE":
                                        myStmt.setDouble(paramIndex, Double.parseDouble(value.toString()));
                                        break;
                                    case "VARCHAR":
                                    case "STRING":
                                        myStmt.setString(paramIndex, value.toString());
                                        break;
                                    case "BIT":
                                    case "BOOLEAN":
                                        myStmt.setBoolean(paramIndex, Boolean.parseBoolean(value.toString()));
                                        break;
                                    case "BLOB":
                                        myStmt.setBytes(paramIndex, (byte[]) value);
                                        break;
                                    case "DATE":
                                        java.util.Date date = (java.util.Date) value;
                                        myStmt.setDate(paramIndex, new java.sql.Date(date.getTime()));
                                        break;
                                    default:
                                        myStmt.setObject(paramIndex, value);
                                        break;
                                }
                            }
                            paramIndex++;
                        }
                    }
                    myStmt.addBatch();
//                    rows += myStmt.executeUpdate();
                }
                int[] batch = myStmt.executeBatch();
                for (int i  : batch) {
                    rows += i;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            sqlCode = removeUninitialized(sqlCode);
            if (logQuery)
                dp.log(sqlCode);

            try (PreparedStatement myStmt = myCon.prepareStatement(sqlCode)) {
                rows = myStmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return rows;
    }

    private static String getColumnTypeFromDatabase(String sqlCode, String columnName, DatabaseMetaData databaseMetaData) throws SQLException {
        ResultSet rs = databaseMetaData.getColumns(null, null, getTableNameFromQuery(sqlCode), null);

        while (rs.next()) {
            String colName = rs.getString("COLUMN_NAME");
            String colType = rs.getString("TYPE_NAME");

            if (colName.equals(columnName)) {
                rs.close();
                return colType;
            }
        }

        rs.close();
        return "STRING";
    }

    private static String getColumnTypeForMySql(String sqlCode, String columnName, DatabaseMetaData databaseMetaData) throws SQLException {
        ResultSet rs = databaseMetaData.getColumns(null, null, getTableNameFromQuery(sqlCode), null);

        while (rs.next()) {
            String colName = rs.getString("COLUMN_NAME");
            String colType = rs.getString("TYPE_NAME");

            if (colName.equals(columnName)) {
                rs.close();
                return colType;
            }
        }

        rs.close();
        return "INTEGER";
    }

    public static String getTableNameFromQuery(String sqlQuery) {
        sqlQuery = sqlQuery.replaceAll("[{}]", "");
        sqlQuery = removeUninitialized(sqlQuery);

        if (sqlQuery.trim().toLowerCase().startsWith("insert")) {
            sqlQuery = sqlQuery.replaceAll("(?i)WHERE[^;]+;", ";");
        }
        try {
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sqlQuery);

            if (statement instanceof Update) {
                Update updateStatement = (Update) statement;
                Table table = updateStatement.getTable();
                return table.getName();
            } else if (statement instanceof Insert) {
                Insert insertStatement = (Insert) statement;
                Table table = insertStatement.getTable();
                return table.getName();
            } else if (statement instanceof Delete) {
                Delete deleteStatement = (Delete) statement;
                Table table = deleteStatement.getTable();
                return table.getName();
            } else {
                return null;
            }
        } catch (JSQLParserException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String removeUninitialized(String sqlCode) {
        String[] placeholders = StringUtils.substringsBetween(sqlCode, "{", "}");
        if (placeholders != null) {
            for (String placeholder : placeholders) {
                String replacement = "NULL";
                sqlCode = sqlCode.replace("'{" + placeholder + "}'", replacement);
            }
        }
        return sqlCode;
    }

    public static String[] DML_RGKs(String sqlCode, List<Map<String, Object>> sqlParameters, Map<String,String> columnsType, Connection myCon, DataPipeline dp, boolean logQuery) throws Exception {
        String ids = "";
        boolean isColumnsTypeEmpty = columnsType.isEmpty();

        if (sqlParameters != null && sqlParameters.size() > 0) {
            DatabaseMetaData metaData = myCon.getMetaData();
            //Map<String, String> columnsType = new HashMap<>();
            String[] parameterNames = StringUtils.substringsBetween(sqlCode, "{", "}");

            if (parameterNames != null) {
                Map<String, Object> firstMap = sqlParameters.get(0);

                for (String paramName : parameterNames) {
                    String valuePlaceholder = StringUtils.replace(("'{" + paramName + "}'"), "'", "");
                    if (firstMap.containsKey(paramName)) {
                        sqlCode = sqlCode.replace(valuePlaceholder, "?");
                        if(isColumnsTypeEmpty) {
                            columnsType.put(paramName, getColumnTypeFromDatabase(sqlCode, paramName, metaData));
                        }
                    }
                }
            }

            sqlCode = StringUtils.replace(sqlCode, "'?'", "?");
            sqlCode = removeUninitialized(sqlCode);
            if (logQuery)
                dp.log(sqlCode);

            try (PreparedStatement myStmt = myCon.prepareStatement(sqlCode, Statement.RETURN_GENERATED_KEYS)) {
                for (Map<String, Object> map : sqlParameters) {
                    int paramIndex = 1;
                    for (String paramName : parameterNames) {
                        if (map.containsKey(paramName)) {
                            Object value = map.get(paramName);
                            String columnType = columnsType.get(paramName);

                            if (value == null) {
                                myStmt.setNull(paramIndex, Types.VARCHAR);
                            } else {
                                switch (columnType) {
                                    case "INT":
                                    case "INTEGER":
                                        if ("true".equalsIgnoreCase(value.toString())) {
                                            myStmt.setInt(paramIndex, 1);
                                        } else if ("false".equalsIgnoreCase(value.toString())) {
                                            myStmt.setInt(paramIndex, 0);
                                        } else {
                                            myStmt.setInt(paramIndex, Integer.parseInt(value.toString()));
                                        }
                                        break;
                                    case "DOUBLE":
                                        myStmt.setDouble(paramIndex, Double.parseDouble(value.toString()));
                                        break;
                                    case "VARCHAR":
                                    case "STRING":
                                        myStmt.setString(paramIndex, value.toString());
                                        break;
                                    case "BIT":
                                    case "BOOLEAN":
                                        myStmt.setBoolean(paramIndex, Boolean.parseBoolean(value.toString()));
                                        break;
                                    case "BLOB":
                                        myStmt.setBytes(paramIndex, (byte[]) value);
                                        break;
                                    case "DATE":
                                        java.util.Date date = (java.util.Date) value;
                                        myStmt.setDate(paramIndex, new java.sql.Date(date.getTime()));
                                        break;
                                    default:
                                        myStmt.setObject(paramIndex, value);
                                        break;
                                }
                            }
                            paramIndex++;
                        }
                    }

                    /*int rows = myStmt.executeUpdate();
                    if (rows > 0) {
                        ResultSet keys = myStmt.getGeneratedKeys();
                        if (keys.next()) {
                            ids += keys.getObject(1) + ",";
                        } else {
                            ids += "null,";
                        }
                    }*/
                    myStmt.addBatch();
                }

                int[] rows = myStmt.executeBatch();
                if (rows.length > 0) {
                    ResultSet keys = myStmt.getGeneratedKeys();
                    if (keys.next()) {
                        ids += keys.getObject(1) + ",";
                    } else {
                        ids += "null,";
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            sqlCode = removeUninitialized(sqlCode);
            if (logQuery)
                dp.log(sqlCode);

            try (PreparedStatement myStmt = myCon.prepareStatement(sqlCode, Statement.RETURN_GENERATED_KEYS)) {
                int rows = myStmt.executeUpdate();
                if (rows > 0) {
                    ResultSet keys = myStmt.getGeneratedKeys();
                    if (keys.next())
                        ids = keys.getObject(1) + ",";
                    else
                        ids = "null";

                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return ids.split(",");
    }

    public static Boolean DDL(String sqlCode, List<Map<String, Object>> sqlParameters, Connection myCon, DataPipeline dp, boolean logQuery) throws Exception {
        List<Map<String, Object>> outputDocList = new ArrayList<Map<String, Object>>();
        Boolean isSuccessful = false;
        PreparedStatement myStmt = null;
        try {
            if (sqlParameters != null && sqlParameters.size() > 0) {
                for (Map<String, Object> map : sqlParameters) {
                    String query = sqlCode;

                    for (String k : map.keySet()) {
                        String v = map.get(k) + "";
                        //query=query.replaceAll(Pattern.quote("{"+k+"}"), v);
                        query = ServiceUtils.replaceAllIgnoreRegx(query, "{" + k + "}", v);
                    }
                    if (logQuery)
                        dp.log(query);
                    myStmt = myCon.prepareStatement(query);
                    isSuccessful = myStmt.execute();
                }
            } else {
                if (logQuery)
                    dp.log(sqlCode);
                myStmt = myCon.prepareStatement(sqlCode);
                isSuccessful = myStmt.execute();
            }
        }finally {
            if (myStmt != null) {
                myStmt.close();
            }
        }
        return isSuccessful;
    }


    public static Connection startTransaction(String jdbcConnection, DataPipeline dp) throws Exception {
        Connection myCon = getConnection(jdbcConnection, dp);
        myCon.setAutoCommit(false);
        return myCon;
    }


    /**
     * @param connection
     * @param isolationLevel
     * @return
     * @throws Exception
     */
    public static Connection setTransactionIsolationLevel(Connection connection, int isolationLevel) throws Exception {
        connection.setTransactionIsolation(isolationLevel);
        return connection;
    }

    public static void commitTransaction(Connection myCon) throws SQLException {
        if (null == myCon) {
            throw new RuntimeException("Null Connection");
        }
        myCon.commit();
    }

    public static void rollbackTransaction(Connection myCon) throws SQLException {
        if (null == myCon) {
            throw new RuntimeException("Null Connection");
        }
        myCon.rollback();
    }

    public static void rollbackTransaction(Connection myCon, Savepoint savepoint) throws SQLException {
        myCon.rollback(savepoint);
    }

    public static Savepoint createSavepoint(Connection myCon, String savePointName) throws SQLException {
        Savepoint savepoint = null;
        if (StringUtils.isBlank(savePointName)) {
            savepoint = myCon.setSavepoint();
        } else {
            savepoint = myCon.setSavepoint(savePointName);
        }
        return savepoint;
    }

    public static Connection getProfileConnection(boolean isTransactional) {
        String connectionURL = "jdbc:sqlite:" + PropertyManager.getConfigFolderPath() + "profiles.db";

        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DBCPDataSource.getConnection(connectionURL, connectionURL, null, null, 5, 0, null, null, "org.sqlite.JDBC");
            if (isTransactional) {
                connection.setAutoCommit(false);
            }
            return connection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection(String jdbcConnection, DataPipeline dp) throws Exception {
        Properties jdbcProperties = new Properties();
        String pPath = PropertyManager.getPackagePath(dp.rp.getTenant());
        String connectionPropFile = pPath + "packages" + jdbcConnection + ".jdbc";
        dp.log("connectionPropFile: " + connectionPropFile, Level.DEBUG);
        //LOGGER.debug("connectionPropFile:\n"+connectionPropFile);
        //jdbcProperties.load(new FileInputStream(new File(connectionPropFile)));

        try (FileInputStream fis = new FileInputStream(new File(connectionPropFile))) {
            jdbcProperties.load(fis);
        }

        String connectionUrl = jdbcProperties.getProperty("url");
        connectionUrl = FlowUtils.placeXPathInternalVariables(connectionUrl, dp);
        //connectionUrl = connectionUrl.replace("#{PackageConfig}", dp.getMyPackageConfigPath());
        //LOGGER.debug("connectionUrl:\n"+connectionUrl);
        dp.log("connectionPropFile: " + connectionPropFile, Level.DEBUG);
        String driver = jdbcProperties.getProperty("driver");
        String username = jdbcProperties.getProperty("username");
        String password = jdbcProperties.getProperty("password");
        String pool = jdbcProperties.getProperty("pool");
        String timeOut = jdbcProperties.getProperty("timeout");
        int pooling = 0;
        if (pool != null) {
            pooling = Integer.parseInt(pool.trim());
        }

        int timeOt = 0;
        if (timeOut != null) {
            timeOt = Integer.parseInt(timeOut.trim());
        }

        Driver driverObj = null;
        CustomClassLoader ccl = null;
        try {
            Class.forName(driver);
        } catch (Exception e) {
            ccl = RTCompile.classLoaderMap.get(dp.rp.getTenant().getName());
            driverObj = (Driver) Class.forName(driver, true, ccl).getConstructor().newInstance();
        }

        Connection myCon = null;

        if (username != null) {
            //myCon=DriverManager.getConnection(connectionUrl,username,password);
            myCon = DBCPDataSource.getConnection(connectionPropFile, connectionUrl, username, password, pooling, timeOt, ccl, driverObj, driver);
        } else {
            //myCon=DriverManager.getConnection(connectionUrl);
            myCon = DBCPDataSource.getConnection(connectionPropFile, connectionUrl, null, null, pooling, timeOt, ccl, driverObj, driver);
        }

        return myCon;
    }


    private static List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<String, Object>(columns);
            for (int i = 1; i <= columns; ++i) {
                int colType = md.getColumnType(i);

                if (colType == Types.BINARY || colType == Types.BLOB || colType == Types.VARBINARY
                        || colType == Types.LONGVARBINARY) {
                    row.put(md.getColumnName(i), rs.getBytes(i));
                } else if (colType == Types.BIGINT || colType == Types.INTEGER) {
                    row.put(md.getColumnName(i), rs.getInt(i));
                } else if (colType == Types.VARCHAR || colType == Types.CHAR) {
                    row.put(md.getColumnName(i), rs.getString(i));
                } else if (colType == Types.BOOLEAN || colType == Types.TINYINT || colType == Types.BIT) {
                    row.put(md.getColumnName(i), rs.getBoolean(i));
                } else if (colType == Types.DATE || colType == Types.TIME || colType == Types.TIMESTAMP) {
                    try {
                        java.util.Date date = new java.util.Date();
                        Date sqlDate = rs.getDate(i);
                        if (null != sqlDate) {
                            date.setTime(sqlDate.getTime());
                        }
                        row.put(md.getColumnName(i), date);
                    } catch (SQLException e) {
                        row.put(md.getColumnName(i), rs.getObject(i));
                    }
                } else {
                    row.put(md.getColumnName(i), rs.getObject(i));
                }
            }
            rows.add(row);
        }
        return rows;
    }

}
