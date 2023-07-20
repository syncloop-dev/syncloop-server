package com.eka.middleware.update;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PluginOwner {

    private String published_by;
    private boolean verified;
    private String website;

    private String privacy_policy;
}
