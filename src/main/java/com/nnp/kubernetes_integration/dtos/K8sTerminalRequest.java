package com.nnp.kubernetes_integration.dtos;

import jakarta.validation.constraints.NotBlank;

public record K8sTerminalRequest(

        @NotBlank
        String envName,

        @NotBlank
        String pod,

        String container
) {
}
