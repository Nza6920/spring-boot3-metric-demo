package com.niu.demo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class MetricsHelper {

    private final MeterRegistry registry;

    public MetricsHelper(MeterRegistry registry) {
        this.registry = registry;
    }

    public <E> E observe(String provider, String endpoint, Supplier<MetricsWrapper<E>> supplier) {
        Timer.Sample sample = Timer.start(registry);
        try {
            MetricsWrapper<E> data = supplier.get();
            boolean error = data.isError();
            // 计数：ok/error
            Counter.builder("thirdparty.response.count")
                    .description("Count of third-party responses by status")
                    .tags("provider", provider, "endpoint", endpoint, "status", error ? "error" : "ok")
                    .register(registry)
                    .increment();
            sample.stop(timer(provider, endpoint, error ? "error" : "ok"));
            return data.getBody();
        } catch (Exception ex) {
            // 计数：error
            Counter.builder("thirdparty.response.count")
                    .description("Count of third-party responses by status")
                    .tags("provider", provider, "endpoint", endpoint, "status", "error")
                    .register(registry)
                    .increment();
            sample.stop(timer(provider, endpoint, "error"));
            throw ex;
        }
    }

    private Timer timer(String provider, String endpoint, String status) {
        return Timer.builder("thirdparty.response.latency")
                .description("Latency of third-party API")
                .tags("provider", provider, "endpoint", endpoint, "status", status)
                // 开启直方图，便于 PromQL 用 histogram_quantile
                .publishPercentileHistogram(true)
                // 显式加 10s 边界，产出 le="10" 的 bucket 方便阈值统计
                .serviceLevelObjectives(Duration.ofSeconds(10))
                .register(registry);
    }
}

