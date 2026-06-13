package com.wcraske.n44.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "phones")
public class Phone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String modelName;
    private String os;
    private Double priceInr;
    private Integer launchYear;
    private Boolean fiveGSupport;
    private Boolean dualSim;
    private Boolean expandableStorage;
    private Boolean waterResistance;
    private Boolean wirelessCharging;
    private Boolean fingerprintSensor;
    private Boolean faceUnlock;
    private Integer gpuScore;
    private Integer cpuScore;
    private Double screenToBodyRatio;
    private String buildMaterial;
    private Integer colorsAvailable;
    private Integer warrantyYears;
    private String bluetoothVersion;
    private String wifiVersion;
    private String chipset;
    private Integer ramGb;
    private Integer storageGb;
    private Double displaySizeInch;
    private String displayType;
    private Integer refreshRateHz;
    private Integer batteryMah;
    private Integer fastChargingW;
    private Integer rearCameraMp;
    private Integer frontCameraMp;
    private String cameraSetup;
    private Double weightG;
    private Double thicknessMm;
}