package com.argocd.SyncWaveSorter.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class RequestPayload {
    private String applicationSetName;
    private Input input;

    @Getter
    public static class Input {
        private Parameters parameters;
    }

    @Getter
    public static class Parameters {
        private String gitRepo;
        private String gitPath;
        private String cluster;
        private String namespace;
        private List<String> resourcePaths;
    }
}