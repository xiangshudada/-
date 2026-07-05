package com.easylink.ruleproduct.api;

import com.easylink.ruleproduct.core.model.IndicatorModel;
import com.easylink.ruleproduct.core.repository.IndicatorModelRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {

    private final IndicatorModelRepository indicatorModelRepository;

    public IndicatorController(IndicatorModelRepository indicatorModelRepository) {
        this.indicatorModelRepository = indicatorModelRepository;
    }

    @GetMapping("/{indicatorCode}/model")
    public IndicatorModel getModel(@PathVariable String indicatorCode) {
        return indicatorModelRepository.loadModel(indicatorCode);
    }
}
