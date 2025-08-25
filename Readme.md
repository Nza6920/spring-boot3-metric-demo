# ThirdPartyMetrics Demo

> Spring Boot + Micrometer + Prometheus：封装第三方接口观测的可复用工具（Supplier 版），配套 PromQL 告警示例。

## 功能亮点

* **工具 Bean：`ThirdPartyMetricsHelper`** — 统一封装第三方调用的观测/打点逻辑。
    * 计数：`thirdparty.response.count{provider,endpoint,status=ok|error}`
    * 时延（含直方图）：`thirdparty.response.latency{provider,endpoint,status=ok|error}`
    * **10s SLO 桶**：`serviceLevelObjectives(10s)` 产出 `le="10"`，便于判断 `>10s` 占比。
* **易用 API**：

  ```java
  <E> List<E> observe(String provider, String endpoint, Supplier<List<E>> supplier)
  ```

  你只需提供“如何取数”的 `Supplier`；其余指标采集自动完成。
* **单测覆盖**：OK / ERROR 路径，验证计数与分状态时延。

## 运行前置

* Spring Boot 3.x（Actuator）
* Micrometer Core + Prometheus Registry
* JDK 21（推荐）


> 暴露 `/actuator/prometheus` 供 Prometheus 抓取。

## 如何使用


```java
helper.observe(provider, endpoint, () -> {
    Optional<List<String>> data = client.fetchData();
        return new MetricsWrapper<>(data.orElse(List.of()), data.isPresent());
});
```

Helper 会：

* 记录 **ok/error** 的计数
* 按 **status** 细分的时延（`Timer`），并开启 **直方图** + **10s SLO 桶**

## Prometheus 抓取配置（示例）

```yaml
scrape_configs:
  - job_name: 'demo-svc'
    static_configs:
      - targets: ['localhost:8080']    # 或容器/Pod 的实际地址
    metrics_path: /actuator/prometheus
```

## 告警与查询示例（PromQL）

### A. 时间窗口内“全为error”

```promql
(
  increase(thirdparty_response_count_total{status="error"}[5m])
)
/
(
  increase(thirdparty_response_count_total[5m])
) == 1
and increase(thirdparty_response_count_total[5m]) > 0
```

### B. 时间窗口内“error返回占比 ≥ 80%”

```promql
(
  sum by (provider, endpoint)(increase(thirdparty_response_count_total{status="error"}[5m]))
)
/
(
  sum by (provider, endpoint)(increase(thirdparty_response_count_total{status!="error"}[5m]))
) >= 0.8
and
sum by (provider, endpoint)(increase(thirdparty_response_count_total[5m])) > 0
```

### C. **5 分钟内，90% 响应时间 > 10 秒**（占比表达）

> 使用直方图桶：`>10s 次数 = le="+Inf" - le="10"`

```promql
(
  sum by (provider, endpoint)(rate(thirdparty_response_latency_seconds_bucket{le="+Inf"}[5m]))
-
  sum by (provider, endpoint)(rate(thirdparty_response_latency_seconds_bucket{le="10"}[5m]))
)
/
  sum by (provider, endpoint)(rate(thirdparty_response_latency_seconds_bucket{le="+Inf"}[5m]))
>= 0.9
and
  sum by (provider, endpoint)(rate(thirdparty_response_latency_seconds_bucket{le="+Inf"}[5m])) > 0
```

### D. **P90 > 10 秒**（分位点表达）

```promql
histogram_quantile(
  0.90,
  sum by (le, provider, endpoint)(rate(thirdparty_response_latency_seconds_bucket[5m]))
) > 10
```

> 生产使用建议：结合 `for: 2m` 等待稳定后触发、加上 `severity` 标签、在 Alertmanager 配置钉钉/飞书等通知。

## 常见问题

* **为什么要开启直方图？**
  为了使用 `histogram_quantile()` 计算 P90/P99，或用桶边界直接计算“超过某阈值的占比”。
* **为什么要设置 10s SLO？**
  让导出的指标中**必定包含** `le="10"` 的桶，便于表达 “>10s” 条件。
* **会不会带来太多时序？**
  桶数据会按标签组合生成，务必控制好 `provider/endpoint/status` 维度数量，必要时做聚合上报。

## 参考文档（官方）

* Micrometer Overview（概览）：[https://docs.micrometer.io/micrometer/reference/overview.html](https://docs.micrometer.io/micrometer/reference/overview.html)
* Micrometer Timers（计时器）：[https://docs.micrometer.io/micrometer/reference/concepts/timers.html](https://docs.micrometer.io/micrometer/reference/concepts/timers.html)
* Histograms & Percentiles（直方图/分位点）：[https://docs.micrometer.io/micrometer/reference/concepts/histogram-quantiles.html](https://docs.micrometer.io/micrometer/reference/concepts/histogram-quantiles.html)
* Micrometer Prometheus 实现：[https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html](https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html)
* Spring Boot Actuator Metrics：[https://docs.spring.io/spring-boot/reference/actuator/metrics.html](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
* Prometheus：Histograms & Summaries 实践：[https://prometheus.io/docs/practices/histograms/](https://prometheus.io/docs/practices/histograms/)
* PromQL 函数：`histogram_quantile()`：[https://prometheus.io/docs/prometheus/latest/querying/functions/](https://prometheus.io/docs/prometheus/latest/querying/functions/)


