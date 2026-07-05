package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.OperatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NeutralOperatorConfiguration {

    @Bean
    RuleOperator ratioCompareOperator() {
        return GenericNeutralOperator.of(OperatorType.RATIO_COMPARE);
    }

    @Bean
    RuleOperator timeIntervalCompareOperator() {
        return GenericNeutralOperator.of(OperatorType.TIME_INTERVAL_COMPARE);
    }

    @Bean
    RuleOperator sequencePatternOperator() {
        return GenericNeutralOperator.of(OperatorType.SEQUENCE_PATTERN);
    }

    @Bean
    RuleOperator priceDeviationOperator() {
        return GenericNeutralOperator.of(OperatorType.PRICE_DEVIATION);
    }

    @Bean
    RuleOperator relationMatchOperator() {
        return GenericNeutralOperator.of(OperatorType.RELATION_MATCH);
    }

    @Bean
    RuleOperator counterpartyMatchOperator() {
        return GenericNeutralOperator.of(OperatorType.COUNTERPARTY_MATCH);
    }

    @Bean
    RuleOperator identityAssetMismatchOperator() {
        return GenericNeutralOperator.of(OperatorType.IDENTITY_ASSET_MISMATCH);
    }
}
