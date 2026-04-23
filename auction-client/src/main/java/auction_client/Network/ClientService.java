package auction_client.Network;

import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;


public class ClientService {
    private static ClientService instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;
    private Consumer<List<Auction>> auctionListCallback;

    public void setAuctionListCallback(Consumer<List<Auction>> callback) {
        this.auctionListCallback = callback;
    }


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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (isRunning) {
                    // Đợi nhận phản hồi (ví dụ: thông báo có người vừa đặt giá mới)
                    NetworkMessage response = (NetworkMessage) in.readObject();
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                System.out.println("Lost connection to server.");
                isRunning = false;
            }
        });
        listenerThread.setDaemon(true); // Tự tắt khi ứng dụng đóng
        listenerThread.start();
    }


    private void handleServerResponse(NetworkMessage response) {
        if (response.getAction().equals("GET_PRODUCTS")){
            List<Auction> auctions = (List<Auction>) response.getData();
            if (auctionListCallback != null) {
                auctionListCallback.accept(auctions);
            }
        }
        else {
            System.out.println("Receive from server: " + response.getAction());
        }


    }

}
