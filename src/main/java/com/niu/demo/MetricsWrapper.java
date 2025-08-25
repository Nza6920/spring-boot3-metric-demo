package com.niu.demo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricsWrapper<E> {

    E body;

    private boolean isError;

    public static <E> MetricsWrapper<E> of(E body, boolean noResult) {
        return MetricsWrapper.<E>builder()
            .body(body)
            .isError(noResult)
            .build();
    }
}
