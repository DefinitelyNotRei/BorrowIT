package com.borrowit.view.admin;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.borrowit.controller.AuthController;
import com.borrowit.controller.EquipmentController;
import com.borrowit.controller.ReservationController;
import com.borrowit.controller.UserController;
import com.borrowit.model.Equipment;
import com.borrowit.model.EquipmentStatus;
import com.borrowit.model.Reservation;
import com.borrowit.model.ReservationStatus;
import com.borrowit.model.User;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;
import com.borrowit.view.MainLauncherFrame;
import com.borrowit.view.auth.RegisterFrame;
import com.borrowit.view.util.DateTimeUtils;
import com.borrowit.view.util.GuiUtils;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class AdminDashboardFrame extends Stage {
    private final User currentAdmin;
    private final AuthController authController = new AuthController();
    private final EquipmentController equipmentController = new EquipmentController();
    private final ReservationController reservationController = new ReservationController();
    private final UserController userController = new UserController();

    private final TableView<Equipment> equipmentTable = new TableView<>();
    private final TableView<Reservation> reservationTable = new TableView<>();
    private final TableView<Reservation> overdueTable = new TableView<>();
    private final TableView<Reservation> borrowerRecordsTable = new TableView<>();
    private final TableView<User> usersTable = new TableView<>();

    private final TextField equipmentSearchField = new TextField();
    private final TextField userSearchField = new TextField();
    private final TextField assetTagField = new TextField();
    private final TextField equipmentNameField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<EquipmentStatus> statusComboBox = new ComboBox<>(FXCollections.observableArrayList(EquipmentStatus.values()));
    private final Spinner<Integer> totalQuantitySpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 1));
    private final Spinner<Integer> availableQuantitySpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 1));

    private final ObservableList<Equipment> equipmentItems = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservationItems = FXCollections.observableArrayList();
    private final ObservableList<Reservation> overdueItems = FXCollections.observableArrayList();
    private final ObservableList<Reservation> borrowerRecordsItems = FXCollections.observableArrayList();
    private final ObservableList<User> userItems = FXCollections.observableArrayList();

    private final Label totalEquipmentLabel = new Label("0");
    private final Label pendingReservationsLabel = new Label("0");
    private final Label overdueReservationsLabel = new Label("0");
    private final Label statusLabel = new Label("Ready");
    private final ProgressIndicator statusIndicator = new ProgressIndicator();

    private int selectedEquipmentId;

    public AdminDashboardFrame(User currentAdmin) {
        this.currentAdmin = currentAdmin;
        setTitle("BorrowIT - Admin Dashboard");
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

        configureEquipmentTable();
        configureReservationTable();
        configureOverdueTable();
        configureUsersTable();
        statusComboBox.setValue(EquipmentStatus.AVAILABLE);
        descriptionArea.setWrapText(true);

        loadEquipment();
        loadReservations();
        loadOverdue();
        loadBorrowerRecords();
        loadUsers();
        refreshDashboard();
    }

    private HBox buildHeader() {
        Label welcomeLabel = new Label("Admin: " + currentAdmin.getFullName());
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button changePasswordButton = new Button("Change Password");
        Button logoutButton = new Button("Logout");

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
                new javafx.scene.control.Tab("Dashboard", buildDashboardTab()),
                new javafx.scene.control.Tab("Equipment", buildEquipmentTab()),
                new javafx.scene.control.Tab("Reservations", buildReservationsTab()),
                new javafx.scene.control.Tab("Overdue", buildOverdueTab()),
                new javafx.scene.control.Tab("Borrower Records", buildBorrowerRecordsTab()),
                new javafx.scene.control.Tab("Users", buildUsersTab())
        );
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        return tabs;
    }

    private VBox createContentContainer() {
        VBox container = new VBox(16, buildTabPane());
        container.setPadding(new Insets(16));
        container.getStyleClass().add("form-container");
        return container;
    }

    private HBox buildDashboardTab() {
        totalEquipmentLabel.getStyleClass().add("metric-value");
        pendingReservationsLabel.getStyleClass().add("metric-value");
        overdueReservationsLabel.getStyleClass().add("metric-value");

        VBox totalCard = metricCard("Total Equipment", totalEquipmentLabel);
        VBox pendingCard = metricCard("Pending Reservations", pendingReservationsLabel);
        VBox overdueCard = metricCard("Overdue Equipment", overdueReservationsLabel);

        HBox dashboard = new HBox(20, totalCard, pendingCard, overdueCard);
        dashboard.setPadding(new Insets(20));
        dashboard.setAlignment(Pos.CENTER);
        return dashboard;
    }

    private VBox metricCard(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("form-label");

        VBox card = new VBox(10, titleLabel, valueLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("metric-card");
        card.setPrefWidth(320);
        return card;
    }

    private BorderPane buildEquipmentTab() {
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        searchButton.setOnAction(event -> loadEquipment());
        refreshButton.setOnAction(event -> {
            equipmentSearchField.clear();
            loadEquipment();
        });

        HBox searchBox = new HBox(10, new Label("Search"), equipmentSearchField, searchButton, refreshButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10));

        equipmentTable.setItems(equipmentItems);
        equipmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> populateEquipmentForm(selected));

        BorderPane content = new BorderPane();
        content.setTop(searchBox);
        content.setCenter(equipmentTable);
        content.setRight(buildEquipmentFormPane());
        return content;
    }

    private BorderPane buildEquipmentFormPane() {
        GridPane formGrid = new GridPane();
        formGrid.setPadding(new Insets(20));
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        ColumnConstraints labelColumn = new ColumnConstraints(110, 110, 140);
        ColumnConstraints fieldColumn = new ColumnConstraints(180, 220, Double.MAX_VALUE);
        fieldColumn.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        Label assetTagLabel = new Label("Asset Tag");
        assetTagLabel.getStyleClass().add("form-label");
        formGrid.add(assetTagLabel, 0, 0);
        formGrid.add(assetTagField, 1, 0);

        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().add("form-label");
        formGrid.add(nameLabel, 0, 1);
        formGrid.add(equipmentNameField, 1, 1);

        Label categoryLabel = new Label("Category");
        categoryLabel.getStyleClass().add("form-label");
        formGrid.add(categoryLabel, 0, 2);
        formGrid.add(categoryField, 1, 2);

        Label descriptionLabel = new Label("Description");
        descriptionLabel.getStyleClass().add("form-label");
        formGrid.add(descriptionLabel, 0, 3);
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setPrefWidth(220);
        formGrid.add(descriptionArea, 1, 3);

        Label statusTextLabel = new Label("Status");
        statusTextLabel.getStyleClass().add("form-label");
        formGrid.add(statusTextLabel, 0, 4);
        statusComboBox.setPrefWidth(200);
        formGrid.add(statusComboBox, 1, 4);

        Label totalQtyLabel = new Label("Total Qty");
        totalQtyLabel.getStyleClass().add("form-label");
        formGrid.add(totalQtyLabel, 0, 5);
        formGrid.add(totalQuantitySpinner, 1, 5);

        Label availableQtyLabel = new Label("Available Qty");
        availableQtyLabel.getStyleClass().add("form-label");
        formGrid.add(availableQtyLabel, 0, 6);
        formGrid.add(availableQuantitySpinner, 1, 6);

        ScrollPane scrollPane = new ScrollPane(formGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addButton = new Button("Add");
        addButton.getStyleClass().add("primary-button");
        Button updateButton = new Button("Update");
        updateButton.getStyleClass().add("success-button");
        Button deleteButton = new Button("Remove");
        deleteButton.getStyleClass().add("danger-button");
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("secondary-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        updateButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setMaxWidth(Double.MAX_VALUE);

        addButton.setOnAction(event -> addEquipment());
        updateButton.setOnAction(event -> updateEquipment());
        deleteButton.setOnAction(event -> deleteEquipment());
        clearButton.setOnAction(event -> clearEquipmentForm());

        VBox buttonBox = new VBox(10, addButton, updateButton, deleteButton, clearButton);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        buttonBox.setFillWidth(true);

        VBox formPane = new VBox(10, new Label("Equipment Details"), scrollPane, buttonBox);
        formPane.setPadding(new Insets(10));
        formPane.setPrefWidth(340);
        return new BorderPane(formPane);
    }

    private BorderPane buildReservationsTab() {
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().add("primary-button");
        Button approveCustomButton = new Button("Approve (Custom Days)");
        approveCustomButton.getStyleClass().add("primary-button");
        Button declineButton = new Button("Decline");
        declineButton.getStyleClass().add("danger-button");
        Button setDueDateButton = new Button("Set Due Date");
        setDueDateButton.getStyleClass().add("secondary-button");
        Button returnedButton = new Button("Mark Returned");
        returnedButton.getStyleClass().add("secondary-button");

        refreshButton.setOnAction(event -> loadReservations());
        approveButton.setOnAction(event -> approveSelectedReservation());
        approveCustomButton.setOnAction(event -> approveSelectedReservationWithDueDate());
        declineButton.setOnAction(event -> declineSelectedReservation());
        setDueDateButton.setOnAction(event -> setDueDateForSelectedReservation());
        returnedButton.setOnAction(event -> returnSelectedReservation(reservationTable));

        HBox actionBar = new HBox(10, refreshButton, approveButton, approveCustomButton, declineButton, setDueDateButton, returnedButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(10));

        reservationTable.setItems(reservationItems);

        BorderPane content = new BorderPane();
        content.setTop(actionBar);
        content.setCenter(reservationTable);
        return content;
    }

    private BorderPane buildOverdueTab() {
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button returnedButton = new Button("Mark Returned");
        returnedButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> loadOverdue());
        returnedButton.setOnAction(event -> returnSelectedReservation(overdueTable));

        HBox actionBar = new HBox(10, refreshButton, returnedButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(10));

        overdueTable.setItems(overdueItems);

        BorderPane content = new BorderPane();
        content.setTop(actionBar);
        content.setCenter(overdueTable);
        return content;
    }

    private BorderPane buildBorrowerRecordsTab() {
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> loadBorrowerRecords());

        HBox actionBar = new HBox(10, refreshButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(10));

        configureBorrowerRecordsTable(borrowerRecordsTable);
        borrowerRecordsTable.setItems(borrowerRecordsItems);

        BorderPane content = new BorderPane();
        content.setTop(actionBar);
        content.setCenter(borrowerRecordsTable);
        return content;
    }

    private void configureBorrowerRecordsTable(TableView<Reservation> table) {
        TableColumn<Reservation, ?> c1 = makeColumn("ID", 70, Reservation::getReservationId);
        TableColumn<Reservation, ?> c2 = makeColumn("Borrower", 160, Reservation::getUserFullName);
        TableColumn<Reservation, ?> c3 = makeColumn("Equipment", 140, Reservation::getEquipmentName);
        TableColumn<Reservation, ?> c4 = makeColumn("Asset Tag", 110, Reservation::getAssetTag);
        TableColumn<Reservation, ?> c5 = makeColumn("Qty", 70, Reservation::getQuantity);
        TableColumn<Reservation, ?> c6 = makeColumn("Status", 110, reservation -> reservation.getStatus().name());
        TableColumn<Reservation, ?> c7 = makeColumn("Due", 130, reservation -> DateTimeUtils.format(reservation.getDueDate()));
        TableColumn<Reservation, ?> c8 = makeColumn("Returned", 130, reservation -> DateTimeUtils.format(reservation.getReturnedAt()));
        TableColumn<Reservation, ?> c9 = makeColumn("Overdue", 90, reservation -> {
            if (reservation.getDueDate() == null) {
                return "No";
            }
            return reservation.getDueDate().isBefore(LocalDateTime.now()) ? "Yes" : "No";
        });
        table.getColumns().setAll(java.util.Arrays.<TableColumn<Reservation, ?>>asList(c1, c2, c3, c4, c5, c6, c7, c8, c9));
    }

    private BorderPane buildUsersTab() {
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button createAccountButton = new Button("Create Account");
        createAccountButton.getStyleClass().add("success-button");
        Button removeUserButton = new Button("Remove User");
        removeUserButton.getStyleClass().add("secondary-button");
        Button deleteUserButton = new Button("Delete User");
        deleteUserButton.getStyleClass().add("danger-button");
        Button viewRecordsButton = new Button("View Records");
        viewRecordsButton.getStyleClass().add("secondary-button");
        Button editUserButton = new Button("Edit User");
        editUserButton.getStyleClass().add("secondary-button");

        searchButton.setOnAction(event -> loadUsers());
        refreshButton.setOnAction(event -> {
            userSearchField.clear();
            loadUsers();
        });
        createAccountButton.setOnAction(event -> {
            RegisterFrame registerFrame = new RegisterFrame(this, true);
            registerFrame.setOnHidden(e -> loadUsers());
            registerFrame.show();
        });
        removeUserButton.setOnAction(event -> removeSelectedUser());
        deleteUserButton.setOnAction(event -> deleteSelectedUser());
        viewRecordsButton.setOnAction(event -> viewSelectedUserRecords());
        editUserButton.setOnAction(event -> editSelectedUser());

        HBox actionBar = new HBox(10, new Label("Search"), userSearchField, searchButton, refreshButton, createAccountButton, removeUserButton, deleteUserButton, viewRecordsButton, editUserButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(10));

        usersTable.setItems(userItems);

        BorderPane content = new BorderPane();
        content.setTop(actionBar);
        content.setCenter(usersTable);
        return content;
    }

    private void configureEquipmentTable() {
        TableColumn<Equipment, ?> colId = makeColumn("ID", 70, Equipment::getEquipmentId);
        TableColumn<Equipment, ?> colAsset = makeColumn("Asset Tag", 120, Equipment::getAssetTag);
        TableColumn<Equipment, ?> colName = makeColumn("Name", 140, Equipment::getName);
        TableColumn<Equipment, ?> colCategory = makeColumn("Category", 120, Equipment::getCategory);
        TableColumn<Equipment, ?> colStatus = makeColumn("Status", 100, equipment -> equipment.getStatus().name());
        TableColumn<Equipment, ?> colTotal = makeColumn("Total", 80, Equipment::getTotalQuantity);
        TableColumn<Equipment, ?> colAvailable = makeColumn("Available", 80, Equipment::getAvailableQuantity);
        TableColumn<Equipment, ?> colDesc = makeColumn("Description", 200, Equipment::getDescription);
        equipmentTable.getColumns().setAll(java.util.Arrays.<TableColumn<Equipment, ?>>asList(colId, colAsset, colName, colCategory, colStatus, colTotal, colAvailable, colDesc));
    }

    private void configureReservationTable() {
        TableColumn<Reservation, ?> r1 = makeColumn("ID", 70, Reservation::getReservationId);
        TableColumn<Reservation, ?> r2 = makeColumn("Borrower", 140, Reservation::getUserFullName);
        TableColumn<Reservation, ?> r3 = makeColumn("User ID", 80, Reservation::getUserId);
        TableColumn<Reservation, ?> r4 = makeColumn("Equipment", 140, Reservation::getEquipmentName);
        TableColumn<Reservation, ?> r5 = makeColumn("Asset Tag", 100, Reservation::getAssetTag);
        TableColumn<Reservation, ?> r6 = makeColumn("Qty", 70, Reservation::getQuantity);
        TableColumn<Reservation, ?> r7 = makeColumn("Status", 110, reservation -> reservation.getStatus().name());
        TableColumn<Reservation, ?> r8 = makeColumn("Requested", 130, reservation -> DateTimeUtils.format(reservation.getRequestDate()));
        TableColumn<Reservation, ?> r9 = makeColumn("Due", 120, reservation -> DateTimeUtils.format(reservation.getDueDate()));
        TableColumn<Reservation, ?> r10 = makeColumn("Returned", 120, reservation -> DateTimeUtils.format(reservation.getReturnedAt()));
        TableColumn<Reservation, ?> r11 = makeColumn("Remarks", 180, Reservation::getRemarks);
        reservationTable.getColumns().setAll(java.util.Arrays.<TableColumn<Reservation, ?>>asList(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11));
    }

    private void configureOverdueTable() {
        TableColumn<Reservation, ?> o1 = makeColumn("ID", 70, Reservation::getReservationId);
        TableColumn<Reservation, ?> o2 = makeColumn("Borrower", 160, Reservation::getUserFullName);
        TableColumn<Reservation, ?> o3 = makeColumn("Equipment", 140, Reservation::getEquipmentName);
        TableColumn<Reservation, ?> o4 = makeColumn("Asset Tag", 110, Reservation::getAssetTag);
        TableColumn<Reservation, ?> o5 = makeColumn("Qty", 70, Reservation::getQuantity);
        TableColumn<Reservation, ?> o6 = makeColumn("Due", 130, reservation -> DateTimeUtils.format(reservation.getDueDate()));
        TableColumn<Reservation, ?> o7 = makeColumn("Remaining", 100, reservation -> DateTimeUtils.remaining(reservation.getDueDate()));
        overdueTable.getColumns().setAll(java.util.Arrays.<TableColumn<Reservation, ?>>asList(o1, o2, o3, o4, o5, o6, o7));
    }

    private void configureUsersTable() {
        TableColumn<User, ?> u1 = makeColumn("ID", 60, User::getUserId);
        TableColumn<User, ?> u2 = makeColumn("Full Name", 160, User::getFullName);
        TableColumn<User, ?> u3 = makeColumn("Student ID", 120, User::getUsername);
        TableColumn<User, ?> u4 = makeColumn("Email", 180, User::getEmail);
        TableColumn<User, ?> u5 = makeColumn("Branch", 140, User::getBranch);
        TableColumn<User, ?> u6 = makeColumn("Course", 170, User::getCourse);
        TableColumn<User, ?> u7 = makeColumn("Block", 70, User::getBlock);
        TableColumn<User, ?> u8 = makeColumn("Role", 90, user -> user.getRole().name());
        TableColumn<User, ?> u9 = makeColumn("Active", 80, user -> user.isActive() ? "Yes" : "No");
        TableColumn<User, ?> u10 = makeColumn("Created", 140, user -> DateTimeUtils.format(user.getCreatedAt()));
        usersTable.getColumns().setAll(java.util.Arrays.<TableColumn<User, ?>>asList(u1, u2, u3, u4, u5, u6, u7, u8, u9, u10));
        usersTable.getSortOrder().setAll(java.util.List.of(u3));
        u3.setSortType(javafx.scene.control.TableColumn.SortType.ASCENDING);
    }

    private <S, T> TableColumn<S, T> makeColumn(String title, int width, Callback<S, T> extractor) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> Bindings.createObjectBinding(() -> extractor.call(cellData.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private ScrollPane wrapInScrollPane(javafx.scene.Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        return scrollPane;
    }

    private HBox buildStatusBar() {
        statusIndicator.setPrefSize(16, 16);
        statusIndicator.setVisible(false);
        statusLabel.getStyleClass().add("status-label");
        HBox statusBar = new HBox(6, statusIndicator, statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(8, 0, 0, 0));
        return statusBar;
    }

    private void setStatus(String message, boolean busy) {
        statusLabel.setText(message);
        statusIndicator.setVisible(busy);
    }

    private void loadEquipment() {
        setStatus("Loading equipment...", true);
        try {
            equipmentItems.setAll(equipmentController.searchEquipment(equipmentSearchField.getText()));
            refreshDashboardQuietly();
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void loadReservations() {
        setStatus("Loading reservations...", true);
        try {
            reservationItems.setAll(reservationController.getAllReservations().stream()
                    .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                    .collect(Collectors.toList()));
            refreshDashboardQuietly();
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void loadOverdue() {
        setStatus("Loading overdue list...", true);
        try {
            overdueItems.setAll(reservationController.getOverdueReservations());
            refreshDashboardQuietly();
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void loadBorrowerRecords() {
        setStatus("Loading borrower history...", true);
        try {
            borrowerRecordsItems.setAll(reservationController.getAllReservations().stream()
                    .filter(reservation -> reservation.getStatus() == ReservationStatus.APPROVED
                            || reservation.getStatus() == ReservationStatus.RETURNED)
                    .collect(Collectors.toList()));
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void loadUsers() {
        setStatus("Loading users...", true);
        try {
            userItems.setAll(userController.searchUsers(userSearchField.getText()));
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        } finally {
            setStatus("Ready", false);
        }
    }

    private void addEquipment() {
        try {
            equipmentController.addEquipment(readEquipmentForm(0));
            GuiUtils.showInfo(this, "Equipment added.");
            clearEquipmentForm();
            loadEquipment();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void updateEquipment() {
        try {
            if (selectedEquipmentId <= 0) {
                GuiUtils.showInfo(this, "Select equipment first.");
                return;
            }
            equipmentController.updateEquipment(readEquipmentForm(selectedEquipmentId));
            GuiUtils.showInfo(this, "Equipment updated.");
            loadEquipment();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void deleteEquipment() {
        if (selectedEquipmentId <= 0) {
            GuiUtils.showInfo(this, "Select equipment first.");
            return;
        }
        if (!GuiUtils.confirm(this, "Remove selected equipment?")) {
            return;
        }
        try {
            equipmentController.deleteEquipment(selectedEquipmentId);
            GuiUtils.showInfo(this, "Equipment removed.");
            clearEquipmentForm();
            loadEquipment();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private Equipment readEquipmentForm(int equipmentId) {
        Equipment equipment = new Equipment(
                assetTagField.getText(),
                equipmentNameField.getText(),
                categoryField.getText(),
                descriptionArea.getText(),
                statusComboBox.getValue(),
                totalQuantitySpinner.getValue(),
                availableQuantitySpinner.getValue()
        );
        equipment.setEquipmentId(equipmentId);
        return equipment;
    }

    private void clearEquipmentForm() {
        selectedEquipmentId = 0;
        equipmentTable.getSelectionModel().clearSelection();
        assetTagField.clear();
        equipmentNameField.clear();
        categoryField.clear();
        descriptionArea.clear();
        statusComboBox.setValue(EquipmentStatus.AVAILABLE);
        totalQuantitySpinner.getValueFactory().setValue(1);
        availableQuantitySpinner.getValueFactory().setValue(1);
    }

    private void populateEquipmentForm(Equipment equipment) {
        if (equipment == null) {
            clearEquipmentForm();
            return;
        }

        selectedEquipmentId = equipment.getEquipmentId();
        assetTagField.setText(equipment.getAssetTag());
        equipmentNameField.setText(equipment.getName());
        categoryField.setText(equipment.getCategory());
        descriptionArea.setText(equipment.getDescription());
        statusComboBox.setValue(equipment.getStatus());
        totalQuantitySpinner.getValueFactory().setValue(equipment.getTotalQuantity());
        availableQuantitySpinner.getValueFactory().setValue(equipment.getAvailableQuantity());
    }

    private void approveSelectedReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a reservation first.");
            return;
        }
        if (!GuiUtils.confirm(this, "Approve selected reservation with default 7 days?")) {
            return;
        }
        try {
            reservationController.approveReservation(selected.getReservationId());
            GuiUtils.showInfo(this, "Reservation approved.");
            refreshAll();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void approveSelectedReservationWithDueDate() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a reservation first.");
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.initOwner(this);
        dialog.setTitle("Approve with Custom Due Date");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> daysSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 7));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Borrow Days (1-7):"), 0, 0);
        grid.add(daysSpinner, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? daysSpinner.getValue() : null);

        dialog.showAndWait().ifPresent(days -> {
            try {
                reservationController.approveReservationWithDueDate(selected.getReservationId(), days);
                GuiUtils.showInfo(this, "Reservation approved for " + days + " days.");
                refreshAll();
            } catch (ValidationException | ServiceException exception) {
                GuiUtils.showError(this, exception);
            }
        });
    }

    private void declineSelectedReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a reservation first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(this);
        dialog.setTitle("Decline Reservation");
        dialog.setHeaderText("Reason for decline:");
        dialog.setContentText("Reason:");

        dialog.showAndWait().ifPresent(reason -> {
            try {
                reservationController.declineReservation(selected.getReservationId(), reason);
                GuiUtils.showInfo(this, "Reservation declined.");
                refreshAll();
            } catch (ValidationException | ServiceException exception) {
                GuiUtils.showError(this, exception);
            }
        });
    }

    private void setDueDateForSelectedReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a reservation first.");
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.initOwner(this);
        dialog.setTitle("Set Due Date");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> daysSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 1));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Due in (days)"), 0, 0);
        grid.add(daysSpinner, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? daysSpinner.getValue() : null);

        dialog.showAndWait().ifPresent(days -> {
            LocalDateTime dueDate = LocalDateTime.now().plusDays(days);
            try {
                boolean success = reservationController.setDueDate(selected.getReservationId(), dueDate);
                if (success) {
                    GuiUtils.showInfo(this, "Due date set successfully.");
                    refreshAll();
                } else {
                    GuiUtils.showInfo(this, "Failed to set due date. Make sure the reservation is approved.");
                }
            } catch (ValidationException | ServiceException exception) {
                GuiUtils.showError(this, exception);
            }
        });
    }

    private void returnSelectedReservation(TableView<Reservation> table) {
        Reservation selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a reservation first.");
            return;
        }
        if (!GuiUtils.confirm(this, "Mark selected equipment as returned?")) {
            return;
        }
        try {
            reservationController.markReturned(selected.getReservationId());
            GuiUtils.showInfo(this, "Equipment marked as returned.");
            refreshAll();
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void removeSelectedUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a user first.");
            return;
        }
        if (!GuiUtils.confirm(this, "Are you sure you want to remove user '" + selected.getFullName() + "'?")) {
            return;
        }
        try {
            boolean success = userController.deactivateUser(selected.getUserId());
            if (success) {
                GuiUtils.showInfo(this, "User removed successfully.");
                loadUsers();
            } else {
                GuiUtils.showInfo(this, "Failed to remove user.");
            }
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void deleteSelectedUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a user first.");
            return;
        }
        if (!GuiUtils.confirm(this, "Permanently delete user '" + selected.getFullName() + "'? This cannot be undone.")) {
            return;
        }
        try {
            boolean success = userController.deleteUser(selected.getUserId());
            if (success) {
                GuiUtils.showInfo(this, "User permanently deleted.");
                loadUsers();
            } else {
                GuiUtils.showInfo(this, "Failed to delete user. The user may have dependent records.");
            }
        } catch (ValidationException | ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void viewSelectedUserRecords() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a user first.");
            return;
        }
        try {
            List<Reservation> reservations = reservationController.getUserReservations(selected.getUserId());
                TableView<Reservation> recordsTable = new TableView<>();
                recordsTable.setItems(FXCollections.observableArrayList(reservations));
                TableColumn<Reservation, ?> c1 = makeColumn("ID", 60, Reservation::getReservationId);
                TableColumn<Reservation, ?> c2 = makeColumn("Equipment", 170, Reservation::getEquipmentName);
                TableColumn<Reservation, ?> c3 = makeColumn("Asset Tag", 110, Reservation::getAssetTag);
                TableColumn<Reservation, ?> c4 = makeColumn("Qty", 70, Reservation::getQuantity);
                TableColumn<Reservation, ?> c5 = makeColumn("Status", 100, reservation -> reservation.getStatus().name());
                TableColumn<Reservation, ?> c6 = makeColumn("Requested", 130, reservation -> DateTimeUtils.format(reservation.getRequestDate()));
                TableColumn<Reservation, ?> c7 = makeColumn("Due Date", 140, reservation -> DateTimeUtils.format(reservation.getDueDate()));
                TableColumn<Reservation, ?> c8 = makeColumn("Return Date", 140, reservation -> DateTimeUtils.format(reservation.getReturnDate()));
                TableColumn<Reservation, ?> c9 = makeColumn("Returned", 140, reservation -> DateTimeUtils.format(reservation.getReturnedAt()));
                TableColumn<Reservation, ?> c10 = makeColumn("Remarks", 180, Reservation::getRemarks);
                recordsTable.getColumns().setAll(java.util.Arrays.<TableColumn<Reservation, ?>>asList(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10));

            BorderPane content = new BorderPane(recordsTable);
            content.setPadding(new Insets(10));
            Stage dialog = new Stage();
            dialog.initOwner(this);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Records for " + selected.getFullName());
            dialog.setScene(new Scene(content, 900, 400));
            dialog.show();
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void editSelectedUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            GuiUtils.showInfo(this, "Select a user first.");
            return;
        }

        String[] nameParts = splitFullName(selected.getFullName());
        TextField firstNameField = new TextField(nameParts[0]);
        TextField middleNameField = new TextField(nameParts[1]);
        TextField lastNameField = new TextField(nameParts[2]);
        TextField suffixField = new TextField(nameParts[3]);
        TextField emailField = new TextField(selected.getEmail());
        TextField branchField = new TextField(selected.getBranch());
        TextField courseField = new TextField(selected.getCourse());
        TextField blockField = new TextField(selected.getBlock());
        TextField yearLevelField = new TextField(String.valueOf(selected.getYearLevel()));
        TextField phoneField = new TextField(selected.getPhoneNumber() != null ? selected.getPhoneNumber() : "");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Middle Name:"), 0, 1);
        grid.add(middleNameField, 1, 1);
        grid.add(new Label("Last Name:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(new Label("Suffix:"), 0, 3);
        grid.add(suffixField, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(new Label("Branch:"), 0, 5);
        grid.add(branchField, 1, 5);
        grid.add(new Label("Course:"), 0, 6);
        grid.add(courseField, 1, 6);
        grid.add(new Label("Block:"), 0, 7);
        grid.add(blockField, 1, 7);
        grid.add(new Label("Year Level:"), 0, 8);
        grid.add(yearLevelField, 1, 8);
        grid.add(new Label("Phone:"), 0, 9);
        grid.add(phoneField, 1, 9);

        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        saveButton.setOnAction(event -> {
            try {
                selected.setFullName(assembleFullName(firstNameField.getText(), middleNameField.getText(), lastNameField.getText(), suffixField.getText()));
                selected.setEmail(emailField.getText().trim());
                selected.setBranch(branchField.getText().trim());
                selected.setCourse(courseField.getText().trim());
                selected.setBlock(blockField.getText().trim());
                selected.setYearLevel(Integer.parseInt(yearLevelField.getText().trim()));
                selected.setPhoneNumber(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

                boolean success = userController.updateUser(selected);
                if (success) {
                    GuiUtils.showInfo(this, "User updated successfully.");
                    loadUsers();
                    ((Stage) saveButton.getScene().getWindow()).close();
                } else {
                    GuiUtils.showError(this, new Exception("Failed to update user."));
                }
            } catch (ValidationException | ServiceException exception) {
                GuiUtils.showError(this, exception);
            }
        });
        cancelButton.setOnAction(event -> ((Stage) cancelButton.getScene().getWindow()).close());

        HBox buttons = new HBox(10, saveButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogContent = new VBox(10, grid, buttons);
        dialogContent.setPadding(new Insets(20));

        Stage editStage = new Stage();
        editStage.initOwner(this);
        editStage.initModality(Modality.WINDOW_MODAL);
        editStage.setTitle("Edit User");
        editStage.setScene(new Scene(dialogContent, 520, 520));
        editStage.show();
    }

    private String[] splitFullName(String fullName) {
        String first = "";
        String middle = "";
        String last = "";
        String suffix = "";
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+");
            switch (parts.length) {
                case 1 -> first = parts[0];
                case 2 -> {
                    first = parts[0];
                    last = parts[1];
                }
                default -> {
                    String lastPart = parts[parts.length - 1];
                    if (lastPart.matches("(?i)(jr|sr|ii|iii|iv|v|phd|md)")) {
                        suffix = lastPart;
                        last = parts[parts.length - 2];
                        first = parts[0];
                        if (parts.length > 3) {
                            middle = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 2));
                        }
                    } else {
                        last = lastPart;
                        first = parts[0];
                        middle = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
                    }
                }
            }
        }
        return new String[]{first, middle, last, suffix};
    }

    private String assembleFullName(String first, String middle, String last, String suffix) {
        StringBuilder fullName = new StringBuilder();
        if (first != null && !first.isBlank()) {
            fullName.append(first.trim());
        }
        if (middle != null && !middle.isBlank()) {
            if (fullName.length() > 0) fullName.append(' ');
            fullName.append(middle.trim());
        }
        if (last != null && !last.isBlank()) {
            if (fullName.length() > 0) fullName.append(' ');
            fullName.append(last.trim());
        }
        if (suffix != null && !suffix.isBlank()) {
            if (fullName.length() > 0) fullName.append(' ');
            fullName.append(suffix.trim());
        }
        return fullName.toString().trim();
    }

    private void refreshDashboardQuietly() {
        try {
            List<Equipment> equipment = equipmentController.getAllEquipment();
            List<Reservation> reservations = reservationController.getAllReservations();
            List<Reservation> overdue = reservationController.getOverdueReservations();
            long pendingCount = reservations.stream()
                    .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                    .count();
            totalEquipmentLabel.setText(String.valueOf(equipment.size()));
            pendingReservationsLabel.setText(String.valueOf(pendingCount));
            overdueReservationsLabel.setText(String.valueOf(overdue.size()));
        } catch (ServiceException exception) {
            // ignore
        }
    }

    private void refreshAll() {
        loadEquipment();
        loadReservations();
        loadOverdue();
        loadBorrowerRecords();
        loadUsers();
        refreshDashboard();
    }

    private void refreshDashboard() {
        try {
            List<Equipment> equipment = equipmentController.getAllEquipment();
            List<Reservation> reservations = reservationController.getAllReservations();
            List<Reservation> overdue = reservationController.getOverdueReservations();
            long pendingCount = reservations.stream()
                    .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                    .count();
            totalEquipmentLabel.setText(String.valueOf(equipment.size()));
            pendingReservationsLabel.setText(String.valueOf(pendingCount));
            overdueReservationsLabel.setText(String.valueOf(overdue.size()));
        } catch (ServiceException exception) {
            GuiUtils.showError(this, exception);
        }
    }

    private void showChangePasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(this);
        dialog.setTitle("Change Password");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.control.PasswordField currentPasswordField = new javafx.scene.control.PasswordField();
        TextField currentPasswordVisibleField = new TextField();
        CheckBox showCurrentPassword = new CheckBox("Show Password");
        javafx.scene.control.PasswordField newPasswordField = new javafx.scene.control.PasswordField();
        TextField newPasswordVisibleField = new TextField();
        CheckBox showNewPassword = new CheckBox("Show Password");
        javafx.scene.control.PasswordField confirmPasswordField = new javafx.scene.control.PasswordField();
        TextField confirmPasswordVisibleField = new TextField();
        CheckBox showConfirmPassword = new CheckBox("Show Password");

        configurePasswordToggle(currentPasswordField, currentPasswordVisibleField, showCurrentPassword);
        configurePasswordToggle(newPasswordField, newPasswordVisibleField, showNewPassword);
        configurePasswordToggle(confirmPasswordField, confirmPasswordVisibleField, showConfirmPassword);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Current Password"), 0, 0);
        grid.add(new HBox(10, currentPasswordField, currentPasswordVisibleField, showCurrentPassword), 1, 0);
        grid.add(new Label("New Password"), 0, 1);
        grid.add(new HBox(10, newPasswordField, newPasswordVisibleField, showNewPassword), 1, 1);
        grid.add(new Label("Confirm Password"), 0, 2);
        grid.add(new HBox(10, confirmPasswordField, confirmPasswordVisibleField, showConfirmPassword), 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button);

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
                authController.changePassword(currentAdmin.getUserId(), currentPassword, newPassword);
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

    private void configurePasswordToggle(javafx.scene.control.PasswordField passwordField, TextField visibleField, CheckBox toggle) {
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
