package com.borrowit.controller;

import java.util.List;

import com.borrowit.dao.impl.JdbcUserDao;
import com.borrowit.model.User;
import com.borrowit.service.ServiceException;
import com.borrowit.service.UserService;
import com.borrowit.service.ValidationException;

public class UserController {
    private final UserService userService;

    public UserController() {
        this.userService = new UserService(new JdbcUserDao());
    }

    public List<User> searchUsers(String keyword) throws ServiceException {
        return userService.searchUsers(keyword);
    }

    public boolean updateUser(User user) throws ValidationException, ServiceException {
        return userService.updateUser(user);
    }

    public boolean deactivateUser(int userId) throws ValidationException, ServiceException {
        return userService.deactivateUser(userId);
    }

    public boolean deleteUser(int userId) throws ValidationException, ServiceException {
        return userService.deleteUser(userId);
    }
}
