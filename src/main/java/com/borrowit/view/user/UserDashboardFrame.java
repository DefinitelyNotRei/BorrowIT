package com.borrowit.view.user;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.borrowit.controller.AuthController;
import com.borrowit.controller.EquipmentController;
import com.borrowit.controller.ReservationController;
import com.borrowit.model.Equipment;
import com.borrowit.model.Reservation;
import com.borrowit.model.ReservationStatus;
import com.borrowit.model.User;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;
import com.borrowit.view.MainLauncherFrame;
import com.borrowit.view.util.DateTimeUtils;
import com.borrowit.view.util.GuiUtils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserDashboardFrame extends Stage {
    private final User currentUser;
    private final AuthController authController = new AuthController();
    private final EquipmentController equipmentController = new EquipmentController();
    private final ReservationController reservationController = new ReservationController();

    private final TableView<Equipment> availableTable = new TableView<>();
    private final TableView<Reservation> borrowedTable = new TableView<>();
    private final TableView<Reservation> historyTable = new TableView<>();
    private final TextField equipmentSearchField = new TextField();
    private final Label statusLabel = new Label("Ready");
    private final ProgressIndicator statusIndicator = new ProgressIndicator();
    private final Label borrowedCountLabel = new Label("0");
    private final Label overdueCountLabel = new Label("0");

    private final ObservableList<Equipment> availableItems = FXCollections.observableArrayList();
    private final ObservableList<Reservation> borrowedItems = FXCollections.observableArrayList();
    private final ObservableList<Reservation> historyItems = FXCollections.observableArrayList();

    public UserDashboardFrame(User currentUser) {
        this.currentUser = currentUser;

        setTitle("BorrowIT - User Dashboard");
        setMaximized(true);
        setOnCloseRequest(event -> System.exit(0));

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(wrapInScrollPane(createContentContainer()));
        root.setBottom(buildStatusBar());
        root.setPadding(new Insets(12));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        setScene(scene);

        loadAvailableEquipment();
        loadBorrowedEquipment();
        loadHistory();
    }

    private HBox buildStatusBar() {
        statusIndicator.setPrefSize(16, 16);
        statusIndicator.setVisible(false);
        statusLabel.getStyleClass().add("status-label");
        HBox statusBar = new HBox(8, statusIndicator, statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(8, 0, 0, 0));
        return statusBar;
    }

    private void setStatus(String message, boolean busy) {
        statusLabel.setText(message);
        statusIndicator.setVisible(busy);
    }

    private HBox buildHeader() {
        Label welcomeLabel = new Label("Welcome, " + currentUser.getFullName());
        welcomeLabel.getStyleClass().add("title-label");

        Button changePasswordButton = new Button("Change Password");
        changePasswordButton.getStyleClass().add("secondary-button");
        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        changePasswordButton.setOnAction(event -> showChangePasswordDialog());
        logoutButton.setOnAction(event -> logout());

        HBox actionPanel = new HBox(10, changePasswordButton, logoutButton);
        actionPanel.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(10, welcomeLabel, actionPanel);
        HBox.setHgrow(actionPanel, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 20, 0));

        return header;
    }

    private TabPane buildTabPane() {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Available Equipment", buildAvailableEquipmentTab()),
                new Tab("Current Borrowed", buildBorrowedTab()),
                new Tab("Reservations and History", buildHistoryTab())
        );
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        return tabs;
    }

    private VBox createContentContainer() {
        VBox container = new VBox(16, buildOverviewPanel(), buildTabPane());
        container.setPadding(new Insets(16));
        container.getStyleClass().add("form-container");
        return container;
    }

    private HBox buildOverviewPanel() {
        borrowedCountLabel.getStyleClass().add("metric-value");
        overdueCountLabel.getStyleClass().add("metric-value");

        VBox borrowedCard = metricCard("Currently Borrowed", borrowedCountLabel);
        VBox overdueCard = metricCard("Overdue Items", overdueCountLabel);

        HBox overview = new HBox(20, borrowedCard, overdueCard);
        overview.setAlignment(Pos.CENTER);
        return overview;
    }

    private VBox metricCard(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("form-label");

        VBox card = new VBox(8, titleLabel, valueLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("metric-card");
        card.setPrefWidth(260);
        return card;
    }

    private void refreshOverview() {
        int borrowedCount = borrowedItems.size();
        int overdueCount = (int) borrowedItems.stream()
                .filter(reservation -> reservation.getDueDate() != null && reservation.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        borrowedCountLabel.setText(String.valueOf(borrowedCount));
        overdueCountLabel.setText(String.valueOf(overdueCount));
    }

    private VBox buildAvailableEquipmentTab() {
        equipmentSearchField.setPromptText("Search equipment...");
        equipmentSearchField.setPrefWidth(280);

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button reserveButton = new Button("Reserve Selected");
        reserveButton.getStyleClass().add("primary-button");

        searchButton.setOnAction(event -> loadAvailableEquipment());
        equipmentSearchField.setOnAction(event -> loadAvailableEquipment());
        refreshButton.setOnAction(event -> {
            equipmentSearchField.clear();
            loadAvailableEquipment();
        });
        reserveButton.setOnAction(event -> reserveSelectedEquipment());

        HBox controls = new HBox(10, new Label("Search"), equipmentSearchField, searchButton, refreshButton, reserveButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 0, 10, 0));

        configureAvailableTable();
        availableTable.setItems(availableItems);

        VBox box = new VBox(10, controls, availableTable);
        VBox.setVgrow(availableTable, Priority.ALWAYS);
        return box;
    }

    private void configureAvailableTable() {
        TableColumn<Equipment, ?> a1 = createEquipmentColumn("ID", 80, equipment -> new SimpleIntegerProperty(equipment.getEquipmentId()));
        TableColumn<Equipment, ?> a2 = createEquipmentColumn("Asset Tag", 130, equipment -> new SimpleStringProperty(equipment.getAssetTag()));
        TableColumn<Equipment, ?> a3 = createEquipmentColumn("Name", 160, equipment -> new SimpleStringProperty(equipment.getName()));
        TableColumn<Equipment, ?> a4 = createEquipmentColumn("Description", 240, equipment -> new SimpleStringProperty(equipment.getDescription()));
        TableColumn<Equipment, ?> a5 = createEquipmentColumn("Available", 90, equipment -> new SimpleIntegerProperty(equipment.getAvailableQuantity()));
        availableTable.getColumns().setAll(java.util.Arrays.<TableColumn<Equipment, ?>>asList(a1, a2, a3, a4, a5));
    }

    private VBox buildBorrowedTab() {
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> loadBorrowedEquipment());

        Button returnButton = new Button("Return Selected");
        returnButton.getStyleClass().add("success-button");
        returnButton.setOnAction(event -> returnSelectedBorrowedItem());

        HBox controls = new HBox(10, refreshButton, returnButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 0, 10, 0));

        configureBorrowedTable();
        borrowedTable.setItems(borrowedItems);

        VBox box = new VBox(10, controls, borrowedTable);
        VBox.setVgrow(borrowedTable, Priority.ALWAYS);
        return box;
    }

    private void configureBorrowedTable() {
        TableColumn<Reservation, ?> b1 = createReservationColumn("Reservation ID", 100, reservation -> new SimpleIntegerProperty(reservation.getReservationId()));
        TableColumn<Reservation, ?> b2 = createReservationColumn("Equipment", 140, reservation -> new SimpleStringProperty(reservation.getEquipmentName()));
        TableColumn<Reservation, ?> b3 = createReservationColumn("Asset Tag", 110, reservation -> new SimpleStringProperty(reservation.getAssetTag()));
        TableColumn<Reservation, ?> b4 = createReservationColumn("Qty", 70, reservation -> new SimpleIntegerProperty(reservation.getQuantity()));
        TableColumn<Reservation, ?> b5 = createReservationColumn("Due Date", 130, reservation -> new SimpleStringProperty(DateTimeUtils.format(reservation.getDueDate())));
        TableColumn<Reservation, ?> b6 = createReservationColumn("Return Date", 130, reservation -> new SimpleStringProperty(DateTimeUtils.format(reservation.getReturnDate())));
        TableColumn<Reservation, ?> b7 = createReservationColumn("Remaining", 100, reservation -> new SimpleStringProperty(DateTimeUtils.remaining(reservation.getDueDate())));
        borrowedTable.getColumns().setAll(java.util.Arrays.<TableColumn<Reservation, ?>>asList(b1, b2, b3, b4, b5, b6, b7));
    }

    private VBox buildHistoryTab() {
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button cancelButton = new Button("Cancel Pending Request");
        cancelButton.getStyleClass().add("danger-button");
        refreshButton.setOnAction(event -> loadHistory());
        cancelButton.setOnAction(event -> cancelSelectedReservation());

        HBox controls = new HBox(10, refreshButton, cancelButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 0, 10, 0));

        configureHistoryTable();
        historyTable.setItems(historyItems);

        VBox box = new VBox(10, controls, historyTable);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        return box;
    }

    private void configureHistoryTable() {
        TableColumn<Reservation, ?> h1 = createReservationColumn("Reservation ID", 100, reservation -> new SimpleIntegerProperty(reservation.getReservationId()));
        TableColumn<Reservation, ?> h2 = createReservationColumn("Equipment", 140, reservation -> new SimpleStringProperty(reservation.getEquipmentName()));
        TableColumn<Reservation, ?> h3 = createReservationColumn("Asset Tag", 120, reservation -> new SimpleStringProperty(reservation.getAssetTag()));
        TableColumn<Reservation, ?> h4 = createReservationColumn("Qty", 70, reservation -> new SimpleIntegerProperty(reservation.getQuantity()));
        TableColumn<Reservation, ?> h5 = createReservationColumn("Status", 100, reservation -> new SimpleStringProperty(reservation.getStatus().name()));
        TableColumn<Reservation, ?> h6 = createReservationColumn("Requested", 130, reservation -> new SimpleStringProperty(DateTimeUtils.format(reservation.getRequestDate())));
        TableColumn<Reservation, ?> h7 = createReservationColumn("Due Date", 130, reservation -> new SimpleStringProperty(DateTimeUtils.format(reservation.getDueDate())));
        TableColumn<Reservation, ?> h8 = createReservationColumn("Return Date", 130, reservation -> new SimpleStringProperty(DateTimeUtils.format(reservation.getReturnDate())));
        TableColumn<Reservation, ?> h9 = createReservationColumn("Returned", 130, reservation -> new SimpleStringProperty(DateTimeUtils.format(reservation.getReturnedAt())));
        TableColumn<Reservation, ?> h10 = createReservationColumn("Remarks", 180, reservation -> new SimpleStringProperty(reservation.getRemarks()));
        historyTable.getColumns().setAll(java.util.Arrays.<TableColumn<Reservation, ?>>asList(h1, h2, h3, h4, h5, h6, h7, h8, h9, h10));
    }

    private ScrollPane wrapInScrollPane(javafx.scene.Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        return scrollPane;
    }

    private <T> TableColumn<Equipment, T> createEquipmentColumn(String title, int width, javafx.util.Callback<Equipment, javafx.beans.value.ObservableValue<T>> valueFactory) {
        TableColumn<Equipment, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> valueFactory.call(cellData.getValue()));
        column.setPrefWidth(width);
        return column;
    }

    private <T> TableColumn<Reservation, T> createReservationColumn(String title, int width, javafx.util.Callback<Reservation, javafx.beans.value.ObservableValue<T>> valueFactory) {
        TableColumn<Reservation, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> valueFactory.call(cellData.getValue()));
        column.setPrefWidth(width);
        return column;
    }

    private void loadAvailableEquipment() {
        setStatus("Loading available equipment...", true);
        try {
            String keyword = equipmentSearchField.getText().trim().toLowerCase();
            List<Equipment> equipmentList = equipmentController.getAvailableEquipment();
            availableItems.clear();
            for (Equipment equipment : equipmentList) {
                if (matchesEquipment(equipment, keyword)) {
                    availableItems.add(equipment);
                }
            }
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void loadBorrowedEquipment() {
        setStatus("Loading current borrowed items...", true);
        try {
            List<Reservation> reservations = reservationController.getCurrentBorrowedByUser(currentUser.getUserId());
            borrowedItems.setAll(reservations);
            refreshOverview();
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void loadHistory() {
        setStatus("Loading reservation history...", true);
        try {
            List<Reservation> reservations = reservationController.getUserReservations(currentUser.getUserId());
            historyItems.setAll(reservations.stream()
                    .filter(reservation -> reservation.getStatus() != ReservationStatus.APPROVED)
                    .collect(Collectors.toList()));
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void reserveSelectedEquipment() {
        Equipment selectedEquipment = availableTable.getSelectionModel().getSelectedItem();
        if (selectedEquipment == null) {
            GuiUtils.showInfo(this, "Select equipment first.");
            return;
        }

        if (!GuiUtils.confirm(this, "Create a reservation request for the selected equipment?")) {
            return;
        }

        try {
            reservationController.createReservation(currentUser.getUserId(), selectedEquipment.getEquipmentId(), 1);
            GuiUtils.showInfo(this, "Reservation request submitted. Status is pending.");
            loadAvailableEquipment();
            loadBorrowedEquipment();
            loadHistory();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void cancelSelectedReservation() {
        Reservation selectedReservation = historyTable.getSelectionModel().getSelectedItem();
        if (selectedReservation == null) {
            GuiUtils.showInfo(this, "Select a pending reservation first.");
            return;
        }
        if (selectedReservation.getStatus() != ReservationStatus.PENDING) {
            GuiUtils.showInfo(this, "Only pending reservation requests can be cancelled.");
            return;
        }
        if (!GuiUtils.confirm(this, "Cancel the selected reservation request?")) {
            return;
        }

        try {
            boolean cancelled = reservationController.cancelReservation(selectedReservation.getReservationId(), currentUser.getUserId());
            if (cancelled) {
                GuiUtils.showInfo(this, "Reservation request cancelled.");
            } else {
                GuiUtils.showInfo(this, "Reservation could not be cancelled.");
            }
            loadBorrowedEquipment();
            loadHistory();
            loadAvailableEquipment();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void returnSelectedBorrowedItem() {
        Reservation selectedReservation = borrowedTable.getSelectionModel().getSelectedItem();
        if (selectedReservation == null) {
            GuiUtils.showInfo(this, "Select a borrowed item first.");
            return;
        }
        if (!GuiUtils.confirm(this, "Return the selected borrowed item?")) {
            return;
        }
        try {
            boolean returned = reservationController.markReturned(selectedReservation.getReservationId());
            if (returned) {
                GuiUtils.showInfo(this, "Item returned successfully. It is now available for others.");
            } else {
                GuiUtils.showInfo(this, "Could not return the selected item.");
            }
            loadBorrowedEquipment();
            loadHistory();
            loadAvailableEquipment();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private boolean matchesEquipment(Equipment equipment, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return contains(equipment.getAssetTag(), keyword)
                || contains(equipment.getName(), keyword)
                || contains(equipment.getDescription(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void showChangePasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(this);
        dialog.setTitle("Change Password");

        PasswordField currentPasswordField = new PasswordField();
        TextField currentPasswordVisibleField = new TextField();
        CheckBox showCurrentPassword = new CheckBox("Show Password");
        PasswordField newPasswordField = new PasswordField();
        TextField newPasswordVisibleField = new TextField();
        CheckBox showNewPassword = new CheckBox("Show Password");
        PasswordField confirmPasswordField = new PasswordField();
        TextField confirmPasswordVisibleField = new TextField();
        CheckBox showConfirmPassword = new CheckBox("Show Password");

        configurePasswordToggle(currentPasswordField, currentPasswordVisibleField, showCurrentPassword);
        configurePasswordToggle(newPasswordField, newPasswordVisibleField, showNewPassword);
        configurePasswordToggle(confirmPasswordField, confirmPasswordVisibleField, showConfirmPassword);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.add(new Label("Current Password"), 0, 0);
        grid.add(new HBox(10, currentPasswordField, currentPasswordVisibleField, showCurrentPassword), 1, 0);
        grid.add(new Label("New Password"), 0, 1);
        grid.add(new HBox(10, newPasswordField, newPasswordVisibleField, showNewPassword), 1, 1);
        grid.add(new Label("Confirm Password"), 0, 2);
        grid.add(new HBox(10, confirmPasswordField, confirmPasswordVisibleField, showConfirmPassword), 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> dialogButton);
        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            char[] currentPassword = currentPasswordField.getText().toCharArray();
            char[] newPassword = newPasswordField.getText().toCharArray();
            char[] confirmPassword = confirmPasswordField.getText().toCharArray();
            try {
                if (!Arrays.equals(newPassword, confirmPassword)) {
                    throw new IllegalArgumentException("New passwords do not match.");
                }
                authController.changePassword(currentUser.getUserId(), currentPassword, newPassword);
                GuiUtils.showInfo(this, "Password updated successfully.");
            } catch (ValidationException | ServiceException | IllegalArgumentException exception) {
                GuiUtils.showError(this, exception);
            } finally {
                Arrays.fill(currentPassword, '\0');
                Arrays.fill(newPassword, '\0');
                Arrays.fill(confirmPassword, '\0');
            }
        });
    }

    private void configurePasswordToggle(PasswordField passwordField, TextField visibleField, CheckBox toggle) {
        passwordField.setPrefWidth(200);
        passwordField.getStyleClass().add("input-field");
        visibleField.setPrefWidth(200);
        visibleField.getStyleClass().add("input-field");
        visibleField.managedProperty().bind(toggle.selectedProperty());
        visibleField.visibleProperty().bind(toggle.selectedProperty());
        passwordField.managedProperty().bind(toggle.selectedProperty().not());
        passwordField.visibleProperty().bind(toggle.selectedProperty().not());
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());
        toggle.setOnAction(event -> {
            if (toggle.isSelected()) {
                visibleField.requestFocus();
                visibleField.positionCaret(visibleField.getText().length());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });
    }

    private void logout() {
        authController.logout();
        new MainLauncherFrame().show();
        close();
    }
}
