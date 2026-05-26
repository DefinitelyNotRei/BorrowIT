package com.borrowit.service;

import java.sql.SQLException;
import java.util.List;

import com.borrowit.dao.UserDao;
import com.borrowit.model.User;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public List<User> searchUsers(String keyword) throws ServiceException {
        try {
            return userDao.search(keyword);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to search users.", exception);
        }
    }

    public boolean updateUser(User user) throws ValidationException, ServiceException {
        validateUserUpdate(user);
        try {
            return userDao.updateUser(user);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to update user.", exception);
        }
    }

    private void validateUserUpdate(User user) throws ValidationException {
        if (user == null) {
            throw new ValidationException("User cannot be null.");
        }
        if (user.getUserId() <= 0) {
            throw new ValidationException("Invalid user ID.");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name is required.");
        }
        if (!user.getFullName().matches("[A-Za-z .'-]{3,255}")) {
            throw new ValidationException("Full name must contain only letters, spaces, periods, apostrophes, or hyphens.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required.");
        }
        if (!user.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new ValidationException("Invalid email format.");
        }
        if (user.getBranch() == null || user.getBranch().trim().isEmpty()) {
            throw new ValidationException("Branch is required.");
        }
        if (user.getCourse() == null || user.getCourse().trim().isEmpty()) {
            throw new ValidationException("Course is required.");
        }
        if (user.getBlock() == null || user.getBlock().trim().isEmpty()) {
            throw new ValidationException("Block is required.");
        }
        if (user.getYearLevel() < 1 || user.getYearLevel() > 4) {
            throw new ValidationException("Year level must be between 1 and 4.");
        }
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().matches("\\d{11}")) {
            throw new ValidationException("Invalid phone number format.");
        }
    }

    public boolean deactivateUser(int userId) throws ValidationException, ServiceException {
        if (userId <= 0) {
            throw new ValidationException("Invalid user ID.");
        }
        try {
            return userDao.deactivate(userId);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to deactivate user.", exception);
        }
    }

    public boolean deleteUser(int userId) throws ValidationException, ServiceException {
        if (userId <= 0) {
            throw new ValidationException("Invalid user ID.");
        }
        try {
            return userDao.delete(userId);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to delete user.", exception);
        }
    }
}
