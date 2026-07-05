package com.easylink.ruleproduct.core.service;

import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.model.DataQuality;
import com.easylink.ruleproduct.core.model.Fact;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FactFactory {

    public Fact fromDataset(DomainDataset dataset, int sampleLimit) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("count", dataset.count());
        putStandardFieldMetrics(dataset, metrics);

        List<Map<String, Object>> samples = dataset.rows().stream()
                .limit(Math.max(sampleLimit, 0))
                .toList();
        DataQuality quality = dataset.count() == 0
                ? DataQuality.missing(dataset.source(), "No rows returned for domain " + dataset.domain())
                : DataQuality.complete(dataset.source());
        return new Fact(dataset.domain() + "Fact", dataset.domain(), metrics, samples, quality);
    }

    private void putStandardFieldMetrics(DomainDataset dataset, Map<String, Object> metrics) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Map<String, Object> row : dataset.rows()) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Number number) {
                    BigDecimal decimal = new BigDecimal(number.toString());
                    metrics.putIfAbsent(key, decimal);
                    totals.merge("total" + Character.toUpperCase(key.charAt(0)) + key.substring(1), decimal, BigDecimal::add);
                } else if (value instanceof String || value instanceof Boolean) {
                    metrics.putIfAbsent(key, value);
                }
            }
        }
        metrics.putAll(totals);
        metrics.putIfAbsent("totalAmount", dataset.sum("amount"));
        metrics.putIfAbsent("totalQuantity", dataset.sum("quantity"));
    }
}
