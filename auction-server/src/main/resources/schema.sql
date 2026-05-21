
-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id       VARCHAR(36)  PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'USER'
);

-- ============================================================
-- ITEMS  (single-table inheritance: Arts / Electronics / Vehicles)
-- ============================================================
CREATE TABLE items (
    id          VARCHAR(36)  PRIMARY KEY,
    item_type   VARCHAR(20) NOT NULL CHECK (item_type IN ('ARTS', 'ELECTRONICS', 'VEHICLES')),
    item_name   VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    VARCHAR(36)  NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- AUCTIONS
-- ============================================================
CREATE TABLE auctions (
    id                  VARCHAR(36)    PRIMARY KEY,
    item_id             VARCHAR(36)    NOT NULL UNIQUE,
    starting_price      DECIMAL(15, 2) NOT NULL,
    buy_out_price       DECIMAL(15, 2) NOT NULL,
    tick_size           DECIMAL(15, 2) NOT NULL,
    current_highest_bid DECIMAL(15, 2) NOT NULL DEFAULT 0,
    start_time          TIMESTAMP      NOT NULL,
    end_time            TIMESTAMP      NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- ============================================================
-- BID_TRANSACTIONS
-- ============================================================
CREATE TABLE bid_transactions (
    id         VARCHAR(36)    PRIMARY KEY,
    auction_id VARCHAR(36)    NOT NULL,
    bidder_id  VARCHAR(36)    NOT NULL,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id)  REFERENCES users(id)    ON DELETE CASCADE
);
