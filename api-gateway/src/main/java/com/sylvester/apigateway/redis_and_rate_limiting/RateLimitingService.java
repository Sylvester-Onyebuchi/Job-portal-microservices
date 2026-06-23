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


    private final ProxyManager<String> proxyManager;


    public Bucket resolveBucket(String key, String path) {

        return proxyManager
                .builder()
                .build(key, () -> getConfig(path));
    }


    private BucketConfiguration getConfig(String path){
        if (path.startsWith("/api/v1/auth/public/")) {
            return publicConfig();
        }

        return defaultConfig();
    }

    private BucketConfiguration publicConfig(){

        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }


    private BucketConfiguration defaultConfig(){

        Bandwidth limit = Bandwidth.builder()
                .capacity(40)
                .refillIntervally(40, Duration.ofMinutes(1))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

}
