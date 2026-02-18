package com.wecureit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@ConditionalOnProperty(name = "neo4j.enabled", havingValue = "true")
@EnableNeo4jRepositories(basePackages = "com.wecureit.graph.repository")
public class Neo4jConfig {
}
