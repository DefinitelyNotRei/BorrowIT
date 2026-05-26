package com.borrowit.service;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import com.borrowit.dao.AdminDao;
import com.borrowit.dao.UserDao;
import com.borrowit.model.User;
import com.borrowit.model.UserRole;

public class AuthenticationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserDao userDao;
    private final AdminDao adminDao;

    public AuthenticationService(UserDao userDao, AdminDao adminDao) {
        this.userDao = userDao;
        this.adminDao = adminDao;
    }

    public User login(String username, char[] password, UserRole expectedRole)
            throws ValidationException, ServiceException {
        String cleanedUsername = clean(username);

        try {
            validateLogin(cleanedUsername, password);
            Optional<User> foundUser = userDao.findByUsername(cleanedUsername);
            if (foundUser.isEmpty()) {
                throw new ValidationException("Invalid username or password.");
            }

            User user = foundUser.get();
            if (!user.isActive()) {
                throw new ValidationException("This account is inactive.");
            }
            if (user.getRole() != expectedRole
                    && !(expectedRole == UserRole.USER && user.getRole() == UserRole.STUDENT)) {
                throw new ValidationException("This account does not have access to this application.");
            }
            if (expectedRole == UserRole.ADMIN && !adminDao.isActiveAdmin(user.getUserId())) {
                throw new ValidationException("This admin account is not active.");
            }
            if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                throw new ValidationException("Invalid username or password.");
            }

            SessionManager.start(user);
            return user;
        } catch (SQLException exception) {
            throw new ServiceException("Unable to sign in because the database request failed.", exception);
        } finally {
            clearPassword(password);
        }
    }

    public User loginByUserId(String userIdStr, char[] password, UserRole expectedRole)
            throws ValidationException, ServiceException {
        try {
            if (userIdStr.isBlank() || password == null || password.length == 0) {
                throw new ValidationException("User ID and password are required.");
            }
            
            int userId;
            try {
                userId = Integer.parseInt(userIdStr.trim());
            } catch (NumberFormatException exception) {
                throw new ValidationException("Invalid User ID format.");
            }

            Optional<User> foundUser = userDao.findById(userId);
            if (foundUser.isEmpty()) {
                throw new ValidationException("Invalid User ID or password.");
            }

            User user = foundUser.get();
            if (!user.isActive()) {
                throw new ValidationException("This account is inactive.");
            }
            if (user.getRole() != expectedRole
                    && !(expectedRole == UserRole.USER && user.getRole() == UserRole.STUDENT)) {
                throw new ValidationException("This account does not have access to this application.");
            }
            if (expectedRole == UserRole.ADMIN && !adminDao.isActiveAdmin(user.getUserId())) {
                throw new ValidationException("This admin account is not active.");
            }
            if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                throw new ValidationException("Invalid User ID or password.");
            }

            SessionManager.start(user);
            return user;
        } catch (SQLException exception) {
            throw new ServiceException("Unable to sign in because the database request failed.", exception);
        } finally {
            clearPassword(password);
        }
    }

    public User registerUser(String fullName, String username, String email, String branch, String course, String block, String yearLevel,
            String phoneNumber, char[] password)
            throws ValidationException, ServiceException {
        String cleanedFullName = clean(fullName);
        String cleanedUsername = clean(username);
        String cleanedEmail = clean(email).toLowerCase();
        String cleanedBranch = clean(branch);
        String cleanedCourse = clean(course);
        String cleanedBlock = clean(block).toUpperCase();
        String cleanedYearLevel = clean(yearLevel);
        String cleanedPhone = clean(phoneNumber);

        try {
            validateRegistration(cleanedFullName, cleanedUsername, cleanedEmail, cleanedBranch, cleanedCourse,
                    cleanedBlock, cleanedYearLevel, cleanedPhone, password);
            if (userDao.usernameExists(cleanedUsername)) {
                throw new ValidationException("User ID is already taken.");
            }
            if (userDao.emailExists(cleanedEmail)) {
                throw new ValidationException("Email is already registered.");
            }

            User user = new User(
                    cleanedFullName,
                    cleanedUsername,
                    cleanedEmail,
                    cleanedBranch,
                    cleanedCourse,
                    cleanedBlock,
                    Integer.parseInt(cleanedYearLevel),
                    cleanedPhone,
                    PasswordHasher.hash(password),
                    UserRole.USER
            );
            int generatedId = userDao.create(user);
            user.setUserId(generatedId);
            SessionManager.start(user);
            return user;
        } catch (SQLException exception) {
            throw new ServiceException("Unable to register because the database request failed.", exception);
        } finally {
            clearPassword(password);
        }
    }

    public void changePassword(int userId, char[] currentPassword, char[] newPassword)
            throws ValidationException, ServiceException {
        try {
            validatePasswordChange(currentPassword, newPassword);
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new ValidationException("User not found."));
            if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
                throw new ValidationException("Current password is incorrect.");
            }
            boolean updated = userDao.updatePassword(userId, PasswordHasher.hash(newPassword));
            if (!updated) {
                throw new ServiceException("Unable to update the password.");
            }
        } catch (SQLException exception) {
            throw new ServiceException("Unable to update password because the database request failed.", exception);
        } finally {
            clearPassword(currentPassword);
            clearPassword(newPassword);
        }
    }

    public void logout() {
        SessionManager.clear();
    }

    private void validatePasswordChange(char[] currentPassword, char[] newPassword) throws ValidationException {
        if (currentPassword == null || currentPassword.length == 0) {
            throw new ValidationException("Current password is required.");
        }
        if (newPassword == null || newPassword.length < 6) {
            throw new ValidationException("New password must be at least 6 characters.");
        }
    }

    private void validateLogin(String username, char[] password) throws ValidationException {
        if (username.isBlank()) {
            throw new ValidationException("Username is required.");
        }
        if (password == null || password.length == 0) {
            throw new ValidationException("Password is required.");
        }
    }

    private void validateRegistration(String fullName, String username, String email, String branch,
            String course, String block, String yearLevel, String phoneNumber, char[] password)
            throws ValidationException {
        if (fullName.isBlank()) {
            throw new ValidationException("First and last name are required.");
        }
        if (!fullName.matches("[A-Za-z .'-]{3,255}")) {
            throw new ValidationException("Full name must contain only letters, spaces, periods, apostrophes, or hyphens.");
        }
        if (username.length() < 3) {
            throw new ValidationException("User ID must be at least 3 characters.");
        }
        if (!username.equals("admin") && !username.matches("\\d+")) {
            throw new ValidationException("Student ID must contain only digits.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        if (branch.isBlank()) {
            throw new ValidationException("Branch is required.");
        }
        if (course.isBlank()) {
            throw new ValidationException("Course is required.");
        }
        if (block.isBlank()) {
            throw new ValidationException("Block is required.");
        }
        if (yearLevel.isBlank() || !yearLevel.matches("[1-4]")) {
            throw new ValidationException("Please select a valid year level.");
        }
        if (phoneNumber.isBlank()) {
            throw new ValidationException("Phone number is required.");
        }
        if (!phoneNumber.matches("\\d{11}")) {
            throw new ValidationException("Enter a valid 11-digit phone number.");
        }
        if (password == null || password.length < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearPassword(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
