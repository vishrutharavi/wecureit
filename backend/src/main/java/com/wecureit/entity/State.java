package com.wecureit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "state")
public class State {

    @Id
    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "state_name", nullable = false, unique = true)
    private String stateName;

    @Column(name = "id", insertable = false, updatable = false)
    private Long id;

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public Long getId() { return id; }
}
