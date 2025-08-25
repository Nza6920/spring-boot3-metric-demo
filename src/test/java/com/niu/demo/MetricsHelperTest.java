package com.niu.demo;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsHelperTest {

    private static final String PROVIDER = "acme";
    private static final String ENDPOINT = "/v1/data";

    private SimpleMeterRegistry registry;
    private MetricsHelper helper;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        helper = new MetricsHelper(registry);
    }

    @Test
    @DisplayName("OK 路径：计数 ok=1，latency(status=ok)=1")
    void okPath_shouldRecordOkCountersAndLatency() {
        var res = helper.observe(PROVIDER, ENDPOINT, () -> new MetricsWrapper<>(List.of("a", "b", "c"), false));
        assertEquals(3, res.size());

        double okCount = registry.get("thirdparty.response.count")
                .tags("provider", PROVIDER, "endpoint", ENDPOINT, "status", "ok")
                .counter().count();
        assertEquals(1.0, okCount, 1e-9);

        long latencyOk = registry.get("thirdparty.response.latency")
                .tags("provider", PROVIDER, "endpoint", ENDPOINT, "status", "ok")
                .timer().count();
        assertEquals(1L, latencyOk);
    }

    @Test
    @DisplayName("EMPTY 路径：计数 error=1，latency(status=empty)=1")
    void emptyPath_shouldRecordErrorCountersAndLatency() {
        var observe = helper.observe(PROVIDER, ENDPOINT, () -> new MetricsWrapper<>(List.of(), true));// 空列表
        assertTrue(observe.isEmpty());

        double emptyCount = registry.get("thirdparty.response.count")
                .tags("provider", PROVIDER, "endpoint", ENDPOINT, "status", "error")
                .counter().count();
        assertEquals(1.0, emptyCount, 1e-9);

        long latencyEmpty = registry.get("thirdparty.response.latency")
                .tags("provider", PROVIDER, "endpoint", ENDPOINT, "status", "error")
                .timer().count();
        assertEquals(1L, latencyEmpty);
    }

    @Test
    @DisplayName("ERROR 路径：抛异常，计数 error=1，latency(status=error)=1")
    void errorPath_shouldRecordErrorCountersAndLatency() {
        assertThrows(RuntimeException.class,
                () -> helper.observe(PROVIDER, ENDPOINT, () -> {
                    throw new RuntimeException("boom");
                }));

        double errCount = registry.get("thirdparty.response.count")
                .tags("provider", PROVIDER, "endpoint", ENDPOINT, "status", "error")
                .counter().count();
        assertEquals(1.0, errCount, 1e-9);

        long latencyError = registry.get("thirdparty.response.latency")
                .tags("provider", PROVIDER, "endpoint", ENDPOINT, "status", "error")
                .timer().count();
        assertEquals(1L, latencyError);
    }
}