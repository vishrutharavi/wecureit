package com.wecureit.graph.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.wecureit.graph.node.PatientNode;

@Repository
public interface PatientNodeRepository extends Neo4jRepository<PatientNode, String> {
}
