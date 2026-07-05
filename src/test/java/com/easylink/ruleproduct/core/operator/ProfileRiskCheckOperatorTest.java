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

class ProfileRiskCheckOperatorTest {

    @Test
    void shouldHitWhenAgeIsGreaterThanHighAgeThreshold() {
        ProfileRiskCheckOperator operator = new ProfileRiskCheckOperator();
        InvestigationStep step = new InvestigationStep(
                1L,
                "STEP",
                "profile step",
                1,
                "",
                "",
                OperatorType.PROFILE_RISK_CHECK,
                Set.of(ClientType.PERSON),
                List.of(),
                new ThresholdProfile("age", new BigDecimal("22"), new BigDecimal("70"), Map.of()),
                new DecisionPolicy("", false),
                new ReportMapping("疑点分析", null)
        );
        Fact fact = new Fact(
                "CustomerProfileFact",
                "CustomerProfile",
                Map.of("age", 72),
                List.of(),
                DataQuality.complete("test")
        );

        var result = operator.evaluate(step, List.of(fact));

        assertThat(result.status()).isEqualTo(DecisionStatus.HIT);
    }
}
