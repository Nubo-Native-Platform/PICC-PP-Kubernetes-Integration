package com.nnp.kubernetes_integration.dtos;

import java.util.List;

public record K8sDeploymentAssociatedResourcesResponse(
        String deploymentName,
        List<K8sResource> pods,
        List<K8sResource> replicaSets,
        List<K8sResource> services,
        List<K8sResource> configMaps,
        List<K8sResource> secrets,
        List<K8sResource> pvcs

) {
}
