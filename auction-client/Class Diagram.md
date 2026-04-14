classDiagram
    %% Root Abstract Class
    class entity.Entity {
        <<abstract>>
        - Long id
        - LocalDateTime createdAt
        + getId() Long
        + getCreatedAt() LocalDateTime
    }

    %% entity.User Hierarchy
    class entity.User {
        <<abstract>>
        - String username
        - String passwordHash
        - String email
        + login(String pass) boolean
        + getRole()* String 
    }

    class entity.Bidder {
        - double balance
        + placeBid(entity.Auction auction, double amount) void
        + getRole() String
    }

    class entity.Seller {
        - double reputationScore
        + createItem(entity.Item item) void
        + getRole() String
    }

    class entity.Admin {
        + cancelAuction(entity.Auction auction) void
        + banUser(entity.User user) void
        + getRole() String
    }

    %% entity.Item Hierarchy
    class entity.Item {
        <<abstract>>
        - String name
        - String description
        - entity.Seller seller
        + getDetails()* String
    }

    class entity.Electronics {
        - String brand
        - int warrantyMonths
        + getDetails() String
    }

    class entity.Art {
        - String artistName
        - int creationYear
        + getDetails() String
    }

    class entity.Vehicle {
        - String engineType
        - int mileage
        + getDetails() String
    }

    %% Core Business Classes
    class entity.Auction {
        - entity.Item item
        - double startingPrice
        - double currentHighestBid
        - LocalDateTime startTime
        - LocalDateTime endTime
        - String status
        + start() void
        + close() void
        + addTransaction(entity.BidTransaction tx) void
    }

    class entity.BidTransaction {
        - entity.Auction auction
        - entity.Bidder bidder
        - double bidAmount
        - LocalDateTime timestamp
        + isValid() boolean
    }

    %% Relationships (Inheritance)
    entity.Entity <|-- entity.User
    entity.Entity <|-- entity.Item
    entity.Entity <|-- entity.Auction
    entity.Entity <|-- entity.BidTransaction

    entity.User <|-- entity.Bidder
    entity.User <|-- entity.Seller
    entity.User <|-- entity.Admin

    entity.Item <|-- entity.Electronics
    entity.Item <|-- entity.Art
    entity.Item <|-- entity.Vehicle

    %% Relationships (Associations)
    entity.Seller "1" --> "*" entity.Item : owns
    entity.Auction "1" --> "1" entity.Item : sells
    entity.Auction "1" *-- "*" entity.BidTransaction : contains
    entity.Bidder "1" --> "*" entity.BidTransaction : makes
