package com.wearenavodayan.authservice.client;

import com.wearenavodayan.authservice.dto.internal.InternalProfileCreateRequest;
import com.wearenavodayan.authservice.dto.internal.InternalProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String internalApiKey;

    public UserServiceClient(
            RestClient.Builder builder,
            @Value("${services.user.base-url}") String baseUrl,
            @Value("${services.internal.api-key}") String internalApiKey) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
        this.internalApiKey = internalApiKey;
    }

    public InternalProfileResponse createProfile(
            InternalProfileCreateRequest request) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/internal/api/v1/user-profiles")
                    .header("X-Internal-Service-Key", internalApiKey)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new UserServiceClientException(
                                "User Service rejected profile creation with HTTP "
                                        + httpResponse.getStatusCode().value());
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new UserServiceClientException(
                        "User Service returned an invalid profile response");
            }

            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> profile)) {
                throw new UserServiceClientException(
                        "User Service returned no profile data");
            }

            return new InternalProfileResponse(
                    java.util.UUID.fromString(profile.get("id").toString()),
                    java.util.UUID.fromString(profile.get("authUserId").toString()),
                    profile.get("fullName").toString(),
                    java.time.Instant.parse(profile.get("createdAt").toString()));
        } catch (UserServiceClientException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new UserServiceClientException(
                    "Unable to create profile through User Service", exception);
        }
    }
}