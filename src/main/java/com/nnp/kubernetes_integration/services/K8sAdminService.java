package com.nnp.kubernetes_integration.services;

import com.nnp.kubernetes_integration.configs.K8sIntgClientFactory;
import com.nnp.kubernetes_integration.dtos.AllK8sPodsResponse;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class K8sAdminService {

    private final K8sIntgClientFactory k8sIntgClientFactory;

    @Autowired
    public K8sAdminService(K8sIntgClientFactory k8sIntgClientFactory) {
        this.k8sIntgClientFactory = k8sIntgClientFactory;
    }

    public AllK8sPodsResponse getAllK8sPods() {
        KubernetesClient masterClient = k8sIntgClientFactory.getK8sMasterClient();
        K8sIntgQuerySvc k8sIntgQuerySvc = new K8sIntgQuerySvc(masterClient);
        List<Pod> pods = k8sIntgQuerySvc.getAllPods();

        List<AllK8sPodsResponse.K8sPod> k8sPods = pods.stream()
                .map(p -> new AllK8sPodsResponse.K8sPod(p.getMetadata().getName(), p.getMetadata().getNamespace()))
                .toList();

        return new AllK8sPodsResponse(k8sPods);
    }
}
