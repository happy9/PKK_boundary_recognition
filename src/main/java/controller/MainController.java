package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.kaluk.pkk.AppLogin;
import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.net.Network;
import com.teamdev.jxbrowser.net.callback.BeforeUrlRequestCallback;
import com.teamdev.jxbrowser.net.callback.VerifyCertificateCallback;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import okhttp3.*;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;
import static controller.LoginController.email;
import static controller.LoginController.userID;

public class MainController {
    @FXML
    private TabPane tabPane;
    @FXML
    Label loginLabel;
    private AppLogin appLogin;
    private Stage primaryStage;
    private Engine engine;

    private static final double EARTH_RADIUS = 6378137.0;
    public static Set<String> tiles = new HashSet<>();
    public static String[] selected_tile = null;
    private String geoJson;
    private Mat imgToRecognise;
    private Mat imgRecognised;

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    @FXML
    public void initialize() {

        loginLabel.setText(email);

        engine = Engine.newInstance(EngineOptions.newBuilder(OFF_SCREEN).licenseKey("1BNDHFSC1G6B28EJDAF9ZAO0H3B190SSYL8XPCWO3BP0DKU1U2U2TIWU75H1ALPLFUOCKI").build());
        Network network = engine.network();
        network.set(VerifyCertificateCallback.class, (params) -> VerifyCertificateCallback.Response.valid());
        network.set(BeforeUrlRequestCallback.class, params -> {
            String url = params.urlRequest().url();
            if (url.startsWith("https://pkk.rosreestr.ru/arcgis/rest/services/Address/pkk_locator_street/GeocodeServer/reverseGeocode?f=json&location=")) {
                try {
                    URL urlObject = new URL(url);
                    String query = urlObject.getQuery();
                    String[] paramsArray = query.split("&");
                    for (String param : paramsArray) {
                        if (param.startsWith("location=")) {
                            String location = param.substring(9);
                            String decodedLocation = URLDecoder.decode(location, StandardCharsets.UTF_8);
                            selected_tile = decodedLocation.split(",");
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (url.startsWith("https://pkk.rosreestr.ru/arcgis/rest/services/PKK6/CadastreObjects/MapServer/export?layers=")) {
                try {
                    URL urlObject = new URL(url);
                    String query = urlObject.getQuery();
                    String[] paramsArray = query.split("&");
                    for (String param : paramsArray) {
                        if (param.startsWith("bbox=")) {
                            String bbox = param.substring(5);
                            String decodedBbox = URLDecoder.decode(bbox, StandardCharsets.UTF_8);
                            tiles.add(decodedBbox);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return BeforeUrlRequestCallback.Response.proceed();
        });
    }
    @FXML
    public void PKK_Button() {

        Button selectTile_button = new Button("Выбрать тайл");
        selectTile_button.setMaxWidth(Double.MAX_VALUE);
        selectTile_button.setFont(new Font(20));
        selectTile_button.setOnAction(event -> selectTile());

        Browser browser = engine.newBrowser();
        BrowserView browserView = BrowserView.newInstance(browser);
        browser.navigation().loadUrl("https://pkk.rosreestr.ru");

        BorderPane borderPane = new BorderPane(browserView);

        VBox vbox = new VBox(selectTile_button, borderPane);
        vbox.getChildren().add(browserView);
        vbox.setFillWidth(true);
        VBox.setVgrow(browserView, Priority.ALWAYS);

        Tab tab = new Tab("ПКК", vbox);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

    }
    @FXML
    public void Nakarte_Button() {

        Browser browser = engine.newBrowser();
        BrowserView browserView = BrowserView.newInstance(browser);
        browser.navigation().loadUrl("https://nakarte.me");

        BorderPane borderPane = new BorderPane(browserView);

        Tab tab = new Tab("Nakarte.me", borderPane);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

    }
    @FXML
    public void History_Button() throws IOException {
        String keyFilePath = "src/main/resources/pkk-project-387310-firebase-adminsdk-a2luz-072270b890.json";
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(keyFilePath));

        String firebaseBucketName = "pkk-project-387310.appspot.com";
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        Bucket bucket = storage.get(firebaseBucketName);
        Page<Blob> blobs = bucket.list(Storage.BlobListOption.prefix(userID + "/"));

        Button loadGeoJSONButton = new Button("Скачать GeoJSON...");
        loadGeoJSONButton.setFont(new Font(20));
        loadGeoJSONButton.setMaxWidth(Double.MAX_VALUE);

        TableColumn<Pair<String, Pair<ImageView, ImageView>>, String> bboxColumn = new TableColumn<>("bbox");
        bboxColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getKey()));
        bboxColumn.setVisible(false);

        TableColumn<Pair<String, Pair<ImageView, ImageView>>, ImageView> tileColumn = new TableColumn<>("Исходный тайл");
        tileColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue().getKey()));

        TableColumn<Pair<String, Pair<ImageView, ImageView>>, ImageView> recognisedColumn = new TableColumn<>("Распознанные границы");
        recognisedColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue().getValue()));

        TableView<Pair<String, Pair<ImageView, ImageView>>> tableView = new TableView<>();
        tableView.getColumns().addAll(bboxColumn, tileColumn, recognisedColumn);

        for (Blob blob : blobs.iterateAll()) {
            String imageName = blob.getName();
            if (imageName.startsWith(userID + "/tile_")) {
                Image tileImage = new Image(blob.getMediaLink(), false);
                ImageView tileView = new ImageView(tileImage);

                String correspondingImageName = imageName.replace("tile_", "");
                Blob correspondingBlob = bucket.get(correspondingImageName);
                Image recognisedImage = new Image(correspondingBlob.getMediaLink(), false);
                ImageView recognisedView = new ImageView(recognisedImage);

                tileView.setFitHeight(512);
                tileView.setFitWidth(512);
                tileView.setPreserveRatio(true);

                recognisedView.setFitHeight(512);
                recognisedView.setFitWidth(512);
                recognisedView.setPreserveRatio(true);

                String[] parts = imageName.split("tile_");
                String lastPart = parts[1];

                String[] lastPartParts = lastPart.split("\\.");
                String bbox = String.join(".", Arrays.copyOfRange(lastPartParts, 0, lastPartParts.length-1));

                tableView.getItems().add(new Pair<>(bbox, new Pair<>(tileView, recognisedView)));
            }
        }
        tableView.refresh();

        loadGeoJSONButton.setOnAction(event -> {
            Pair<String, Pair<ImageView, ImageView>> selectedRow = tableView.getSelectionModel().getSelectedItem();
            if (selectedRow != null) {
                String bbox = selectedRow.getKey();
                String filePath = userID+"/"+bbox+".geojson";
                Blob blob = bucket.get(filePath);

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Сохранить GeoJSON");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("GeoJSON Файлы", "*.geojson"));
                fileChooser.setInitialFileName("Новый.geojson");
                File outputFile = fileChooser.showSaveDialog(primaryStage);
                if (outputFile != null) {
                    blob.downloadTo(outputFile.toPath());
                }
            }
        });

        VBox vBox = new VBox(loadGeoJSONButton, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Tab tab = new Tab("История");
        tab.setContent(vBox);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }
    @FXML
    public void About_Button() {
        BorderPane borderPane = new BorderPane();

        Label label = new Label("Студент группы ЗБ-ПИ21-1с\nЛукашов Кирилл Александрович");
        label.setFont(new Font(20));
        borderPane.setCenter(label);

        Tab tab = new Tab("Об авторе", borderPane);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }
    @FXML
    public void Exit_Button() throws IOException {
        if (appLogin != null) {
            appLogin.start(new Stage());
            primaryStage.close();
        }
    }


    public void selectTile() {
        Set<String> selected_tiles = new HashSet<>();
        String[] selected_tile = MainController.selected_tile;
        Set<String> tiles = MainController.tiles;

        if (selected_tile != null) {
            double[] mercatorCoords = lonLatToWebMercator(Double.parseDouble(selected_tile[0]), Double.parseDouble(selected_tile[1]));

            for (String tile : tiles) {
                if (isBoundingBoxTouching(mercatorCoords, tile)) {
                    selected_tiles.add(tile);
                }
            }

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            FlowPane tilePane = new FlowPane();
            tilePane.setVgap(10);
            tilePane.setHgap(10);

            ScrollPane scrollPane = new ScrollPane(tilePane);
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);

            Scene imageSelectionScene = new Scene(scrollPane);

            Stage imageSelectionStage = new Stage();
            imageSelectionStage.setTitle("Выберите нужный тайл");
            imageSelectionStage.setScene(imageSelectionScene);
            imageSelectionStage.setWidth(1200);
            imageSelectionStage.setHeight(800);
            imageSelectionStage.initModality(Modality.APPLICATION_MODAL);

            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

                for (String tile : selected_tiles) {
                    String[] bboxArray = tile.split(",");
                    String bbox = bboxArray[0] + "%2C" + bboxArray[1] + "%2C" + bboxArray[2] + "%2C" + bboxArray[3];
                    String url = "https://pkk.rosreestr.ru/arcgis/rest/services/PKK6/CadastreObjects/MapServer/export?layers=show%3A27%2C24%2C23%2C22&dpi=96&format=PNG32&bbox=" + bbox + "&bboxSR=102100&imageSR=102100&size=1024%2C1024&transparent=true&f=image&_ts=false";

                    try {
                        URL imageUrl = new URL(url);
                        BufferedImage bufferedImage = ImageIO.read(imageUrl);

                        if (bufferedImage != null) {
                            Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                            ImageView imageView = new ImageView(fxImage);
                            imageView.setStyle("-fx-border-color: black; -fx-border-width: 2;");
                            imageView.setPreserveRatio(true);
                            imageView.setFitHeight(512);
                            imageView.setFitWidth(512);

                            Pane imagePane = new Pane(imageView);
                            imagePane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
                            tilePane.getChildren().add(imagePane);

                            imagePane.setOnMouseReleased(eventSelectImage -> {
                                String filePath = "src/main/resources/temp/tile_" + tile + ".png";

                                saveSelectedImage(filePath, bufferedImage);
                                runRecognition(filePath);
                                imageSelectionStage.close();
                            });
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    imageSelectionStage.show();
                }
            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка");
                    alert.setHeaderText("Произошла ошибка при подключении к серверу! Пожалуйста, попробуйте еще раз...");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Тайл не выбран");
                alert.showAndWait();
            });
        }
    }
    private void detectLines(String imagePath) {

        try {
            String tempPath = "src/main/resources/temp/temptile.png";
            removeTransparencyAndSave(imagePath, tempPath);

            imgToRecognise = Imgcodecs.imread(tempPath);
            imgRecognised = Mat.zeros(imgToRecognise.size(), CvType.CV_8UC3);

            if (imgToRecognise.empty()) {
                System.out.println("Ошибка загрузки изображения: " + tempPath);
                return;
            }

            String imageName = new File(imagePath).getName();
            String bboxName = imageName.replace("tile_", "");
            bboxName = bboxName.replace(".png", "");
            String[] bboxValues = bboxName.split(",");

            double minLong = Double.parseDouble(bboxValues[0]);
            double minLat = Double.parseDouble(bboxValues[1]);
            double maxLong = Double.parseDouble(bboxValues[2]);
            double maxLat = Double.parseDouble(bboxValues[3]);

            CoordinateReferenceSystem crs3857 = CRS.decode("EPSG:3857", true);
            CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326", true);

            MathTransform transform = CRS.findMathTransform(crs3857, crs4326, true);

            double imageWidth = imgToRecognise.width();
            double imageHeight = imgToRecognise.height();

            double metersPerPixelWidth = (maxLong - minLong) / imageWidth;
            double metersPerPixelHeight = (maxLat - minLat) / imageHeight;

            Mat gray = new Mat();
            Mat edges = new Mat();
            Mat blurred = new Mat();

            Imgproc.cvtColor(imgToRecognise, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            Imgproc.adaptiveThreshold(blurred, edges, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 2);
            Imgproc.Canny(edges, edges, 50, 150, 3, false);

            Mat lines = new Mat();
            Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 50, 50, 10);

            for (int i = 0; i < lines.rows(); i++) {
                double[] l = lines.get(i, 0);
                Imgproc.line(imgRecognised, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 255, 0), 2);
            }

            Imgcodecs.imwrite("src/main/resources/temp/"+bboxName+".png", imgRecognised);

            //Сохранение в GeoJSON
            FeatureCollection featureCollection = new FeatureCollection();

            for (int i = 0; i < lines.rows(); i++) {
                double[] l = lines.get(i, 0);
                Imgproc.line(imgRecognised, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 255, 0), 2);

                double lineStartLong = minLong + (l[0] * metersPerPixelWidth);
                double lineStartLat = maxLat - (l[1] * metersPerPixelHeight);
                double lineEndLong = minLong + (l[2] * metersPerPixelWidth);
                double lineEndLat = maxLat - (l[3] * metersPerPixelHeight);

                Coordinate coordStart = JTS.transform(new Coordinate(lineStartLong, lineStartLat), null, transform);
                Coordinate coordEnd = JTS.transform(new Coordinate(lineEndLong, lineEndLat), null, transform);

                LineString line = new LineString();
                line.add(new LngLatAlt(coordStart.x, coordStart.y));
                line.add(new LngLatAlt(coordEnd.x, coordEnd.y));

                Feature feature = new Feature();
                feature.setGeometry(line);

                featureCollection.add(feature);
            }

            geoJson = new ObjectMapper().writeValueAsString(featureCollection);
            try (FileWriter file = new FileWriter("src/main/resources/temp/"+bboxName+".geojson")) {
                file.write(geoJson);
            }

            uploadFileToBase(bboxName+".geojson", "src/main/resources/temp/"+bboxName+".geojson");
            uploadFileToBase("tile_"+bboxName+".png","src/main/resources/temp/tile_"+bboxName+".png");
            uploadFileToBase(bboxName+".png","src/main/resources/temp/"+bboxName+".png");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void runRecognition(String filePath){

        detectLines(filePath);

        Tab recognitionTab = new Tab("Распознавание");
        String currentGeoJSON = geoJson;

        Button saveGeoJSONButton = new Button("Сохранить GeoJSON...");
        saveGeoJSONButton.setMaxWidth(Double.MAX_VALUE);
        saveGeoJSONButton.setFont(new Font(20));
        saveGeoJSONButton.setOnAction(eventSaveJSON -> {
            saveGeoJSON(currentGeoJSON);
        });

        VBox vBox = new VBox();
        vBox.setFillWidth(true);
        VBox.setVgrow(saveGeoJSONButton, Priority.ALWAYS);
        vBox.getChildren().add(saveGeoJSONButton);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(vBox);

        ImageView imgToRecogniseView = createImageViewFromMat(imgToRecognise);
        imgToRecogniseView.setPreserveRatio(true);

        ImageView imgRecognisedView = createImageViewFromMat(imgRecognised);
        imgRecognisedView.setPreserveRatio(true);

        HBox hBox = new HBox(imgToRecogniseView, imgRecognisedView);
        borderPane.setCenter(hBox);

        recognitionTab.setContent(borderPane);
        tabPane.getTabs().add(recognitionTab);
        tabPane.getSelectionModel().select(recognitionTab);

        Platform.runLater(() -> {
            imgToRecogniseView.fitWidthProperty().bind(recognitionTab.getTabPane().widthProperty().divide(2));
            imgToRecogniseView.fitHeightProperty().bind(recognitionTab.getTabPane().heightProperty());
            imgRecognisedView.fitWidthProperty().bind(recognitionTab.getTabPane().widthProperty().divide(2));
            imgRecognisedView.fitHeightProperty().bind(recognitionTab.getTabPane().heightProperty());
        });

    }


    private void saveSelectedImage(String filePath, BufferedImage bufferedImage) {
        File outputFile = new File(filePath);
        outputFile.deleteOnExit();

        try {
            ImageIO.write(bufferedImage, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void saveGeoJSON(String currentGeoJSON){
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить GeoJSON");
            fileChooser.setInitialFileName("Новый.geojson");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("GeoJSON Файлы", "*.geojson")
            );

            File outputPath = fileChooser.showSaveDialog(tabPane.getScene().getWindow());

            if (outputPath != null) {
                try (FileWriter file = new FileWriter(outputPath)) {
                    file.write(currentGeoJSON);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void uploadFileToBase(String fileName, String pathName) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String url = "https://firebasestorage.googleapis.com/v0/b/pkk-project-387310.appspot.com/o/" + userID + "%2F" + fileName;

        File file = new File(pathName);

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Ошибка: " + response);
        }
    }


    public boolean isBoundingBoxTouching(double[] selectedTile, String bbox) {
        String[] bboxArray = bbox.split(",");
        if (bboxArray.length == 4) {
            try {
                double selectedTileLong = selectedTile[0];
                double selectedTileLat = selectedTile[1];
                double bboxLeft = Double.parseDouble(bboxArray[0]);
                double bboxBottom = Double.parseDouble(bboxArray[1]);
                double bboxRight = Double.parseDouble(bboxArray[2]);
                double bboxTop = Double.parseDouble(bboxArray[3]);

                return selectedTileLong >= bboxLeft && selectedTileLong <= bboxRight && selectedTileLat >= bboxBottom && selectedTileLat <= bboxTop;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    private void removeTransparencyAndSave(String originalPath, String tempPath) {
        try {
            BufferedImage originalImage = ImageIO.read(new File(originalPath));
            BufferedImage imageCopy = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

            Graphics2D g = imageCopy.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, imageCopy.getWidth(), imageCopy.getHeight());

            g.drawImage(originalImage, 0, 0, null);
            g.dispose();

            ImageIO.write(imageCopy, "png", new File(tempPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private ImageView createImageViewFromMat(Mat mat) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", mat, mob);
        byte ba[] = mob.toArray();

        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(ba));
            if (bufferedImage.getTransparency() != Transparency.OPAQUE) {
                BufferedImage newImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = newImage.createGraphics();
                g.drawImage(bufferedImage, 0, 0, null);
                g.dispose();
                bufferedImage = newImage;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert bufferedImage != null;
        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
        ImageView imageView = new ImageView(image);

        return imageView;
    }
    public double[] lonLatToWebMercator(double lon, double lat) {
        double x = Math.toRadians(lon) * EARTH_RADIUS;
        double y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(lat) / 2)) * EARTH_RADIUS;

        return new double[]{x, y};
    }


    public void setAppLogin(AppLogin appLogin) {
        this.appLogin = appLogin;
    }
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
