package com.borrowit.dao;

import com.borrowit.model.Admin;
import java.sql.SQLException;
import java.util.Optional;

public interface AdminDao {
    Optional<Admin> findByUserId(int userId) throws SQLException;

    boolean isActiveAdmin(int userId) throws SQLException;
}
