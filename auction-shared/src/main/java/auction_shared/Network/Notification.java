package auction_shared.Network;

import java.io.Serializable;
import java.time.LocalTime;

public class Notification implements Serializable {
    private String notificationMSG;
    private LocalTime notificationTime;

    public Notification(String notificationMSG, LocalTime notificationTime) {
        this.notificationMSG = notificationMSG;
        this.notificationTime = notificationTime;
    }

    public String getNotificationMSG(){
        return this.notificationMSG;
    }

    public LocalTime getNotificationTime() {
        return this.notificationTime;
    }

}
