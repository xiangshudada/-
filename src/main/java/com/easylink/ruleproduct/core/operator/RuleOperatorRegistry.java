package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.OperatorType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RuleOperatorRegistry {

    private final Map<OperatorType, RuleOperator> operators = new EnumMap<>(OperatorType.class);

    public RuleOperatorRegistry(List<RuleOperator> ruleOperators) {
        for (RuleOperator ruleOperator : ruleOperators) {
            operators.put(ruleOperator.type(), ruleOperator);
        }
    }

    public RuleOperator get(OperatorType type) {
        RuleOperator operator = operators.get(type);
        if (operator == null) {
            throw new IllegalArgumentException("No rule operator for type: " + type);
        }
        return operator;
    }
}
