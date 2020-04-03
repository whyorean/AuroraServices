package com.aurora.services.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class App {
    private String displayName;
    private String packageName;
    private String versionName;
    private String installer;
    private String installLocation;
    private String iconBase64;
    private Long installedTime;
    private Long versionCode;
}
