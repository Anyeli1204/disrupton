package com.disrupton.config;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class CulquiConfig {

    @Value("${culqi.api.key}")
    private String apiKey;

    @Value("${culqi.api.url}")
    private String apiUrl;

    @Value("${culqi.plan.monthly_plan_id}")
    private String monthlyPlanId;
}
