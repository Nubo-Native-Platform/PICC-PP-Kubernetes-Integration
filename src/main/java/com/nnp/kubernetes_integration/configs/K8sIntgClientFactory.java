package com.nnp.kubernetes_integration.configs;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class K8sIntgClientFactory {

    @Value("${k8s.master.token:}")
    private String k8sMasterToken;

    @Value("${k8s.url:https://kubernetes.default.svc}")
    private String k8sUrl;

    public KubernetesClient getK8sClient(String k8sToken){
        Config config = new ConfigBuilder()
                .withMasterUrl(k8sUrl)
                .withOauthToken(k8sToken)
                .withTrustCerts(true)
                .build();

        return new KubernetesClientBuilder()
                .withConfig(config)
                .build();
    }

    public KubernetesClient getK8sMasterClient(){
        Config config = new ConfigBuilder()
                .withMasterUrl(k8sUrl)
                .withOauthToken(k8sMasterToken)
                .withTrustCerts(true)
                .build();

        return new KubernetesClientBuilder()
                .withConfig(config)
                .build();
    }
}
