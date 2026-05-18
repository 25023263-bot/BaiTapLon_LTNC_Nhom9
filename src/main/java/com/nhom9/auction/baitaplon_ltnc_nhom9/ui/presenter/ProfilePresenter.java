package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.CurrencyFormatHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tab Cá nhân: thông tin user, ví, dialog nạp tiền.
 *
 * <h3>Thay đổi so với phiên bản cũ:</h3>
 * <ul>
 *   <li>Bỏ hoàn toàn {@code WalletDepositService} — không còn gọi DB trực tiếp.</li>
 *   <li>{@link #confirmDeposit()} gửi {@code DEPOSIT_WALLET} qua
 *       {@link ServerConnection#depositWallet(int, BigDecimal)} trên background thread.</li>
 *   <li>{@link #refresh(boolean)} đọc từ {@code UserSession.getCurrentUserDTO()}
 *       thay vì {@code getCurrentUser()} — phù hợp kiến trúc socket-login.</li>
 *   <li>Sau khi deposit thành công, cập nhật số dư trong {@code UserSession}
 *       mà không cần reload toàn bộ profile.</li>
 * </ul>
 */
public final class ProfilePresenter {

    private static final Logger LOG = Logger.getLogger(ProfilePresenter.class.getName());
    private static final String DEPOSIT_ACCOUNT_NUMBER = "0366855207";
    private static final BigDecimal MIN_DEPOSIT = new BigDecimal("10000");

    private ProfileView view;
    private Runnable onNavigateHome;

    public ProfilePresenter() {}

    public void bind(ProfileView view, Runnable onNavigateHome) {
        this.view = view;
        this.onNavigateHome = onNavigateHome;
    }

    // ── Refresh UI ────────────────────────────────────────────────────────────

    public void refresh(boolean logged) {
        if (view == null || view.profileTitleLabel() == null) return;

        if (logged) {
            UserDTO dto = resolveDTO();
            if (dto == null) return;

            setVisible(view.profileScrollPane(), true);
            setVisible(view.guestProfilePane(), false);

            String displayName = (dto.getFullName() != null && !dto.getFullName().isBlank())
                    ? dto.getFullName() : dto.getUsername();
            view.profileTitleLabel().setText(displayName);
            view.profileHintLabel().setText("Quyền: " + dto.getRole() + " · " + dto.getEmail());
            view.profileTabLoginButton().setVisible(false);
            view.profileTabLoginButton().setManaged(false);
            view.profileLogoutButton().setVisible(true);
            view.profileLogoutButton().setManaged(true);

            updateHeroMonogram(dto.getUsername(), dto.getFullName());
            refreshInfo(dto);
            refreshWallet(dto);
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

    // ── Deposit overlay ───────────────────────────────────────────────────────

    public void openDeposit() {
        view.depositAmountField().clear();
        view.depositAmountHint().setText("Tối thiểu: 10.000 ₫");
        view.depositAmountHint().setStyle("");
        setVisible(view.depositStatusBox(), false);
        view.btnConfirmDeposit().setDisable(false);
        view.btnConfirmDeposit().setText("Xác nhận đã chuyển khoản");
        view.btnConfirmDeposit().setOnAction(e -> confirmDeposit());
        setVisible(view.depositOverlay(), true);
    }

    public void closeDeposit() {
        setVisible(view.depositOverlay(), false);
    }

    public boolean handleBackdropClick() {
        if (view.depositOverlay().isVisible()) {
            setVisible(view.depositOverlay(), false);
            return true;
        }
        if (onNavigateHome != null) onNavigateHome.run();
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

    /**
     * Xác nhận nạp tiền — gửi DEPOSIT_WALLET qua socket (background thread).
     *
     * Luồng:
     *   1. Validate amount trên FX thread
     *   2. Hiện trạng thái "đang xử lý"
     *   3. Gửi request qua background thread (PauseTransition 2s để UX mượt)
     *   4. Cập nhật UI và UserSession sau khi thành công
     */
    public void confirmDeposit() {
        BigDecimal amount;
        try {
            amount = parseAndValidate(view.depositAmountField().getText());
        } catch (IllegalArgumentException e) {
            view.depositAmountHint().setText("⚠  " + e.getMessage());
            view.depositAmountHint().setStyle("-fx-text-fill: #e05555;");
            return;
        }

        // Hiện trạng thái đang xử lý
        view.btnConfirmDeposit().setDisable(true);
        view.depositAmountField().setDisable(true);
        view.depositStatusIcon().setText("⏳");
        view.depositStatusText().setText("Đang xác nhận giao dịch...\nVui lòng chờ trong giây lát.");
        setVisible(view.depositStatusBox(), true);

        // Delay 2 giây để UX mượt, rồi gửi socket
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> sendDepositToServer(amount));
        pause.play();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Gửi DEPOSIT_WALLET request qua socket trên background thread.
     * Phải gọi trên FX thread (vì PauseTransition callback chạy trên FX thread).
     */
    private void sendDepositToServer(BigDecimal amount) {
        int userId = UserSession.getInstance().getCurrentUserId();

        Thread t = new Thread(() -> {
            try {
                ServerConnection.depositWallet(userId, amount);
                Platform.runLater(() -> applyDepositSuccess(amount));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Nạp ví thất bại", e);
                Platform.runLater(() -> showDepositError(
                        "Lỗi kết nối server. Vui lòng thử lại.\n" + e.getMessage()));
            }
        }, "deposit-wallet-thread");
        t.setDaemon(true);
        t.start();
    }

    private void applyDepositSuccess(BigDecimal amount) {
        // Cập nhật số dư trong UserSession (không cần reload toàn bộ profile)
        UserDTO dto = resolveDTO();
        if (dto != null && dto.getWalletBalance() != null) {
            dto.setWalletBalance(dto.getWalletBalance().add(amount));
        }

        view.depositStatusIcon().setText("✅");
        view.depositStatusText().setText("Nạp tiền thành công!\n+"
                + CurrencyFormatHelper.formatVnd(amount) + " đã được cộng vào tài khoản.");

        view.depositAmountField().setDisable(false);
        view.btnConfirmDeposit().setText("Đóng");
        view.btnConfirmDeposit().setDisable(false);
        view.btnConfirmDeposit().setOnAction(e -> {
            closeDeposit();
            if (dto != null) refreshWallet(dto);
            view.btnConfirmDeposit().setOnAction(ev -> confirmDeposit());
        });

        if (dto != null) refreshWallet(dto);
    }

    private void showDepositError(String message) {
        view.depositStatusIcon().setText("❌");
        view.depositStatusText().setText(message);
        view.btnConfirmDeposit().setDisable(false);
        view.depositAmountField().setDisable(false);
    }

    private void refreshInfo(UserDTO dto) {
        // Bật card thông tin cá nhân lên (mặc định ẩn trong FXML)
        if (view.profileInfoSection() != null) setVisible(view.profileInfoSection(), true);

        if (view.infoFullName() != null)
            view.infoFullName().setText(dto.getFullName() != null ? dto.getFullName() : "—");
        if (view.infoEmail() != null)
            view.infoEmail().setText(dto.getEmail() != null ? dto.getEmail() : "—");
        if (view.infoPhone() != null)
            view.infoPhone().setText(dto.getPhone() != null ? dto.getPhone() : "—");
        if (view.infoRole() != null)
            view.infoRole().setText(dto.getRole() != null ? dto.getRole().name() : "—");
        if (view.infoCreatedAt() != null)
            view.infoCreatedAt().setText("—"); // UserDTO không có createdAt
    }

    private void refreshWallet(UserDTO dto) {
        if (view.walletBalanceLabel() == null) return;

        boolean isSeller = dto.getRole() == UserRole.SELLER;

        BigDecimal balance = isSeller
                ? (dto.getEarningsBalance() != null ? dto.getEarningsBalance() : BigDecimal.ZERO)
                : (dto.getWalletBalance()   != null ? dto.getWalletBalance()   : BigDecimal.ZERO);

        view.walletBalanceLabel().setText(CurrencyFormatHelper.formatVnd(balance));
        if (view.walletTypeLabel() != null)
            view.walletTypeLabel().setText(isSeller ? "Thu nhập tích luỹ" : "Số dư ví");

        // Ẩn/hiện nút nạp tiền (Seller không nạp tiền, chỉ rút)
        if (view.walletDivider() != null) setVisible(view.walletDivider(), !isSeller);
        if (view.profileWalletSection() != null) setVisible(view.profileWalletSection(), true);
    }

    /**
     * Lấy UserDTO từ session — ưu tiên DTO (socket login), fallback sang User (local login).
     */
    private UserDTO resolveDTO() {
        UserSession s = UserSession.getInstance();
        if (!s.isLoggedIn()) return null;
        return s.getCurrentUserDTO();
    }

    /**
     * Parse và validate số tiền nhập từ TextField.
     * Hỗ trợ các định dạng: "100000", "100,000", "100.000"
     */
    private BigDecimal parseAndValidate(String raw) {
        if (raw == null || raw.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập số tiền cần nạp.");

        String cleaned = raw.trim().replaceAll("[,.]", "").replaceAll("\\s+", "");
        BigDecimal amount;
        try {
            amount = new BigDecimal(cleaned).setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ. Ví dụ: 50000");
        }
        if (amount.compareTo(MIN_DEPOSIT) < 0)
            throw new IllegalArgumentException(
                    "Số tiền tối thiểu là " + CurrencyFormatHelper.formatVnd(MIN_DEPOSIT));
        if (amount.compareTo(new BigDecimal("500000000")) > 0)
            throw new IllegalArgumentException("Số tiền tối đa một lần nạp là 500.000.000 ₫");

        return amount;
    }

    private static String firstLetter(String s) {
        if (s == null || s.isBlank()) return "?";
        return String.valueOf(Character.toUpperCase(s.charAt(0)));
    }

    private static void setVisible(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }
}