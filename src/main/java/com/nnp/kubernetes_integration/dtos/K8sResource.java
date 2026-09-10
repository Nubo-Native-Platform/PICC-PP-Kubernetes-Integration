package com.nnp.kubernetes_integration.dtos;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;

public record K8sResource(
        String resourceName,
        String resourceKind,
        boolean willBeForceDeleted
){

    public K8sResource(Pod pod){
        this(pod.getMetadata().getName(), pod.getKind(), true);
    }

    public K8sResource(ReplicaSet rs){
        this(rs.getMetadata().getName(), rs.getKind(), true);
    }

    public K8sResource(Service service){
        this(service.getMetadata().getName(), service.getKind(), false);
    }
}