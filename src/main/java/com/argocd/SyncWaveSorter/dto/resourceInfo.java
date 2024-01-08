package com.argocd.SyncWaveSorter.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class resourceInfo {
    private String filename;
    private float cpuCores;
    private float memGi; // in GiB
    private int syncWaveBucketLabel;

    public resourceInfo(String filename, float cpuCores, float memGi) {
        this.filename = filename;
        this.cpuCores = cpuCores;
        this.memGi = memGi;
    }
}