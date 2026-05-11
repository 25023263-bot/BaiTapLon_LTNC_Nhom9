-- Chèn dữ liệu vào bảng users
INSERT IGNORE INTO users (id, username, email, password_hash, full_name, phone, role, active)
VALUES
  (1, 'admin',    'admin@auction.vn',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Quản trị viên', '0900000000', 'ADMIN',  1),
  (2, 'seller1',  'seller1@gmail.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Nguyễn Văn Bán', '0911111111', 'SELLER', 1),
  (3, 'seller2',  'seller2@gmail.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Trần Thị Bán',  '0922222222', 'SELLER', 1),
  (4, 'buyer1',   'buyer1@gmail.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Lê Văn Mua',    '0933333333', 'BUYER',  1),
  (5, 'buyer2',   'buyer2@gmail.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y', 'Phạm Thị Mua',  '0944444444', 'BUYER',  1);

-- Chèn dữ liệu vào các bảng con của users
INSERT IGNORE INTO admins   (user_id, access_level) VALUES (1, 2);
INSERT IGNORE INTO sellers  (user_id, earnings_balance, total_sold, rating, rating_count) VALUES (2, 5000000, 3, 4.5, 6);
INSERT IGNORE INTO sellers  (user_id, earnings_balance, total_sold, rating, rating_count) VALUES (3, 2000000, 1, 4.0, 2);
INSERT IGNORE INTO buyers   (user_id, wallet_balance, total_wins) VALUES (4, 10000000, 2);
INSERT IGNORE INTO buyers   (user_id, wallet_balance, total_wins) VALUES (5, 5000000,  1);

-- Chèn dữ liệu vào bảng auctions
-- Lưu ý: MySQL dùng NOW() thay vì datetime('now')
INSERT IGNORE INTO auctions
  (id, seller_id, title, description, category, item_type,
   starting_price, min_bid_increment, buy_now_price, current_price,
   leading_bidder_id, status, start_time, end_time)
VALUES
  (1, 2, 'iPhone 14 Pro Max 256GB – Tím Đậm',
   'Máy mới 100%, còn bảo hành 11 tháng, đầy đủ phụ kiện.',
   'Điện thoại', 'PHYSICAL',
   20000000, 500000, 28000000, 22500000,
   4, 'ACTIVE',
   DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY)),
  (2, 2, 'MacBook Air M2 8GB/256GB',
   'Máy đẹp 99%, dùng 3 tháng, còn bảo hành Apple.',
   'Laptop', 'PHYSICAL',
   25000000, 1000000, NULL, 26000000,
   5, 'ACTIVE',
   DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY)),
  (3, 3, 'Adobe Photoshop 2024 – License Key 1 năm',
   'Key chính hãng, kích hoạt được 1 thiết bị, hỗ trợ Windows & Mac.',
   'Phần mềm', 'DIGITAL',
   500000, 50000, 900000, 650000,
   4, 'ACTIVE',
   DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY)),
  (4, 3, 'Nintendo Switch OLED – White',
   'Máy mới nguyên seal, nhập Nhật.',
   'Game', 'PHYSICAL',
   8000000, 200000, 11000000, 8000000,
   NULL, 'PENDING',
   DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY)),
  (5, 2, 'AirPods Pro 2nd Gen',
   'Hộp nguyên seal, mua Mỹ tháng trước.',
   'Phụ kiện', 'PHYSICAL',
   3000000, 100000, NULL, 4200000,
   5, 'CLOSED',
   DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Chèn vào các bảng chi tiết vật lý/kỹ thuật số
INSERT IGNORE INTO physical_items (auction_id, condition_text, weight_grams, dimensions, location, shipping_cost, allow_pickup)
VALUES
  (1, 'NEW',       206,  '16.1×7.8×0.8 cm', 'Hà Nội', 50000,  1),
  (2, 'LIKE_NEW', 1290, '30.4×21.5×1.1 cm', 'TP.HCM', 80000,  0),
  (4, 'NEW',       420, '23.9×10.2×1.4 cm', 'Hà Nội', 60000,  1),
  (5, 'NEW',        56,   '6.3×4.5×2.0 cm', 'Đà Nẵng', 30000, 0);

INSERT IGNORE INTO digital_items (auction_id, digital_type, platform, file_size_mb, delivery_content, replacement_guarantee)
VALUES
  (3, 'SOFTWARE_KEY', 'Windows, Mac', NULL, 'XXXX-XXXX-XXXX-DEMO', 1);

-- Chèn dữ liệu lịch sử đấu giá (bids)
INSERT IGNORE INTO bids (auction_id, buyer_id, amount, bid_time, auto_bid)
VALUES
  (1, 4, 20500000, DATE_SUB(NOW(), INTERVAL 50 MINUTE), 0),
  (1, 5, 21000000, DATE_SUB(NOW(), INTERVAL 40 MINUTE), 0),
  (1, 4, 22000000, DATE_SUB(NOW(), INTERVAL 30 MINUTE), 0),
  (1, 4, 22500000, DATE_SUB(NOW(), INTERVAL 10 MINUTE), 1),
  (2, 5, 26000000, DATE_SUB(NOW(), INTERVAL 2 HOUR),    0),
  (3, 4, 600000,   DATE_SUB(NOW(), INTERVAL 20 MINUTE), 0),
  (3, 4, 650000,   DATE_SUB(NOW(), INTERVAL 5 MINUTE),  0),
  (5, 5, 4200000,  DATE_SUB(NOW(), INTERVAL 2 DAY),     0);

-- Chèn vào danh sách theo dõi (watchlist)
INSERT IGNORE INTO watchlist (buyer_id, auction_id) VALUES (4, 2);
INSERT IGNORE INTO watchlist (buyer_id, auction_id) VALUES (4, 3);
INSERT IGNORE INTO watchlist (buyer_id, auction_id) VALUES (5, 1);

-- Chèn vào giao dịch (transactions)
INSERT IGNORE INTO transactions
  (auction_id, buyer_id, seller_id, amount, shipping_fee, platform_fee, total_paid, seller_receives, payment_status, payment_method, completed_at)
VALUES
  (5, 5, 2, 4200000, 30000, 84000, 4230000, 4116000, 'COMPLETED', 'WALLET', DATE_SUB(NOW(), INTERVAL 1 DAY));