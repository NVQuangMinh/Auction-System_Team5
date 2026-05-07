package auction_client.interfaces;

import auction_shared.Network.NetworkMessage;

public interface AuctionUpdateListener {
    void onUpdateReceived(NetworkMessage msg);
}
