package com.aurora.services.model;

import lombok.Data;

@Data
public class Stats {
    private String packageName;
    private String installerPackageName;
    private long timeStamp;
    private boolean granted;
    private boolean success;
    private boolean install;
}
