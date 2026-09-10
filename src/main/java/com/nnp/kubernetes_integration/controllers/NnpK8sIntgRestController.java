package com.nnp.kubernetes_integration.controllers;

import com.nnp.kubernetes_integration.dtos.K8sDeleteResourcesRequest;
import com.nnp.kubernetes_integration.dtos.K8sDeploymentAssociatedResourcesResponse;
import com.nnp.kubernetes_integration.dtos.NnpPodMetricsResponse;
import com.nnp.kubernetes_integration.services.NnpK8sSvc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("")
@Tag(name = "Kubernetes Integration REST Controller", description = "Core APIs for pod metrics, deployment resource introspection, and lifecycle teardowns")
public class NnpK8sIntgRestController {

    private final NnpK8sSvc nnpK8sSvc;

    @Autowired
    public NnpK8sIntgRestController(NnpK8sSvc nnpK8sSvc) {
        this.nnpK8sSvc = nnpK8sSvc;
    }

    @Operation(summary = "Get Pod Metrics For Namespace", description = "Fetches real-time CPU, memory, and status metrics for all pods running inside the environment namespace")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pod metrics successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Failed to communicate with cluster or environment missing")
    })
    @GetMapping("pods/details")
    public NnpPodMetricsResponse getPodMetricsForNamespace(
            @Parameter(description = "Environment identifier name", required = true) @RequestParam String envName
    ) {
        return nnpK8sSvc.getPodMetricsForNamespace(envName);
    }

    @Operation(summary = "Get Deployment Associated Resources", description = "Discovers all Kubernetes resources (Pods, ReplicaSets, Services, ConfigMaps, Secrets, PVCs, Ingresses) bound to a specific deployment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Associated resources successfully cataloged"),
            @ApiResponse(responseCode = "500", description = "Error querying cluster resources")
    })
    @GetMapping("associated/deployment/resources")
    public K8sDeploymentAssociatedResourcesResponse getDeploymentAssociatedResources(
            @Parameter(description = "Environment identifier name", required = true) @RequestParam String envName,
            @Parameter(description = "Kubernetes Deployment name", required = true) @RequestParam String deploymentName
    ) {
        return nnpK8sSvc.getDeploymentAssociatedResources(envName, deploymentName);
    }

    @Operation(summary = "Delete Pod", description = "Terminates a specific Kubernetes pod by name in the designated environment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pod deletion command completed"),
            @ApiResponse(responseCode = "500", description = "Error executing pod deletion")
    })
    @DeleteMapping("pod")
    public Map<String, Object> deletePod(
            @Parameter(description = "Environment identifier name", required = true) @RequestParam String envName,
            @Parameter(description = "Target Pod name to delete", required = true) @RequestParam String podName
    ) {
        boolean ifPodDeleted = nnpK8sSvc.deletePod(envName, podName);
        return Map.of(
                "couldDelete", ifPodDeleted,
                "podName", podName
        );
    }

    @Operation(summary = "Delete Deployment And Associated Resources", description = "Initiates batch deletion of a deployment and its associated secrets, configmaps, services, PVCs, and ingresses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Teardown initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid resource deletion payload")
    })
    @PostMapping("delete/resources")
    public Map<String, Object> deleteDeploymentAndAssociatedResources(
            @RequestBody @Valid K8sDeleteResourcesRequest k8sDeleteResourcesRequest
    ) {
        nnpK8sSvc.deleteDeploymentAndAssociatedResources(k8sDeleteResourcesRequest);
        return Map.of(
                "message", "Deleting Process Has Begun"
        );
    }

}
