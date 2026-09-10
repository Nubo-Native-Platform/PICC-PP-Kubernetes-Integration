package com.nnp.kubernetes_integration.services;

import com.nnp.kubernetes_integration.dtos.NnpEnvironment;
import com.nnp.kubernetes_integration.exceptions.K8sIntgException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NnpService {

    @Value("${nnp.dash.baseurl:http://localhost:8082}")
    private String nnpDashBaseUrl;

    @Cacheable(cacheNames = "nnp-environment")
    public Map<String, NnpEnvironment> getEnvironmentMap() {
        RestClient restClient = RestClient.create();
        var response = restClient.get()
                .uri(nnpDashBaseUrl + "/env/read/v3")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<NnpEnvironment>>() {});

        if (!response.getStatusCode().is2xxSuccessful())
            throw new K8sIntgException("Couldn't Fetch All Environments", HttpStatus.INTERNAL_SERVER_ERROR);

        List<NnpEnvironment> nnpEnvironments = response.getBody();

        if (CollectionUtils.isEmpty(nnpEnvironments))
            throw new K8sIntgException("No Environments Available In The System (Please Add Environments)", HttpStatus.INTERNAL_SERVER_ERROR);

        log.info("Successfully Fetched All Environment From nnp-portal-backend / nnp-dashboard-service");
        return nnpEnvironments.stream()
                .collect(Collectors.toMap(NnpEnvironment::envName, Function.identity()));
    }

    @CacheEvict(cacheNames = "nnp-environment")
    public void clearAllEnvironmentsCache(){
        // log.info("Clearing Cache For All Environments");
    }

}