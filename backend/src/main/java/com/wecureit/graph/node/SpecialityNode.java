package com.wecureit.graph.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Speciality")
public class SpecialityNode {

    @Id
    private String code;

    private String name;

    public SpecialityNode() {}

    public SpecialityNode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
