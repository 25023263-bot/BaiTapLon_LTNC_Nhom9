package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.wallet.WalletDepositService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.CurrencyFormatHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Tab Cá nhân: thông tin user, ví, dialog nạp tiền.
 * {@link com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.HomeController} giữ @FXML và delegate.
 */
public final class ProfilePresenter {

    private static final String DEPOSIT_ACCOUNT_NUMBER = "0366855207";

    private final WalletDepositService walletDepositService;
    private ProfileView view;
    private Runnable onNavigateHome;

    public ProfilePresenter(WalletDepositService walletDepositService) {
        this.walletDepositService = walletDepositService;
    }

    public void bind(ProfileView view, Runnable onNavigateHome) {
        this.view = view;
        this.onNavigateHome = onNavigateHome;
    }

    public void refresh(boolean logged) {
        if (view == null || view.profileTitleLabel() == null) return;
        if (logged) {
            User u = UserSession.getInstance().getCurrentUser();
            setVisible(view.profileScrollPane(), true);
            setVisible(view.guestProfilePane(), false);
            view.profileTitleLabel().setText(
                    u.getFullName() != null && !u.getFullName().isBlank()
                            ? u.getFullName() : u.getUsername());
            view.profileHintLabel().setText("Quyền: " + u.getRole() + " · " + u.getEmail());
            view.profileTabLoginButton().setVisible(false);
            view.profileTabLoginButton().setManaged(false);
            view.profileLogoutButton().setVisible(true);
            view.profileLogoutButton().setManaged(true);
            updateHeroMonogram(u.getUsername(), u.getFullName());
            refreshInfo(u);
            refreshWallet(u);
        } else {
            setVisible(view.profileScrollPane(), false);
            setVisible(view.guestProfilePane(), true);
        }
    }

    public void updateHeroMonogram(String username, String fullName) {
        if (view == null || view.profileAvatarGlyph() == null) return;
        view.profileAvatarGlyph().setText(
                fullName != null && !fullName.isBlank()
                        ? firstLetter(fullName)
                        : firstLetter(username));
    }

    public void openDeposit() {
        view.depositAmountField().clear();
        view.depositAmountHint().setText("Tối thiểu: 10.000 ₫");
        view.depositAmountHint().setStyle("");
        setVisible(view.depositStatusBox(), false);
        view.btnConfirmDeposit().setDisable(false);
        view.btnConfirmDeposit().setText("Xác nhận đã chuyển khoản");
        setVisible(view.depositOverlay(), true);
    }

    public void closeDeposit() {
        setVisible(view.depositOverlay(), false);
    }

    /**
     * Xử lý click backdrop profile. Trả về true nếu chỉ đóng dialog nạp tiền.
     */
    public boolean handleBackdropClick() {
        if (view.depositOverlay().isVisible()) {
            setVisible(view.depositOverlay(), false);
            return true;
        }
        if (onNavigateHome != null) {
            onNavigateHome.run();
        }
        return false;
    }

    public void quickDeposit(String amountText) {
        view.depositAmountField().setText(amountText);
        openDeposit();
    }

    public void copyAccountNumber() {
        ClipboardContent content = new ClipboardContent();
        content.putString(DEPOSIT_ACCOUNT_NUMBER);
        Clipboard.getSystemClipboard().setContent(content);
        AlertHelper.showToast("Đã sao chép số tài khoản!");
    }

    public void confirmDeposit() {
        BigDecimal amount;
        try {
            amount = walletDepositService.parseAmount(view.depositAmountField().getText());
            walletDepositService.validateAmount(amount);
        } catch (IllegalArgumentException e) {
            view.depositAmountHint().setText("⚠  " + e.getMessage());
            view.depositAmountHint().setStyle("-fx-text-fill: #e05555;");
            return;
        }

        view.btnConfirmDeposit().setDisable(true);
        view.depositAmountField().setDisable(true);
        view.depositStatusIcon().setText("⏳");
        view.depositStatusText().setText("Đang xác nhận giao dịch...\nVui lòng chờ trong giây lát.");
        setVisible(view.depositStatusBox(), true);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> Platform.runLater(() -> applyDepositSuccess(amount)));
        pause.play();
    }

    private void applyDepositSuccess(BigDecimal amount) {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        try {
            walletDepositService.deposit(u, amount);
        } catch (SQLException e) {
            showDepositError("Lỗi lưu số dư. Vui lòng thử lại.\n" + e.getMessage());
            return;
        } catch (IllegalStateException e) {
            showDepositError(e.getMessage());
            return;
        }

        view.depositStatusIcon().setText("✅");
        view.depositStatusText().setText("Nạp tiền thành công!\n+"
                + CurrencyFormatHelper.formatVnd(amount) + " đã được cộng vào tài khoản.");

        view.depositAmountField().setDisable(false);
        view.btnConfirmDeposit().setText("Đóng");
        view.btnConfirmDeposit().setDisable(false);
        view.btnConfirmDeposit().setOnAction(e -> {
            closeDeposit();
            refreshWallet(UserSession.getInstance().getCurrentUser());
            view.btnConfirmDeposit().setOnAction(ev -> confirmDeposit());
        });

        refreshWallet(u);
    }

    private void showDepositError(String message) {
        view.depositStatusIcon().setText("❌");
        view.depositStatusText().setText(message);
        view.btnConfirmDeposit().setDisable(false);
        view.depositAmountField().setDisable(false);
    }

    private void refreshInfo(User u) {
        boolean show = u != null;
        setVisible(view.profileInfoSection(), show);
        if (!show) return;

        view.infoFullName().setText(
                u.getFullName() != null && !u.getFullName().isBlank()
                        ? u.getFullName() : "Chưa cập nhật");
        view.infoEmail().setText(u.getEmail() != null ? u.getEmail() : "—");
        view.infoPhone().setText(
                u.getPhone() != null && !u.getPhone().isBlank()
                        ? u.getPhone() : "Chưa cập nhật");
        view.infoRole().setText(
                u.getRole() != null ? formatRole(u.getRole().name()) : "—");
        view.infoCreatedAt().setText(
                u.getCreatedAt() != null
                        ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "—");
    }

    private void refreshWallet(User u) {
        boolean show = u != null;
        setVisible(view.walletDivider(), show);
        setVisible(view.profileWalletSection(), show);
        if (!show) return;

        if (u instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer buyer) {
            BigDecimal balance = buyer.getWalletBalance() != null
                    ? buyer.getWalletBalance() : BigDecimal.ZERO;
            view.walletBalanceLabel().setText(CurrencyFormatHelper.formatVnd(balance));
            view.walletTypeLabel().setText("Ví Người mua");
        } else if (u instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller seller) {
            BigDecimal balance = seller.getEarningsBalance() != null
                    ? seller.getEarningsBalance() : BigDecimal.ZERO;
            view.walletBalanceLabel().setText(CurrencyFormatHelper.formatVnd(balance));
            view.walletTypeLabel().setText("Thu nhập Người bán");
        } else {
            view.walletBalanceLabel().setText("—");
            view.walletTypeLabel().setText("");
        }
    }

    private static String formatRole(String roleName) {
        return switch (roleName.toUpperCase()) {
            case "BUYER" -> "Người mua";
            case "SELLER" -> "Người bán";
            case "ADMIN" -> "Quản trị viên";
            default -> roleName;
        };
    }

    private static String firstLetter(String s) {
        String t = s.trim();
        if (t.isEmpty()) return "?";
        return t.substring(0, 1).toUpperCase();
    }

    private static void setVisible(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

}
