package com.kaluk.pkk;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.IOException;

public class NewMain {
    public static void main(String[] args) throws IOException {
        FileInputStream serviceAccount =
                new FileInputStream("src/main/resources/pkk-project-387310-firebase-adminsdk-a2luz-072270b890.json");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://pkk-project-387310.firebaseio.com/")
                .build();

        FirebaseApp.initializeApp(options);
        AppLogin.main(args);
    }

}