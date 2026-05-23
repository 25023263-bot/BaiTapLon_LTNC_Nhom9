-- =============================================================================
-- schema.sql – SQLite (dùng để phát triển local)
-- Đồng bộ với Java domain models hiện tại (tháng 5/2026)
--
-- Khác biệt so với MySQL:
--   INTEGER PRIMARY KEY = rowid (tương đương AUTO_INCREMENT)
--   Không có ENUM – dùng TEXT + CHECK constraint
--   Không có TINYINT(1) – dùng INTEGER (0 = false, 1 = true)
--   Không có ON UPDATE CURRENT_TIMESTAMP
-- =============================================================================

PRAGMA foreign_keys = ON;

-- ─── users ───────────────────────────────────────────────────────────────────
-- Ánh xạ: User.java (abstract) – Buyer/Seller/Admin extends User
CREATE TABLE IF NOT EXISTS users (
                                     id            INTEGER PRIMARY KEY,
                                     username      TEXT    NOT NULL UNIQUE,
                                     email         TEXT    NOT NULL UNIQUE,
                                     password_hash TEXT    NOT NULL,
                                     full_name     TEXT,
                                     phone         TEXT,
                                     role          TEXT    NOT NULL CHECK (role IN ('BUYER', 'SELLER', 'ADMIN')),
                                     active        INTEGER NOT NULL DEFAULT 1,
                                     created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
                                     updated_at    TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- ─── buyers ──────────────────────────────────────────────────────────────────
-- Ánh xạ: Buyer.java – chỉ có walletBalance (total_wins chưa có trong model)
CREATE TABLE IF NOT EXISTS buyers (
                                      user_id        INTEGER PRIMARY KEY,
                                      wallet_balance REAL    NOT NULL DEFAULT 0.0,

                                      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                      CHECK (wallet_balance >= 0)
);

-- ─── sellers ─────────────────────────────────────────────────────────────────
-- Ánh xạ: Seller.java – chỉ có earningsBalance
-- (total_sold, rating, rating_count chưa có trong model)
CREATE TABLE IF NOT EXISTS sellers (
                                       user_id          INTEGER PRIMARY KEY,
                                       earnings_balance REAL    NOT NULL DEFAULT 0.0,

                                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                       CHECK (earnings_balance >= 0)
);

-- ─── admins ──────────────────────────────────────────────────────────────────
-- Ánh xạ: Admin.java
-- Admin.java không có field đặc thù nào ngoài thông tin kế thừa từ User.java
-- Bảng này chỉ dùng để đánh dấu user_id là admin (tách biệt khỏi buyers/sellers)
CREATE TABLE IF NOT EXISTS admins (
                                      user_id INTEGER PRIMARY KEY,

                                      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ─── auctions ────────────────────────────────────────────────────────────────
-- Ánh xạ: AuctionItem.java (abstract) – PhysicalItem / DigitalItem extends AuctionItem
CREATE TABLE IF NOT EXISTS auctions (
                                        id                INTEGER PRIMARY KEY,
                                        seller_id         INTEGER NOT NULL,
                                        title             TEXT    NOT NULL,
                                        description       TEXT,
                                        category          TEXT,
                                        image_url         TEXT,
                                        item_type         TEXT    NOT NULL CHECK (item_type IN ('PHYSICAL', 'DIGITAL')),
                                        starting_price    REAL    NOT NULL,
                                        min_bid_increment REAL    NOT NULL DEFAULT 1000.0,
                                        current_price     REAL    NOT NULL,
                                        leading_bidder_id INTEGER,
                                        status            TEXT    NOT NULL DEFAULT 'PENDING'
                                            CHECK (status IN ('PENDING', 'ACTIVE', 'CLOSED', 'EXPIRED', 'CANCELLED')),
                                        start_time        TEXT    NOT NULL,
                                        end_time          TEXT    NOT NULL,
                                        created_at        TEXT    NOT NULL DEFAULT (datetime('now')),

                                        FOREIGN KEY (seller_id)         REFERENCES users(id) ON DELETE CASCADE,
                                        FOREIGN KEY (leading_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
                                        CHECK (starting_price > 0),
                                        CHECK (min_bid_increment > 0)
);

-- ─── physical_items ──────────────────────────────────────────────────────────
-- Ánh xạ: PhysicalItem.java
-- PhysicalItem.java đã xoá toàn bộ physical-only fields (condition, weight,
-- dimensions, location, shippingCost, allowPickup) vì UI hiện tại không
-- thu thập và không hiển thị những thông tin này.
--
-- Bảng vẫn được giữ lại vì:
--   1. Khi sau này thêm DigitalItem, cần bảng riêng để lưu thuộc tính đặc thù
--   2. Dễ mở rộng thêm cột sau mà không phải sửa bảng auctions
CREATE TABLE IF NOT EXISTS physical_items (
                                              auction_id INTEGER PRIMARY KEY,

                                              FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);


-- ─── bids ────────────────────────────────────────────────────────────────────
-- Ánh xạ: Bid.java
CREATE TABLE IF NOT EXISTS bids (
                                    id             INTEGER PRIMARY KEY,
                                    auction_id     INTEGER NOT NULL,
                                    buyer_id       INTEGER NOT NULL,
                                    amount         REAL    NOT NULL,
                                    bid_time       TEXT    NOT NULL DEFAULT (datetime('now')),
                                    auto_bid       INTEGER NOT NULL DEFAULT 0,
                                    auto_bid_limit REAL,

                                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                    FOREIGN KEY (buyer_id)   REFERENCES users(id)    ON DELETE CASCADE,
                                    CHECK (amount > 0)
);



-- ─── notifications ───────────────────────────────────────────────────────────
-- Ánh xạ: Notification.java
CREATE TABLE IF NOT EXISTS notifications (
                                             id         INTEGER PRIMARY KEY,
                                             user_id    INTEGER NOT NULL,
                                             auction_id INTEGER,
                                             type       TEXT    NOT NULL
                                                 CHECK (type IN ('NEW_BID', 'OUTBID', 'AUCTION_CLOSED',
                                                                 'AUCTION_STARTED', 'AUCTION_CANCELLED', 'ANTI_SNIPE')),
                                             message    TEXT    NOT NULL,
                                             is_read    INTEGER NOT NULL DEFAULT 0,
                                             created_at TEXT    NOT NULL DEFAULT (datetime('now')),

                                             FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
                                             FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE SET NULL
);

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_auctions_seller   ON auctions(seller_id);
CREATE INDEX IF NOT EXISTS idx_auctions_status   ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_auctions_end_time ON auctions(end_time);
CREATE INDEX IF NOT EXISTS idx_auctions_category ON auctions(category);

CREATE INDEX IF NOT EXISTS idx_bids_auction      ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_buyer        ON bids(buyer_id);



CREATE INDEX IF NOT EXISTS idx_notif_user        ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_auction     ON notifications(auction_id);