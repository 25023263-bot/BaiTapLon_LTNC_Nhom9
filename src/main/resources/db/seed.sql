INSERT INTO users (username, email, password_hash, full_name, phone, role, active, created_at, updated_at)
VALUES (
           'admin',
           'admin@auction.vn',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9y',
           'Quản trị viên',
           '0900000000',
           'ADMIN',
           1,
           NOW(),
           NOW()
       );

INSERT INTO admins (user_id, access_level)
VALUES (LAST_INSERT_ID(), 2);