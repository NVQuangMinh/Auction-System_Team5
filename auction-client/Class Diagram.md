classDiagram
%% Root Abstract Class
class Entity {
<<abstract>>
- Long id
- LocalDateTime createdAt
+ getId() Long
+ getCreatedAt() LocalDateTime
}

    %% User Hierarchy
    class User {
        <<abstract>>
        - String username
        - String passwordHash
        - String email
        + login(String pass) boolean
        + getRole()* String 
    }

    class Bidder {
        - double balance
        + placeBid(Auction auction, double amount) void
        + getRole() String
    }

    class Seller {
        - double reputationScore
        + createItem(Item item) void
        + getRole() String
    }

    class Admin {
        + cancelAuction(Auction auction) void
        + banUser(User user) void
        + getRole() String
    }

    %% Item Hierarchy
    class Item {
        <<abstract>>
        - String name
        - String description
        - Seller seller
        + getDetails()* String
    }

    class Electronics {
        - String brand
        - int warrantyMonths
        + getDetails() String
    }

    class Art {
        - String artistName
        - int creationYear
        + getDetails() String
    }

    class Vehicle {
        - String engineType
        - int mileage
        + getDetails() String
    }

    %% Core Business Classes
    class Auction {
        - Item item
        - double startingPrice
        - double currentHighestBid
        - LocalDateTime startTime
        - LocalDateTime endTime
        - String status
        + start() void
        + close() void
        + addTransaction(BidTransaction tx) void
    }

    class BidTransaction {
        - Auction auction
        - Bidder bidder
        - double bidAmount
        - LocalDateTime timestamp
        + isValid() boolean
    }

    %% Relationships (Inheritance)
    Entity <|-- User
    Entity <|-- Item
    Entity <|-- Auction
    Entity <|-- BidTransaction

    User <|-- Bidder
    User <|-- Seller
    User <|-- Admin

    Item <|-- Electronics
    Item <|-- Art
    Item <|-- Vehicle

    %% Relationships (Associations)
    Seller "1" --> "*" Item : owns
    Auction "1" --> "1" Item : sells
    Auction "1" *-- "*" BidTransaction : contains
    Bidder "1" --> "*" BidTransaction : makes