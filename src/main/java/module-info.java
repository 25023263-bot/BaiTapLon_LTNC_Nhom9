module com.nhom9.auction.baitaplon_ltnc_nhom9 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires java.sql;
    requires jbcrypt;

    // Database drivers
    requires org.xerial.sqlitejdbc;  // SQLite JDBC

    // Connection pool – BẮT BUỘC phải thêm vì DatabaseConnection.java dùng HikariCP
    requires com.zaxxer.hikari;

    // MySQL driver – bỏ comment khi chuyển sang MySQL (AppConfig.USE_MYSQL = true)
    // requires com.mysql.cj;

    opens com.nhom9.auction.baitaplon_ltnc_nhom9 to javafx.fxml;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9;

    opens com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller to javafx.fxml;

    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

    opens com.nhom9.auction to javafx.graphics;
    exports com.nhom9.auction;
}