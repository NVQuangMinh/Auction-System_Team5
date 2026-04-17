package auction_server.core;

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
            if (transaction.getBidAmount() > auction.getCurrentHighestBid()){
                System.out.println(transaction.getBidder() + " have successfully placed the bid for item: " + auction.getItem());
                //logic bla bla change the price and broadcast it to all the clients(not my job)

                auction.addTransaction(transaction);
                auction.setCurrentHighestBid(transaction.getBidAmount());
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

    public Auction getAuction() {
        return auction;
    }
}
