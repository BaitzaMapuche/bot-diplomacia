package com.example.botdiplomacia.diplomacia;

import com.example.botdiplomacia.config.DiplomaciaProperties;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class DiplomaciaApiClient {
    private static final Logger log = LoggerFactory.getLogger(DiplomaciaApiClient.class);

    private final RestTemplate restTemplate;
    private final DiplomaciaProperties properties;

    public DiplomaciaApiClient(RestTemplate restTemplate, DiplomaciaProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public UpgradeResult upgradeSkill(String sessionToken, String skillCode, String costType) {
        String url = properties.getApiBaseUrl() + properties.getUpgradePath();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sessionToken);

        Map<String, Object> body = new HashMap<>();
        body.put("skill", skillCode);
        body.put("type", costType);

        try {
            UpgradeResponse response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), UpgradeResponse.class);
            if (response == null) {
                return UpgradeResult.failure("Respuesta vacia de la API");
            }
            if (!response.isSuccess()) {
                String reason = response.getError() != null ? response.getError() : response.getMessage();
                return UpgradeResult.failure(reason != null ? reason : "La API respondio success=false");
            }
            return UpgradeResult.success(response.getCooldownMs(), response.getCurrentLevel(), response.getTargetLevel(), response.getCost());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return UpgradeResult.authExpired();
            }
            log.warn("Error de cliente al subir skill {}: {} {}", skillCode, e.getStatusCode(), e.getResponseBodyAsString());
            return UpgradeResult.failure("Error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error inesperado subiendo skill {}", skillCode, e);
            return UpgradeResult.failure("Error inesperado: " + e.getMessage());
        }
    }
}
