package com.nnp.kubernetes_integration.dtos;


import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record K8sDeleteResourcesRequest(
        @NotBlank
        String envName,

        List<String> deploymentNames,

        List<String> serviceNames,

        List<String> configMapNames,

        List<String> secretsNames,

        List<String> PVCNames
) {
}
