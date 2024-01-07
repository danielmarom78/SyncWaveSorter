package com.argocd.SyncWaveSorter.service;

import com.argocd.SyncWaveSorter.dto.AppInfo;
import com.argocd.SyncWaveSorter.dto.RequestPayload;
import com.argocd.SyncWaveSorter.dto.ResponsePayload;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BucketService {

    private static final Logger logger = LoggerFactory.getLogger(BucketService.class);
    private static final String CLONE_PATH = "/tmp/clonedRepository";
    String OCP_USER_PREFIX = "OCP_USER:";
    String OCP_PASS_PREFIX = "OCP_PASS:";
    String GIT_USER_PREFIX = "GIT_USER:";
    String GIT_PASS_PREFIX = "GIT_PASS:";

    public ResponsePayload processGitRepo(RequestPayload.Parameters request) {
        ResponsePayload response = new ResponsePayload();
        try {
            File repoDir = cloneRepository(request.getGitRepo());
            response.setResources(processYamlFiles(repoDir, request.getGitPath(), request.getResourcePaths()));
            getQuotasFromCluster(request, response);
            divideResourcesIntoBucketsAndAssignLabels(response);
        } catch (Exception e) {
            logger.error("Error processing Git repository: {}", e.getMessage(), e);
        }

        response.sortResources();

        return response;
    }

    private void getQuotasFromCluster(RequestPayload.Parameters request, ResponsePayload response) {
        String cluster = request.getCluster();
        String namespace = request.getNamespace();

        String username = System.getenv(OCP_USER_PREFIX + cluster);
        String password = System.getenv(OCP_PASS_PREFIX + cluster);

        // Initialize the OpenShift client. Credentials should be set using environment variables or secrets.
        OpenShiftClient openShiftClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withUsername(username)
                .withPassword(password)
                .build());
        // Fetch the resource quota for the specified namespace
        ResourceQuota quota = openShiftClient.resourceQuotas().inNamespace(namespace).list().getItems().stream()
                .findFirst()
                .orElse(null);

        float memoryLimitGi = 0;
        float cpuLimitCores = 0;

        if (quota != null) {
            String memoryLimit = quota.getSpec().getHard().get("limits.memory").toString();
            String cpuLimit = quota.getSpec().getHard().get("limits.cpu").toString();

            memoryLimitGi = convertMemoryStringToGi(memoryLimit);
            cpuLimitCores = convertCpuStringToCores(cpuLimit);

            logger.info("Got quota from cluster=" + cluster + " namespace=" + namespace + " cpuLimitCores=" + cpuLimitCores + " memoryLimitGi=" + memoryLimitGi);
        } else {
            logger.warn("No quota found for cluster=" + cluster + " namespace=" + namespace);
        }

        response.setInfo(cluster, namespace, cpuLimitCores, memoryLimitGi);
    }


    private File cloneRepository(String gitRepoUrl) throws GitAPIException, IOException {
        File repoDir = new File(CLONE_PATH);
        try {
            deleteAndCreateDirectory(repoDir);

            // Retrieve the username and token from environment variables
            String githubUsername = System.getenv(GIT_USER_PREFIX + gitRepoUrl);
            String githubToken = System.getenv(GIT_PASS_PREFIX + gitRepoUrl);

            // Use the credentials for cloning
            Git.cloneRepository()
                    .setURI(gitRepoUrl)
                    .setDirectory(repoDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubUsername, githubToken))
                    .call();

            return repoDir;

        } catch (IOException | GitAPIException e) {
            logger.error("Error while cloning repository: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void deleteAndCreateDirectory(File directory) throws IOException {
        if (directory.exists()) {
            Files.walk(directory.toPath()).map(Path::toFile).sorted((o1, o2) -> -o1.compareTo(o2)) // sort in reverse order to delete files before directories
                    .forEach(File::delete);
            logger.info("Deleted directory: {}", directory.getAbsolutePath());
        }
        if (!directory.mkdir()) {
            throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }
    }


    private List<AppInfo> processYamlFiles(File repoDir, String gitPath, List<String> resourcePaths) {
        List<AppInfo> resourceInfos = new ArrayList<>();
        File dir = new File(repoDir, gitPath);
        File[] yamlFiles = dir.listFiles((d, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));

        if (yamlFiles != null) {
            for (File file : yamlFiles) {
                try (InputStream in = new FileInputStream(file)) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(in);
                    AppInfo appInfo = extractResourceInfo(file.getName(), data, resourcePaths);
                    resourceInfos.add(appInfo);
                } catch (FileNotFoundException e) {
                    logger.error("YAML file not found: {}", file.getName(), e);
                } catch (Exception e) {
                    logger.error("Error processing YAML file: {}", file.getName(), e);
                }
            }
        } else {
            logger.warn("No YAML files found in the directory: {}", dir.getAbsolutePath());
        }
        return resourceInfos;
    }

    private AppInfo extractResourceInfo(String filename, Map<String, Object> data, List<String> resourcePaths) {
        float totalCpu = 0.0f;
        float totalMemoryGi = 0.0f;

        for (String path : resourcePaths) {
            Map<String, Object> nestedData = getNestedMap(data, path);
            if (nestedData.containsKey("cpu")) {
                Object cpuObj = nestedData.get("cpu");
                float cpu;
                if (cpuObj instanceof String) {
                    String cpuStr = (String) cpuObj;
                    cpu = convertCpuStringToCores(cpuStr);
                } else {
                    cpu = (Integer) cpuObj;
                }

                totalCpu += cpu;
            }
            if (nestedData.containsKey("memory")) {
                String memoryStr = (String) nestedData.get("memory");
                totalMemoryGi += convertMemoryStringToGi(memoryStr);
            }
        }

        return new AppInfo(filename, totalCpu, totalMemoryGi);
    }

    private float convertCpuStringToCores(String cpuStr) {
        if (cpuStr.endsWith("m")) {
            return Float.parseFloat(cpuStr.substring(0, cpuStr.length() - 1)) / 1000;
        } else {
            return Float.parseFloat(cpuStr);
        }
    }

    private float convertMemoryStringToGi(String memoryStr) {
        if (memoryStr.endsWith("Mi")) {
            return Float.parseFloat(memoryStr.substring(0, memoryStr.length() - 2)) / 1024;
        } else if (memoryStr.endsWith("Gi")) {
            return Float.parseFloat(memoryStr.substring(0, memoryStr.length() - 2));
        }
        throw new NumberFormatException("could not convert " + memoryStr);
    }

    private Map<String, Object> getNestedMap(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> currentMap = data;
        for (String part : parts) {
            Object value = currentMap.get(part);
            if (value instanceof Map) {
                currentMap = (Map<String, Object>) value;
            } else {
                // Path does not resolve to a Map, return an empty map
                return Collections.emptyMap();
            }
        }
        return currentMap;
    }


    public void divideResourcesIntoBucketsAndAssignLabels(ResponsePayload response) {
        float cpuLimitCores = response.getCpuLimitCores();
        float memoryLimitGi = response.getMemoryLimitGi();

        List<Bucket> buckets = new ArrayList<>();
        for (AppInfo resource : response.getResources()) {
            for (int i = 0; ; i++) {
                if (i > buckets.size() - 1) {
                    buckets.add(new Bucket());
                }

                Bucket bucket = buckets.get(i);

                if (bucket.tryAddResource(resource, memoryLimitGi, cpuLimitCores)) {
                    resource.setSyncWaveBucketLabel(i);
                    break;
                }
            }
        }
    }

    public class Bucket {
        private int totalMemoryGi = 0;
        private int totalCpuCores = 0;

        public boolean tryAddResource(AppInfo resource, float memoryLimitGi, float cpuLimitCores) {
            if (((memoryLimitGi == 0) || ((totalMemoryGi + resource.getMemoryGi() <= memoryLimitGi)) &&
                    ((cpuLimitCores == 0) || (totalCpuCores + resource.getCpuCores() <= cpuLimitCores)))) {
                totalMemoryGi += resource.getMemoryGi();
                totalCpuCores += resource.getCpuCores();
                return true;
            } else {
                return false;
            }
        }
    }
}