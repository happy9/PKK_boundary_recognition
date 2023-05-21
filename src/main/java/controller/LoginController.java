package controller;

import com.kaluk.pkk.AppInitializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import okhttp3.*;
import org.controlsfx.control.Notifications;

import java.io.IOException;
import java.util.Map;

public class LoginController {
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    private AppInitializer appInitializer;
    private Stage primaryStage;

    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "AIzaSyDiHm5GpoifS4p7PgXWnRX3g_nucFL1J7Y";
    public static String email;
    public static String userID;

    @FXML
    public void Login_Button() throws IOException {

        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;

        email = loginField.getText();
        String password = passField.getText();

        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"returnSecureToken\":true}";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = client.newCall(request).execute();

        if(response.isSuccessful()) {
            assert response.body() != null;
            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            userID = (String) responseMap.get("localId");

            client.newCall(request).execute();

            if (appInitializer != null) {
                appInitializer.start(new Stage());
                primaryStage.close();
            }
        } else {
            assert response.body() != null;
            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });
                Map<String, Object> errorMap = (Map<String, Object>) responseMap.get("error");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Ошибка при входе в систему: " + errorMap.get("message"));
                alert.showAndWait();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }


    }

    @FXML
    public void Registr_Button() throws IOException {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey;

        String email = loginField.getText();
        String password = passField.getText();

        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"returnSecureToken\":true}";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = client.newCall(request).execute();

        if(response.isSuccessful()) {
            Notifications.create()
                    .title("Успешная регистрация")
                    .text("Регистрация прошла успешно!")
                    .owner(primaryStage)
                    .position(Pos.BOTTOM_RIGHT)
                    .showInformation();
        } else {
            assert response.body() != null;
            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });
                Map<String, Object> errorMap = (Map<String, Object>) responseMap.get("error");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Ошибка при входе в систему: " + errorMap.get("message"));
                alert.showAndWait();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public void setAppInitializer(AppInitializer appInitializer) {
        this.appInitializer = appInitializer;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
