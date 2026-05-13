-- 1. Bật kiểm tra khóa ngoại (Thay cho PRAGMA)
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS users (
                                     id INT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(100) NOT NULL UNIQUE,
                                     email VARCHAR(150) NOT NULL UNIQUE,
                                     password_hash VARCHAR(255) NOT NULL,
                                     full_name VARCHAR(255),
                                     phone VARCHAR(20),
                                     role ENUM('BUYER', 'SELLER', 'ADMIN') NOT NULL,
                                     active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0: Inactive, 1: Active',
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS buyers (
                                      user_id INT PRIMARY KEY,
                                      wallet_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                      total_wins INT NOT NULL DEFAULT 0,
                                      CONSTRAINT fk_buyer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                      CONSTRAINT check_wallet CHECK (wallet_balance >= 0),
                                      CONSTRAINT check_wins CHECK (total_wins >= 0)
);

CREATE TABLE IF NOT EXISTS sellers (
                                       user_id INT PRIMARY KEY,
                                       earnings_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                       total_sold INT NOT NULL DEFAULT 0,
                                       rating DOUBLE NOT NULL DEFAULT 0.0,
                                       rating_count INT NOT NULL DEFAULT 0,
                                       CONSTRAINT fk_seller_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                       CONSTRAINT check_earnings CHECK (earnings_balance >= 0),
                                       CONSTRAINT check_sold CHECK (total_sold >= 0)
);

CREATE TABLE IF NOT EXISTS admins (
                                      user_id INT PRIMARY KEY,
                                      access_level INT NOT NULL DEFAULT 1,
                                      notes TEXT,
                                      CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
                                        id INT PRIMARY KEY AUTO_INCREMENT,
                                        seller_id INT NOT NULL,
                                        title VARCHAR(255) NOT NULL,
                                        description TEXT,
                                        category VARCHAR(100),
                                        image_url VARCHAR(500),
                                        item_type ENUM('PHYSICAL', 'DIGITAL') NOT NULL,
                                        starting_price DECIMAL(15, 2) NOT NULL,
                                        min_bid_increment DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
                                        buy_now_price DECIMAL(15, 2),
                                        current_price DECIMAL(15, 2) NOT NULL,
                                        leading_bidder_id INT,
                                        status ENUM('PENDING', 'ACTIVE', 'CLOSED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
                                        start_time DATETIME NOT NULL,
                                        end_time DATETIME NOT NULL,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT fk_auction_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_auction_bidder FOREIGN KEY (leading_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
                                        CONSTRAINT check_starting_price CHECK (starting_price > 0),
                                        CONSTRAINT check_increment CHECK (min_bid_increment > 0)
);

CREATE TABLE IF NOT EXISTS physical_items (
                                              auction_id INT PRIMARY KEY,
                                              condition_text VARCHAR(100) NOT NULL DEFAULT 'GOOD',
                                              weight_grams DOUBLE NOT NULL DEFAULT 0,
                                              dimensions VARCHAR(100),
                                              location VARCHAR(255),
                                              shipping_cost DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                              allow_pickup TINYINT(1) NOT NULL DEFAULT 0,
                                              CONSTRAINT fk_physical_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                              CONSTRAINT check_shipping CHECK (shipping_cost >= 0)
);

CREATE TABLE IF NOT EXISTS digital_items (
                                             auction_id INT PRIMARY KEY,
                                             digital_type VARCHAR(100) NOT NULL,
                                             platform VARCHAR(100),
                                             file_size_mb DOUBLE,
                                             expiry_date DATETIME,
                                             delivery_content TEXT NOT NULL,
                                             replacement_guarantee TINYINT(1) NOT NULL DEFAULT 0,
                                             CONSTRAINT fk_digital_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bids (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    auction_id INT NOT NULL,
                                    buyer_id INT NOT NULL,
                                    amount DECIMAL(15, 2) NOT NULL,
                                    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    auto_bid TINYINT(1) NOT NULL DEFAULT 0,
                                    auto_bid_limit DECIMAL(15, 2),
                                    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_bid_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
                                    CONSTRAINT check_bid_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS watchlist (
                                         id INT PRIMARY KEY AUTO_INCREMENT,
                                         buyer_id INT NOT NULL,
                                         auction_id INT NOT NULL,
                                         added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         UNIQUE (buyer_id, auction_id),
                                         CONSTRAINT fk_watch_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
                                         CONSTRAINT fk_watch_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
                                            id INT PRIMARY KEY AUTO_INCREMENT,
                                            auction_id INT NOT NULL,
                                            buyer_id INT NOT NULL,
                                            seller_id INT NOT NULL,
                                            amount DECIMAL(15, 2) NOT NULL,
                                            shipping_fee DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                            platform_fee DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                            total_paid DECIMAL(15, 2) NOT NULL,
                                            seller_receives DECIMAL(15, 2) NOT NULL,
                                            payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
                                            payment_method VARCHAR(50) NOT NULL DEFAULT 'WALLET',
                                            external_ref VARCHAR(255),
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            completed_at DATETIME,
                                            CONSTRAINT fk_trans_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
                                            CONSTRAINT fk_trans_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
                                            CONSTRAINT fk_trans_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Index trong MySQL không cần "IF NOT EXISTS" nếu khai báo sau CREATE TABLE
CREATE INDEX idx_auctions_seller ON auctions(seller_id);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time);
CREATE INDEX idx_auctions_category ON auctions(category);
CREATE INDEX idx_bids_auction ON bids(auction_id);
CREATE INDEX idx_bids_buyer ON bids(buyer_id);
CREATE INDEX idx_watchlist_buyer ON watchlist(buyer_id);
CREATE INDEX idx_watchlist_auction ON watchlist(auction_id);
CREATE INDEX idx_transactions_buyer ON transactions(buyer_id);
CREATE INDEX idx_transactions_seller ON transactions(seller_id);
CREATE INDEX idx_transactions_auction ON transactions(auction_id);
-- ─── Notifications ────────────────────────────────────────────────────────────
-- Lưu thông báo persistent: mất điện / restart vẫn còn.
-- Hai loại người nhận:
--   1. seller_id của phiên đấu giá
--   2. buyer đã từng bid vào phiên đó (DISTINCT buyer_id từ bảng bids)
CREATE TABLE IF NOT EXISTS notifications (
                                             id          INT PRIMARY KEY AUTO_INCREMENT,
                                             user_id     INT  NOT NULL,                -- người nhận thông báo
                                             auction_id  INT,                          -- phiên liên quan (nullable: thông báo hệ thống)
                                             type        VARCHAR(30) NOT NULL,         -- 'NEW_BID' | 'OUTBID' | 'AUCTION_CLOSED' |
    -- 'AUCTION_STARTED' | 'AUCTION_CANCELLED' | 'ANTI_SNIPE'
                                             message     TEXT NOT NULL,
                                             is_read     TINYINT(1) NOT NULL DEFAULT 0,
                                             created_at  DATETIME NOT NULL,
                                             CONSTRAINT fk_notif_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
                                             CONSTRAINT fk_notif_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_notif_user    ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_auction ON notifications(auction_id);