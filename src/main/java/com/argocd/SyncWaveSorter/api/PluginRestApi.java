package com.argocd.SyncWaveSorter.api;

import com.argocd.SyncWaveSorter.dto.RequestPayload;
import com.argocd.SyncWaveSorter.dto.ResponsePayload;
import com.argocd.SyncWaveSorter.service.BucketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PluginRestApi {

    private final BucketService bucketService;

    public PluginRestApi(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @PostMapping("/getparams.execute")
    public ResponseEntity<?> getGitRepoParameters(@RequestBody RequestPayload payload) {
        try {
            ResponsePayload response = bucketService.processGitRepo(payload.getInput().getParameters());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
        }
    }
}
