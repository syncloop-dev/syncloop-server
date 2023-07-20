package com.eka.middleware.update;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter @Getter
public class Plugins {

    private String name;
    private String name_slug;
    private String unique_id;
    private String short_description;
    private String description;
    private Set<String> tags;
    private String latest_version;
    private int latest_version_number;
    private String digest;
    private Set<String> screenshorts;
    private long created_on;
    private long modified_on;

    private License license;

    private PluginOwner owner;

    private PurchaseRule purchase_rule;

    private boolean installed;
    private boolean requiredUpdate;
}
