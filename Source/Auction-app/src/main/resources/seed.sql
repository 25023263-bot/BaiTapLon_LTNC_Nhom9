-- =============================================================================
-- seed.sql  –  Dữ liệu mẫu cho Auction House
-- =============================================================================
-- Password hash là BCrypt của "password123" cho tất cả tài khoản test.
-- Chạy sau schema.sql.
-- =============================================================================

-- ─── Users ───────────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO users (id, username, email, password_hash, full_name, phone, role, active)
VALUES
  (1, 'admin',    'admin@auction.vn',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Quản trị viên', '0900000000', 'ADMIN',  1),
  (2, 'seller1',  'seller1@gmail.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Nguyễn Văn Bán', '0911111111', 'SELLER', 1),
  (3, 'seller2',  'seller2@gmail.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Trần Thị Bán',  '0922222222', 'SELLER', 1),
  (4, 'buyer1',   'buyer1@gmail.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Lê Văn Mua',    '0933333333', 'BUYER',  1),
  (5, 'buyer2',   'buyer2@gmail.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Phạm Thị Mua',  '0944444444', 'BUYER',  1);

INSERT OR IGNORE INTO admins   (user_id, access_level) VALUES (1, 2);
INSERT OR IGNORE INTO sellers  (user_id, earnings_balance, total_sold, rating, rating_count) VALUES (2, 5000000, 3, 4.5, 6);
INSERT OR IGNORE INTO sellers  (user_id, earnings_balance, total_sold, rating, rating_count) VALUES (3, 2000000, 1, 4.0, 2);
INSERT OR IGNORE INTO buyers   (user_id, wallet_balance, total_wins) VALUES (4, 10000000, 2);
INSERT OR IGNORE INTO buyers   (user_id, wallet_balance, total_wins) VALUES (5, 5000000,  1);

-- ─── Auction Items ────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO auction_items
  (id, seller_id, title, description, category, item_type,
   starting_price, min_bid_increment, buy_now_price, current_price,
   leading_bidder_id, status, start_time, end_time)
VALUES
  -- Phiên đang ACTIVE
  (1, 2, 'iPhone 14 Pro Max 256GB – Tím Đậm',
   'Máy mới 100%, còn bảo hành 11 tháng, đầy đủ phụ kiện.',
   'Điện thoại', 'PHYSICAL',
   20000000, 500000, 28000000, 22500000,
   4, 'ACTIVE',
   datetime('now','-1 hour','localtime'), datetime('now','+2 day','localtime')),

  (2, 2, 'MacBook Air M2 8GB/256GB',
   'Máy đẹp 99%, dùng 3 tháng, còn bảo hành Apple.',
   'Laptop', 'PHYSICAL',
   25000000, 1000000, NULL, 26000000,
   5, 'ACTIVE',
   datetime('now','-3 hour','localtime'), datetime('now','+5 day','localtime')),

  (3, 3, 'Adobe Photoshop 2024 – License Key 1 năm',
   'Key chính hãng, kích hoạt được 1 thiết bị, hỗ trợ Windows & Mac.',
   'Phần mềm', 'DIGITAL',
   500000, 50000, 900000, 650000,
   4, 'ACTIVE',
   datetime('now','-30 minute','localtime'), datetime('now','+1 day','localtime')),

  -- Phiên sắp mở (PENDING)
  (4, 3, 'Nintendo Switch OLED – White',
   'Máy mới nguyên seal, nhập Nhật.',
   'Game', 'PHYSICAL',
   8000000, 200000, 11000000, 8000000,
   NULL, 'PENDING',
   datetime('now','+1 day','localtime'), datetime('now','+4 day','localtime')),

  -- Phiên đã kết thúc (CLOSED)
  (5, 2, 'AirPods Pro 2nd Gen',
   'Hộp nguyên seal, mua Mỹ tháng trước.',
   'Phụ kiện', 'PHYSICAL',
   3000000, 100000, NULL, 4200000,
   5, 'CLOSED',
   datetime('now','-5 day','localtime'), datetime('now','-1 day','localtime'));

-- Physical item details
INSERT OR IGNORE INTO physical_items (item_id, condition_text, weight_grams, dimensions, location, shipping_cost, allow_pickup)
VALUES
  (1, 'NEW',       206,  '16.1×7.8×0.8 cm', 'Hà Nội', 50000,  1),
  (2, 'LIKE_NEW', 1290, '30.4×21.5×1.1 cm', 'TP.HCM', 80000,  0),
  (4, 'NEW',       420, '23.9×10.2×1.4 cm', 'Hà Nội', 60000,  1),
  (5, 'NEW',        56,   '6.3×4.5×2.0 cm', 'Đà Nẵng', 30000, 0);

-- Digital item details
INSERT OR IGNORE INTO digital_items (item_id, digital_type, platform, file_size_mb, delivery_content, replacement_guarantee)
VALUES
  (3, 'SOFTWARE_KEY', 'Windows, Mac', NULL, 'XXXX-XXXX-XXXX-DEMO', 1);

-- ─── Bids ─────────────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO bids (item_id, bidder_id, amount, bid_time, auto_bid)
VALUES
  (1, 4, 20500000, datetime('now','-50 minute','localtime'), 0),
  (1, 5, 21000000, datetime('now','-40 minute','localtime'), 0),
  (1, 4, 22000000, datetime('now','-30 minute','localtime'), 0),
  (1, 4, 22500000, datetime('now','-10 minute','localtime'), 1),
  (2, 5, 26000000, datetime('now','-2 hour','localtime'),    0),
  (3, 4, 600000,   datetime('now','-20 minute','localtime'), 0),
  (3, 4, 650000,   datetime('now','-5 minute','localtime'),  0),
  (5, 5, 4200000,  datetime('now','-2 day','localtime'),     0);

-- ─── Watchlist ────────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO watchlist (buyer_id, item_id) VALUES (4, 2);
INSERT OR IGNORE INTO watchlist (buyer_id, item_id) VALUES (4, 3);
INSERT OR IGNORE INTO watchlist (buyer_id, item_id) VALUES (5, 1);

-- ─── Transactions ─────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO transactions
  (item_id, buyer_id, seller_id, amount, shipping_fee, platform_fee, total_paid, seller_receives, payment_status, payment_method, completed_at)
VALUES
  (5, 5, 2, 4200000, 30000, 84000, 4230000, 4116000, 'COMPLETED', 'WALLET', datetime('now','-1 day','localtime'));