-- =================================================================
-- SCHEMA SCRIPT FOR AUCTION SYSTEM (v2)
-- =================================================================
-- This script creates all necessary tables, relationships, and
-- indexes based on the application's class diagram.
-- It also includes sample data for testing purposes.
-- =================================================================

-- Sử dụng một database tên là 'auction_db', bạn có thể đổi tên nếu muốn
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- Xóa các bảng theo thứ tự ngược lại để tránh lỗi khóa ngoại
DROP TABLE IF EXISTS bid_transactions;
DROP TABLE IF EXISTS auctions;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;


-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- -----------------------------------------------------
-- Table `user_roles`
-- -----------------------------------------------------
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_name VARCHAR(50) NOT NULL, -- e.g., 'BIDDER', 'SELLER', 'ADMIN'
  PRIMARY KEY (user_id, role_name),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- -----------------------------------------------------
-- Table `items`
-- -----------------------------------------------------
CREATE TABLE items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  owner_id BIGINT NOT NULL,
  type VARCHAR(50) NOT NULL, -- 'art', 'vehicle', 'electronics'
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  artist_name VARCHAR(255) NULL,
  brand VARCHAR(255) NULL,
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- -----------------------------------------------------
-- Table `auctions`
-- -----------------------------------------------------
CREATE TABLE auctions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_id BIGINT NOT NULL UNIQUE,
  starting_price DECIMAL(19, 4) NOT NULL,
  buy_out_price DECIMAL(19, 4) NOT NULL DEFAULT 0, -- Mới: Giá mua đứt, 0 nếu không có
  tick_size DECIMAL(19, 4) NOT NULL DEFAULT 1.00,   -- Mới: Bước giá tối thiểu
  current_highest_bid DECIMAL(19, 4) NOT NULL,
  status VARCHAR(50) NOT NULL, -- 'ACTIVE', 'CLOSED', 'CANCELLED'
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,

  FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
  INDEX idx_status (status)
) ENGINE=InnoDB;


-- -----------------------------------------------------
-- Table `bid_transactions`
-- -----------------------------------------------------
CREATE TABLE bid_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  auction_id BIGINT NOT NULL,
  bidder_id BIGINT NOT NULL,
  bid_amount DECIMAL(19, 4) NOT NULL,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
  FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_auction_id (auction_id)
) ENGINE=InnoDB;


-- =================================================================
-- SAMPLE DATA
-- =================================================================
INSERT INTO users (id, username, password_hash, created_at) VALUES
(1, 'seller_user', '$2a$10$...', NOW()),
(2, 'bidder_user', '$2a$10$...', NOW()),
(3, 'admin_user', '$2a$10$...', NOW());

INSERT INTO user_roles (user_id, role_name) VALUES
(1, 'SELLER'),
(2, 'BIDDER'),
(3, 'ADMIN'), (3, 'SELLER'), (3, 'BIDDER');

INSERT INTO items (id, name, description, owner_id, type, created_at, artist_name, brand) VALUES
(101, 'Mona Lisa Print', 'A high-quality print of the famous painting.', 1, 'art', NOW(), 'Leonardo da Vinci', NULL),
(102, '2022 Honda Civic', 'A reliable used car, good condition.', 1, 'vehicle', NOW(), NULL, 'Honda'),
(103, 'Used MacBook Pro 16"', '2019 model, 16GB RAM, 512GB SSD.', 3, 'electronics', NOW(), NULL, 'Apple');

INSERT INTO auctions (id, item_id, starting_price, buy_out_price, tick_size, current_highest_bid, status, start_time, end_time) VALUES
(1001, 101, 100.00, 500.00, 10.00, 100.00, 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
(1002, 102, 15000.00, 18000.00, 100.00, 15000.00, 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY));

INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp) VALUES
(1001, 2, 110.00, NOW());

-- =================================================================
-- END OF SCRIPT
-- =================================================================
