package com.wecureit.graph.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.wecureit.graph.node.DoctorNode;

@Repository
public interface DoctorNodeRepository extends Neo4jRepository<DoctorNode, String> {
}
