package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/** Node FXML cho danh sách đấu giá trên trang chủ. */
public record HomeCatalogView(
        HBox hotCardsContainer,
        GridPane allProductsGrid,
        TextField searchField,
        Label resultCountLabel,
        Button chipAll
) {}
