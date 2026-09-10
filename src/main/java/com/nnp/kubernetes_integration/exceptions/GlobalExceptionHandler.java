package com.nnp.kubernetes_integration.exceptions;

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({K8sIntgException.class})
    public ResponseEntity<Object> handleK8sIntgException(K8sIntgException ex) {

        HttpStatus httpStatus =
                Objects.isNull(ex.getStatusCode()) ? HttpStatus.INTERNAL_SERVER_ERROR : ex.getStatusCode();

        return ResponseEntity
                .status(httpStatus)
                .body(new K8sExceptionResponse(ex.getMessage(), ex.getStatusCode().value()));
    }

    @ExceptionHandler({KubernetesClientException.class})
    public ResponseEntity<Object> handleK8sClientException(KubernetesClientException ex) {

        HttpStatus httpStatus = HttpStatus.resolve(ex.getCode());
        if (Objects.isNull(httpStatus))
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(httpStatus)
                .body(new K8sExceptionResponse(ex.getMessage(), httpStatus.value()));
    }

    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new K8sExceptionResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

}
