package com.eka.middleware.sdk.api.outline;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DataOutline {

    private String acn;
    private String[] argumentsWrapper;
    private String returnWrapper;
    private String[] arguments;
    private String outputArguments;
    private String function;

    private boolean staticFunction;

}
