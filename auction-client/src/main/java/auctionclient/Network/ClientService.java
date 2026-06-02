package auctionclient.Network;

import auctionclient.interfaces.AuctionUpdateListener;
import auctionshared.Network.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;



public class ClientService {
    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private static ClientService instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;
    private final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();


    private ClientService(){}
    public static synchronized ClientService getInstance(){
        if (instance != null){
            return instance;
        }
        instance = new ClientService();
        return instance;
    }
    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.socket = new Socket(host, port);
            // Quan trọng: Khởi tạo Output trước Input để tránh Deadlock
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.isRunning = true;

            // Chạy một luồng ngầm để liên tục nghe phản hồi từ Server
            startListening();
        }
    }


    public void sendMessage(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startListening() {
        ///NOTE để sau chuyển thành virtual thread.
        Thread listenerThread = new Thread(() -> {
            try {
                while (isRunning) {
                    // Đợi nhận phản hồi (ví dụ: thông báo có người vừa đặt giá mới)
                    NetworkMessage response = (NetworkMessage) in.readObject();
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                log.warn("Không thể kết nối tới máy chủ.");
                isRunning = false;
            }
        });
        listenerThread.setDaemon(true); // Tự tắt khi ứng dụng đóng
        listenerThread.start();
    }



    public void addListener(AuctionUpdateListener listener) {
        listeners.add(listener);
    }


    public void removeListener(AuctionUpdateListener listener) {
        listeners.remove(listener);
    }

    private void handleServerResponse(NetworkMessage response) {
        for (AuctionUpdateListener listener : listeners){
            listener.onUpdateReceived(response);
        }
    }

}
