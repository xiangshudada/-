package com.easylink.ruleproduct.api;

import com.easylink.ruleproduct.api.dto.RuleExecutionRequest;
import com.easylink.ruleproduct.core.model.ExecutionResult;
import com.easylink.ruleproduct.core.service.RuleExecutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final RuleExecutionService ruleExecutionService;

    public ExecutionController(RuleExecutionService ruleExecutionService) {
        this.ruleExecutionService = ruleExecutionService;
    }

    @PostMapping
    public ExecutionResult execute(@Valid @RequestBody RuleExecutionRequest request) {
        return ruleExecutionService.execute(request);
    }
}
