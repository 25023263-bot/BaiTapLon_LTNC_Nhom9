module com.nhom9.auction.baitaplon_ltnc_nhom9 {

    // ── JavaFX ──────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;

    // ── Third-party JavaFX libraries ────────────────────────────────────────
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;

    // ── Database & security ─────────────────────────────────────────────────
    requires java.sql;
    requires jbcrypt;
    requires org.xerial.sqlitejdbc;
    requires com.zaxxer.hikari;

    // ── MySQL connector ─────────────────────────────────────────────────────
    requires mysql.connector.j;

    // ── Root package ────────────────────────────────────────────────────────
    opens   com.nhom9.auction.baitaplon_ltnc_nhom9 to javafx.fxml;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9;

    // ── UI Controllers (opened for FXML reflection) ──────────────────────────
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller to javafx.fxml;

    // ── Domain model ─────────────────────────────────────────────────────────
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;

    // ── Service layer ────────────────────────────────────────────────────────
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing;

    // ── UI ───────────────────────────────────────────────────────────────────
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.factory;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.navigation;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network;

    // ── Socket client & server ────────────────────────────────────────────────
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.client;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol;

    // ── Mở toàn bộ module cho Mockito (chỉ dùng khi test) ────────────────────
    // WHY: Mockito cần reflection để tạo subclass giả của các concrete class
    // (Repository, Service...). JPMS chặn điều này theo mặc định.
    // "opens X" không giới hạn module cụ thể → cho phép tất cả, bao gồm
    // byte-buddy (thư viện Mockito dùng bên trong) và test runner của Maven.
    //
    // Đây là cách đơn giản nhất cho project học tập. Khi deploy production
    // thật sự, nên dùng interface thay vì concrete class để không cần opens.
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.repository;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service.wallet;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.exception;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.config;
}
