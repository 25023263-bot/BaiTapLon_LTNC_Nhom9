package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Optional;

/**
 * Tiện ích hiển thị các hộp thoại (Alert) trong JavaFX.
 */
public class AlertHelper {

    private AlertHelper() {}

    // ─── Info / Success ───────────────────────────────────────────────────────

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showSuccess(String message) {
        showInfo("Thành công", message);
    }

    // ─── Warning ──────────────────────────────────────────────────────────────

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    // ─── Error ────────────────────────────────────────────────────────────────

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void showError(String message) {
        showError("Lỗi", message);
    }

    /**
     * Hiển thị lỗi kèm stack trace (dùng khi debug).
     */
    public static void showException(String message, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi hệ thống");
        alert.setHeaderText(message);

        String stackTrace = getStackTrace(ex);
        TextArea ta = new TextArea(stackTrace);
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(ta, Priority.ALWAYS);
        GridPane.setHgrow(ta, Priority.ALWAYS);

        GridPane gp = new GridPane();
        gp.setMaxWidth(Double.MAX_VALUE);
        gp.add(ta, 0, 0);

        alert.getDialogPane().setExpandableContent(gp);
        alert.showAndWait();
    }

    // ─── Confirm ──────────────────────────────────────────────────────────────

    /**
     * Hộp thoại xác nhận Yes/No.
     * @return true nếu user bấm OK/Yes
     */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static boolean showConfirmDelete(String itemName) {
        return showConfirm("Xác nhận xoá",
                "Bạn có chắc muốn xoá \"" + itemName + "\"?\nThao tác này không thể hoàn tác.");
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex).append("\n");
        for (StackTraceElement e : ex.getStackTrace()) sb.append("\tat ").append(e).append("\n");
        if (ex.getCause() != null) {
            sb.append("Caused by: ").append(ex.getCause()).append("\n");
            for (StackTraceElement e : ex.getCause().getStackTrace())
                sb.append("\tat ").append(e).append("\n");
        }
        return sb.toString();
    }
}
