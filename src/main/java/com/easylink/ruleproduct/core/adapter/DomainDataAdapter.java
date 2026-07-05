package com.easylink.ruleproduct.core.adapter;

import com.easylink.ruleproduct.core.model.DataRequirement;

public interface DomainDataAdapter {

    boolean supports(String domain);

    /**
     * 带租户上下文的能力判断。可配置适配器可以根据请求上下文判断是否存在绑定配置；
     * 静态示例适配器仍可沿用只按领域判断的默认实现。
     */
    default boolean supports(String domain, QueryContext context) {
        return supports(domain);
    }

    /**
     * 数值越小优先级越高。客户配置型适配器应优先于内置示例适配器。
     */
    default int priority() {
        return 100;
    }

    DomainDataset query(DataRequirement requirement, QueryContext context);
}
