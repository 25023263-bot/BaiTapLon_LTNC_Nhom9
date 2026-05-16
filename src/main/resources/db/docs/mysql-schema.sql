-- =============================================================================
-- mysql-schema.sql – MySQL 8+ (dùng khi tích hợp Spring Boot)
-- Đồng bộ với Java domain models hiện tại (tháng 5/2026)
--
-- Quy ước:
--   utf8mb4 cho full Unicode (emoji, tiếng Việt, v.v.)
--   DECIMAL(19,4) cho tiền tệ – tránh lỗi làm tròn của FLOAT/DOUBLE
--   DATETIME(3) cho timestamp – độ chính xác millisecond
--   BIGINT UNSIGNED cho id
-- =============================================================================

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS watchlist;
DROP TABLE IF EXISTS bids;
DROP TABLE IF EXISTS digital_items;
DROP TABLE IF EXISTS physical_items;
DROP TABLE IF EXISTS auctions;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS sellers;
DROP TABLE IF EXISTS buyers;
DROP TABLE IF EXISTS users;

-- ─── users ───────────────────────────────────────────────────────────────────
-- Ánh xạ: User.java (abstract) – Buyer/Seller/Admin extends User
CREATE TABLE users (
                       id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                       username      VARCHAR(100)     NOT NULL,
                       email         VARCHAR(255)     NOT NULL,
                       password_hash VARCHAR(255)     NOT NULL,
                       full_name     VARCHAR(255),
                       phone         VARCHAR(50),
                       role          ENUM('BUYER','SELLER','ADMIN') NOT NULL,
                       active        TINYINT(1)       NOT NULL DEFAULT 1,
                       created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                       updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                   ON UPDATE CURRENT_TIMESTAMP(3),

                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_username (username),
                       UNIQUE KEY uk_users_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── buyers ──────────────────────────────────────────────────────────────────
-- Ánh xạ: Buyer.java – chỉ có walletBalance
-- (total_wins chưa có trong model, sẽ thêm sau)
CREATE TABLE buyers (
                        user_id        BIGINT UNSIGNED NOT NULL,
                        wallet_balance DECIMAL(19,4)   NOT NULL DEFAULT 0.0,

                        PRIMARY KEY (user_id),
                        CONSTRAINT fk_buyers_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT chk_buyers_wallet CHECK (wallet_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── sellers ─────────────────────────────────────────────────────────────────
-- Ánh xạ: Seller.java – chỉ có earningsBalance
-- (total_sold, rating, rating_count chưa có trong model, sẽ thêm sau)
CREATE TABLE sellers (
                         user_id          BIGINT UNSIGNED NOT NULL,
                         earnings_balance DECIMAL(19,4)   NOT NULL DEFAULT 0.0,

                         PRIMARY KEY (user_id),
                         CONSTRAINT fk_sellers_user
                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         CONSTRAINT chk_sellers_earnings CHECK (earnings_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── admins ──────────────────────────────────────────────────────────────────
-- Ánh xạ: Admin.java
CREATE TABLE admins (
                        user_id      BIGINT UNSIGNED NOT NULL,
                        access_level INT             NOT NULL DEFAULT 1,
                        notes        TEXT,

                        PRIMARY KEY (user_id),
                        CONSTRAINT fk_admins_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── auctions ────────────────────────────────────────────────────────────────
-- Ánh xạ: AuctionItem.java (abstract) – PhysicalItem / DigitalItem extends AuctionItem
CREATE TABLE auctions (
                          id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                          seller_id         BIGINT UNSIGNED  NOT NULL,
                          title             VARCHAR(255)     NOT NULL,
                          description       TEXT,
                          category          VARCHAR(100),
                          image_url         VARCHAR(500),
                          item_type         ENUM('PHYSICAL','DIGITAL') NOT NULL,
                          starting_price    DECIMAL(19,4)    NOT NULL,
                          min_bid_increment DECIMAL(19,4)    NOT NULL DEFAULT 1000.0,
                          buy_now_price     DECIMAL(19,4),
                          current_price     DECIMAL(19,4)    NOT NULL,
                          leading_bidder_id BIGINT UNSIGNED,
                          status            ENUM('PENDING','ACTIVE','CLOSED','EXPIRED','CANCELLED')
                                       NOT NULL DEFAULT 'PENDING',
                          start_time        DATETIME(3)      NOT NULL,
                          end_time          DATETIME(3)      NOT NULL,
                          created_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                          PRIMARY KEY (id),
                          KEY idx_auctions_seller   (seller_id),
                          KEY idx_auctions_status   (status),
                          KEY idx_auctions_end_time (end_time),
                          KEY idx_auctions_category (category),
                          CONSTRAINT fk_auctions_seller
                              FOREIGN KEY (seller_id)         REFERENCES users(id) ON DELETE CASCADE,
                          CONSTRAINT fk_auctions_leading_bidder
                              FOREIGN KEY (leading_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
                          CONSTRAINT chk_auctions_starting_price    CHECK (starting_price > 0),
                          CONSTRAINT chk_auctions_min_bid_increment CHECK (min_bid_increment > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── physical_items ──────────────────────────────────────────────────────────
-- Ánh xạ: PhysicalItem.java
-- Lưu ý: cột đặt tên là condition_text vì CONDITION là từ khoá trong SQL
CREATE TABLE physical_items (
                                auction_id     BIGINT UNSIGNED NOT NULL,
                                condition_text VARCHAR(100)    NOT NULL DEFAULT 'GOOD',
                                weight_grams   DECIMAL(10,2)   NOT NULL DEFAULT 0.0,
                                dimensions     VARCHAR(100),
                                location       VARCHAR(255),
                                shipping_cost  DECIMAL(19,4)   NOT NULL DEFAULT 0.0,
                                allow_pickup   TINYINT(1)      NOT NULL DEFAULT 0,

                                PRIMARY KEY (auction_id),
                                CONSTRAINT fk_physical_items_auction
                                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                CONSTRAINT chk_physical_shipping CHECK (shipping_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── digital_items ───────────────────────────────────────────────────────────
-- Ánh xạ: DigitalItem.java
CREATE TABLE digital_items (
                               auction_id            BIGINT UNSIGNED NOT NULL,
                               digital_type          VARCHAR(100)    NOT NULL,
                               platform              VARCHAR(100),
                               file_size_mb          DECIMAL(10,2),
                               expiry_date           DATETIME(3),
                               delivery_content      TEXT            NOT NULL,
                               replacement_guarantee TINYINT(1)      NOT NULL DEFAULT 0,

                               PRIMARY KEY (auction_id),
                               CONSTRAINT fk_digital_items_auction
                                   FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── bids ────────────────────────────────────────────────────────────────────
-- Ánh xạ: Bid.java
CREATE TABLE bids (
                      id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                      auction_id     BIGINT UNSIGNED NOT NULL,
                      buyer_id       BIGINT UNSIGNED NOT NULL,
                      amount         DECIMAL(19,4)   NOT NULL,
                      bid_time       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                      auto_bid       TINYINT(1)      NOT NULL DEFAULT 0,
                      auto_bid_limit DECIMAL(19,4),

                      PRIMARY KEY (id),
                      KEY idx_bids_auction (auction_id),
                      KEY idx_bids_buyer   (buyer_id),
                      CONSTRAINT fk_bids_auction
                          FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                      CONSTRAINT fk_bids_buyer
                          FOREIGN KEY (buyer_id)   REFERENCES users(id)    ON DELETE CASCADE,
                      CONSTRAINT chk_bids_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── watchlist ───────────────────────────────────────────────────────────────
-- Ánh xạ: WatchlistRepository.java (không có domain model riêng)
CREATE TABLE watchlist (
                           id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                           buyer_id   BIGINT UNSIGNED NOT NULL,
                           auction_id BIGINT UNSIGNED NOT NULL,
                           added_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                           PRIMARY KEY (id),
                           UNIQUE KEY uk_watchlist_buyer_auction (buyer_id, auction_id),
                           KEY idx_watchlist_buyer   (buyer_id),
                           KEY idx_watchlist_auction (auction_id),
                           CONSTRAINT fk_watchlist_buyer
                               FOREIGN KEY (buyer_id)   REFERENCES users(id)    ON DELETE CASCADE,
                           CONSTRAINT fk_watchlist_auction
                               FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── transactions ────────────────────────────────────────────────────────────
-- Ánh xạ: Transaction.java
-- Các cột đã xoá so với phiên bản cũ (chưa có trong model):
--   shipping_fee, platform_fee, total_paid, seller_receives, external_ref
CREATE TABLE transactions (
                              id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                              auction_id     BIGINT UNSIGNED  NOT NULL,
                              buyer_id       BIGINT UNSIGNED  NOT NULL,
                              seller_id      BIGINT UNSIGNED  NOT NULL,
                              amount         DECIMAL(19,4)    NOT NULL,
                              payment_method VARCHAR(50)      NOT NULL DEFAULT 'WALLET',
                              payment_status ENUM('PENDING','COMPLETED','FAILED','REFUNDED')
                                    NOT NULL DEFAULT 'PENDING',
                              created_at     DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                              completed_at   DATETIME(3),

                              PRIMARY KEY (id),
                              KEY idx_tx_buyer   (buyer_id),
                              KEY idx_tx_seller  (seller_id),
                              KEY idx_tx_auction (auction_id),
                              CONSTRAINT fk_tx_auction
                                  FOREIGN KEY (auction_id) REFERENCES auctions(id),
                              CONSTRAINT fk_tx_buyer
                                  FOREIGN KEY (buyer_id)   REFERENCES users(id),
                              CONSTRAINT fk_tx_seller
                                  FOREIGN KEY (seller_id)  REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── notifications ───────────────────────────────────────────────────────────
-- Ánh xạ: Notification.java – thêm CHECK constraint cho cột type
--         để đảm bảo khớp với Notification.Type enum trong Java
CREATE TABLE notifications (
                               id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                               user_id    BIGINT UNSIGNED NOT NULL,
                               auction_id BIGINT UNSIGNED,
                               type       ENUM('NEW_BID','OUTBID','AUCTION_CLOSED',
                    'AUCTION_STARTED','AUCTION_CANCELLED','ANTI_SNIPE') NOT NULL,
                               message    TEXT            NOT NULL,
                               is_read    TINYINT(1)      NOT NULL DEFAULT 0,
                               created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                               PRIMARY KEY (id),
                               KEY idx_notif_user    (user_id, is_read),
                               KEY idx_notif_auction (auction_id),
                               CONSTRAINT fk_notif_user
                                   FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
                               CONSTRAINT fk_notif_auction
                                   FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;