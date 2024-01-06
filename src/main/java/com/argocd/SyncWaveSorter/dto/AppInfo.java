package com.argocd.SyncWaveSorter.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppInfo {
    private String filename;
    private float cpuCores;
    private float memoryGi; // in GiB
    private int syncWaveBucketLabel;

    public AppInfo(String filename, float cpuCores, float memoryGi) {
        this.filename = filename;
        this.cpuCores = cpuCores;
        this.memoryGi = memoryGi;
    }
}