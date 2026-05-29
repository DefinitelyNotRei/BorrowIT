package com.borrowit.view;

import com.borrowit.view.admin.AdminLoginFrame;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainLauncherFrame extends Stage {
    public MainLauncherFrame() {
        setTitle("BorrowIT Admin Console");
        setWidth(500);
        setHeight(320);
        setOnCloseRequest(event -> System.exit(0));
        centerOnScreen();

        Label titleLabel = new Label("BorrowIT Admin Console");
        titleLabel.getStyleClass().add("title-label");

        Label subtitleLabel = new Label("Admin Desktop Application");
        subtitleLabel.getStyleClass().add("subtitle-label");

        VBox headerPanel = new VBox(6, titleLabel, subtitleLabel);
        headerPanel.setPadding(new Insets(18, 16, 6, 16));
        headerPanel.setAlignment(Pos.CENTER);

        Button adminButton = new Button("Open Admin Application");
        adminButton.setPrefWidth(250);
        adminButton.setPrefHeight(40);
        adminButton.getStyleClass().add("primary-button");
        adminButton.setOnAction(event -> {
            new AdminLoginFrame().show();
            close();
        });

        VBox buttonPanel = new VBox(8, adminButton);
        buttonPanel.setPadding(new Insets(8, 50, 20, 50));
        buttonPanel.setAlignment(Pos.CENTER);

        VBox root = new VBox(12, headerPanel, buttonPanel);
        root.setPadding(new Insets(12));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root);
        // load app stylesheet
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        setScene(scene);
    }
}
