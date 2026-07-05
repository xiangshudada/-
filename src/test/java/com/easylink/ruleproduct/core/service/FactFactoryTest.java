package com.easylink.ruleproduct.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.easylink.ruleproduct.core.adapter.DomainDataset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FactFactoryTest {

    @Test
    void shouldExposeFirstScalarAndNumericTotalsAsMetrics() {
        DomainDataset dataset = new DomainDataset(
                "CustomerProfile",
                "test",
                List.of(
                        Map.of("clientType", "PERSON", "amount", new BigDecimal("100"), "annualIncome", new BigDecimal("50000")),
                        Map.of("clientType", "PERSON", "amount", new BigDecimal("300"), "annualIncome", new BigDecimal("50000"))
                )
        );

        var fact = new FactFactory().fromDataset(dataset, 20);

        assertThat(fact.metrics()).containsEntry("clientType", "PERSON");
        assertThat(fact.metrics()).containsEntry("annualIncome", new BigDecimal("50000"));
        assertThat(fact.metrics()).containsEntry("totalAmount", new BigDecimal("400"));
    }
}
