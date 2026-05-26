package com.borrowit.model;

import java.time.LocalDateTime;

public class Equipment {
    private int equipmentId;
    private String assetTag;
    private String name;
    private String category;
    private String description;
    private EquipmentStatus status;
    private int totalQuantity;
    private int availableQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Equipment() {
    }

    public Equipment(String assetTag, String name, String category, String description, EquipmentStatus status,
            int totalQuantity, int availableQuantity) {
        this.assetTag = assetTag;
        this.name = name;
        this.category = category;
        this.description = description;
        this.status = status;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
