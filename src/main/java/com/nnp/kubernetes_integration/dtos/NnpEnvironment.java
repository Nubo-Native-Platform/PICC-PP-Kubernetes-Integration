package com.nnp.kubernetes_integration.dtos;

public record NnpEnvironment(
        String envId,
        String envCode,
        String envName,
        String envTypeId,
        String envCustId,
        String envCustName,
        String envDesc,
        String envTenantId,
        String envFapId,
        String envFatNo,
        String envEmail,
        String envStatus,
        String envNamespace,
        String envDomain,
        String envRepo,
        String envIp,
        String adminK8sNsToken,
        String userK8sNsToken
) {
}
