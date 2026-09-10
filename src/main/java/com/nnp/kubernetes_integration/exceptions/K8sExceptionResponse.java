package com.nnp.kubernetes_integration.exceptions;

public record K8sExceptionResponse(
        String message,
        int code
) {
}
