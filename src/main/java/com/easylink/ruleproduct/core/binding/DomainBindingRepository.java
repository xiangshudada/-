package com.easylink.ruleproduct.core.binding;

import java.util.Optional;

/**
 * 查询租户、数据源 profile、标准领域三者对应的数据投影契约。
 * 实现可以来自 MySQL、配置文件或远程控制面。
 */
public interface DomainBindingRepository {

    Optional<DomainBinding> findBinding(String tenantId, String dataSourceProfile, String domain);
}
