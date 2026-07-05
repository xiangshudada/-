package com.easylink.ruleproduct.core.adapter;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DomainDataAdapterRegistry {

    private final List<DomainDataAdapter> adapters;

    public DomainDataAdapterRegistry(List<DomainDataAdapter> adapters) {
        this.adapters = adapters.stream()
                .sorted(java.util.Comparator.comparingInt(DomainDataAdapter::priority))
                .toList();
    }

    public DomainDataAdapter find(String domain) {
        return find(domain, null);
    }

    public DomainDataAdapter find(String domain, QueryContext context) {
        return adapters.stream()
                .filter(adapter -> context == null ? adapter.supports(domain) : adapter.supports(domain, context))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No data adapter for domain: " + domain));
    }
}
