package com.borrowit.controller;

import com.borrowit.dao.impl.JdbcEquipmentDao;
import com.borrowit.model.Equipment;
import com.borrowit.service.EquipmentService;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;
import java.util.List;

public class EquipmentController {
    private final EquipmentService equipmentService;

    public EquipmentController() {
        this.equipmentService = new EquipmentService(new JdbcEquipmentDao());
    }

    public int addEquipment(Equipment equipment) throws ValidationException, ServiceException {
        return equipmentService.addEquipment(equipment);
    }

    public boolean updateEquipment(Equipment equipment) throws ValidationException, ServiceException {
        return equipmentService.updateEquipment(equipment);
    }

    public boolean deleteEquipment(int equipmentId) throws ValidationException, ServiceException {
        return equipmentService.deleteEquipment(equipmentId);
    }

    public List<Equipment> getAllEquipment() throws ServiceException {
        return equipmentService.getAllEquipment();
    }

    public List<Equipment> getAvailableEquipment() throws ServiceException {
        return equipmentService.getAvailableEquipment();
    }

    public List<Equipment> searchEquipment(String keyword) throws ServiceException {
        return equipmentService.searchEquipment(keyword);
    }
}
