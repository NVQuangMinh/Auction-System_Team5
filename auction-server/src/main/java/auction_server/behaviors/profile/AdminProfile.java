package auction_server.behaviors.profile;

public interface AdminProfile {
    void manageUsers();
    void viewReports();
    void banUser(String userId);
}
