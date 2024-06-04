package com.eka.middleware.sdk.api.outline;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class LatestOutline {

    private ApiInfoOutline api_info;
    private List<IOOutline> input;
    private List<IOOutline> output;
    private DataOutline data;

}
