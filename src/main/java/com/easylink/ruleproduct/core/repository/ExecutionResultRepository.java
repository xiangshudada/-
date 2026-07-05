package com.easylink.ruleproduct.core.repository;

import com.easylink.ruleproduct.core.model.ExecutionResult;

/**
 * 持久化执行输出，同时避免规则核心依赖具体存储实现。
 * 产品部署时可替换为消息、文件或其他存储方式。
 */
public interface ExecutionResultRepository {

    void save(ExecutionResult result);
}
