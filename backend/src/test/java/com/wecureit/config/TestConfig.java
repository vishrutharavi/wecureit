package com.wecureit.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;
import com.wecureit.graph.service.BottleneckAnalyzerService;
import com.wecureit.graph.service.CarePathService;
import com.wecureit.graph.service.GraphSyncService;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public BottleneckAnalyzerService bottleneckAnalyzerService() {
        return Mockito.mock(BottleneckAnalyzerService.class);
    }

    @Bean
    @Primary
    public CarePathService carePathService() {
        return Mockito.mock(CarePathService.class);
    }

    @Bean
    @Primary
    public GraphSyncService graphSyncService() {
        return Mockito.mock(GraphSyncService.class);
    }
}
