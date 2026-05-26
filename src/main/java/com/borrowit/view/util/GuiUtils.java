package com.borrowit.view.util;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class GuiUtils {
    private GuiUtils() {
    }

    public static void showInfo(Object parent, String message) {
        Alert alert = createAlert(AlertType.INFORMATION, parent, "BorrowIT", message);
        alert.showAndWait();
    }

    public static void showError(Object parent, Exception exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            message += "\n\nDetails: " + cause.getMessage();
        }

        Alert alert = createAlert(AlertType.ERROR, parent, "BorrowIT Error", message);
        alert.showAndWait();
    }

    public static boolean confirm(Object parent, String message) {
        Alert alert = createAlert(AlertType.CONFIRMATION, parent, "BorrowIT", message);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        return result.filter(buttonType -> buttonType == ButtonType.OK).isPresent();
    }

    private static Window toFxWindow(Object parent) {
        if (parent instanceof Window window) {
            return window;
        }
        if (parent instanceof Node node && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return null;
    }

    private static Alert createAlert(AlertType type, Object parent, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Window owner = toFxWindow(parent);
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert;
    }
}
