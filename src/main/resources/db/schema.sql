PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username TEXT NOT NULL UNIQUE,
                                     email TEXT NOT NULL UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     full_name TEXT,
                                     phone TEXT,

                                     role TEXT NOT NULL
                                         CHECK (role IN ('BUYER','SELLER','ADMIN')),

                                     active INTEGER NOT NULL DEFAULT 1,

                                     created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS buyers (
                                      user_id INTEGER PRIMARY KEY,
                                      wallet_balance REAL NOT NULL DEFAULT 0.0,
                                      total_wins INTEGER NOT NULL DEFAULT 0,

                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id)
                                          ON DELETE CASCADE,

                                      CHECK (wallet_balance >= 0),
                                      CHECK (total_wins >= 0)
);

CREATE TABLE IF NOT EXISTS sellers (
                                       user_id INTEGER PRIMARY KEY,
                                       earnings_balance REAL NOT NULL DEFAULT 0.0,
                                       total_sold INTEGER NOT NULL DEFAULT 0,
                                       rating REAL NOT NULL DEFAULT 0.0,
                                       rating_count INTEGER NOT NULL DEFAULT 0,

                                       FOREIGN KEY (user_id)
                                           REFERENCES users(id)
                                           ON DELETE CASCADE,

                                       CHECK (earnings_balance >= 0)
);

CREATE TABLE IF NOT EXISTS admins (
                                      user_id INTEGER PRIMARY KEY,
                                      access_level INTEGER NOT NULL DEFAULT 1,
                                      notes TEXT,

                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id)
                                          ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,

                                        seller_id INTEGER NOT NULL,
                                        title TEXT NOT NULL,
                                        description TEXT,
                                        category TEXT,
                                        image_url TEXT,

                                        item_type TEXT NOT NULL
                                            CHECK (item_type IN ('PHYSICAL','DIGITAL')),

                                        starting_price REAL NOT NULL,
                                        min_bid_increment REAL NOT NULL DEFAULT 1000.0,
                                        buy_now_price REAL,
                                        current_price REAL NOT NULL,
                                        leading_bidder_id INTEGER,

                                        status TEXT NOT NULL DEFAULT 'PENDING'
                                            CHECK (status IN ('PENDING','ACTIVE','CLOSED','EXPIRED','CANCELLED')),

                                        start_time TEXT NOT NULL,
                                        end_time TEXT NOT NULL,

                                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,

                                        FOREIGN KEY (seller_id)
                                            REFERENCES users(id)
                                            ON DELETE CASCADE,

                                        FOREIGN KEY (leading_bidder_id)
                                            REFERENCES users(id)
                                            ON DELETE SET NULL,

                                        CHECK (starting_price > 0),
                                        CHECK (min_bid_increment > 0)
);

CREATE TABLE IF NOT EXISTS physical_items (
                                              auction_id INTEGER PRIMARY KEY,

                                              condition_text TEXT NOT NULL DEFAULT 'GOOD',
                                              weight_grams REAL NOT NULL DEFAULT 0,
                                              dimensions TEXT,
                                              location TEXT,

                                              shipping_cost REAL NOT NULL DEFAULT 0.0,
                                              allow_pickup INTEGER NOT NULL DEFAULT 0,

                                              FOREIGN KEY (auction_id)
                                                  REFERENCES auctions(id)
                                                  ON DELETE CASCADE,

                                              CHECK (shipping_cost >= 0)
);

CREATE TABLE IF NOT EXISTS digital_items (
                                             auction_id INTEGER PRIMARY KEY,

                                             digital_type TEXT NOT NULL,
                                             platform TEXT,
                                             file_size_mb REAL,
                                             expiry_date TEXT,
                                             delivery_content TEXT NOT NULL,
                                             replacement_guarantee INTEGER NOT NULL DEFAULT 0,

                                             FOREIGN KEY (auction_id)
                                                 REFERENCES auctions(id)
                                                 ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bids (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                                    auction_id INTEGER NOT NULL,
                                    buyer_id INTEGER NOT NULL,

                                    amount REAL NOT NULL,

                                    bid_time TEXT DEFAULT CURRENT_TIMESTAMP,

                                    auto_bid INTEGER NOT NULL DEFAULT 0,
                                    auto_bid_limit REAL,

                                    FOREIGN KEY (auction_id)
                                        REFERENCES auctions(id)
                                        ON DELETE CASCADE,

                                    FOREIGN KEY (buyer_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                    CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS watchlist (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,

                                         buyer_id INTEGER NOT NULL,
                                         auction_id INTEGER NOT NULL,

                                         added_at TEXT DEFAULT CURRENT_TIMESTAMP,

                                         UNIQUE (buyer_id, auction_id),

                                         FOREIGN KEY (buyer_id)
                                             REFERENCES users(id)
                                             ON DELETE CASCADE,

                                         FOREIGN KEY (auction_id)
                                             REFERENCES auctions(id)
                                             ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,

                                            auction_id INTEGER NOT NULL,
                                            buyer_id INTEGER NOT NULL,
                                            seller_id INTEGER NOT NULL,

                                            amount REAL NOT NULL,

                                            shipping_fee REAL NOT NULL DEFAULT 0.0,
                                            platform_fee REAL NOT NULL DEFAULT 0.0,

                                            total_paid REAL NOT NULL,
                                            seller_receives REAL NOT NULL,

                                            payment_status TEXT NOT NULL DEFAULT 'PENDING'
                                                CHECK (payment_status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),

                                            payment_method TEXT NOT NULL DEFAULT 'WALLET',

                                            external_ref TEXT,

                                            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                                            completed_at TEXT,

                                            FOREIGN KEY (auction_id) REFERENCES auctions(id),
                                            FOREIGN KEY (buyer_id) REFERENCES users(id),
                                            FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,

                                             user_id INTEGER NOT NULL,
                                             auction_id INTEGER,

                                             type TEXT NOT NULL,
                                             message TEXT NOT NULL,

                                             is_read INTEGER NOT NULL DEFAULT 0,

                                             created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             FOREIGN KEY (user_id)
                                                 REFERENCES users(id)
                                                 ON DELETE CASCADE,

                                             FOREIGN KEY (auction_id)
                                                 REFERENCES auctions(id)
                                                 ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_auctions_seller
    ON auctions(seller_id);

CREATE INDEX IF NOT EXISTS idx_auctions_status
    ON auctions(status);

CREATE INDEX IF NOT EXISTS idx_auctions_end_time
    ON auctions(end_time);

CREATE INDEX IF NOT EXISTS idx_auctions_category
    ON auctions(category);

CREATE INDEX IF NOT EXISTS idx_bids_auction
    ON bids(auction_id);

CREATE INDEX IF NOT EXISTS idx_bids_buyer
    ON bids(buyer_id);

CREATE INDEX IF NOT EXISTS idx_watchlist_buyer
    ON watchlist(buyer_id);

CREATE INDEX IF NOT EXISTS idx_watchlist_auction
    ON watchlist(auction_id);

CREATE INDEX IF NOT EXISTS idx_transactions_buyer
    ON transactions(buyer_id);

CREATE INDEX IF NOT EXISTS idx_transactions_seller
    ON transactions(seller_id);

CREATE INDEX IF NOT EXISTS idx_transactions_auction
    ON transactions(auction_id);

CREATE INDEX IF NOT EXISTS idx_notif_user
    ON notifications(user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_notif_auction
    ON notifications(auction_id);