package com.smartcms.smartcontent.client;

import com.smartcms.smartcontent.exception.ApiException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class ApiInvoker {
    private final RestClient restClient;
    private final HttpServiceProxyFactory proxyFactory;
    private final ExecutorService executorService;

    @Builder
    public ApiInvoker(String baseUrl, Duration timeout) {
        this.executorService = Executors.newFixedThreadPool(10); // Configurable thread pool size
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                    String errorBody = res.getBody() != null ? res.getBody().toString() : "No error details";
                    throw new ApiException("API call failed", res.getStatusCode(), errorBody);
                })
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().add("X-SmartCMS-Request-ID", UUID.randomUUID().toString());
                    return execution.execute(request, body);
                })
                .build();
        this.proxyFactory = HttpServiceProxyFactory.builderFor(
                        RestClientAdapter.create(restClient))
                .build();
    }

    /**
     * Java 17 compatible parallel execution using CompletableFuture
     */
    public <T, U> Pair<T, U> fetchParallel(Supplier<T> task1, Supplier<U> task2) {
        CompletableFuture<T> future1 = CompletableFuture.supplyAsync(task1, executorService);
        CompletableFuture<U> future2 = CompletableFuture.supplyAsync(task2, executorService);

        try {
            return Pair.of(
                    future1.get(5, TimeUnit.SECONDS), // Configurable timeout
                    future2.get(5, TimeUnit.SECONDS)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Parallel fetch interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (TimeoutException e) {
            throw new ApiException("Parallel fetch timed out", HttpStatus.REQUEST_TIMEOUT);
        } catch (ExecutionException e) {
            throw new ApiException("Parallel fetch failed: " + e.getCause().getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Value
    @Builder
    public static class ApiRequest {
        String path;
        @NonNull HttpMethod method;
        @With Object body;
        @Singular Map<String, String> headers;
    }

    public <T> T execute(@NonNull ApiRequest request, Class<T> responseType) {
        log.debug("Executing {} request to {}", request.getMethod(), request.getPath());

        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            request.getHeaders().forEach(headers::add);

            return restClient.method(request.getMethod())
                    .uri(request.getPath())
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(request.getBody())
                    .retrieve()
                    .body(responseType);
        } catch (ApiException e) {
            log.error("API Error ({}): {} - {}", e.getStatusCode(), request.getPath(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error: {} {}", request.getPath(), e.getMessage());
            throw new ApiException("Request failed: " + request.getPath(),
                    HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public <T> T createClient(@NonNull Class<T> serviceType) {
        return proxyFactory.createClient(serviceType);
    }

    @Value
    public static class Pair<T, U> {
        T first;
        U second;

        public static <T, U> Pair<T, U> of(T first, U second) {
            return new Pair<>(first, second);
        }
    }
}