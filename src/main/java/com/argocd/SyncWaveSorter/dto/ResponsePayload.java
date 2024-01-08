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
    String namespace;
    float freeMemGi;
    float freeCpuCores;
    List<resourceInfo> resources;

    public void setInfo(String cluster, String namespace, float freeCpuCores, float freeMemGi) {
        this.cluster = cluster;
        this.namespace = namespace;
        this.freeMemGi = freeMemGi;
        this.freeCpuCores = freeCpuCores;
    }

    public void sortResources() {
        Collections.sort(resources, new Comparator<resourceInfo>() {
            @Override
            public int compare(resourceInfo o1, resourceInfo o2) {
                return Integer.compare(o1.getSyncWaveBucketLabel(), o2.getSyncWaveBucketLabel());
            }
        });
    }
}
