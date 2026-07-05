package com.easylink.ruleproduct.core.repository;

import com.easylink.ruleproduct.core.model.IndicatorModel;

public interface IndicatorModelRepository {

    IndicatorModel loadModel(String indicatorCode);
}
