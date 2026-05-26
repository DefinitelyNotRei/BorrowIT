package com.borrowit.view.admin;

import com.borrowit.controller.AuthController;
import com.borrowit.model.User;
import com.borrowit.model.UserRole;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;
import com.borrowit.view.MainLauncherFrame;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminLoginFrame extends Stage {
    private final AuthController authController = new AuthController();
    private final TextField userIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField passwordVisibleField = new TextField();
    private final CheckBox showPasswordCheckbox = new CheckBox("Show Password");
    private final Label errorLabel = new Label();
    private final Label statusLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Button loginButton = new Button("Login");
    private final Button backButton = new Button("Back");

    public AdminLoginFrame() {
        setTitle("BorrowIT - Admin Login");
        setMaximized(true);
        setOnCloseRequest(event -> System.exit(0));

        Label titleLabel = new Label("Admin/Staff Application");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox headerPanel = new VBox();
        headerPanel.setPadding(new Insets(18, 10, 6, 10));
        headerPanel.setAlignment(Pos.CENTER);
        headerPanel.getChildren().add(titleLabel);

        GridPane formPanel = new GridPane();
        formPanel.setPadding(new Insets(10, 30, 10, 30));
        formPanel.setHgap(10);
        formPanel.setVgap(10);
        formPanel.setAlignment(Pos.CENTER);

        userIdField.setPrefWidth(200);
        userIdField.setTooltip(new Tooltip("Enter the admin or staff ID."));

        passwordField.setPrefWidth(200);
        passwordField.setTooltip(new Tooltip("Enter your password."));
        passwordField.setOnAction(event -> login());
        passwordField.getStyleClass().add("input-field");

        passwordVisibleField.setPrefWidth(200);
        passwordVisibleField.setTooltip(new Tooltip("Enter your password."));
        passwordVisibleField.setOnAction(event -> login());
        passwordVisibleField.getStyleClass().add("input-field");
        passwordVisibleField.managedProperty().bind(showPasswordCheckbox.selectedProperty());
        passwordVisibleField.visibleProperty().bind(showPasswordCheckbox.selectedProperty());

        passwordField.managedProperty().bind(showPasswordCheckbox.selectedProperty().not());
        passwordField.visibleProperty().bind(showPasswordCheckbox.selectedProperty().not());
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordCheckbox.setOnAction(event -> {
            if (showPasswordCheckbox.isSelected()) {
                passwordVisibleField.requestFocus();
                passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });

        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);
        HBox statusBar = new HBox(10, progressIndicator, statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);

        formPanel.add(new Label("Admin ID"), 0, 0);
        formPanel.add(userIdField, 1, 0);
        formPanel.add(new Label("Password"), 0, 1);
        HBox passwordRow = new HBox(10, passwordField, passwordVisibleField, showPasswordCheckbox);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        formPanel.add(passwordRow, 1, 1);

        loginButton.setPrefWidth(80);
        loginButton.getStyleClass().add("primary-button");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> login());

        backButton.setPrefWidth(80);
        backButton.getStyleClass().add("secondary-button");
        backButton.setCancelButton(true);
        backButton.setOnAction(event -> {
            new MainLauncherFrame().show();
            close();
        });

        HBox buttonPanel = new HBox(10);
        buttonPanel.setPadding(new Insets(10));
        buttonPanel.setAlignment(Pos.CENTER);
        buttonPanel.getChildren().addAll(loginButton, backButton);

        VBox root = new VBox(20, headerPanel, errorLabel, statusBar, formPanel, buttonPanel);
        root.setPadding(new Insets(12));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("form-container");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        setScene(scene);
    }

    private void login() {
        clearError();
        char[] password = passwordField.getText().toCharArray();
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authController.login(userIdField.getText(), password, UserRole.ADMIN);
            }
        };

        task.setOnRunning(event -> setWorking(true, "Signing in..."));
        task.setOnSucceeded(event -> {
            setWorking(false, "Signed in successfully.");
            User user = task.getValue();
            new AdminDashboardFrame(user).show();
            close();
        });
        task.setOnFailed(event -> {
            setWorking(false, null);
            Throwable exception = task.getException();
            if (exception instanceof ValidationException || exception instanceof ServiceException) {
                setError(exception.getMessage());
            } else {
                setError("Unable to sign in. Please try again.");
            }
        });

        new Thread(task).start();
    }

    private void setError(String message) {
        errorLabel.setText(message);
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void setWorking(boolean working, String message) {
        userIdField.setDisable(working);
        passwordField.setDisable(working);
        loginButton.setDisable(working);
        backButton.setDisable(working);
        progressIndicator.setVisible(working);
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setVisible(working || (message != null && !message.isBlank()));
    }
}

