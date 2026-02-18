package com.wecureit.graph.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Patient")
public class PatientNode {

    @Id
    private String pgId;

    private String name;

    private String stateCode;

    public PatientNode() {}

    public PatientNode(String pgId, String name, String stateCode) {
        this.pgId = pgId;
        this.name = name;
        this.stateCode = stateCode;
    }

    public String getPgId() { return pgId; }
    public void setPgId(String pgId) { this.pgId = pgId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
}
