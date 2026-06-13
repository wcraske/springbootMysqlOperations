package com.wcraske.n44.repository;

import java.io.Serializable;

public class PhoneProjection implements Serializable {
    private String brand;
    private String modelName;
    private Integer storageGb;
    private String os;

    // constructor for JPQL mapping
    public PhoneProjection(String brand, String modelName, Integer storageGb, String os) {
        this.brand = brand;
        this.modelName = modelName;
        this.storageGb = storageGb;
        this.os = os;
    }

    // no-arg constructor for deserialization
    public PhoneProjection() {}

    // getters
    public String getBrand() { return brand; }
    public String getModelName() { return modelName; }
    public Integer getStorageGb() { return storageGb; }
    public String getOs() { return os; }
}