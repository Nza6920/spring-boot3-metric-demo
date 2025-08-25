package com.niu.demo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;


@Service
class ThirdPartyService {

    static final String provider = "acme";    // 可改为配置注入
    static final String endpoint = "/v1/data";

    private final MetricsHelper helper;
    private final ThirdPartyClient client;

    public ThirdPartyService(MetricsHelper helper, ThirdPartyClient client) {
        this.helper = helper;
        this.client = client;
    }

    public List<String> getData() {
        return helper.observe(provider, endpoint, () -> {
            Optional<List<String>> data = client.fetchData();
            return new MetricsWrapper<>(data.orElse(List.of()), data.isPresent());
        });
    }
}
