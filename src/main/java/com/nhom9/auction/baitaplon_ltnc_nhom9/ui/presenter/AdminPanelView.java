package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Node FXML cho bảng quản trị Admin. */
public record AdminPanelView(
        StackPane adminOverlay,
        Label adminSubtitleLabel,
        Button adminTabUsers,
        Button adminTabAuctions,
        Region adminTabUsersIndicator,
        Region adminTabAuctionsIndicator,
        VBox adminUsersPanel,
        VBox adminAuctionsPanel,
        TextField adminUserSearchField,
        ComboBox<String> adminUserRoleFilter,
        VBox adminUsersList,
        VBox adminUsersEmpty,
        TextField adminAuctionSearchField,
        ComboBox<String> adminAuctionStatusFilter,
        VBox adminAuctionsList,
        VBox adminAuctionsEmpty
) {}
