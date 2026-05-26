package com.borrowit.view.auth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.borrowit.controller.AuthController;
import com.borrowit.model.User;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;
import com.borrowit.view.user.UserDashboardFrame;
import com.borrowit.view.util.GuiUtils;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class RegisterFrame extends Stage {
    private final AuthController authController = new AuthController();
    private final TextField firstNameField = new TextField();
    private final TextField middleNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField suffixField = new TextField();
    private final CheckBox noMiddleNameCheckBox = new CheckBox("Doesn't have Middle Name");
    private final CheckBox noSuffixCheckBox = new CheckBox("No suffix");
    private final TextField usernameField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final ComboBox<String> branchField = new ComboBox<>();
    private final Button registerButton = new Button("Register");
    private final Button backButton = new Button();
    private final Label statusLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ComboBox<String> courseField = new ComboBox<>();
    private final ComboBox<String> yearLevelField = new ComboBox<>();
    private final ComboBox<String> blockField = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final TextField passwordVisibleField = new TextField();
    private final CheckBox showPasswordCheckbox = new CheckBox("Show Password");
    private final PasswordField confirmPasswordField = new PasswordField();
    private final TextField confirmPasswordVisibleField = new TextField();
    private final CheckBox showConfirmPasswordCheckbox = new CheckBox("Show Password");
    private final Label errorLabel = new Label();

    private final Map<String, String[]> coursesByBranch = new HashMap<>();
    private final boolean isAdminMode;

    public RegisterFrame() {
        this(null, false);
    }

    public RegisterFrame(Stage owner, boolean isAdminMode) {
        this.isAdminMode = isAdminMode;
        setTitle("BorrowIT - Register");
        setMaximized(true);
        setOnCloseRequest(event -> {
            if (!isAdminMode) {
                new UserLoginFrame().show();
            }
        });
        centerOnScreen();
        if (owner != null) {
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);
        }

        initializeCourseData();
        initializeFormFields();

        Label titleLabel = new Label("Create User Account");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setVisible(false);

        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);

        HBox statusBar = new HBox(10, progressIndicator, statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);

        GridPane formPanel = new GridPane();
        formPanel.setPadding(new Insets(20, 40, 20, 40));
        formPanel.setHgap(10);
        formPanel.setVgap(10);

        addRow(formPanel, 0, "First Name", firstNameField);
        addRow(formPanel, 1, "Middle Name", createFieldWithCheckbox(middleNameField, noMiddleNameCheckBox));
        addRow(formPanel, 2, "Last Name", lastNameField);
        addRow(formPanel, 3, "Suffix", createFieldWithCheckbox(suffixField, noSuffixCheckBox));
        addRow(formPanel, 4, "Student ID", usernameField);
        addRow(formPanel, 5, "Phone Number", phoneField);
        addRow(formPanel, 6, "Branch", branchField);
        addRow(formPanel, 7, "Course", courseField);
        addRow(formPanel, 8, "Year Level", yearLevelField);
        addRow(formPanel, 9, "Block", blockField);
        addRow(formPanel, 10, "Password", createPasswordToggle(passwordField, passwordVisibleField, showPasswordCheckbox));
        addRow(formPanel, 11, "Confirm Password", createPasswordToggle(confirmPasswordField, confirmPasswordVisibleField, showConfirmPasswordCheckbox));
        Label passwordHintLabel = new Label("Passwords are auto-generated using GC<lastname><last4digits>.");
        passwordHintLabel.getStyleClass().add("subtitle-label");
        formPanel.add(passwordHintLabel, 1, 12);
        branchField.setOnAction(event -> updateCourseField());

        registerButton.setText("Confirm");
        registerButton.getStyleClass().add("success-button");
        registerButton.setDefaultButton(true);
        backButton.setText("Cancel");
        backButton.getStyleClass().add("danger-button");
        backButton.setCancelButton(true);

        registerButton.setOnAction(event -> register());
        backButton.setOnAction(event -> {
            if (isAdminMode) {
                close();
            } else {
                new UserLoginFrame().show();
                close();
            }
        });

        HBox buttonPanel = new HBox(10, backButton, registerButton);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        buttonPanel.setPadding(new Insets(10));

        VBox root = new VBox(12, titleLabel, errorLabel, statusBar, formPanel, buttonPanel);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("form-container");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        setScene(scene);

        updateCourseField();
        updateGeneratedPassword();
    }

    private void initializeFormFields() {
        branchField.getItems().addAll(
                "Select a branch first",
                "College of Computer Studies(CCS)",
                "College of Business and Accountancy(CBA)",
                "College of Education, Arts and Sciences(CEAS)",
                "College of Allied Health Studies(CAHS)",
                "College of Hospitality and Tourism Management(CHTM)"
        );
        branchField.getSelectionModel().selectFirst();

        usernameField.setPromptText("e.g. 20210001");
        usernameField.setTooltip(new Tooltip("Student email will be generated as StudentID@gordoncollege.edu.ph"));
        usernameField.setTextFormatter(new TextFormatter<>(createDigitFilter()));
        usernameField.getStyleClass().add("input-field");
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> updateGeneratedPassword());
        firstNameField.setTextFormatter(new TextFormatter<>(createNameFilter()));
        firstNameField.getStyleClass().add("input-field");
        middleNameField.setTextFormatter(new TextFormatter<>(createNameFilter()));
        middleNameField.getStyleClass().add("input-field");
        lastNameField.setTextFormatter(new TextFormatter<>(createNameFilter()));
        lastNameField.getStyleClass().add("input-field");
        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> updateGeneratedPassword());
        suffixField.setTextFormatter(new TextFormatter<>(createSuffixFilter()));
        suffixField.getStyleClass().add("input-field");
        emailField.setVisible(false);
        emailField.setManaged(false);
        phoneField.setPromptText("e.g. 09123456789");
        phoneField.setTextFormatter(new TextFormatter<>(createPhoneFilter()));
        phoneField.getStyleClass().add("input-field");
        branchField.getStyleClass().add("input-field");
        courseField.getStyleClass().add("input-field");
        yearLevelField.getStyleClass().add("input-field");
        blockField.getStyleClass().add("input-field");

        courseField.setDisable(true);
        courseField.getItems().add("Select a course");
        courseField.getSelectionModel().selectFirst();

        yearLevelField.getItems().addAll("Select year level", "1st", "2nd", "3rd", "4th");
        yearLevelField.getSelectionModel().selectFirst();

        blockField.getItems().addAll("A", "B", "C", "D", "E", "F");
        blockField.getSelectionModel().selectFirst();
    }

    private void addRow(GridPane panel, int row, String label, Node content) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        panel.add(labelNode, 0, row);
        if (content instanceof javafx.scene.control.Control control) {
            control.getStyleClass().add("input-field");
        }
        panel.add(content, 1, row);
        GridPane.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);
    }

    private HBox createFieldWithCheckbox(TextField field, CheckBox checkbox) {
        field.setMaxWidth(Double.MAX_VALUE);
        checkbox.setOnAction(event -> {
            boolean selected = checkbox.isSelected();
            field.clear();
            field.setDisable(selected);
        });
        HBox box = new HBox(10, field, checkbox);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    private UnaryOperator<TextFormatter.Change> createDigitFilter() {
        return change -> {
            String text = change.getText();
            return text.matches("[0-9]*") ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> createNameFilter() {
        return change -> {
            String text = change.getText();
            return text.matches("[a-zA-Z .'-]*") ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> createSuffixFilter() {
        return change -> {
            String text = change.getText();
            return text.matches("[a-zA-Z0-9 .'-]*") ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> createPhoneFilter() {
        return change -> {
            String text = change.getText();
            if (!text.matches("[0-9]*")) {
                return null;
            }
            return change.getControlNewText().length() <= 11 ? change : null;
        };
    }

    private void setWorking(boolean working, String message) {
        setFormDisabled(working);
        progressIndicator.setVisible(working);
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setVisible(working || (message != null && !message.isBlank()));
    }

    private void updateGeneratedPassword() {
        String generatedPassword = buildDefaultPassword(lastNameField.getText(), usernameField.getText());
        passwordField.setText(generatedPassword);
        confirmPasswordField.setText(generatedPassword);
    }

    private String buildDefaultPassword(String lastName, String userId) {
        String cleanedLastName = lastName == null ? "" : lastName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        if (cleanedLastName.isBlank()) {
            cleanedLastName = "user";
        }
        String digits = userId == null ? "" : userId.replaceAll("\\D", "");
        if (digits.isBlank()) {
            digits = "0000";
        }
        String suffix = digits.length() <= 4 ? String.format("%4s", digits).replace(' ', '0') : digits.substring(digits.length() - 4);
        return "GC" + cleanedLastName + suffix;
    }

    private HBox createPasswordToggle(PasswordField passwordField, TextField visibleField, CheckBox toggle) {
        passwordField.setPrefWidth(300);
        passwordField.getStyleClass().add("input-field");
        passwordField.setEditable(false);
        visibleField.setPrefWidth(300);
        visibleField.getStyleClass().add("input-field");
        visibleField.setEditable(false);
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
        HBox box = new HBox(10, passwordField, visibleField, toggle);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void setFormDisabled(boolean disabled) {
        firstNameField.setDisable(disabled);
        middleNameField.setDisable(disabled || noMiddleNameCheckBox.isSelected());
        lastNameField.setDisable(disabled);
        suffixField.setDisable(disabled || noSuffixCheckBox.isSelected());
        noMiddleNameCheckBox.setDisable(disabled);
        noSuffixCheckBox.setDisable(disabled);
        usernameField.setDisable(disabled);
        phoneField.setDisable(disabled);
        branchField.setDisable(disabled);
        courseField.setDisable(disabled);
        yearLevelField.setDisable(disabled);
        blockField.setDisable(disabled);
        passwordField.setDisable(disabled);
        passwordVisibleField.setDisable(disabled);
        confirmPasswordField.setDisable(disabled);
        confirmPasswordVisibleField.setDisable(disabled);
        registerButton.setDisable(disabled);
        backButton.setDisable(disabled);
    }

    private String assembleFullName() {
        String first = firstNameField.getText().trim();
        String middle = middleNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String suffix = suffixField.getText().trim();
        StringBuilder fullName = new StringBuilder();
        if (!first.isEmpty()) {
            fullName.append(first);
        }
        if (!middle.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(' ');
            }
            fullName.append(middle);
        }
        if (!last.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(' ');
            }
            fullName.append(last);
        }
        if (!suffix.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(' ');
            }
            fullName.append(suffix);
        }
        return fullName.toString().trim();
    }

    private void initializeCourseData() {
        coursesByBranch.put("College of Computer Studies(CCS)", new String[]{
                "Bachelor of Science in Computer Science (BSCS)",
                "Bachelor of Science in Information Technology (BSIT)",
                "Bachelor of Science in Entertainment Multimedia Computing (BSEMC)"
        });
        coursesByBranch.put("College of Business and Accountancy(CBA)", new String[]{
                "Bachelor of Science in Administration Major in Financial Management (BS-FM)",
                "Bachelor of Science in Administration Major in Human Resourse Management (BS-HRM)",
                "Bachelor of Science in Administration Major in Marketing Management (BSBA-MM)",
                "Bachelor of Science in Administration Major in Customs Administration (BSCA)"
        });
        coursesByBranch.put("College of Education, Arts and Sciences(CEAS)", new String[]{
                "Bachelor of Arts in Communication",
                "Bachelor of Early Childhood Education",
                "Bachelor of Culture and Arts Education",
                "Bachelor of Physical Education",
                "Bachelor of Elementary Education",
                "Bachelor of Secondary Major in English",
                "Bachelor of Secondary Major in Filipino",
                "Bachelor of Secondary Major in Mathematics",
                "Bachelor of Secondary Major in Social Studies",
                "Bachelor of Secondary Science Education major",
                "Teacher Certificate Program (TCP)"
        });
        coursesByBranch.put("College of Allied Health Studies(CAHS)", new String[]{
                "Bachelor of Science in Nursing (BSN)",
                "Bachelor of Science in Midwifery (BSM)"
        });
        coursesByBranch.put("College of Hospitality and Tourism Management(CHTM)", new String[]{
                "Bachelor of Science in Hospitality Management (BSHM)",
                "Bachelor of Science in Tourism Management (BSTM)"
        });
    }

    private void updateCourseField() {
        String selectedBranch = branchField.getValue();
        if (selectedBranch == null || selectedBranch.equals("Select a branch first")) {
            courseField.setDisable(true);
            courseField.getItems().setAll("Select a course");
            courseField.getSelectionModel().selectFirst();
            return;
        }

        String[] courses = coursesByBranch.getOrDefault(selectedBranch, new String[]{"Select a course"});
        courseField.setDisable(false);
        courseField.getItems().setAll(courses);
        courseField.getSelectionModel().selectFirst();
    }

    private void setError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private String normalizeYearLevel(String yearLevel) {
        return switch (yearLevel) {
            case "1st" -> "1";
            case "2nd" -> "2";
            case "3rd" -> "3";
            case "4th" -> "4";
            default -> "";
        };
    }

    private void register() {
        clearError();
        char[] password = passwordField.getText().toCharArray();
        char[] confirmation = confirmPasswordField.getText().toCharArray();
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                if (!Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("Passwords do not match.");
                }
                String selectedBranch = branchField.getValue();
                String selectedCourse = courseField.getValue();
                String selectedYearLevel = yearLevelField.getValue();
                String phoneNumber = phoneField.getText();
                if (selectedBranch == null || selectedBranch.equals("Select a branch first")) {
                    throw new IllegalArgumentException("Please select your branch before registering.");
                }
                if (selectedCourse == null || selectedCourse.equals("Select a course")) {
                    throw new IllegalArgumentException("Please select a valid course before registering.");
                }
                if (selectedYearLevel == null || selectedYearLevel.equals("Select year level")) {
                    throw new IllegalArgumentException("Please select a year level before registering.");
                }
                if (phoneNumber.isBlank()) {
                    throw new IllegalArgumentException("Phone number is required.");
                }
                if (!phoneNumber.matches("\\d{11}")) {
                    throw new IllegalArgumentException("Phone number must be 11 digits.");
                }
                String email = usernameField.getText().trim() + "@gordoncollege.edu.ph";
                return authController.registerUser(
                        assembleFullName(),
                        usernameField.getText(),
                        email,
                        selectedBranch,
                        selectedCourse,
                        blockField.getValue(),
                        normalizeYearLevel(selectedYearLevel),
                        phoneNumber,
                        password
                );
            }
        };

        task.setOnRunning(event -> setWorking(true, "Creating account..."));
        task.setOnSucceeded(event -> {
            setWorking(false, "Account created successfully.");
            User user = task.getValue();
            GuiUtils.showInfo(this, "Account created successfully.");
            if (isAdminMode) {
                close();
            } else {
                new UserDashboardFrame(user).show();
                close();
            }
            clearSensitive(password);
            clearSensitive(confirmation);
            passwordField.clear();
            confirmPasswordField.clear();
        });
        task.setOnFailed(event -> {
            setWorking(false, null);
            Throwable exception = task.getException();
            if (exception instanceof ValidationException || exception instanceof ServiceException || exception instanceof IllegalArgumentException) {
                setError(exception.getMessage());
            } else {
                setError("An unexpected error occurred.");
            }
            clearSensitive(password);
            clearSensitive(confirmation);
            passwordField.clear();
            confirmPasswordField.clear();
        });

        new Thread(task).start();
    }

    private void clearSensitive(char[] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }
}
