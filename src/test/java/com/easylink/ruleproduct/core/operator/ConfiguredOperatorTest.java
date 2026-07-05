package com.easylink.ruleproduct.core.operator;

import static org.assertj.core.api.Assertions.assertThat;

import com.easylink.ruleproduct.core.model.ClientType;
import com.easylink.ruleproduct.core.model.DataQuality;
import com.easylink.ruleproduct.core.model.DecisionPolicy;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import com.easylink.ruleproduct.core.model.ReportMapping;
import com.easylink.ruleproduct.core.model.ThresholdProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfiguredOperatorTest {

    @Test
    void ratioCompareShouldCalculateBlockTradeAmountRatio() {
        var step = step(OperatorType.RATIO_COMPARE, new ThresholdProfile(
                null,
                new BigDecimal("0.80"),
                null,
                Map.of("amountRatioMetric", "blockTradeAmountRatio")
        ));
        Fact fact = fact("SecurityTrade", List.of(
                Map.of("tradeMode", "BLOCK", "amount", new BigDecimal("800")),
                Map.of("tradeMode", "NORMAL", "amount", new BigDecimal("200"))
        ));

        var result = new RatioCompareOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
        assertThat(result.details()).containsEntry("ratio", new BigDecimal("0.80000000"));
    }

    @Test
    void ratioCompareShouldTreatEmptyFundTransferDatasetAsZeroRatio() {
        var step = step(OperatorType.RATIO_COMPARE, new ThresholdProfile(
                "postTradeTransferOutRatio",
                new BigDecimal("0.80"),
                null,
                Map.of()
        ));
        Fact fact = new Fact(
                "FundTransferFact",
                "FundTransfer",
                Map.of("count", 0),
                List.of(),
                DataQuality.missing("test", "No rows returned")
        );

        var result = new RatioCompareOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.NOT_HIT);
        assertThat(result.details()).containsEntry("ratio", BigDecimal.ZERO);
    }


    @Test
    void priceDeviationShouldCalculateDeviationAgainstBenchmarkPrice() {
        var step = step(OperatorType.PRICE_DEVIATION, new ThresholdProfile(
                "priceDeviationRate",
                new BigDecimal("0.08"),
                null,
                Map.of()
        ));
        Fact fact = fact("BlockTrade", List.of(
                Map.of("price", new BigDecimal("8.50"), "previousClosePrice", new BigDecimal("10.00"))
        ));

        var result = new PriceDeviationOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
        assertThat(result.details()).containsEntry("value", new BigDecimal("-0.15000000"));
    }

    @Test
    void timeIntervalCompareShouldUseClientTypeSpecificThreshold() {
        var step = step(OperatorType.TIME_INTERVAL_COMPARE, new ThresholdProfile(
                "accountToFirstBlockTradeDays",
                null,
                null,
                Map.of("maxByClientType", Map.of("PERSON", 10, "ORG", 15))
        ));
        Fact fact = fact("BlockTrade", List.of(
                Map.of("clientType", "PERSON", "accountOpenDate", LocalDate.parse("2026-06-01"), "tradeDate", LocalDate.parse("2026-06-08"))
        ));

        var result = new TimeIntervalCompareOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
        assertThat(result.details()).containsEntry("days", new BigDecimal("7"));
    }

    @Test
    void sequencePatternShouldHitWhenConfiguredSequenceEvidenceExists() {
        var step = step(OperatorType.SEQUENCE_PATTERN, new ThresholdProfile(
                null,
                null,
                null,
                Map.of("sequence", List.of("BLOCK_TRADE", "FUND_TRANSFER_OUT"))
        ));
        Fact tradeFact = fact("SecurityTrade", List.of(Map.of("tradeMode", "BLOCK", "amount", new BigDecimal("1000"))));
        Fact transferFact = fact("FundTransfer", List.of(Map.of("direction", "OUT", "amount", new BigDecimal("900"))));

        var result = new SequencePatternOperator().evaluate(step, List.of(tradeFact, transferFact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
    }

    @Test
    void sequencePatternShouldCompareConfiguredDayMetric() {
        var step = step(OperatorType.SEQUENCE_PATTERN, new ThresholdProfile(
                "emptyToCloseAccountDays",
                null,
                new BigDecimal("30"),
                Map.of()
        ));
        Fact fact = new Fact(
                "CustomerProfileFact",
                "CustomerProfile",
                Map.of("emptyToCloseAccountDays", new BigDecimal("20")),
                List.of(),
                DataQuality.complete("test")
        );

        var result = new SequencePatternOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
    }

    @Test
    void relationMatchShouldHitConfiguredRelationType() {
        var step = step(OperatorType.RELATION_MATCH, new ThresholdProfile(
                null,
                null,
                null,
                Map.of("relationTypes", List.of("SAME_DEVICE", "SAME_PHONE"))
        ));
        Fact fact = fact("CustomerRelation", List.of(Map.of("relationType", "SAME_DEVICE")));

        var result = new RelationMatchOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
    }

    @Test
    void counterpartyMatchShouldHitWhenCounterpartyIsClient() {
        var step = step(OperatorType.COUNTERPARTY_MATCH, new ThresholdProfile(
                "counterpartyIsClient",
                null,
                null,
                Map.of()
        ));
        Fact fact = fact("BlockTrade", List.of(Map.of("counterpartyIsClient", true)));

        var result = new CounterpartyMatchOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
    }

    @Test
    void identityAssetMismatchShouldCompareAssetToIncomeMultiple() {
        var step = step(OperatorType.IDENTITY_ASSET_MISMATCH, new ThresholdProfile(
                null,
                null,
                null,
                Map.of("identityMetric", "annualIncome", "multiple", 10)
        ));
        Fact fact = new Fact(
                "CustomerProfileFact",
                "CustomerProfile",
                Map.of("annualIncome", new BigDecimal("100000"), "accountAsset", new BigDecimal("1500000")),
                List.of(),
                DataQuality.complete("test")
        );

        var result = new IdentityAssetMismatchOperator().evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
        assertThat(result.details()).containsEntry("ratio", new BigDecimal("15.00000000"));
    }

    private InvestigationStep step(OperatorType type, ThresholdProfile thresholdProfile) {
        return new InvestigationStep(
                1L,
                "STEP",
                "step",
                1,
                "",
                "",
                type,
                Set.of(ClientType.PERSON),
                List.of(),
                thresholdProfile,
                new DecisionPolicy("", false),
                new ReportMapping("疑点分析", null)
        );
    }

    private Fact fact(String domain, List<Map<String, Object>> samples) {
        return new Fact(domain + "Fact", domain, Map.of(), samples, DataQuality.complete("test"));
    }
}
