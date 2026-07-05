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
        metrics.put("totalAmount", dataset.sum("amount"));
        metrics.put("totalQuantity", dataset.sum("quantity"));
        putFirstNumericMetric(dataset, metrics, "age");
        putFirstNumericMetric(dataset, metrics, "annualIncome");
        putFirstNumericMetric(dataset, metrics, "registeredCapital");

        List<Map<String, Object>> samples = dataset.rows().stream()
                .limit(Math.max(sampleLimit, 0))
                .toList();
        DataQuality quality = dataset.count() == 0
                ? DataQuality.missing(dataset.source(), "No rows returned for domain " + dataset.domain())
                : DataQuality.complete(dataset.source());
        return new Fact(dataset.domain() + "Fact", dataset.domain(), metrics, samples, quality);
    }

    private void putFirstNumericMetric(DomainDataset dataset, Map<String, Object> metrics, String key) {
        for (Map<String, Object> row : dataset.rows()) {
            Object value = row.get(key);
            if (value instanceof Number number) {
                metrics.put(key, new BigDecimal(number.toString()));
                return;
            }
        }
    }
}
