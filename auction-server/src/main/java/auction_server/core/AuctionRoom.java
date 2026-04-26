package auction_server.core;

import auction_server.Network.ClientHandler;
import auction_shared.entities.Auction;
import auction_shared.entities.BidTransaction;
import java.util.concurrent.locks.ReentrantLock;
public class AuctionRoom {
    private Auction auction;
    private final ReentrantLock lock = new ReentrantLock();
    public AuctionRoom(Auction auction) {
        this.auction = auction;
    }
    public boolean placeBid(BidTransaction transaction){
        lock.lock();
        try{
            if (transaction.getBidAmount() > auction.getCurrentHighestBid() && auction.getItem().getOwner() != transaction.getBidder()){
                // we also have to deal with the price that exceed the buy out price

                // I guess this shit is gonna be used to build the diagram.
                // oh yeah and this shit is gonna be used to determine who is the winner too.
                auction.addTransaction(transaction);
                auction.setCurrentHighestBid(transaction.getBidAmount()); //this line is good, leave it!
                return true;
            }
            else{
                return false;
            }
        }
        finally{
            lock.unlock();
        }
    }
    public boolean buyOut(BidTransaction transaction){
        if (transaction.getBidder() != auction.getItem().getOwner()){
            return true;
        }
        else{
            return false;
        }
    }
    public Auction getAuction() {
        return auction;
    }
}
