package com.eka.middleware.auth.db.repository;


import com.eka.middleware.adapter.SQL;
import com.eka.middleware.auth.db.entity.Groups;
import com.eka.middleware.auth.db.entity.Users;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SystemException;
import org.ldaptive.auth.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersRepository {

    @Deprecated
    public static Map<String, Object> getUsers() throws SystemException {
        Map<String, Object> usersMap = new HashMap<>();

        try (Connection conn = SQL.getProfileConnection(false)) {
            //  String userSql = "SELECT u.* FROM users u";
            String userSql = "SELECT u.* FROM users u WHERE u.deleted = 0";
            try (PreparedStatement userStatement = conn.prepareStatement(userSql)) {
                ResultSet userResultSet = userStatement.executeQuery();

                while (userResultSet.next()) {
                    String userId = userResultSet.getString("user_id");
                    int id = userResultSet.getInt("id");
                    int tenantId = userResultSet.getInt("tenant_id");

                    String passwordHash = userResultSet.getString("password");
                    String name = userResultSet.getString("name");
                    String email = userResultSet.getString("email");
                    String status = userResultSet.getString("status");

                    String tenantSql = "SELECT t.name FROM tenant t WHERE t.tenant_id = ?";
                    String groupSql = "SELECT g.name FROM \"groups\" g " +
                            "JOIN user_group_mapping ug ON g.group_id = ug.group_id " +
                            "WHERE ug.user_id = ?";


                    try (PreparedStatement tenantStatement = conn.prepareStatement(tenantSql);
                         PreparedStatement groupStatement = conn.prepareStatement(groupSql)) {

                        tenantStatement.setInt(1, tenantId);
                        ResultSet tenantResultSet = tenantStatement.executeQuery();

                        String tenantName = null;
                        if (tenantResultSet.next()) {
                            tenantName = tenantResultSet.getString("name");
                        }

                        groupStatement.setInt(1, id);
                        ResultSet groupResultSet = groupStatement.executeQuery();

                        List<String> groupNames = new ArrayList<>();
                        while (groupResultSet.next()) {
                            String groupName = groupResultSet.getString("name");
                            groupNames.add(groupName);
                        }

                        Map<String, Object> profile = new HashMap<>();
                        profile.put("name", name);
                        profile.put("groups", groupNames);
                        profile.put("email", email);
                        profile.put("tenant", tenantName);

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("password", passwordHash);
                        userMap.put("profile", profile);
                        userMap.put("status", status);

                        usersMap.put(userId, userMap);
                    }
                }
            }
        } catch (SQLException e) {
            throw new SystemException("EKA_MWS_1001", e);
        }
        return usersMap;
    }

    public static Map<String, Object> getUserById(String userId) throws SystemException {
        Map<String, Object> userMap = new HashMap<>();

        try (Connection conn = SQL.getProfileConnection(false)) {
            String userSql = "SELECT * FROM users WHERE user_id = ?";
            try (PreparedStatement userStatement = conn.prepareStatement(userSql)) {
                userStatement.setString(1, userId);
                ResultSet userResultSet = userStatement.executeQuery();

                if (userResultSet.next()) {
                    int tenantId = userResultSet.getInt("tenant_id");
                    String passwordHash = userResultSet.getString("password");
                    String name = userResultSet.getString("name");
                    String email = userResultSet.getString("email");
                    String status = userResultSet.getString("status");
                    String verification_secret = userResultSet.getString("verification_secret");

                    String tenantSql = "SELECT name FROM tenant WHERE tenant_id = ?";
                    try (PreparedStatement tenantStatement = conn.prepareStatement(tenantSql)) {
                        tenantStatement.setInt(1, tenantId);
                        ResultSet tenantResultSet = tenantStatement.executeQuery();

                        String tenantName = null;
                        if (tenantResultSet.next()) {
                            tenantName = tenantResultSet.getString("name");
                        }

                        List<String> groupNames = new ArrayList<>();
                        String groupSql = "SELECT g.name FROM \"groups\" g " +
                                "JOIN user_group_mapping ug ON g.group_id = ug.group_id " +
                                "WHERE ug.user_id = ?";
                        try (PreparedStatement groupStatement = conn.prepareStatement(groupSql)) {
                            groupStatement.setString(1, userId);
                            ResultSet groupResultSet = groupStatement.executeQuery();

                            while (groupResultSet.next()) {
                                String groupName = groupResultSet.getString("name");
                                groupNames.add(groupName);
                            }
                        }

                        Map<String, Object> profile = new HashMap<>();
                        profile.put("name", name);
                        profile.put("groups", groupNames);
                        profile.put("email", email);
                        profile.put("verification_secret", verification_secret);
                        profile.put("tenant", tenantName);

                        userMap.put("password", passwordHash);
                        userMap.put("profile", profile);
                        userMap.put("status", status);
                        userMap.put(userId, userMap);
                    }
                }
            }
        } catch (SQLException e) {
            throw new SystemException("EKA_MWS_1001", e);
        }

        return userMap;
    }

    public static int addUser(Users user) throws SystemException {
        try (Connection conn = SQL.getProfileConnection(false)) {
            /*if (isUserExist(conn, user.getEmail())) {
                throw new SystemException("EKA_MWS_1002", new Exception("User already exists with email: " + user.getEmail()));
            }*/
            String sql = "INSERT INTO users (password, name, email, tenant_id, status, user_id, created_date, modified_date, deleted, verification_secret) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            if (null == user.getPassword()) {
                sql = "INSERT INTO users (name, email, tenant_id, status, user_id, created_date, modified_date, deleted, verification_secret) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }
            try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int index=0;
                if (null != user.getPassword()) {
                    statement.setString(++index, user.getPassword());
                }
                statement.setString(++index, user.getName());
                statement.setString(++index, user.getEmail());
                statement.setInt(++index, user.getTenant());
                statement.setString(++index, user.getStatus());
                statement.setString(++index, user.getUser_id());
                statement.setTimestamp(++index, user.getCreated_date());
                statement.setTimestamp(++index, user.getModified_date());
                statement.setInt(++index, user.getDeleted());
                statement.setString(++index, user.getVerificationSecret());
                statement.executeUpdate();
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                 //   String user_id = user.getUser_id();
                    addGroupsForUser(conn, userId, user.getGroups());
                    return userId;
                } else {
                    throw new SystemException("EKA_MWS_1002", new Exception("Failed to add user"));
                }
            }
        } catch (SQLException e) {
            throw new SystemException("EKA_MWS_1001", e);
        }
    }

    public static void updateVerificationSecret(String email, String verificationSecret) throws SystemException {
        try (Connection conn = SQL.getProfileConnection(false)) {
            String sql = "UPDATE \"users\" SET  modified_date = ? , verification_secret = ? WHERE email = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
//                    statement.setString(1, user.getPassword());
                statement.setTimestamp(1, new Timestamp(new Date().getTime()));
                statement.setString(2, verificationSecret);
                statement.setString(3, email);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new SystemException("EKA_MWS_1001", e);
        }
    }

    public static void updateUser(String email, Users user) throws SystemException {
        try (Connection conn = SQL.getProfileConnection(false)) {
            if (null == user.getPassword()) {
                String sql = "UPDATE \"users\" SET name = ?, email = ?, tenant_id = ?, status = ?, modified_date = ? , verification_secret = ? WHERE email = ?";
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
//                    statement.setString(1, user.getPassword());
                    statement.setString(1, user.getName());
                    statement.setString(2, user.getEmail());
                    statement.setInt(3, user.getTenant());
                    statement.setString(4, user.getStatus());
                    statement.setTimestamp(5, user.getModified_date());
                    statement.setString(6, user.getVerificationSecret());
                    statement.setString(7, email);
                    statement.executeUpdate();
                }
            } else {
                String sql = "UPDATE \"users\" SET password = ?, name = ?, email = ?, tenant_id = ?, status = ?, modified_date = ?, verification_secret = ? WHERE email = ?";
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.setString(1, user.getPassword());
                    statement.setString(2, user.getName());
                    statement.setString(3, user.getEmail());
                    statement.setInt(4, user.getTenant());
                    statement.setString(5, user.getStatus());
                    statement.setTimestamp(6, user.getModified_date());
                    statement.setString(7, user.getVerificationSecret());
                    statement.setString(8, email);
                    statement.executeUpdate();
                }
            }
            int user_Id = getUserIdByEmailAsInt(conn, user.getEmail());
          //  String userId = getUserIdByEmail(conn, user.getEmail());
            updateGroupsForUser(conn, user_Id, user.getGroups());
        } catch (SQLException e) {
            throw new SystemException("EKA_MWS_1001", e);
        }
    }

    public static void deleteUser(String email) throws SystemException {
        try (Connection conn = SQL.getProfileConnection(false)) {
            String userId = getUserIdByEmail(conn, email);
            deleteGroupsForUser(conn, userId);
            Timestamp modifiedDate = new Timestamp(System.currentTimeMillis());
            String sql = "UPDATE users SET deleted = 1, modified_date = ? WHERE email = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setTimestamp(1, modifiedDate);
                statement.setString(2, email);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new SystemException("EKA_MWS_1001", e);
        }
    }

    public static String getUserIdByEmail(Connection conn, String email) throws SQLException {
        String userIdSql = "SELECT user_id FROM \"users\" WHERE email = ?";
        try (PreparedStatement userIdStatement = conn.prepareStatement(userIdSql)) {
            userIdStatement.setString(1, email);
            try (ResultSet userIdResultSet = userIdStatement.executeQuery()) {
                return userIdResultSet.next() ? userIdResultSet.getString("user_id") : null;
            }
        }
    }

    public static int getUserIdByEmailAsInt(Connection conn, String email) throws SQLException {
        String userIdSql = "SELECT id FROM \"users\" WHERE email = ?";
        try (PreparedStatement userIdStatement = conn.prepareStatement(userIdSql)) {
            userIdStatement.setString(1, email);
            try (ResultSet userIdResultSet = userIdStatement.executeQuery()) {
                return userIdResultSet.next() ? userIdResultSet.getInt("id") : -1; // Return -1 if not found
            }
        }
    }


    public static boolean isUserExist(Connection conn, String userIdOrEmail) throws SQLException {
        String userIdSql = "SELECT user_id FROM \"users\" WHERE email = ? OR user_id = ?";
        try (PreparedStatement userIdStatement = conn.prepareStatement(userIdSql)) {
            userIdStatement.setString(1, userIdOrEmail);
            userIdStatement.setString(2, userIdOrEmail);
            try (ResultSet userIdResultSet = userIdStatement.executeQuery()) {
                return userIdResultSet.next();
            }
        }
    }

    public static void addGroupsForUser(Connection conn, int userId, List<Groups> groups) throws SQLException {
        String insertGroupSql = "INSERT INTO user_group_mapping (user_id, group_id) VALUES (?, ?)";
        try (PreparedStatement insertGroupStatement = conn.prepareStatement(insertGroupSql)) {
            for (Groups group : groups) {
                int groupId = getGroupIdByName(conn, group.getGroupName());
                if (groupId != -1) {
                    insertGroupStatement.setInt(1, userId);
                    insertGroupStatement.setInt(2, groupId);
                    insertGroupStatement.executeUpdate();
                } else {
                    System.out.println("Group not found: " + group.getGroupName());
                }
            }
        }
    }

    private static void updateGroupsForUser(Connection conn, int userId, List<Groups> groups) throws SQLException {
        String deleteGroupSql = "DELETE FROM user_group_mapping WHERE user_id = ?";
        try (PreparedStatement deleteGroupStatement = conn.prepareStatement(deleteGroupSql)) {
            deleteGroupStatement.setInt(1, userId);
            deleteGroupStatement.executeUpdate();
        }

        addGroupsForUser(conn, userId, groups);
    }

    private static void deleteGroupsForUser(Connection conn, String userId) throws SQLException {
        String deleteGroupSql = "DELETE FROM user_group_mapping WHERE user_id = ?";
        try (PreparedStatement deleteGroupStatement = conn.prepareStatement(deleteGroupSql)) {
            deleteGroupStatement.setString(1, userId);
            deleteGroupStatement.executeUpdate();
        }
    }

    public static int getGroupIdByName(Connection conn, String groupName) throws SQLException {
        String groupIdSql = "SELECT group_id FROM \"groups\" WHERE name = ?";
        try (PreparedStatement groupIdStatement = conn.prepareStatement(groupIdSql)) {
            groupIdStatement.setString(1, groupName);
            try (ResultSet groupIdResultSet = groupIdStatement.executeQuery()) {
                if (groupIdResultSet.next()) {
                    return groupIdResultSet.getInt("group_id");
                } else {
                    return -1;
                }
            }
        }
    }
    public static boolean doesMappingExist(String username, int groupId, Connection connection) throws SQLException {
        String checkMappingSQL = "SELECT 1 FROM user_group_mapping WHERE user_id = ? AND group_id = ?";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkMappingSQL)) {
            checkStatement.setString(1, username);
            checkStatement.setInt(2, groupId);

            ResultSet resultSet = checkStatement.executeQuery();
            return resultSet.next();
        }
    }
}






