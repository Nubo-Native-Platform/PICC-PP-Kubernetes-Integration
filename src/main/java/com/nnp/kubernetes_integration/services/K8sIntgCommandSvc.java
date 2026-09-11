package com.nnp.kubernetes_integration.services;

import com.nnp.kubernetes_integration.dtos.K8sDeleteResourcesRequest;
import com.nnp.kubernetes_integration.exceptions.K8sIntgException;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class K8sIntgCommandSvc {

    private final KubernetesClient k8sClient;

    public K8sIntgCommandSvc(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    public void deleteK8sResources(K8sDeleteResourcesRequest k8sDeleteResourcesRequest, String namespace) {

        k8sDeleteResourcesRequest.deploymentNames()
                .forEach(d -> {
                    boolean ifDeploymentDeleted = deleteDeployment(namespace, d);
                    if (!ifDeploymentDeleted)
                        throw new K8sIntgException(d + " : Deployment Was Not Deleted", HttpStatus.BAD_REQUEST);
                });

        k8sDeleteResourcesRequest.serviceNames().forEach(sn -> deleteService(namespace, sn));
        k8sDeleteResourcesRequest.configMapNames().forEach(cm -> deleteConfigMap(namespace, cm));
        k8sDeleteResourcesRequest.secretsNames().forEach(s -> deleteSecrets(namespace, s));
        k8sDeleteResourcesRequest.PVCNames().forEach(pvc -> deletePVC(namespace, pvc));
    }

    private void logDeleteResourceStatus(List<StatusDetails> statusDetails) {
        statusDetails.forEach(sd -> {
            // log.info(
            //         "Delete Resource Group : {}, Kind : {}, Name : {}, Causes : {}",
            //         sd.getGroup(), sd.getKind(), sd.getName(),
            //         sd.getCauses().stream().map(StatusCause::getMessage).collect(Collectors.joining(" ,"))
            // );
        });
    }

    // Also Restarts A Pod
    public boolean deleteAPod(String namespace, String podName) {
        // log.info("Deleting Pod From Namespace : {} With Pod Name : {}", namespace, podName);
        List<StatusDetails> deletedPods = k8sClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .delete();
        logDeleteResourceStatus(deletedPods);
        return !CollectionUtils.isEmpty(deletedPods);
    }

    private boolean deleteDeployment(String namespace, String deploymentName) {
        // log.info("Deleting Deployment From Namespace : {} With Deployment Name : {}", namespace, deploymentName);
        List<StatusDetails> deletedDeployment = k8sClient.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .withPropagationPolicy(DeletionPropagation.FOREGROUND)
                .delete();
        logDeleteResourceStatus(deletedDeployment);
        return !CollectionUtils.isEmpty(deletedDeployment);
    }

    private boolean deleteService(String namespace, String serviceName) {
        // log.info("Deleting Service From Namespace : {} With Service Name : {}", namespace, serviceName);
        List<StatusDetails> deletedServices = k8sClient.services()
                .inNamespace(namespace)
                .withName(serviceName)
                .delete();
        logDeleteResourceStatus(deletedServices);
        return !CollectionUtils.isEmpty(deletedServices);
    }

    private boolean deleteConfigMap(String namespace, String configMapName) {
        // log.info("Deleting ConfigMap From Namespace : {} With ConfigMap Name : {}", namespace, configMapName);
        List<StatusDetails> deletedConfigMaps = k8sClient.configMaps()
                .inNamespace(namespace)
                .withName(configMapName)
                .delete();
        logDeleteResourceStatus(deletedConfigMaps);
        return !CollectionUtils.isEmpty(deletedConfigMaps);
    }

    private boolean deleteSecrets(String namespace, String secretName) {
        // log.info("Deleting Secret From Namespace : {} With Secret Name : {}", namespace, secretName);
        List<StatusDetails> deletedSecrets = k8sClient.secrets()
                .inNamespace(namespace)
                .withName(secretName)
                .delete();
        logDeleteResourceStatus(deletedSecrets);
        return !CollectionUtils.isEmpty(deletedSecrets);
    }

    private boolean deletePVC(String namespace, String pvcName) {
        // log.info("Deleting PVC From Namespace : {} With PVC Name : {}", namespace, pvcName);
        List<StatusDetails> deletedPVC = k8sClient.persistentVolumeClaims()
                .inNamespace(namespace)
                .withName(pvcName)
                .delete();
        logDeleteResourceStatus(deletedPVC);
        return !CollectionUtils.isEmpty(deletedPVC);
    }
}
