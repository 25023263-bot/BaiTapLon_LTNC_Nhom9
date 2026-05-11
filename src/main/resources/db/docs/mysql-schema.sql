-- =============================================================================
-- mysql-schema.sql – Auction platform (MySQL 8+)
-- Generated from SQLite schema, adapted to MySQL types & syntax.
--
-- Notes:
--   - Uses utf8mb4 for full Unicode.
--   - Monetary fields use DECIMAL(19,4).
--   - DATETIME(3) for timestamp columns.
--   - All FKs use InnoDB with ON DELETE CASCADE / SET NULL.
-- =============================================================================

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

-- ─── users (table-per-subclass root) ────────────────────────────────────────
CREATE TABLE users (
                       id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                       username         VARCHAR(100)    NOT NULL,
                       email            VARCHAR(255)    NOT NULL,
                       password_hash    VARCHAR(255)    NOT NULL,
                       full_name        VARCHAR(255),
                       phone            VARCHAR(50),
                       role             ENUM('BUYER','SELLER','ADMIN') NOT NULL,
                       active           TINYINT(1)     NOT NULL DEFAULT 1,
                       created_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                       updated_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_username (username),
                       UNIQUE KEY uk_users_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── buyers ─────────────────────────────────────────────────────────────────
CREATE TABLE buyers (
                        user_id          BIGINT UNSIGNED NOT NULL,
                        wallet_balance   DECIMAL(19,4)   NOT NULL DEFAULT 0.0,
                        total_wins       INT UNSIGNED    NOT NULL DEFAULT 0,
                        PRIMARY KEY (user_id),
                        CONSTRAINT fk_buyers_user
                            FOREIGN KEY (user_id) REFERENCES users(id)
                                ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── sellers ────────────────────────────────────────────────────────────────
CREATE TABLE sellers (
                         user_id          BIGINT UNSIGNED NOT NULL,
                         earnings_balance DECIMAL(19,4)   NOT NULL DEFAULT 0.0,
                         total_sold       INT UNSIGNED    NOT NULL DEFAULT 0,
                         rating           DECIMAL(4,2)    NOT NULL DEFAULT 0.0,
                         rating_count     INT UNSIGNED    NOT NULL DEFAULT 0,
                         PRIMARY KEY (user_id),
                         CONSTRAINT fk_sellers_user
                             FOREIGN KEY (user_id) REFERENCES users(id)
                                 ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── admins ─────────────────────────────────────────────────────────────────
CREATE TABLE admins (
                        user_id          BIGINT UNSIGNED NOT NULL,
                        access_level     INT             NOT NULL DEFAULT 1,
                        notes            TEXT,
                        PRIMARY KEY (user_id),
                        CONSTRAINT fk_admins_user
                            FOREIGN KEY (user_id) REFERENCES users(id)
                                ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── auctions ───────────────────────────────────────────────────────────────
CREATE TABLE auctions (
                          id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                          seller_id           BIGINT UNSIGNED NOT NULL,
                          title               VARCHAR(255)    NOT NULL,
                          description         TEXT,
                          category            VARCHAR(100),
                          image_url           VARCHAR(500),
                          item_type           ENUM('PHYSICAL','DIGITAL') NOT NULL,
                          starting_price      DECIMAL(19,4)   NOT NULL,
                          min_bid_increment   DECIMAL(19,4)   NOT NULL DEFAULT 1000.0,
                          buy_now_price       DECIMAL(19,4)            DEFAULT NULL,
                          current_price       DECIMAL(19,4)   NOT NULL,
                          leading_bidder_id   BIGINT UNSIGNED          DEFAULT NULL,
                          status              ENUM('PENDING','ACTIVE','CLOSED','EXPIRED','CANCELLED')
                        NOT NULL DEFAULT 'PENDING',
                          start_time          DATETIME(3)     NOT NULL,
                          end_time            DATETIME(3)     NOT NULL,
                          created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                          PRIMARY KEY (id),
                          KEY idx_auctions_seller    (seller_id),
                          KEY idx_auctions_status    (status),
                          KEY idx_auctions_end_time  (end_time),
                          KEY idx_auctions_category  (category),
                          CONSTRAINT fk_auctions_seller
                              FOREIGN KEY (seller_id) REFERENCES users(id)
                                  ON DELETE CASCADE,
                          CONSTRAINT fk_auctions_leading_bidder
                              FOREIGN KEY (leading_bidder_id) REFERENCES users(id)
                                  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── physical_items ────────────────────────────────────────────────────────
CREATE TABLE physical_items (
                                auction_id       BIGINT UNSIGNED NOT NULL,
                                condition_text   VARCHAR(100)    NOT NULL DEFAULT 'GOOD',
                                weight_grams     DECIMAL(19,4)   NOT NULL DEFAULT 0,
                                dimensions       VARCHAR(100),
                                location         VARCHAR(255),
                                shipping_cost    DECIMAL(19,4)   NOT NULL DEFAULT 0.0,
                                allow_pickup     TINYINT(1)      NOT NULL DEFAULT 0,
                                PRIMARY KEY (auction_id),
                                CONSTRAINT fk_physical_items_auction
                                    FOREIGN KEY (auction_id) REFERENCES auctions(id)
                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── digital_items ─────────────────────────────────────────────────────────
CREATE TABLE digital_items (
                               auction_id               BIGINT UNSIGNED NOT NULL,
                               digital_type             VARCHAR(100)    NOT NULL,
                               platform                 VARCHAR(100),
                               file_size_mb             DECIMAL(19,4),
                               expiry_date              DATETIME(3),
                               delivery_content         TEXT            NOT NULL,
                               replacement_guarantee    TINYINT(1)      NOT NULL DEFAULT 0,
                               PRIMARY KEY (auction_id),
                               CONSTRAINT fk_digital_items_auction
                                   FOREIGN KEY (auction_id) REFERENCES auctions(id)
                                       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── bids ───────────────────────────────────────────────────────────────────
CREATE TABLE bids (
                      id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                      auction_id       BIGINT UNSIGNED NOT NULL,
                      buyer_id         BIGINT UNSIGNED NOT NULL,
                      amount           DECIMAL(19,4)   NOT NULL,
                      bid_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                      auto_bid         TINYINT(1)      NOT NULL DEFAULT 0,
                      auto_bid_limit   DECIMAL(19,4)            DEFAULT NULL,
                      PRIMARY KEY (id),
                      KEY idx_bids_auction (auction_id),
                      KEY idx_bids_buyer   (buyer_id),
                      CONSTRAINT fk_bids_auction
                          FOREIGN KEY (auction_id) REFERENCES auctions(id)
                              ON DELETE CASCADE,
                      CONSTRAINT fk_bids_buyer
                          FOREIGN KEY (buyer_id) REFERENCES users(id)
                              ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── watchlist ─────────────────────────────────────────────────────────────
CREATE TABLE watchlist (
                           id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                           buyer_id         BIGINT UNSIGNED NOT NULL,
                           auction_id       BIGINT UNSIGNED NOT NULL,
                           added_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                           PRIMARY KEY (id),
                           UNIQUE KEY uk_watchlist_buyer_auction (buyer_id, auction_id),
                           KEY idx_watchlist_buyer   (buyer_id),
                           KEY idx_watchlist_auction (auction_id),
                           CONSTRAINT fk_watchlist_buyer
                               FOREIGN KEY (buyer_id) REFERENCES users(id)
                                   ON DELETE CASCADE,
                           CONSTRAINT fk_watchlist_auction
                               FOREIGN KEY (auction_id) REFERENCES auctions(id)
                                   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── transactions ──────────────────────────────────────────────────────────
CREATE TABLE transactions (
                              id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                              auction_id       BIGINT UNSIGNED NOT NULL,
                              buyer_id         BIGINT UNSIGNED NOT NULL,
                              seller_id        BIGINT UNSIGNED NOT NULL,
                              amount           DECIMAL(19,4)   NOT NULL,
                              shipping_fee     DECIMAL(19,4)   NOT NULL DEFAULT 0.0,
                              platform_fee     DECIMAL(19,4)   NOT NULL DEFAULT 0.0,
                              total_paid       DECIMAL(19,4)   NOT NULL,
                              seller_receives  DECIMAL(19,4)   NOT NULL,
                              payment_status   ENUM('PENDING','COMPLETED','FAILED','REFUNDED')
                        NOT NULL DEFAULT 'PENDING',
                              payment_method   VARCHAR(50)     NOT NULL DEFAULT 'WALLET',
                              external_ref     VARCHAR(255),
                              created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                              completed_at     DATETIME(3),
                              PRIMARY KEY (id),
                              KEY idx_tx_buyer   (buyer_id),
                              KEY idx_tx_seller  (seller_id),
                              KEY idx_tx_auction (auction_id),
                              CONSTRAINT fk_tx_auction
                                  FOREIGN KEY (auction_id) REFERENCES auctions(id),
                              CONSTRAINT fk_tx_buyer
                                  FOREIGN KEY (buyer_id) REFERENCES users(id),
                              CONSTRAINT fk_tx_seller
                                  FOREIGN KEY (seller_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;