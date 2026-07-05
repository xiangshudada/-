package com.easylink.ruleproduct.core.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.easylink.ruleproduct.core.model.DataRequirement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainDataAdapterRegistryTest {

    @Test
    void shouldUseLowerPriorityAdapterFirst() {
        DomainDataAdapter fallback = new TestAdapter("fallback", 100);
        DomainDataAdapter configured = new TestAdapter("configured", 10);
        DomainDataAdapterRegistry registry = new DomainDataAdapterRegistry(List.of(fallback, configured));
        QueryContext context = new QueryContext(
                "default",
                "default",
                "1214_1001",
                "C001",
                LocalDate.parse("2026-06-24"),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30")
        );

        DomainDataAdapter adapter = registry.find("SecurityTrade", context);

        assertThat(((TestAdapter) adapter).name).isEqualTo("configured");
    }

    private record TestAdapter(String name, int priority) implements DomainDataAdapter {
        @Override
        public boolean supports(String domain) {
            return "SecurityTrade".equals(domain);
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public DomainDataset query(DataRequirement requirement, QueryContext context) {
            return new DomainDataset(requirement.domain(), name, List.of());
        }
    }
}
