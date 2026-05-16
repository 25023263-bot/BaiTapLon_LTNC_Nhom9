package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.util.Duration;
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

    /** Toast tự đóng sau ~1.5 giây (không chặn UI). */
    public static void showToast(String message) {
        Alert toast = new Alert(Alert.AlertType.INFORMATION);
        toast.setTitle("Thông báo");
        toast.setHeaderText(null);
        toast.setContentText(message);
        toast.show();
        PauseTransition close = new PauseTransition(Duration.millis(1500));
        close.setOnFinished(e -> toast.close());
        close.play();
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
