package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Node FXML: Sản phẩm của tôi, điều khoản Seller, form đăng bán. */
public record SellerProductsView(
        ToggleButton bottomNavHome,
        ToggleButton bottomNavMyProducts,
        VBox myProductsList,
        StackPane sellerTermsOverlay,
        CheckBox upgradeTermsMerchandise,
        CheckBox upgradeTermsContent,
        CheckBox upgradeTermsPrivacy,
        Label upgradeTermsMerchandiseError,
        Label upgradeTermsContentError,
        Label upgradeTermsPrivacyError,
        StackPane listProductOverlay,
        TextField listProductTitleField,
        ComboBox<String> listProductCategoryCombo,
        TextArea listProductDescArea,
        TextField listProductPriceField,
        DatePicker listProductEndDate,
        ComboBox<String> listProductEndHour,
        ComboBox<String> listProductEndMinute,
        Label lblEndTimePreview,
        StackPane imageUploadBox,
        Label listProductTitleError,
        Label listProductCategoryError,
        Label listProductDescError,
        Label listProductPriceError,
        Label listProductDateError,
        Label listProductImageError,
        Label submitProductError
) {}
