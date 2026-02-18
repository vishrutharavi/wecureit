package com.wecureit.graph.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.wecureit.graph.node.SpecialityNode;

@Repository
public interface SpecialityNodeRepository extends Neo4jRepository<SpecialityNode, String> {
}
