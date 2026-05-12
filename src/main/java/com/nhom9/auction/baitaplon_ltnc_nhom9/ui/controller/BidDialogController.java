package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Controller cho popup Đặt Giá Thầu (BidDialogView.fxml).
 *
 * Luồng hoạt động:
 *   ItemDetailCoordinator.openBidDialog(item, ownerStage)
 *       → load BidDialogView.fxml
 *       → gọi configure(currentBid, stage)
 *       → người dùng chọn / nhập giá → bấm "Tiếp tục"
 *       → onBidConfirmed.run(selectedAmount) (callback trả về coordinator)
 */
public class BidDialogController {

    // ── FXML fields ──────────────────────────────────────────────
    @FXML private Label  lblCurrentPrice;
    @FXML private Label  lblMinPrice;

    @FXML private Button btnQ1;
    @FXML private Button btnQ2;
    @FXML private Button btnQ3;
    @FXML private Button btnQ4;

    @FXML private HBox      bidInputWrapper; // wrapper để đổi viền khi focus
    @FXML private TextField tfAmount;
    @FXML private Label     lblError;

    // ── State ────────────────────────────────────────────────────
    /** Stage này chính là cửa sổ popup — lưu để có thể đóng từ handleClose/handleSubmit */
    private Stage thisStage;

    /** Bước tăng tối thiểu mỗi lần đặt giá */
    private double increment;

    /** Bốn mức giá nhanh tương ứng btnQ1-Q4 */
    private final double[] quickAmounts = new double[4];

    /** Callback được gọi khi user xác nhận — truyền số tiền đã chọn */
    private java.util.function.Consumer<Double> onBidConfirmed;

    // ── Public API ───────────────────────────────────────────────

    /**
     * Gọi sau khi load FXML để truyền dữ liệu vào dialog.
     *
     * @param currentBid      Giá đấu cao nhất hiện tại (đơn vị: đồng)
     * @param stage           Stage của dialog này (để tự đóng khi cần)
     * @param onBidConfirmed  Callback nhận số tiền user đã xác nhận
     */
    public void configure(double currentBid, Stage stage,
                          java.util.function.Consumer<Double> onBidConfirmed) {

        this.thisStage       = stage;
        this.onBidConfirmed  = onBidConfirmed;

        // Tính bước tăng hợp lý dựa trên quy mô giá
        // Ví dụ: giá 200 triệu → increment 1 triệu; giá 1 tỷ → increment 5 triệu
        this.increment = computeIncrement(currentBid);

        // Tính 4 mức giá nhanh
        double minBid = currentBid + increment;
        quickAmounts[0] = minBid;
        quickAmounts[1] = minBid + increment * 5;
        quickAmounts[2] = minBid + increment * 10;
        quickAmounts[3] = minBid + increment * 25;

        // Cập nhật label giá
        lblCurrentPrice.setText(formatVnd(currentBid));
        lblMinPrice.setText(formatVnd(minBid));

        // Cập nhật text 4 nút nhanh
        btnQ1.setText(formatVnd(quickAmounts[0]));
        btnQ2.setText(formatVnd(quickAmounts[1]));
        btnQ3.setText(formatVnd(quickAmounts[2]));
        btnQ4.setText(formatVnd(quickAmounts[3]));

        // Điền sẵn mức tối thiểu vào ô nhập
        tfAmount.setText(String.valueOf((long) minBid));

        // Highlight btnQ1 mặc định (mức tối thiểu)
        selectQuickButton(btnQ1);

        // Khi người dùng gõ vào ô nhập → bỏ highlight nút nhanh
        tfAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            clearQuickSelection();
            hideError();
        });

        // Hiệu ứng viền vàng khi focus ô nhập
        tfAmount.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                bidInputWrapper.getStyleClass().add("bid-input-wrapper-focused");
            } else {
                bidInputWrapper.getStyleClass().remove("bid-input-wrapper-focused");
            }
        });
    }

    // ── FXML handlers ────────────────────────────────────────────

    @FXML private void handleClose()  { closeDialog(); }

    @FXML private void handleQuick1() { pickQuick(0, btnQ1); }
    @FXML private void handleQuick2() { pickQuick(1, btnQ2); }
    @FXML private void handleQuick3() { pickQuick(2, btnQ3); }
    @FXML private void handleQuick4() { pickQuick(3, btnQ4); }

    @FXML
    private void handleSubmit() {
        String raw = tfAmount.getText().trim().replaceAll("[^\\d]", ""); // chỉ giữ số

        if (raw.isEmpty()) {
            showError("Vui lòng nhập số tiền đặt giá.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ.");
            return;
        }

        // Lấy giá tối thiểu từ label để validate
        double minBid = quickAmounts[0];
        if (amount < minBid) {
            showError("Giá đặt phải từ " + formatVnd(minBid) + " trở lên.");
            return;
        }

        // Hợp lệ → gọi callback rồi đóng dialog
        if (onBidConfirmed != null) {
            onBidConfirmed.accept(amount);
        }
        closeDialog();
    }

    // ── Private helpers ──────────────────────────────────────────

    /** Chọn 1 mức giá nhanh: highlight nút, điền vào ô nhập */
    private void pickQuick(int index, Button btn) {
        selectQuickButton(btn);
        tfAmount.setText(String.valueOf((long) quickAmounts[index]));
        hideError();
    }

    /** Highlight nút được chọn, bỏ các nút còn lại */
    private void selectQuickButton(Button selected) {
        for (Button b : new Button[]{btnQ1, btnQ2, btnQ3, btnQ4}) {
            b.getStyleClass().remove("selected");
        }
        if (!selected.getStyleClass().contains("selected")) {
            selected.getStyleClass().add("selected");
        }
    }

    /** Bỏ highlight tất cả nút nhanh (khi user tự gõ) */
    private void clearQuickSelection() {
        for (Button b : new Button[]{btnQ1, btnQ2, btnQ3, btnQ4}) {
            b.getStyleClass().remove("selected");
        }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void closeDialog() {
        if (thisStage != null) thisStage.close();
    }

    // ── Formatting ───────────────────────────────────────────────

    /**
     * Format số tiền VNĐ theo kiểu Việt Nam: 247.000.000 đ
     * Dùng dấu chấm làm phân cách hàng nghìn, không có phần thập phân.
     */
    private String formatVnd(double amount) {
        // NumberFormat của Locale("vi", "VN") dùng dấu "." làm thousands separator
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        return nf.format(Math.round(amount)) + " đ";
    }

    /**
     * Tính bước tăng tối thiểu hợp lý dựa trên quy mô giá.
     *
     * Quy tắc đơn giản:
     *   < 10 triệu    →  100.000 đ / bước
     *   < 100 triệu   →  1.000.000 đ / bước
     *   < 1 tỷ        →  5.000.000 đ / bước
     *   >= 1 tỷ       →  10.000.000 đ / bước
     */
    private double computeIncrement(double bid) {
        if (bid < 10_000_000)  return 100_000;
        if (bid < 100_000_000) return 1_000_000;
        if (bid < 1_000_000_000) return 5_000_000;
        return 10_000_000;
    }
}