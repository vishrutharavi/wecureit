package com.wecureit.graph.node;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.wecureit.graph.relationship.ReferredToRelationship;

@Node("Doctor")
public class DoctorNode {

    @Id
    private String pgId;

    private String name;

    private String email;

    private boolean isActive;

    @Relationship(type = "REFERRED_TO", direction = Relationship.Direction.OUTGOING)
    private List<ReferredToRelationship> referralsOut = new ArrayList<>();

    public DoctorNode() {}

    public DoctorNode(String pgId, String name, String email, boolean isActive) {
        this.pgId = pgId;
        this.name = name;
        this.email = email;
        this.isActive = isActive;
    }

    public String getPgId() { return pgId; }
    public void setPgId(String pgId) { this.pgId = pgId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public List<ReferredToRelationship> getReferralsOut() { return referralsOut; }
    public void setReferralsOut(List<ReferredToRelationship> referralsOut) { this.referralsOut = referralsOut; }
}
