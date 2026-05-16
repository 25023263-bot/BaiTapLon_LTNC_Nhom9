package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Tham chiếu các node FXML của tab Cá nhân / nạp tiền. */
public record ProfileView(
        ScrollPane profileScrollPane,
        VBox guestProfilePane,
        Label profileTitleLabel,
        Label profileHintLabel,
        Label profileAvatarGlyph,
        Button profileTabLoginButton,
        Button profileLogoutButton,
        VBox profileInfoSection,
        Label infoFullName,
        Label infoEmail,
        Label infoPhone,
        Label infoRole,
        Label infoCreatedAt,
        Region walletDivider,
        VBox profileWalletSection,
        Label walletBalanceLabel,
        Label walletTypeLabel,
        VBox depositOverlay,
        VBox depositStatusBox,
        Label depositStatusIcon,
        Label depositStatusText,
        TextField depositAmountField,
        Label depositAmountHint,
        Button btnConfirmDeposit
) {}
