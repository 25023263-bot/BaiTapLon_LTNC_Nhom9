# Hướng dẫn: Chuyển từ SQLite sang MySQL

## Tổng quan

Kiến trúc hiện tại được thiết kế để việc chuyển đổi database **chỉ cần sửa ở
một vài chỗ**, không cần đụng vào business logic hay service layer.

---

## Bước 1 – Cài MySQL và tạo database

```sql
-- Chạy trong MySQL Workbench hoặc terminal mysql
CREATE DATABASE auction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER 'auction_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON auction_db.* TO 'auction_user'@'localhost';
FLUSH PRIVILEGES;
```

Sau đó chạy file schema:
```
mysql -u auction_user -p auction_db < src/main/resources/db/docs/mysql-schema.sql
```

---

## Bước 2 – Sửa `pom.xml`

Bỏ comment phần MySQL connector và **giữ nguyên** SQLite (dùng để test):

```xml
<!-- Bỏ comment dòng này: -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.1.0</version>
</dependency>
```

---

## Bước 3 – Sửa `AppConfig.java`

```java
// Dòng duy nhất cần đổi:
public static final boolean USE_MYSQL = true;  // false → true

// Điền thông tin server:
public static final String MYSQL_HOST     = "localhost";   // hoặc IP server
public static final String MYSQL_DATABASE = "auction_db";
public static final String MYSQL_USER     = "auction_user";
public static final String MYSQL_PASSWORD = "your_password";
```

> ℹ️ **Tại sao chỉ cần đổi 1 dòng?**
> Vì `DatabaseConnection` đọc `AppConfig.USE_MYSQL` để quyết định dùng driver nào,
> URL nào, pool size bao nhiêu. Các repository không biết đang dùng database gì.

---

## Bước 4 – Sửa `module-info.java`

Thêm 2 dòng sau vào file `src/main/java/module-info.java`:

```java
requires com.mysql.cj;         // MySQL JDBC driver
requires com.zaxxer.hikari;    // HikariCP (phải thêm dù dùng SQLite hay MySQL)
```

File hoàn chỉnh sẽ trông như thế này:
```java
module com.nhom9.auction.baitaplon_ltnc_nhom9 {
    requires javafx.controls;
    requires javafx.fxml;
    // ... các requires cũ ...

    requires com.zaxxer.hikari;   // ← THÊM
    requires com.mysql.cj;        // ← THÊM (chỉ khi dùng MySQL)
    requires org.xerial.sqlitejdbc; // ← GIỮ (dùng khi test với SQLite)
}
```

---

## Bước 5 – Seed data cho MySQL

File `seed.sql` hiện dùng cú pháp SQLite (`INSERT OR IGNORE`).
Cho MySQL, dùng:

```sql
-- Thay INSERT OR IGNORE → INSERT IGNORE (MySQL syntax)
INSERT IGNORE INTO users (id, username, email, ...) VALUES (...);
INSERT IGNORE INTO buyers (user_id, ...) VALUES (...);
-- ... tương tự cho các bảng khác
```

Hoặc chạy seed từ code Java (gọi `DatabaseConnection` sau khi `USE_MYSQL = true`).

---

## Kiểm tra nhanh sau khi chuyển đổi

```java
// Trong main() hoặc một test đơn giản:
try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
    DatabaseMetaData meta = conn.getMetaData();
    System.out.println("Database: " + meta.getDatabaseProductName());
    // Phải in ra "MySQL" (hoặc "SQLite" nếu chưa đổi)
}
```

---

## Những gì KHÔNG cần thay đổi

| Thứ | Lý do |
|-----|-------|
| `UserRepository`, `AuctionRepository`, ... | SQL viết theo chuẩn ANSI, không SQLite-specific |
| `AuctionHouse`, `AuthService`, ... | Không biết gì về database |
| `DatabaseConnection.java` | Đã tự switch giữa SQLite và MySQL qua `USE_MYSQL` |
| `schema.sql` | Vẫn dùng cho SQLite. MySQL dùng `mysql-schema.sql` |

---

## Điểm khác biệt đã được xử lý

| Vấn đề | SQLite | MySQL | Giải pháp trong code |
|--------|--------|-------|----------------------|
| Hàm thời gian | `datetime('now','localtime')` | `NOW()` | `DbUtil.nowSql()` |
| Timestamp format | TEXT `"2025-05-10 14:30:00"` | DATETIME `"2025-05-10 14:30:00"` | `DbUtil.DB_FMT` |
| UPSERT | `INSERT OR REPLACE` | `ON DUPLICATE KEY UPDATE` | Check-then-insert/update |
| Watchlist add | `INSERT OR IGNORE` | `INSERT IGNORE` | Check `isWatching()` trước |
| Connection pool | pool size = 1 | pool size = 10 | `AppConfig.DB_POOL_SIZE` |
| Boolean | `INTEGER (0/1)` | `TINYINT(1)` | JDBC `getInt() == 1` (giống nhau) |

---

## Lưu ý quan trọng về timezone

MySQL lưu `DATETIME` theo timezone của server. Đảm bảo cấu hình đúng:

```
# Trong MYSQL_URL (đã có sẵn trong AppConfig):
&serverTimezone=Asia/Ho_Chi_Minh
```

Nếu server MySQL đặt ở múi giờ khác (ví dụ UTC), dữ liệu thời gian sẽ bị lệch 7 giờ.

---

## Câu hỏi thường gặp

**Q: Dữ liệu SQLite có chuyển sang MySQL được không?**

A: Có, nhưng cần export-import thủ công:
1. Export SQLite → CSV (dùng DBeaver hoặc sqlite3 CLI)
2. Import CSV → MySQL (dùng LOAD DATA INFILE hoặc MySQL Workbench)
3. Đảm bảo thứ tự import đúng (users trước, rồi buyers/sellers, rồi auctions, ...)

**Q: Có thể test với SQLite rồi deploy với MySQL không?**

A: Có! Đây chính là lý do thiết kế với `USE_MYSQL` flag.
Dùng SQLite khi develop (nhanh, không cần server), MySQL khi deploy (ổn định hơn).