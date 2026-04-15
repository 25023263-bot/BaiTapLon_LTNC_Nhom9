-- =============================================================================
-- schema.sql  –  Auction House Database Schema (SQLite)
-- =============================================================================
-- Chạy với: PRAGMA foreign_keys = ON;
-- Tất cả bảng dùng IF NOT EXISTS để an toàn khi chạy lại.
-- =============================================================================

PRAGMA foreign_keys = ON;

-- ─── users ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
                                     id               INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username         TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                                     email            TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                                     password_hash    TEXT    NOT NULL,
                                     full_name        TEXT,
                                     phone            TEXT,
                                     role             TEXT    NOT NULL CHECK (role IN ('BUYER','SELLER','ADMIN')),
    active           INTEGER NOT NULL DEFAULT 1,          -- 0/1 boolean
    created_at       TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at       TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
    );

-- ─── buyers (thông tin mở rộng cho role BUYER) ────────────────────────────────
CREATE TABLE IF NOT EXISTS buyers (
                                      user_id          INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    wallet_balance   REAL    NOT NULL DEFAULT 0.0 CHECK (wallet_balance >= 0),
    total_wins       INTEGER NOT NULL DEFAULT 0   CHECK (total_wins >= 0)
    );

-- ─── sellers (thông tin mở rộng cho role SELLER) ──────────────────────────────
CREATE TABLE IF NOT EXISTS sellers (
                                       user_id          INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    earnings_balance REAL    NOT NULL DEFAULT 0.0 CHECK (earnings_balance >= 0),
    total_sold       INTEGER NOT NULL DEFAULT 0   CHECK (total_sold >= 0),
    rating           REAL    NOT NULL DEFAULT 0.0,
    rating_count     INTEGER NOT NULL DEFAULT 0
    );

-- ─── admins (thông tin mở rộng cho role ADMIN) ────────────────────────────────
CREATE TABLE IF NOT EXISTS admins (
                                      user_id          INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    access_level     INTEGER NOT NULL DEFAULT 1,
    notes            TEXT
    );

-- ─── auction_items ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auction_items (
                                             id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                                             seller_id           INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               TEXT    NOT NULL,
    description         TEXT,
    category            TEXT,
    image_url           TEXT,
    item_type           TEXT    NOT NULL CHECK (item_type IN ('PHYSICAL','DIGITAL')),
    starting_price      REAL    NOT NULL CHECK (starting_price > 0),
    min_bid_increment   REAL    NOT NULL DEFAULT 1000 CHECK (min_bid_increment > 0),
    buy_now_price       REAL,                              -- NULL = không có Buy Now
    current_price       REAL    NOT NULL,
    leading_bidder_id   INTEGER REFERENCES users(id) ON DELETE SET NULL,
    status              TEXT    NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','ACTIVE','CLOSED','EXPIRED','CANCELLED')),
    start_time          TEXT    NOT NULL,
    end_time            TEXT    NOT NULL,
    created_at          TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
    );

-- ─── physical_items (chi tiết vật phẩm vật lý) ───────────────────────────────
CREATE TABLE IF NOT EXISTS physical_items (
                                              item_id          INTEGER PRIMARY KEY REFERENCES auction_items(id) ON DELETE CASCADE,
    condition_text   TEXT    NOT NULL DEFAULT 'GOOD',
    weight_grams     REAL    NOT NULL DEFAULT 0,
    dimensions       TEXT,
    location         TEXT,
    shipping_cost    REAL    NOT NULL DEFAULT 0 CHECK (shipping_cost >= 0),
    allow_pickup     INTEGER NOT NULL DEFAULT 0
    );

-- ─── digital_items (chi tiết vật phẩm kỹ thuật số) ──────────────────────────
CREATE TABLE IF NOT EXISTS digital_items (
                                             item_id                  INTEGER PRIMARY KEY REFERENCES auction_items(id) ON DELETE CASCADE,
    digital_type             TEXT    NOT NULL,
    platform                 TEXT,
    file_size_mb             REAL,
    expiry_date              TEXT,
    delivery_content         TEXT    NOT NULL,  -- key/link, chỉ lộ sau thanh toán
    replacement_guarantee    INTEGER NOT NULL DEFAULT 0
    );

-- ─── bids ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bids (
                                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                                    item_id          INTEGER NOT NULL REFERENCES auction_items(id) ON DELETE CASCADE,
    bidder_id        INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount           REAL    NOT NULL CHECK (amount > 0),
    bid_time         TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    auto_bid         INTEGER NOT NULL DEFAULT 0,
    auto_bid_limit   REAL
    );

-- ─── watchlist ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS watchlist (
                                         id               INTEGER PRIMARY KEY AUTOINCREMENT,
                                         buyer_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id          INTEGER NOT NULL REFERENCES auction_items(id) ON DELETE CASCADE,
    added_at         TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (buyer_id, item_id)
    );

-- ─── transactions ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
                                            id               INTEGER PRIMARY KEY AUTOINCREMENT,
                                            item_id          INTEGER NOT NULL REFERENCES auction_items(id),
    buyer_id         INTEGER NOT NULL REFERENCES users(id),
    seller_id        INTEGER NOT NULL REFERENCES users(id),
    amount           REAL    NOT NULL,
    shipping_fee     REAL    NOT NULL DEFAULT 0,
    platform_fee     REAL    NOT NULL DEFAULT 0,
    total_paid       REAL    NOT NULL,
    seller_receives  REAL    NOT NULL,
    payment_status   TEXT    NOT NULL DEFAULT 'PENDING'
    CHECK (payment_status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),
    payment_method   TEXT    NOT NULL DEFAULT 'WALLET',
    external_ref     TEXT,
    created_at       TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    completed_at     TEXT
    );

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_items_seller      ON auction_items(seller_id);
CREATE INDEX IF NOT EXISTS idx_items_status      ON auction_items(status);
CREATE INDEX IF NOT EXISTS idx_items_end_time    ON auction_items(end_time);
CREATE INDEX IF NOT EXISTS idx_items_category    ON auction_items(category);
CREATE INDEX IF NOT EXISTS idx_bids_item         ON bids(item_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder       ON bids(bidder_id);
CREATE INDEX IF NOT EXISTS idx_watchlist_buyer   ON watchlist(buyer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_buyer  ON transactions(buyer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_seller ON transactions(seller_id);
CREATE INDEX IF NOT EXISTS idx_transactions_item   ON transactions(item_id);