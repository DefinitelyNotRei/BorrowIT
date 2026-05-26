package com.borrowit.dao;

import com.borrowit.model.Equipment;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface EquipmentDao {
    int create(Equipment equipment) throws SQLException;

    boolean update(Equipment equipment) throws SQLException;

    boolean delete(int equipmentId) throws SQLException;

    Optional<Equipment> findById(int equipmentId) throws SQLException;

    List<Equipment> findAll() throws SQLException;

    List<Equipment> findAvailable() throws SQLException;

    List<Equipment> search(String keyword) throws SQLException;
}
