package com.eka.middleware.auth.db.entity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class Users {

    private String password;
    private String name;
    private List<Groups> groups;
    private String email;
    private int tenant;
    private String status;
    private Map<String, Object> profile;
    private String user_id;
    private Timestamp created_date;
    private Timestamp modified_date;
    private int deleted;


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Groups> getGroups() {
        return groups;
    }

    public void setGroups(List<Groups> groups) {
        this.groups = groups;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public int getTenant() {
        return tenant;
    }

    public void setTenant(int tenant) {
        this.tenant = tenant;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getProfile() {
        return profile;
    }

    public void setProfile(Map<String, Object> profile) {
        this.profile = profile;
    }

    public Timestamp getCreated_date() {
        return created_date;
    }

    public void setCreated_date(Timestamp created_date) {
        this.created_date = created_date;
    }

    public Timestamp getModified_date() {
        return modified_date;
    }

    public void setModified_date(Timestamp modified_date) {
        this.modified_date = modified_date;
    }


    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }
    public Users() {
    }

    public Users(String password, String email, int tenant, String name, String status, String user_id, List<Groups> groups) {
        this.user_id = user_id;
        this.password = password;
        this.name = name;
        this.groups = groups;
        this.email = email;
        this.tenant = tenant;
        this.status = status;
    }
    public Users(String password, String email, int tenant, String name, String status, String user_id, List<Groups> groups,Timestamp created_date,Timestamp modified_date,int deleted) {
        this.user_id = user_id;
        this.password = password;
        this.name = name;
        this.groups = groups;
        this.email = email;
        this.tenant = tenant;
        this.status = status;
        this.created_date = created_date;
        this.modified_date = modified_date;
        this.deleted = deleted;
    }

    public Users(String password,String email, int tenant, String name, String status, String user_id, List<Groups> groups, Timestamp modified_date, int deleted) {
        this.password = password;
        this.user_id = user_id;
        this.name = name;
        this.groups = groups;
        this.email = email;
        this.tenant = tenant;
        this.status = status;
        this.modified_date = modified_date;
        this.deleted = deleted;

    }


    public void addGroup(String groupName) {
        // Implement this method if needed.
    }
}
