package com.nnp.kubernetes_integration.controllers;

import com.nnp.kubernetes_integration.dtos.AllK8sPodsResponse;
import com.nnp.kubernetes_integration.services.K8sAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative endpoints for cluster-wide introspection.
 */
@RestController
@RequestMapping("admin")
@Tag(name = "Kubernetes Admin Controller", description = "Cluster-wide administrative queries across all namespaces")
public class NnpK8sAdminController {

    private final K8sAdminService k8sAdminService;

    @Autowired
    public NnpK8sAdminController(K8sAdminService k8sAdminService) {
        this.k8sAdminService = k8sAdminService;
    }

    @Operation(summary = "Get All Cluster Pods", description = "Retrieves a cluster-wide list of all pods across every namespace using master client credentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pod list successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Failed to query master cluster API")
    })
    @GetMapping("pods")
    public AllK8sPodsResponse getAllPods() {
        return k8sAdminService.getAllK8sPods();
    }

}
