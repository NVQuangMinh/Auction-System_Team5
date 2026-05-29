package auctionserver.service;

import auctionserver.dao.DAOProvider;
import auctionserver.dao.UserDAO;
import auctionserver.entities.User;
import auctionserver.exception.DatabaseException;
import auctionserver.exception.DuplicateUsernameException;
import auctionserver.exception.UserBannedException;
import auctionserver.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService(DAOProvider daoProvider) {
        this.userDAO = daoProvider.userDAO();
    }

    public void register(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            User existingUser = userDAO.getUserByUsername(user.getUsername());
            if (existingUser != null) {
                throw new DuplicateUsernameException("Username already exists: " + user.getUsername());
            }
            userDAO.insertUser(user);
            log.info("User registered successfully: {}", user.getUsername());
        } catch (DatabaseException e) {
            log.error("Database error during registration for user: {}", user.getUsername(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception occurred during registration for user: {}", user.getUsername(), e);
            throw new DatabaseException("Unexpected error during registration", e);
        }
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            User user = userDAO.getUserByUsername(username);
            if (user == null) {
                throw new UserNotFoundException("User not found: " + username);
            }
            if ("BANNED".equals(user.getUserStatus())) {
                throw new UserBannedException("User is banned: " + username);
            }
            if (!user.getPassword().equals(password)) {
                throw new UserNotFoundException("Invalid credentials for user: " + username);
            }
            log.info("User logged in successfully: {}", username);
            return user;
        } catch (DatabaseException e) {
            log.error("Database error during login for user: {}", username, e);
            throw e;
        } catch (UserBannedException | UserNotFoundException e) {
            log.error("Unexpected exception occurred during login for user: {}", username, e);
            throw e;
        }
    }
}
