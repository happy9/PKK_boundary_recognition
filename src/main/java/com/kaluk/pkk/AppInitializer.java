package com.kaluk.pkk;

import controller.MainController;
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

public class AppInitializer extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainForm.fxml"));
        StackPane root = loader.load();

        MainController mainController = loader.getController();
        AppLogin appLogin = new AppLogin();
        mainController.setAppLogin(appLogin);
        mainController.setPrimaryStage(primaryStage);

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

    public static void deleteAllFilesInDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteAllFilesInDirectory(file);
                    } else {
                        if (!file.delete()) {
                            throw new IOException("Ошибка при удалении файла: " + file);
                        }
                    }
                }
            }
        }
    }

}
