package auctionshared.Network;

import java.io.Serializable;
import java.time.LocalTime;

public class Notification implements Serializable {
    private String notificationMessage;
    private LocalTime notificationTime;

    public Notification(String notificationMessage, LocalTime notificationTime) {
        this.notificationMessage = notificationMessage;
        this.notificationTime = notificationTime;
    }

    public String getNotificationMessage(){
        return this.notificationMessage;
    }

    public LocalTime getNotificationTime() {
        return this.notificationTime;
    }

}
