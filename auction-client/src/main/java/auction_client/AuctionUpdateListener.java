package auction_client;

import auction_shared.Network.NetworkMessage;

public interface AuctionUpdateListener {
    void onUpdateReceived(NetworkMessage msg);
}
