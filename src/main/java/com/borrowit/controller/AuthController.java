package com.borrowit.controller;

import com.borrowit.dao.impl.JdbcAdminDao;
import com.borrowit.dao.impl.JdbcUserDao;
import com.borrowit.model.User;
import com.borrowit.model.UserRole;
import com.borrowit.service.AuthenticationService;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;

public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController() {
        this.authenticationService = new AuthenticationService(new JdbcUserDao(), new JdbcAdminDao());
    }

    public User login(String username, char[] password, UserRole expectedRole)
            throws ValidationException, ServiceException {
        return authenticationService.login(username, password, expectedRole);
    }

    public User loginByUserId(String userId, char[] password, UserRole expectedRole)
            throws ValidationException, ServiceException {
        return authenticationService.loginByUserId(userId, password, expectedRole);
    }

    public User registerUser(String fullName, String username, String email, String branch, String course,
            String block, String yearLevel, String phoneNumber, char[] password)
            throws ValidationException, ServiceException {
        return authenticationService.registerUser(fullName, username, email, branch, course, block, yearLevel,
                phoneNumber, password);
    }

    public void changePassword(int userId, char[] currentPassword, char[] newPassword)
            throws ValidationException, ServiceException {
        authenticationService.changePassword(userId, currentPassword, newPassword);
    }

    public void logout() {
        authenticationService.logout();
    }
}
