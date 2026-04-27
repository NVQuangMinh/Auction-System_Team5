package auction_client;

import auction_shared.entities.User;

public class UserSession {
    private static UserSession self = null;
    private User user;
    private String username = "";
    private UserSession(){}

    public synchronized static UserSession getInstance(){
        if (self == null){
            self = new UserSession();
            return self;
        }
        return self;
    }
    public String getUsername(){
        return this.username;
    }
    public void setUsername(String input){
        this.username = input;
    }
    public void setUser(User user){this.user = user;}
    public User getUser(){return user;}
    public void closeApp(){
        this.self = null;
        this.username = null;
    }

}
