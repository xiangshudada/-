package com.easylink.ruleproduct.core.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 核查步骤适用的客户类型。
 */
public enum ClientType {
    /**
     * 个人客户。
     */
    PERSON,

    /**
     * 机构客户。
     */
    ORG,

    /**
     * 产品户或资管产品。
     */
    PRODUCT;

    public static Set<ClientType> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<ClientType> types = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(text -> !text.isEmpty() && !"-".equals(text))
                .map(ClientType::valueOf)
                .forEach(types::add);
        return Set.copyOf(types);
    }
}
