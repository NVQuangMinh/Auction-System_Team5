package auction_client;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.UserDTO;

public class UserSession {
    private static UserSession self = null;
    private UserDTO user;
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
    public void setUser(UserDTO user){this.user = user;}
    public UserDTO getUser(){return user;}

    public void closeApp(){
        ClientService.getInstance().sendMessage(new NetworkMessage("LOGOUT",null));
        self = null;
        this.username = null;
    }

}
