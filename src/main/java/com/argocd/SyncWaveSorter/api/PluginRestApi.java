package com.argocd.SyncWaveSorter.api;

import com.argocd.SyncWaveSorter.dto.RequestPayload;
import com.argocd.SyncWaveSorter.dto.AppInfo;
import com.argocd.SyncWaveSorter.service.GitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
    @RequestMapping("/api/v1")
    public class PluginRestApi {

        private final GitService gitService;

        public PluginRestApi(GitService gitService) {
            this.gitService = gitService;
        }

        @PostMapping("/getparams.execute")
        public ResponseEntity<?> getGitRepoParameters(@RequestBody RequestPayload payload) {
            try {
                RequestPayload.Parameters params = payload.getInput().getParameters();
                List<AppInfo> appInfoList = gitService.processGitRepo(params.getGitRepo(),
                        params.getGitPath(), params.getResourcePaths());
                return ResponseEntity.ok(appInfoList);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
            }
        }
}
