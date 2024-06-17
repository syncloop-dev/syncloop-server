package com.eka.middleware.sdk.api.outline;

import com.beust.jcommander.internal.Lists;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class IOOutline {

    private String text;
    private String type;
    private List<IOOutline> children = Lists.newArrayList();
}
