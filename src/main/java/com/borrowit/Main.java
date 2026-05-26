package com.borrowit;

import com.borrowit.view.MainLauncherFrame;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        MainLauncherFrame launcherFrame = new MainLauncherFrame();
        launcherFrame.show();
    }
}
                                                                                            