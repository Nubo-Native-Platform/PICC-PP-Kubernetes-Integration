package com.nnp.kubernetes_integration.services;

import com.nnp.kubernetes_integration.configs.K8sIntgClientFactory;
import com.nnp.kubernetes_integration.dtos.K8sDeleteResourcesRequest;
import com.nnp.kubernetes_integration.dtos.K8sDeploymentAssociatedResourcesResponse;
import com.nnp.kubernetes_integration.dtos.NnpEnvironment;
import com.nnp.kubernetes_integration.dtos.NnpPodMetricsResponse;
import com.nnp.kubernetes_integration.exceptions.K8sIntgException;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NnpK8sSvc {

    private final K8sIntgClientFactory k8sIntgClientFactory;
    private final NnpService nnpService;

    public NnpK8sSvc(K8sIntgClientFactory k8sIntgClientFactory, NnpService nnpService) {
        this.k8sIntgClientFactory = k8sIntgClientFactory;
        this.nnpService = nnpService;
    }

    public NnpPodMetricsResponse getPodMetricsForNamespace(String envName) {
        NnpEnvironment nnpEnvironment = getNnpEnvironment(envName);
        KubernetesClient k8sClient = k8sIntgClientFactory.getK8sClient(nnpEnvironment.adminK8sNsToken());
        K8sIntgQuerySvc k8sIntgQuerySvc = new K8sIntgQuerySvc(k8sClient);
        return k8sIntgQuerySvc.getPodMetricsForNamespace(nnpEnvironment.envNamespace());
    }

    public K8sDeploymentAssociatedResourcesResponse getDeploymentAssociatedResources(String envName, String deploymentName) {
        NnpEnvironment nnpEnvironment = getNnpEnvironment(envName);
        KubernetesClient k8sClient = k8sIntgClientFactory.getK8sClient(nnpEnvironment.adminK8sNsToken());
        K8sIntgQuerySvc k8sIntgQuerySvc = new K8sIntgQuerySvc(k8sClient);
        return k8sIntgQuerySvc.getDeploymentAssociatedResources(nnpEnvironment.envNamespace(), deploymentName);
    }

    public boolean deletePod(String envName, String podName) {
        NnpEnvironment nnpEnvironment = getNnpEnvironment(envName);
        KubernetesClient k8sClient = k8sIntgClientFactory.getK8sClient(nnpEnvironment.adminK8sNsToken());
        K8sIntgCommandSvc k8sIntgCommandSvc = new K8sIntgCommandSvc(k8sClient);
        return k8sIntgCommandSvc.deleteAPod(nnpEnvironment.envNamespace(), podName);
    }

    public void deleteDeploymentAndAssociatedResources(K8sDeleteResourcesRequest k8sDeleteResourcesRequest) {
        NnpEnvironment nnpEnvironment = getNnpEnvironment(k8sDeleteResourcesRequest.envName());
        KubernetesClient k8sClient = k8sIntgClientFactory.getK8sClient(nnpEnvironment.adminK8sNsToken());
        K8sIntgCommandSvc k8sIntgCommandSvc = new K8sIntgCommandSvc(k8sClient);
        k8sIntgCommandSvc.deleteK8sResources(k8sDeleteResourcesRequest, nnpEnvironment.envNamespace());
    }

    public NnpEnvironment getNnpEnvironment(String envName){
        Map<String, NnpEnvironment> environmentMap = nnpService.getEnvironmentMap();
        if (!environmentMap.containsKey(envName))
            throw new K8sIntgException("This Environment Does Not Exist In Cache", HttpStatus.INTERNAL_SERVER_ERROR);

        return environmentMap.get(envName);
    }
}
