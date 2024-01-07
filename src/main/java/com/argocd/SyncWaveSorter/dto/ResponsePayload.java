package com.argocd.SyncWaveSorter.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public class ResponsePayload {
    String cluster;

    public void setInfo(String cluster, String namespace, float cpuLimitCores, float memoryLimitGi) {
        this.cluster = cluster;
        this.namespace = namespace;
        this.memoryLimitGi = memoryLimitGi;
        this.cpuLimitCores = cpuLimitCores;
    }


    String namespace;
    float memoryLimitGi;
    float cpuLimitCores;
    List<AppInfo> resources;

    public void sortResources(){
        Collections.sort(resources, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return Integer.compare(o1.getSyncWaveBucketLabel(), o2.getSyncWaveBucketLabel());
            }
        });
    }
}
