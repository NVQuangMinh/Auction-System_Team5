package auction_server.dao;

import auction_server.behaviors.AdminProfile;
import auction_server.behaviors.BidderProfile;
import auction_server.behaviors.SellerProfile;
import auction_server.entities.User;

public class UserDAO {

    public User findByUsername(String username) {
        // TODO: Implement JDBC/JPA logic to find a user by username.
        // Example: "SELECT * FROM users WHERE username = ?"
        // This is a placeholder.
        if ("testuser".equals(username)) {
            // In a real implementation, you would load the password hash from the DB.
            User user = new User(1L, "testuser", "password_hash");

            // TODO: After finding the user, you MUST determine their roles.
            // This could be done with JOINs or separate queries.
            // For example, check a 'user_roles' table.
            // e.g., "SELECT role_name FROM user_roles WHERE user_id = ?"
            loadUserProfiles(user);
            return user;
        }
        return null;
    }

    public User save(User user) {
        // TODO: Implement JDBC/JPA logic to insert or update a user.
        // If user.getId() is null, it's an INSERT. Otherwise, it's an UPDATE.
        // You also need to save the user's roles.
        System.out.println("Saving user: " + user.getUsername());
        return user;
    }

    /**
     * This is a helper method to simulate loading a user's roles from the database.
     * In a real application, this logic would be part of your data access layer.
     * @param user The user object to populate with profiles.
     */
    private void loadUserProfiles(User user) {
        // TODO: Replace this with actual database queries.
        // For example, you might have a 'user_roles' table.
        // Based on the roles found, you instantiate and set the profiles.
        System.out.println("Loading profiles for user: " + user.getUsername());
        user.setBidderProfile(new BidderProfile());
        user.setSellerProfile(new SellerProfile());
        // user.setAdminProfile(new AdminProfile()); // Only for admin users
    }
}
