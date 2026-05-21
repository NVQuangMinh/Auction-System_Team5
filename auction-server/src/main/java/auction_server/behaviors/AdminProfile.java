package auction_server.behaviors;

public interface AdminProfile {
    void manageUsers();
    void viewReports();
    void banUser(String userId);
}
