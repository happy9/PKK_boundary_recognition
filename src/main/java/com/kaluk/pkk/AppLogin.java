package com.kaluk.pkk;

import controller.LoginController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.kaluk.pkk.AppInitializer.deleteAllFilesInDirectory;

public class AppLogin extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginForm.fxml"));
        StackPane root = loader.load();

        LoginController loginController = loader.getController();
        AppInitializer appInitializer = new AppInitializer();
        loginController.setAppInitializer(appInitializer);
        loginController.setPrimaryStage(primaryStage);

        primaryStage.setTitle("Распознавание и векторизация границ по растровым тайлам публичной кадастровой карты");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo.png")));
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            try {
                deleteAllFilesInDirectory(new File("src/main/resources/temp"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });
    }
}
