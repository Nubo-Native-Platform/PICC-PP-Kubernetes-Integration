package com.nnp.kubernetes_integration.services;

import com.nnp.kubernetes_integration.dtos.K8sDeploymentAssociatedResourcesResponse;
import com.nnp.kubernetes_integration.dtos.K8sResource;
import com.nnp.kubernetes_integration.dtos.K8sResources;
import com.nnp.kubernetes_integration.dtos.NnpPodMetricsResponse;
import com.nnp.kubernetes_integration.exceptions.K8sIntgException;
import com.nnp.kubernetes_integration.utils.K8sConversionUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class K8sIntgQuerySvc {

    private static final String POD_DETAILS_MEMORY_FORMAT = "mb";
    private static final String POD_DETAILS_CPU_FORMAT = "m";

    private final KubernetesClient k8sClient;

    public K8sIntgQuerySvc(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    private BigDecimal totalMemoryConsumedByPod(List<ContainerMetrics> container) {
        return container.stream()
                .map(c -> {
                    BigDecimal value = BigDecimal.valueOf(Double.parseDouble(c.getUsage().get("memory").getAmount()));
                    String format = c.getUsage().get("memory").getFormat();
                    return K8sConversionUtils.parseMemoryToBytes(value, format)
                            .setScale(0, RoundingMode.HALF_UP);
                })
                .reduce(new BigDecimal(0), BigDecimal::add);
    }

    private BigDecimal totalCpuConsumedByPod(List<ContainerMetrics> container) {
        return container.stream()
                .map(c -> {
                    BigDecimal value = BigDecimal.valueOf(Double.parseDouble(c.getUsage().get("cpu").getAmount()));
                    String format = c.getUsage().get("cpu").getFormat();
                    return K8sConversionUtils.parseCpuToNanoCores(value, format)
                            .setScale(2, RoundingMode.HALF_UP);
                })
                .reduce(new BigDecimal(0), BigDecimal::add);
    }

    private String getDeploymentName(Pod pod) {
        if (Objects.isNull(pod.getMetadata().getOwnerReferences()))
            return null;

        return pod.getMetadata().getOwnerReferences().stream()
                .filter(owner -> "ReplicaSet".equals(owner.getKind()))
                .findFirst()
                .map(owner -> owner.getName().replaceFirst("-[^-]+$", ""))
                .orElse(null);
    }

    public NnpPodMetricsResponse getPodMetricsForNamespace(String namespace) {

        List<Pod> pods;
        List<PodMetrics> podMetricsList;
        List<Service> services;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()){
            Future<PodList> podsFuture = executor.submit(() -> k8sClient.pods().inNamespace(namespace).list());
            Future<PodMetricsList> podMetricsListFuture =
                    executor.submit(() -> k8sClient.top().pods().inNamespace(namespace).metrics());
            Future<ServiceList> servicesFuture =
                    executor.submit(() -> k8sClient.services().inNamespace(namespace).list());

            pods = podsFuture.get().getItems();
            podMetricsList = podMetricsListFuture.get().getItems();
            services = servicesFuture.get().getItems();

        } catch (ExecutionException | InterruptedException e) {
            throw new K8sIntgException(
                    "Error Trying Fetch Pods|PodMetics|Services Concurrently \nError Message: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Map<String, Pod> podIdentifierPodMapping = pods.stream()
                .collect(Collectors.toMap(
                                pod -> pod.getMetadata().getName() + "#" + pod.getMetadata().getNamespace(),
                                Function.identity()
                ));

        List<NnpPodMetricsResponse.NnpPodMetric> podMetrics = podMetricsList.stream()
                .map(pm -> {
                    Pod pod = podIdentifierPodMapping.get(pm.getMetadata().getName() + "#" + pm.getMetadata().getNamespace());
                    List<ContainerMetrics> containers = pm.getContainers();
                    List<Service> podServices = getServices(pod.getMetadata().getLabels(), services);
                    BigDecimal bytesConsumed = totalMemoryConsumedByPod(containers);
                    BigDecimal nanoCoresConsumed = totalCpuConsumedByPod(containers);
                    return new NnpPodMetricsResponse.NnpPodMetric(
                            pm.getMetadata().getName(),
                            pod.getStatus().getPhase(),
                            getDeploymentName(pod),
                            pm.getMetadata().getNamespace(),
                            new NnpPodMetricsResponse.NnpUsage(K8sConversionUtils.convertBytesToFormat(bytesConsumed, POD_DETAILS_MEMORY_FORMAT), POD_DETAILS_MEMORY_FORMAT),
                            new NnpPodMetricsResponse.NnpUsage(K8sConversionUtils.convertNanoCoresToFormat(nanoCoresConsumed, POD_DETAILS_CPU_FORMAT), POD_DETAILS_CPU_FORMAT),
                            getNnpK8SServices(podServices)
                    );
                })
                .toList();

        return new NnpPodMetricsResponse(podMetrics.size(), podMetrics);
    }

    private List<NnpPodMetricsResponse.K8sService> getNnpK8SServices(List<Service> services){

        if (CollectionUtils.isEmpty(services))
            return new ArrayList<>();

        return services.stream()
                .map(s -> new NnpPodMetricsResponse.K8sService(s.getMetadata().getName(), getInternalEndpoints(s)))
                .toList();
    }

    private List<String> getInternalEndpoints(Service service){
        String serviceName = service.getMetadata().getName();
        String namespace = service.getMetadata().getNamespace();
        return service.getSpec().getPorts()
                .stream()
                .map(p -> String.format("%s.%s:%s",serviceName, namespace, p.getPort())).toList();
    }

    private void ifNamespaceExists(String namespaceName) {
        Namespace namespace = k8sClient.namespaces().withName(namespaceName).get();
        if (Objects.isNull(namespace))
            throw new K8sIntgException("Namespace With Name : " + namespaceName + " Does Not Exist", HttpStatus.BAD_REQUEST);
    }

    public K8sDeploymentAssociatedResourcesResponse getDeploymentAssociatedResources(String namespace, String deploymentName) {

        Deployment deployment = k8sClient.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .get();

        if (Objects.isNull(deployment))
            throw new K8sIntgException("No Deployment Named : " + deploymentName + " Exists", HttpStatus.BAD_REQUEST);

        Map<String, String> labels = deployment.getSpec()
                .getSelector()
                .getMatchLabels();

        K8sResources k8sResources = getK8sResourcesConcurrently(namespace, labels);

        return new K8sDeploymentAssociatedResourcesResponse(
                deploymentName,
                k8sResources.getPods().stream().map(K8sResource::new).toList(),
                k8sResources.getReplicaSets().stream().map(K8sResource::new).toList(),
                k8sResources.getServices().stream().map(K8sResource::new).toList(),
                getConfigMapsFromDeployment(deployment),
                getSecretsFromDeployment(deployment),
                getPVCsFromDeployment(deployment)
        );
    }

    public List<K8sResource> getSecretsFromDeployment(Deployment deployment) {
        PodSpec spec = deployment.getSpec().getTemplate().getSpec();

        // Getting Secrets From Volumes Attached To Deployment
        if (Objects.isNull(spec.getVolumes()))
            return new ArrayList<>();

        return spec.getVolumes().stream()
                .filter(v -> v.getSecret() != null)
                .map(v -> new K8sResource(v.getName(), "Secret", false))
                .toList();
    }

    // Get ConfigMap names
    public List<K8sResource> getConfigMapsFromDeployment(Deployment deployment) {
        PodSpec spec = deployment.getSpec().getTemplate().getSpec();

        if (Objects.isNull(spec.getVolumes()))
            return new ArrayList<>();

        return spec.getVolumes().stream()
                .filter(cm -> cm.getConfigMap() != null)
                .map(cm -> new K8sResource(cm.getConfigMap().getName(), "ConfigMap", false))
                .toList();

    }

    public List<K8sResource> getPVCsFromDeployment(Deployment deployment) {
        PodSpec spec = deployment.getSpec().getTemplate().getSpec();

        if (Objects.isNull(spec.getVolumes()))
            return new ArrayList<>();

        return spec.getVolumes().stream()
                .filter(v -> v.getPersistentVolumeClaim() != null)
                .map(v -> new K8sResource(v.getPersistentVolumeClaim().getClaimName(), "PersistentVolumeClaim", false))
                .toList();
    }


    private K8sResources getK8sResourcesConcurrently(String namespace, Map<String, String> labels) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

            Future<List<Pod>> getPodsFuture = executorService.submit(() -> getPods(namespace, labels));
            Future<List<ReplicaSet>> getReplicaSetFuture = executorService.submit(() -> getReplicaSets(namespace, labels));
            Future<List<io.fabric8.kubernetes.api.model.Service>> getServicesFuture =
                    executorService.submit(() -> getServices(namespace, labels));

            return K8sResources.builder()
                    .pods(getPodsFuture.get())
                    .replicaSets(getReplicaSetFuture.get())
                    .services(getServicesFuture.get())
                    .build();

        } catch (Exception e) {
            throw new K8sIntgException("Some Error While Getting K8s Resources Concurrently", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private List<Pod> getPods(String namespace, Map<String, String> labels) {
        return k8sClient.pods()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems();
    }

    public List<Pod> getAllPods() {
        return k8sClient.pods()
                .inAnyNamespace()
                .list()
                .getItems();
    }

    private List<ReplicaSet> getReplicaSets(String namespace, Map<String, String> labels) {
        return k8sClient.apps().replicaSets()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems();
    }

    private List<PersistentVolumeClaim> getPersistentVolumeClaims(String namespace, Map<String, String> labels) {
        return k8sClient.persistentVolumeClaims()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems();
    }

    private List<Secret> getSecrets(String namespace, Map<String, String> labels) {
        return k8sClient.secrets()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems();
    }

    private List<ConfigMap> getConfigMaps(String namespace, Map<String, String> labels) {
        return k8sClient.configMaps()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems();
    }

    private List<Service> getServices(String namespace, Map<String, String> labels) {
        return k8sClient.services()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems();
    }

    private List<Service> getServices(Map<String, String> superSetLabels, List<Service> services) {
        if (CollectionUtils.isEmpty(services))
            return new ArrayList<>();
        return services.stream()
                .filter(s -> ifMapIsSubset(s.getSpec().getSelector(), superSetLabels))
                .toList();
    }

    private boolean ifMapIsSubset(Map<String, String> subset, Map<String, String> superset){
        for (Map.Entry<String, String> entry : subset.entrySet()){
            boolean ifEntryExistInSuperset =
                    superset.containsKey(entry.getKey()) && superset.get(entry.getKey()).equals(entry.getValue());
            if (!ifEntryExistInSuperset)
                return false;
        }
        return true;
    }


}
