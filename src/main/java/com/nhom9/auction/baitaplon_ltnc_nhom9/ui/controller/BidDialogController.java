package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
 *
 * Auto-bid:
 *   Khi user bật checkbox "Tự động đặt giá" và nhập giá tối đa,
 *   dialog trả về BidResult chứa cả amount lẫn maxAutoBid.
 *   Phần logic tự động tăng giá sẽ được xử lý ở backend sau này.
 */
public class BidDialogController {

    // ── FXML fields — đặt giá thủ công ──────────────────────────
    @FXML private Label  lblCurrentPrice;
    @FXML private Label  lblMinPrice;

    @FXML private Button btnQ1;
    @FXML private Button btnQ2;
    @FXML private Button btnQ3;
    @FXML private Button btnQ4;

    @FXML private HBox      bidInputWrapper;
    @FXML private TextField tfAmount;
    @FXML private Label     lblError;
    @FXML private Button    btnSubmit;

    // ── FXML fields — auto-bid ───────────────────────────────────
    @FXML private CheckBox  cbAutoBid;
    @FXML private VBox      autoBidPanel;
    @FXML private HBox      autoBidInputWrapper;
    @FXML private TextField tfMaxBid;
    @FXML private Label     lblAutoBidError;
    @FXML private Button    btnSuggest1;
    @FXML private Button    btnSuggest2;
    @FXML private Button    btnSuggest3;
    @FXML private Button    btnAutoBidConfirm;

    // ── State ────────────────────────────────────────────────────
    private Stage thisStage;
    private double increment;
    private final double[] quickAmounts = new double[4];

    /** Ba mức gợi ý cho giá tối đa auto-bid */
    private final double[] suggestAmounts = new double[3];

    /** Callback được gọi khi user xác nhận — truyền BidResult */
    private java.util.function.Consumer<Double> onBidConfirmed;

    // ── Public API ───────────────────────────────────────────────

    public void configure(double currentBid, Stage stage,
                          java.util.function.Consumer<Double> onBidConfirmed) {

        this.thisStage      = stage;
        this.onBidConfirmed = onBidConfirmed;
        this.increment      = computeIncrement(currentBid);

        // Tính 4 mức giá nhanh
        double minBid       = currentBid + increment;
        quickAmounts[0]     = minBid;
        quickAmounts[1]     = minBid + increment * 5;
        quickAmounts[2]     = minBid + increment * 10;
        quickAmounts[3]     = minBid + increment * 25;

        // Tính 3 mức gợi ý auto-bid (cao hơn mức nhanh để trông hợp lý)
        suggestAmounts[0]   = minBid + increment * 10;
        suggestAmounts[1]   = minBid + increment * 20;
        suggestAmounts[2]   = minBid + increment * 50;

        // Cập nhật labels
        lblCurrentPrice.setText(formatVnd(currentBid));
        lblMinPrice.setText(formatVnd(minBid));

        // Cập nhật nút nhanh
        btnQ1.setText(formatVnd(quickAmounts[0]));
        btnQ2.setText(formatVnd(quickAmounts[1]));
        btnQ3.setText(formatVnd(quickAmounts[2]));
        btnQ4.setText(formatVnd(quickAmounts[3]));

        // Cập nhật nút gợi ý auto-bid
        btnSuggest1.setText(formatVnd(suggestAmounts[0]));
        btnSuggest2.setText(formatVnd(suggestAmounts[1]));
        btnSuggest3.setText(formatVnd(suggestAmounts[2]));

        // Điền sẵn mức tối thiểu
        tfAmount.setText(String.valueOf((long) minBid));
        selectQuickButton(btnQ1);

        // Khi gõ vào ô giá thủ công → bỏ highlight nút nhanh
        tfAmount.textProperty().addListener((obs, o, n) -> {
            clearQuickSelection();
            hideError();
        });

        // Khi gõ vào ô giá tối đa auto-bid → ẩn lỗi
        tfMaxBid.textProperty().addListener((obs, o, n) -> hideAutoBidError());

        // Viền vàng khi focus ô nhập thủ công
        tfAmount.focusedProperty().addListener((obs, was, is) -> {
            if (is) bidInputWrapper.getStyleClass().add("bid-input-wrapper-focused");
            else     bidInputWrapper.getStyleClass().remove("bid-input-wrapper-focused");
        });

        // Viền vàng khi focus ô nhập auto-bid
        tfMaxBid.focusedProperty().addListener((obs, was, is) -> {
            if (is) autoBidInputWrapper.getStyleClass().add("bid-input-wrapper-focused");
            else     autoBidInputWrapper.getStyleClass().remove("bid-input-wrapper-focused");
        });
    }

    // ── FXML handlers — thủ công ─────────────────────────────────

    @FXML private void handleClose()  { closeDialog(); }

    @FXML private void handleQuick1() { pickQuick(0, btnQ1); }
    @FXML private void handleQuick2() { pickQuick(1, btnQ2); }
    @FXML private void handleQuick3() { pickQuick(2, btnQ3); }
    @FXML private void handleQuick4() { pickQuick(3, btnQ4); }

    // ── FXML handlers — auto-bid ─────────────────────────────────

    /**
     * Bật/tắt panel auto-bid khi user click vào checkbox.
     *
     * Tại sao dùng visible + managed cùng lúc?
     * → visible=false ẩn node nhưng vẫn chiếm chỗ trong layout.
     *   managed=false mới khiến layout bỏ qua node đó hoàn toàn.
     *   Phải đặt cả hai để UI co lại đúng cách.
     */
    @FXML
    private void handleAutoBidToggle() {
        boolean on = cbAutoBid.isSelected();

        // Mở/đóng panel auto-bid
        autoBidPanel.setVisible(on);
        autoBidPanel.setManaged(on);

        // Ẩn nút submit chung khi auto-bid bật,
        // hiện lại khi tắt — để tránh nhầm lẫn
        btnSubmit.setVisible(!on);
        btnSubmit.setManaged(!on);

        if (on) {
            tfMaxBid.setText(String.valueOf((long) suggestAmounts[0]));
            tfMaxBid.requestFocus();
        }
    }

    /** Ba nút gợi ý mức giá tối đa */
    @FXML private void handleSuggest1() { pickSuggest(0); }
    @FXML private void handleSuggest2() { pickSuggest(1); }
    @FXML private void handleSuggest3() { pickSuggest(2); }

    @FXML
    private void handleSubmit() {
        if (cbAutoBid.isSelected()) {
            submitAutoBid();
        } else {
            submitManualBid();
        }
    }

    // ── Private — submit logic ───────────────────────────────────

    private void submitManualBid() {
        String raw = tfAmount.getText().trim().replaceAll("[^\\d]", "");
        if (raw.isEmpty()) { showError("Vui lòng nhập số tiền đặt giá."); return; }

        double amount;
        try { amount = Double.parseDouble(raw); }
        catch (NumberFormatException e) { showError("Số tiền không hợp lệ."); return; }

        double minBid = quickAmounts[0];
        if (amount < minBid) {
            showError("Giá đặt phải từ " + formatVnd(minBid) + " trở lên.");
            return;
        }

        if (onBidConfirmed != null) onBidConfirmed.accept(amount);
        closeDialog();
    }

    /**
     * Validate và submit auto-bid.
     *
     * Lưu ý: hiện tại callback vẫn trả về Double (giá tối đa).
     * Khi backend sẵn sàng, bạn có thể tạo class BidRequest riêng
     * chứa cả firstBid lẫn maxAutoBid, rồi đổi kiểu callback.
     */
    private void submitAutoBid() {
        String raw = tfMaxBid.getText().trim().replaceAll("[^\\d]", "");
        if (raw.isEmpty()) { showAutoBidError("Vui lòng nhập giá tối đa."); return; }

        double maxBid;
        try { maxBid = Double.parseDouble(raw); }
        catch (NumberFormatException e) { showAutoBidError("Số tiền không hợp lệ."); return; }

        double minBid = quickAmounts[0];
        if (maxBid <= minBid) {
            showAutoBidError("Giá tối đa phải lớn hơn " + formatVnd(minBid) + ".");
            return;
        }

        // TODO (backend): gửi cả minBid lẫn maxBid để server biết
        // phải tự động đặt từ minBid lên đến maxBid khi bị vượt qua.
        // Hiện tại chỉ confirm bằng maxBid như một bid thủ công.
        if (onBidConfirmed != null) onBidConfirmed.accept(maxBid);
        closeDialog();
    }

    // ── Private helpers ──────────────────────────────────────────

    private void pickQuick(int index, Button btn) {
        selectQuickButton(btn);
        tfAmount.setText(String.valueOf((long) quickAmounts[index]));
        hideError();
    }

    private void pickSuggest(int index) {
        tfMaxBid.setText(String.valueOf((long) suggestAmounts[index]));
        hideAutoBidError();
    }

    private void selectQuickButton(Button selected) {
        for (Button b : new Button[]{btnQ1, btnQ2, btnQ3, btnQ4})
            b.getStyleClass().remove("selected");
        if (!selected.getStyleClass().contains("selected"))
            selected.getStyleClass().add("selected");
    }

    private void clearQuickSelection() {
        for (Button b : new Button[]{btnQ1, btnQ2, btnQ3, btnQ4})
            b.getStyleClass().remove("selected");
    }

    private void showError(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
    private void hideError() {
        lblError.setVisible(false); lblError.setManaged(false);
    }
    private void showAutoBidError(String msg) {
        lblAutoBidError.setText(msg); lblAutoBidError.setVisible(true); lblAutoBidError.setManaged(true);
    }
    private void hideAutoBidError() {
        lblAutoBidError.setVisible(false); lblAutoBidError.setManaged(false);
    }

    private void closeDialog() { if (thisStage != null) thisStage.close(); }

    private String formatVnd(double amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        return nf.format(Math.round(amount)) + " đ";
    }

    private double computeIncrement(double bid) {
        if (bid < 10_000_000)    return 100_000;
        if (bid < 100_000_000)   return 1_000_000;
        if (bid < 1_000_000_000) return 5_000_000;
        return 10_000_000;
    }
}