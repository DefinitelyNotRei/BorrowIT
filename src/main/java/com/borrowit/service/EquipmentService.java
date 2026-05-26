package com.borrowit.service;

import com.borrowit.dao.EquipmentDao;
import com.borrowit.model.Equipment;
import java.sql.SQLException;
import java.util.List;

public class EquipmentService {
    private final EquipmentDao equipmentDao;

    public EquipmentService(EquipmentDao equipmentDao) {
        this.equipmentDao = equipmentDao;
    }

    public int addEquipment(Equipment equipment) throws ValidationException, ServiceException {
        validateEquipment(equipment);
        try {
            return equipmentDao.create(equipment);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to add equipment. Check for duplicate asset tags.", exception);
        }
    }

    public boolean updateEquipment(Equipment equipment) throws ValidationException, ServiceException {
        if (equipment.getEquipmentId() <= 0) {
            throw new ValidationException("Select equipment to update.");
        }
        validateEquipment(equipment);
        try {
            return equipmentDao.update(equipment);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to update equipment. Check for duplicate asset tags.", exception);
        }
    }

    public boolean deleteEquipment(int equipmentId) throws ValidationException, ServiceException {
        if (equipmentId <= 0) {
            throw new ValidationException("Select equipment to delete.");
        }
        try {
            return equipmentDao.delete(equipmentId);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to delete equipment. If it has reservations, mark it retired instead.", exception);
        }
    }

    public List<Equipment> getAllEquipment() throws ServiceException {
        try {
            return equipmentDao.findAll();
        } catch (SQLException exception) {
            throw new ServiceException("Unable to load equipment.", exception);
        }
    }

    public List<Equipment> getAvailableEquipment() throws ServiceException {
        try {
            return equipmentDao.findAvailable();
        } catch (SQLException exception) {
            throw new ServiceException("Unable to load available equipment.", exception);
        }
    }

    public List<Equipment> searchEquipment(String keyword) throws ServiceException {
        try {
            return equipmentDao.search(keyword);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to search equipment.", exception);
        }
    }

    private void validateEquipment(Equipment equipment) throws ValidationException {
        if (equipment == null) {
            throw new ValidationException("Equipment details are required.");
        }
        equipment.setAssetTag(clean(equipment.getAssetTag()).toUpperCase());
        equipment.setName(clean(equipment.getName()));
        equipment.setDescription(clean(equipment.getDescription()));

        if (equipment.getAssetTag().isBlank()) {
            throw new ValidationException("Asset tag is required.");
        }
        if (equipment.getName().isBlank()) {
            throw new ValidationException("Equipment name is required.");
        }
        if (equipment.getStatus() == null) {
            throw new ValidationException("Equipment status is required.");
        }
        if (equipment.getTotalQuantity() < 0) {
            throw new ValidationException("Total quantity cannot be negative.");
        }
        if (equipment.getAvailableQuantity() < 0) {
            throw new ValidationException("Available quantity cannot be negative.");
        }
        if (equipment.getAvailableQuantity() > equipment.getTotalQuantity()) {
            throw new ValidationException("Available quantity cannot exceed total quantity.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
