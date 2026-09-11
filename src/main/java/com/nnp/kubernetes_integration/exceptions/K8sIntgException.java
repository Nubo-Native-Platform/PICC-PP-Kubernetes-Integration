package com.nnp.kubernetes_integration.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sIntgException extends RuntimeException{

    private String message;
    private HttpStatus statusCode;

    public K8sIntgException(String message) {
        super(message);
        this.message = message;
        this.statusCode = null;
    }

    public K8sIntgException(String message, HttpStatus statusCode) {
        super(message);
        this.message = message;
        this.statusCode = statusCode;
    }

    public K8sIntgException(String message, HttpStatus statusCode, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.statusCode = statusCode;
    }

}
