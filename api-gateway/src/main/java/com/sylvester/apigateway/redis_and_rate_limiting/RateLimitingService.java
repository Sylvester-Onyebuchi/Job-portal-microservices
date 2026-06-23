package com.sylvester.apigateway.redis_and_rate_limiting;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitingService {

    private static final int REQUEST_PER_MINUTE = 20;

    private final ProxyManager<String> proxyManager;


    public Bucket resolveBucket(String key) {

        Supplier<BucketConfiguration> configSupplier = this::getConfig;
        return proxyManager
                .builder()
                .build(key, configSupplier);
    }

    private BucketConfiguration getConfig(){
       var limit = Bandwidth.builder()
                .capacity(REQUEST_PER_MINUTE)
                .refillIntervally(REQUEST_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return  BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
