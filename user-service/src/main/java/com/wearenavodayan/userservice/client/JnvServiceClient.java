package com.wearenavodayan.userservice.client;

import com.wearenavodayan.userservice.dto.response.ApiResponse;
import com.wearenavodayan.userservice.exception.JnvServiceClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JnvServiceClient {

    private final RestClient restClient;
    private final String baseUrl;

    public JnvServiceClient(
            RestClient restClient,
            @Value("${services.jnv.base-url}") String baseUrl) {

        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public ApiResponse<Object> getJnvById(String jnvId) {

        try {
            return restClient
                    .get()
                    .uri(baseUrl + "/api/v1/jnvs/" + jnvId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new JnvServiceClientException(
                                        "JNV service returned client error: "
                                                + response.getStatusCode());
                            })
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new JnvServiceClientException(
                                        "JNV service returned server error: "
                                                + response.getStatusCode());
                            })
                    .body(ApiResponse.class);

        } catch (JnvServiceClientException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new JnvServiceClientException(
                    "Unable to communicate with JNV service", ex);
        }
    }
}