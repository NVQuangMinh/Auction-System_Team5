package auctionclient.interfaces;

import auctionshared.Network.NetworkMessage;

public interface AuctionUpdateListener {
    void onUpdateReceived(NetworkMessage msg);
}
