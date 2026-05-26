package com.borrowit.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.borrowit.model.User;

public interface UserDao {
    int create(User user) throws SQLException;

    Optional<User> findById(int userId) throws SQLException;

    Optional<User> findByUsername(String username) throws SQLException;

    boolean usernameExists(String username) throws SQLException;

    boolean emailExists(String email) throws SQLException;

    boolean updatePassword(int userId, String passwordHash) throws SQLException;

    boolean updateUser(User user) throws SQLException;

    boolean deactivate(int userId) throws SQLException;

    boolean delete(int userId) throws SQLException;

    List<User> findAll() throws SQLException;

    List<User> search(String keyword) throws SQLException;
}
