package com.easylink.ruleproduct.core.operator;

import static org.assertj.core.api.Assertions.assertThat;

import com.easylink.ruleproduct.core.model.DataQuality;
import com.easylink.ruleproduct.core.model.ClientType;
import com.easylink.ruleproduct.core.model.DecisionPolicy;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import com.easylink.ruleproduct.core.model.ReportMapping;
import com.easylink.ruleproduct.core.model.ThresholdProfile;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ThresholdCompareOperatorTest {

    @Test
    void shouldHitWhenMetricIsGreaterThanThreshold() {
        ThresholdCompareOperator operator = new ThresholdCompareOperator();
        InvestigationStep step = new InvestigationStep(
                1L,
                "STEP",
                "threshold step",
                1,
                "",
                "",
                OperatorType.THRESHOLD_COMPARE,
                Set.of(ClientType.PERSON),
                List.of(),
                new ThresholdProfile("totalAmount", new BigDecimal("1000"), null, Map.of()),
                new DecisionPolicy("", false),
                new ReportMapping("疑点分析", null)
        );
        Fact fact = new Fact(
                "TradeFact",
                "SecurityTrade",
                Map.of("totalAmount", new BigDecimal("1200")),
                List.of(),
                DataQuality.complete("test")
        );

        var result = operator.evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
    }

    @Test
    void shouldUseClientTypeThresholdWhenConfigured() {
        ThresholdCompareOperator operator = new ThresholdCompareOperator();
        InvestigationStep step = new InvestigationStep(
                1L,
                "STEP",
                "threshold step",
                1,
                "",
                "",
                OperatorType.THRESHOLD_COMPARE,
                Set.of(ClientType.PERSON),
                List.of(),
                new ThresholdProfile(
                        "shortTermNetInflowAmount",
                        null,
                        null,
                        Map.of("minByClientType", Map.of("PERSON", 500000, "ORG", 1000000))
                ),
                new DecisionPolicy("", false),
                new ReportMapping("疑点分析", null)
        );
        Fact fact = new Fact(
                "FundTransferFact",
                "FundTransfer",
                Map.of("clientType", "PERSON", "shortTermNetInflowAmount", new BigDecimal("600000")),
                List.of(),
                DataQuality.complete("test")
        );

        var result = operator.evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
        assertThat(result.details()).containsEntry("min", new BigDecimal("500000"));
    }

    @Test
    void shouldCalculateShortTermNetInflowAmountFromTransferSamples() {
        ThresholdCompareOperator operator = new ThresholdCompareOperator();
        InvestigationStep step = new InvestigationStep(
                1L,
                "STEP",
                "threshold step",
                1,
                "",
                "",
                OperatorType.THRESHOLD_COMPARE,
                Set.of(ClientType.PERSON),
                List.of(),
                new ThresholdProfile("shortTermNetInflowAmount", new BigDecimal("500000"), null, Map.of()),
                new DecisionPolicy("", false),
                new ReportMapping("疑点分析", null)
        );
        Fact fact = new Fact(
                "FundTransferFact",
                "FundTransfer",
                Map.of(),
                List.of(
                        Map.of("direction", "IN", "amount", new BigDecimal("800000")),
                        Map.of("direction", "OUT", "amount", new BigDecimal("100000"))
                ),
                DataQuality.complete("test")
        );

        var result = operator.evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
        assertThat(result.details()).containsEntry("value", new BigDecimal("700000"));
    }

    @Test
    void shouldTreatEmptyFundTransferDatasetAsZeroNetInflow() {
        ThresholdCompareOperator operator = new ThresholdCompareOperator();
        InvestigationStep step = new InvestigationStep(
                1L,
                "STEP",
                "threshold step",
                1,
                "",
                "",
                OperatorType.THRESHOLD_COMPARE,
                Set.of(ClientType.PERSON),
                List.of(),
                new ThresholdProfile("shortTermNetInflowAmount", new BigDecimal("500000"), null, Map.of()),
                new DecisionPolicy("", false),
                new ReportMapping("疑点分析", null)
        );
        Fact fact = new Fact(
                "FundTransferFact",
                "FundTransfer",
                Map.of("count", 0),
                List.of(),
                DataQuality.missing("test", "No rows returned")
        );

        var result = operator.evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.NOT_HIT);
        assertThat(result.details()).containsEntry("value", BigDecimal.ZERO);
    }
}
