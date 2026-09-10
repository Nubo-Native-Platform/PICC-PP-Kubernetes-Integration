package com.nnp.kubernetes_integration.dtos;

import java.math.BigDecimal;
import java.util.List;

public record NnpPodMetricsResponse(
    int count,
    List<NnpPodMetric> podMetric
) {
    public record NnpPodMetric(
            String podName,
            String podStatus,
            String deploymentName,
            String namespace,
            NnpUsage memoryConsumed,
            NnpUsage cpuConsumed,
            List<K8sService> services
    ) {}

    public record NnpUsage(
            BigDecimal value,
            String unit
    ){}
    public record K8sService(
            String serviceName,
            List<String> endPoints
    ){ }
}
