package com.aurora.services.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class App {
    private String displayName;
    private String packageName;
    private String versionName;
    private String installer;
    private String installLocation;
    private String category = null;
    private String description;
    private boolean isSystem = false;
    private boolean isBackupAvailable = false;
    private boolean isSplit = false;
    private Integer targetSDK = null;
    private Integer pid = null;
    private Long lastUpdated = null;
    private Long installedTime = null;
    private Long versionCode = null;
    private String iconBase64;
}
