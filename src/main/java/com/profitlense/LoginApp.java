package com.profitlense;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair; // <-- NEW IMPORT

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Optional; // <-- NEW IMPORT
import java.util.stream.Collectors;

import java.io.InputStream;
import java.io.InputStreamReader;

public class LoginApp extends Application {

    // --- User data storage ---
    private Map<String, UserData> users = new HashMap<>();
    private UserData currentUser = null;
    private final File userFile = new File("src/main/resources/user.csv");
    private final String CSV_HEADER = "email,password,firstName,lastName,phone,location,photoPath,role";
    private final Set<String> ADMIN_EMAILS = Set.of("fahim@gmail.com", "talha@gmail.com", "sanjida@gmail.com");

    // --- App Settings ---
    private Properties appSettings = new Properties();
    private final File settingsFile = new File("src/main/resources/settings.properties");

    private final String[] districts = {
            "Bagerhat", "Bandarban", "Barguna", "Barisal", "Bhola", "Bogra", "Brahmanbaria",
            "Chandpur", "Chapai Nawabganj", "Chattogram", "Chuadanga", "Comilla", "Cox's Bazar",
            "Dhaka", "Dinajpur", "Faridpur", "Feni", "Gaibandha", "Gazipur", "Gopalganj",
            "Habiganj", "Jamalpur", "Jashore", "Jhalokati", "Jhenaidah", "Joypurhat",
            "Khagrachhari", "Khulna", "Kishoreganj", "Kurigram", "Kushtia", "Lakshmipur",
            "Lalmonirhat", "Madaripur", "Magura", "Manikganj", "Meherpur", "Moulvibazar",
            "Munshiganj", "Mymensingh", "Naogaon", "Narail", "Narayanganj", "Narsingdi",
            "Natore", "Netrokona", "Nilphamari", "Noakhali", "Pabna", "Panchagarh",
            "Patuakhali", "Pirojpur", "Rajbari", "Rajshahi", "Rangamati", "Rangpur",
            "Satkhira", "Shariatpur", "Sherpur", "Sirajganj", "Sunamganj", "Sylhet",
            "Tangail", "Thakurgaon"
    };

    private StackPane root;
    private HBox loginPane, signupPane;
    private Stage primaryStage;

    // --- UI Fields (for Dashboard) ---
    private Label reportDistrictLabel, reportInvestmentLabel, suggestionTextLabel;
    private Label source1DetailLabel, source2DetailLabel, source3DetailLabel;
    private PieChart profitPieChart;
    private ObservableList<PieChart.Data> pieChartData;
    private StackPane contentArea;
    private VBox suggestionPage;
    private VBox profilePage;
    private AdminPanel adminPage;

    // --- UI Fields for Profile Page ---
    private ImageView profilePageImageView;
    private Label profilePageNameValue;
    private Label profilePageEmailValue;
    private Label profilePagePhoneValue;
    private Label profilePageLocationValue;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        loadSettings();
        loadUsers();

        loginPane = createLoginScreen(stage);
        signupPane = createSignupScreen(stage);

        root.getChildren().add(loginPane);
        root.setStyle("-fx-background-color: #FFFFFF;");
        Scene scene = new Scene(root, 900, 650);
        stage.setScene(scene);
        stage.setTitle("ProfitLense");
        stage.show();
    }

    private void loadSettings() {
        if (!settingsFile.exists()) {
            try (OutputStream output = new FileOutputStream(settingsFile)) {
                appSettings.setProperty("enable.registration", "true");
                appSettings.setProperty("default.reset.password", "password123");
                appSettings.setProperty("maintenance.mode", "false");
                appSettings.store(output, "ProfitLense Application Settings");
            } catch (IOException e) {
                System.err.println("Error creating default settings.properties: " + e.getMessage());
            }
        } else {
            try (InputStream input = new FileInputStream(settingsFile)) {
                appSettings.load(input);
            } catch (IOException e) {
                System.err.println("Error loading settings.properties: " + e.getMessage());
            }
        }
    }

    private void loadUsers() {
        users.clear();
        if (!userFile.exists()) {
            System.out.println("user.csv not found. Creating a new one...");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile))) {
                writer.write(CSV_HEADER);
                writer.newLine();
            } catch (IOException e) { e.printStackTrace(); }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader && line.trim().equals(CSV_HEADER)) { isHeader = false; continue; }
                isHeader = false;
                if (line.trim().isEmpty()) continue;
                UserData userData = UserData.fromCsvString(line);
                if (userData != null && userData.getEmail() != null && !userData.getEmail().isEmpty()) {
                    users.put(userData.getEmail(), userData);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        for (String adminEmail : ADMIN_EMAILS) {
            UserData adminUser = users.get(adminEmail);
            if (adminUser == null) {
                String pass = "admin123";
                String first = adminEmail.split("@")[0];
                first = first.substring(0, 1).toUpperCase() + first.substring(1);
                adminUser = new UserData(adminEmail, pass, first, "Admin", "", "Dhaka", "", "ADMIN");
                saveUser(adminUser, false);
            } else {
                adminUser.setRole("ADMIN");
                if (!adminUser.getPassword().equals("admin123")) {
                    users.put(adminEmail, new UserData(adminUser.getEmail(), "admin123", adminUser.getFirstName(), adminUser.getLastName(), adminUser.getPhone(), adminUser.getLocation(), adminUser.getPhotoPath(), "ADMIN"));
                }
            }
        }
    }


    private VBox createLeftPane(String title, String subtitle) {
        VBox leftPane = new VBox(10);
        leftPane.setStyle("-fx-background-color: #243B55;");
        leftPane.setAlignment(Pos.CENTER_LEFT);
        leftPane.setPadding(new Insets(40));
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE); titleLabel.setWrapText(true);
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitleLabel.setTextFill(Color.web("#B0C4DE")); subtitleLabel.setWrapText(true);
        leftPane.getChildren().addAll(titleLabel, subtitleLabel);
        return leftPane;
    }

    // --- (UPDATED) createLoginRightPane ---
    private BorderPane createLoginRightPane(Stage stage, HBox signupLinkBox) {
        Label title = new Label("Login");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28)); title.setTextFill(Color.BLACK);
        Label emailLabel = new Label("Username or Email");
        emailLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email"); emailField.setStyle("-fx-font-size: 14px;");
        VBox emailBox = new VBox(5, emailLabel, emailField);
        Label passLabel = new Label("Password");
        passLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password"); passwordField.setStyle("-fx-font-size: 14px;");
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Enter your password"); visiblePasswordField.setStyle("-fx-font-size: 14px;");
        visiblePasswordField.setManaged(false); visiblePasswordField.setVisible(false);
        StackPane passwordContainer = new StackPane(passwordField, visiblePasswordField);
        VBox passwordBox = new VBox(5, passLabel, passwordContainer);
        CheckBox rememberMe = new CheckBox("Remember Me");
        CheckBox showPassword = new CheckBox("Show Password");

        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        // --- (NEW) Add action to the hyperlink ---
        forgotPassword.setOnAction(e -> showForgotPasswordDialog());
        // --- (END OF NEW) ---

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        visiblePasswordField.managedProperty().bind(showPassword.selectedProperty());
        visiblePasswordField.visibleProperty().bind(showPassword.selectedProperty());
        passwordField.managedProperty().bind(showPassword.selectedProperty().not());
        passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        HBox optionsBox = new HBox(10, rememberMe, showPassword);
        HBox optionsBox2 = new HBox(0, spacer, forgotPassword);
        Label message = new Label();
        message.setTextFill(Color.RED); message.setPadding(new Insets(5, 0, 5, 0));
        Button loginBtn = new Button("Login");
        loginBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14)); loginBtn.setTextFill(Color.WHITE);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #0078D7; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("-fx-background-color: #005A9E; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;"));

        Runnable loginAction = () -> {
            String email = emailField.getText().trim();
            String pass = passwordField.getText().trim();
            UserData foundUser = users.get(email);

            boolean maintenance = Boolean.parseBoolean(appSettings.getProperty("maintenance.mode", "false"));

            if (foundUser != null && foundUser.getPassword().equals(pass)) {
                if(ADMIN_EMAILS.contains(email)) {
                    foundUser.setRole("ADMIN");
                }

                if (maintenance && !"ADMIN".equals(foundUser.getRole())) {
                    message.setTextFill(Color.RED);
                    message.setText("Software is in maintenance. Only admins can log in.");
                    return;
                }

                currentUser = foundUser;
                message.setTextFill(Color.GREEN); message.setText("Login Successful!");
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), loginPane);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    root.getChildren().remove(loginPane);
                    loadDashboard();
                });
                fadeOut.play();
            } else {
                message.setTextFill(Color.RED); message.setText("Invalid email or password!");
            }
        };
        loginBtn.setOnAction(e -> loginAction.run());
        emailField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) loginAction.run(); });
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) loginAction.run(); });
        visiblePasswordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) loginAction.run(); });

        Separator separator1 = new Separator(); separator1.setPadding(new Insets(10, 0, 10, 0));
        ImageView fbIcon = createSocialIcon("/icons/facebook.png");
        ImageView liIcon = createSocialIcon("/icons/linkedin.png");
        ImageView twIcon = createSocialIcon("/icons/twitter.png");
        HBox socialIcons = new HBox(20, fbIcon, liIcon, twIcon);
        socialIcons.setAlignment(Pos.CENTER); socialIcons.setPadding(new Insets(10, 0, 10, 0));
        Separator separator2 = new Separator(); separator2.setPadding(new Insets(10, 0, 0, 0));

        VBox formContainer = new VBox(15,
                title, emailBox, passwordBox, optionsBox, optionsBox2,
                loginBtn, message, separator1, socialIcons, separator2
        );
        formContainer.setAlignment(Pos.CENTER_LEFT);
        formContainer.setMaxWidth(350);
        formContainer.setPadding(new Insets(30));

        BorderPane rightPane = new BorderPane();
        rightPane.setStyle("-fx-background-color: white;");
        rightPane.setCenter(formContainer);
        rightPane.setBottom(signupLinkBox);
        BorderPane.setAlignment(signupLinkBox, Pos.CENTER);
        BorderPane.setMargin(signupLinkBox, new Insets(0, 0, 30, 0));

        return rightPane;
    }

    private BorderPane createSignupRightPane(HBox loginLinkBox) {
        BorderPane rightPane = new BorderPane();
        rightPane.setStyle("-fx-background-color: white;");
        rightPane.setBottom(loginLinkBox);
        BorderPane.setAlignment(loginLinkBox, Pos.CENTER);
        BorderPane.setMargin(loginLinkBox, new Insets(0, 0, 30, 0));

        boolean registrationEnabled = Boolean.parseBoolean(appSettings.getProperty("enable.registration", "true"));

        if (!registrationEnabled) {
            Label disabledLabel = new Label("New user registration is currently disabled by the administrator.");
            disabledLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            disabledLabel.setTextFill(Color.RED);
            disabledLabel.setWrapText(true);
            disabledLabel.setPadding(new Insets(40));
            rightPane.setCenter(disabledLabel);
            return rightPane;
        }

        Label title = new Label("Sign Up");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28)); title.setTextFill(Color.BLACK);
        TextField firstNameField = new TextField(); firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField(); lastNameField.setPromptText("Last Name");
        HBox nameBox = new HBox(10, firstNameField, lastNameField); HBox.setHgrow(firstNameField, Priority.ALWAYS); HBox.setHgrow(lastNameField, Priority.ALWAYS);
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone Number");
        ComboBox<String> locationComboBox = new ComboBox<>(FXCollections.observableArrayList(districts));
        locationComboBox.setPromptText("Select Location"); locationComboBox.setMaxWidth(Double.MAX_VALUE);
        PasswordField passwordField = new PasswordField(); passwordField.setPromptText("Create Password");
        TextField visiblePasswordField = new TextField(); visiblePasswordField.setPromptText("Create Password");
        visiblePasswordField.setManaged(false); visiblePasswordField.setVisible(false);
        StackPane passwordContainer = new StackPane(passwordField, visiblePasswordField);
        PasswordField confirmPassword = new PasswordField(); confirmPassword.setPromptText("Confirm Password");
        TextField visibleConfirmField = new TextField(); visibleConfirmField.setPromptText("Confirm Password");
        visibleConfirmField.setManaged(false); visibleConfirmField.setVisible(false);
        StackPane confirmContainer = new StackPane(confirmPassword, visibleConfirmField);
        CheckBox showPassword = new CheckBox("Show Password");
        Button choosePhotoButton = new Button("Choose Profile Photo");
        ImageView photoPreview = new ImageView();
        photoPreview.setFitHeight(50); photoPreview.setFitWidth(50); photoPreview.setPreserveRatio(true);
        Label photoPathLabel = new Label("");
        choosePhotoButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser(); fileChooser.setTitle("Select Profile Photo");
            fileChooser.getExtensionFilters().addAll( new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif") );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                try {
                    String imagePath = selectedFile.toURI().toURL().toString();
                    photoPathLabel.setText(imagePath);
                    photoPreview.setImage(new Image(imagePath, 50, 50, true, true));
                } catch (MalformedURLException ex) { System.err.println("Error creating URL for image: " + ex.getMessage()); }
            }
        });
        HBox photoBox = new HBox(10, choosePhotoButton, photoPreview); photoBox.setAlignment(Pos.CENTER_LEFT);
        visiblePasswordField.managedProperty().bind(showPassword.selectedProperty()); visiblePasswordField.visibleProperty().bind(showPassword.selectedProperty());
        passwordField.managedProperty().bind(showPassword.selectedProperty().not()); passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleConfirmField.managedProperty().bind(showPassword.selectedProperty()); visibleConfirmField.visibleProperty().bind(showPassword.selectedProperty());
        confirmPassword.managedProperty().bind(showPassword.selectedProperty().not()); confirmPassword.visibleProperty().bind(showPassword.selectedProperty().not());
        visibleConfirmField.textProperty().bindBidirectional(confirmPassword.textProperty());
        Label message = new Label();
        message.setTextFill(Color.RED); message.setPadding(new Insets(5, 0, 5, 0));
        Button signupBtn = new Button("Create Account");
        signupBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14)); signupBtn.setTextFill(Color.WHITE);
        signupBtn.setMaxWidth(Double.MAX_VALUE);
        signupBtn.setStyle("-fx-background-color: #0078D7; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
        signupBtn.setOnMouseEntered(e -> signupBtn.setStyle("-fx-background-color: #005A9E; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;"));
        signupBtn.setOnMouseExited(e -> signupBtn.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;"));

        signupBtn.setOnAction(e -> {
            String email = emailField.getText().trim(); String pass = passwordField.getText().trim();
            String first = firstNameField.getText().trim(); String last = lastNameField.getText().trim();
            String phone = phoneField.getText().trim(); String location = locationComboBox.getValue();
            String photoPath = photoPathLabel.getText();

            if (email.isEmpty() || pass.isEmpty() || confirmPassword.getText().isEmpty() || first.isEmpty() || last.isEmpty() || location == null) { message.setTextFill(Color.RED); message.setText("Please fill in all required fields."); }
            else if (!pass.equals(confirmPassword.getText())) { message.setTextFill(Color.RED); message.setText("Passwords do not match!"); }
            else if (users.containsKey(email)) { message.setTextFill(Color.RED); message.setText("This email is already registered."); }
            else {
                String role = ADMIN_EMAILS.contains(email) ? "ADMIN" : "USER";
                UserData newUser = new UserData(email, pass, first, last, phone, location, photoPath, role);
                saveUser(newUser, false);
                message.setTextFill(Color.GREEN); message.setText("Signup Successful! You can now log in.");
                firstNameField.clear(); lastNameField.clear(); emailField.clear(); phoneField.clear();
                passwordField.clear(); confirmPassword.clear(); locationComboBox.setValue(null);
                photoPathLabel.setText(""); photoPreview.setImage(null);
            }
        });

        VBox formContainer = new VBox(15,
                title, nameBox, emailField, phoneField, locationComboBox,
                passwordContainer, confirmContainer, showPassword, photoBox,
                signupBtn, message
        );
        formContainer.setAlignment(Pos.CENTER_LEFT);
        formContainer.setMaxWidth(350);
        formContainer.setPadding(new Insets(30));

        ScrollPane scrollPane = new ScrollPane(formContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: white;");
        rightPane.setCenter(scrollPane);

        return rightPane;
    }

    private HBox createLoginScreen(Stage stage) {
        HBox loginScreen = new HBox();
        Hyperlink toSignup = new Hyperlink("Sign Up");
        toSignup.setOnAction(e -> switchPane(signupPane, loginPane));
        HBox signupLinkBox = new HBox(5, new Label("Don't have an account?"), toSignup);
        signupLinkBox.setAlignment(Pos.CENTER);
        VBox leftPane = createLeftPane("Welcome to ProfitLense", "Analyze your profit");
        BorderPane rightPane = createLoginRightPane(stage, signupLinkBox);
        leftPane.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
        rightPane.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
        loginScreen.getChildren().addAll(leftPane, rightPane);
        return loginScreen;
    }

    private HBox createSignupScreen(Stage stage) {
        HBox signupScreen = new HBox();
        Hyperlink toLogin = new Hyperlink("Login");
        toLogin.setOnAction(e -> switchPane(loginPane, signupScreen));
        HBox loginLinkBox = new HBox(5, new Label("Already have an account?"), toLogin);
        loginLinkBox.setAlignment(Pos.CENTER);
        VBox leftPane = createLeftPane("Create your Account", "Get started with ProfitLense");
        BorderPane rightPane = createSignupRightPane(loginLinkBox);
        leftPane.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
        rightPane.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
        signupScreen.getChildren().addAll(leftPane, rightPane);
        return signupScreen;
    }

    private void saveUser(UserData userData, boolean isHeaderOnly) {
        try {
            userFile.getParentFile().mkdirs();
            boolean writeHeader = isHeaderOnly || !userFile.exists() || userFile.length() == 0;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile, !writeHeader))) { // Use !writeHeader to append
                if (writeHeader) {
                    writer.write(CSV_HEADER);
                    writer.newLine();
                }
                if (!isHeaderOnly && userData != null) {
                    writer.write(userData.toCsvString());
                    writer.newLine();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        if (!isHeaderOnly && userData != null && userData.getEmail() != null) {
            users.put(userData.getEmail(), userData);
        }
    }

    // --- (NEW) Helper method to rewrite the entire user CSV file ---
    // This is needed when changing a password or user role
    private void rewriteUserCsvFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile, false))) { // false = Overwrite
            writer.write(CSV_HEADER);
            writer.newLine();
            for (UserData user : users.values()) {
                writer.write(user.toCsvString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error rewriting user.csv file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void switchPane(Pane show, Pane hide) {
        if (!root.getChildren().contains(show)) root.getChildren().add(show);
        show.setOpacity(0); show.setScaleX(0.8); show.setScaleY(0.8);
        Timeline fadeIn = new Timeline( new KeyFrame(Duration.seconds(0.6), new KeyValue(show.opacityProperty(), 1, Interpolator.EASE_BOTH), new KeyValue(show.scaleXProperty(), 1, Interpolator.EASE_OUT), new KeyValue(show.scaleYProperty(), 1, Interpolator.EASE_OUT) ) );
        Timeline fadeOut = new Timeline( new KeyFrame(Duration.seconds(0.6), new KeyValue(hide.opacityProperty(), 0, Interpolator.EASE_BOTH), new KeyValue(hide.scaleXProperty(), 0.8, Interpolator.EASE_IN), new KeyValue(hide.scaleYProperty(), 0.8, Interpolator.EASE_IN) ) );
        fadeOut.setOnFinished(e -> root.getChildren().remove(hide));
        new ParallelTransition(fadeIn, fadeOut).play();
    }

    private void switchDashboardPage(Node pageToShow) {
        if (contentArea == null || contentArea.getChildren().contains(pageToShow)) return;
        Node currentPage = contentArea.getChildren().isEmpty() ? null : contentArea.getChildren().get(0);
        pageToShow.setOpacity(0.0); pageToShow.setScaleX(0.95); pageToShow.setScaleY(0.95);
        contentArea.getChildren().add(pageToShow);
        FadeTransition fadeOut = null;
        if (currentPage != null) { fadeOut = new FadeTransition(Duration.millis(300), currentPage); fadeOut.setToValue(0.0); fadeOut.setOnFinished(e -> contentArea.getChildren().remove(currentPage)); }
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), pageToShow); fadeIn.setToValue(1.0);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), pageToShow); scaleIn.setToX(1.0); scaleIn.setToY(1.0);
        ParallelTransition transitionIn = new ParallelTransition(fadeIn, scaleIn);
        if (fadeOut != null) { fadeOut.play(); fadeOut.setOnFinished(e -> { contentArea.getChildren().remove(currentPage); transitionIn.play(); }); }
        else { transitionIn.play(); }
    }

    private ImageView createSocialIcon(String imagePath) {
        ImageView imageView = null;
        try {
            InputStream stream = getClass().getResourceAsStream(imagePath);
            if (stream == null) { System.err.println("Warning: Icon not found at " + imagePath); return new ImageView(); }
            Image image = new Image(stream, 24, 24, true, true);
            imageView = new ImageView(image);
            imageView.setCursor(javafx.scene.Cursor.HAND);
        } catch (Exception e) { System.err.println("Error loading icon: " + imagePath); e.printStackTrace(); imageView = new ImageView(); }
        return imageView;
    }

    // --- (NEW) Forgot Password Dialog Logic ---
    private void showForgotPasswordDialog() {
        // 1. Ask for Email
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Forgot Password");
        emailDialog.setHeaderText("Password Reset");
        emailDialog.setContentText("Please enter your registered email address:");

        Optional<String> emailResult = emailDialog.showAndWait();

        if (emailResult.isEmpty() || emailResult.get().trim().isEmpty()) {
            return; // User cancelled
        }

        String email = emailResult.get().trim();

        // 2. Check if email exists
        if (!users.containsKey(email)) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("User Not Found");
            errorAlert.setContentText("No account is registered with that email address.");
            errorAlert.showAndWait();
            return;
        }

        // 3. Ask for New Password (Custom Dialog)
        Dialog<Pair<String, String>> passwordDialog = new Dialog<>();
        passwordDialog.setTitle("Create New Password");
        passwordDialog.setHeaderText("Enter your new password for " + email);

        ButtonType setPasswordButtonType = new ButtonType("Set Password", ButtonBar.ButtonData.OK_DONE);
        passwordDialog.getDialogPane().getButtonTypes().addAll(setPasswordButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("New Password");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm Password");

        grid.add(new Label("New Password:"), 0, 0);
        grid.add(newPassword, 1, 0);
        grid.add(new Label("Confirm Password:"), 0, 1);
        grid.add(confirmPassword, 1, 1);

        passwordDialog.getDialogPane().setContent(grid);

        // Convert the result to a pair of passwords when the button is clicked
        passwordDialog.setResultConverter(dialogButton -> {
            if (dialogButton == setPasswordButtonType) {
                return new Pair<>(newPassword.getText(), confirmPassword.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> passwordResult = passwordDialog.showAndWait();

        passwordResult.ifPresent(passwords -> {
            String newPass = passwords.getKey();
            String confirmPass = passwords.getValue();

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Password fields cannot be empty.");
                alert.showAndWait();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "The passwords do not match.");
                alert.showAndWait();
                return;
            }

            // 4. Success. Update the user and save.
            UserData user = users.get(email);
            user.setPassword(newPass); // Update in the map

            // 5. Rewrite the *entire* CSV file
            rewriteUserCsvFile();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Success");
            success.setHeaderText(null);
            success.setContentText("Password for " + email + " has been updated successfully.");
            success.showAndWait();
        });
    }


    // ---------------- DASHBOARD ----------------
    private void loadDashboard() {
        BorderPane dashboard = new BorderPane();
        dashboard.setStyle(String.format("-fx-background-color: linear-gradient(to right, %s, %s);", "#141E30", "#243B55"));
        dashboard.setOpacity(0.0);

        VBox sidebar = new VBox(20);
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 0 20 20 0;");
        sidebar.setPadding(new Insets(30));
        sidebar.setPrefWidth(200);

        Label title = new Label("ProfitLense");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);

        Button homeBtn = new Button("🏠 Home");
        Button graphBtn = new Button("📊 Graph");
        Button suggestionBtn = new Button("💡 Suggestion");
        Button profileBtn = new Button("👤 Profile");
        Button logoutBtn = new Button("🚪 Logout");
        Button adminBtn = new Button("⚙️ Admin Panel");

        String btnIdle = String.format("-fx-background-color: transparent; -fx-font-size: 15px; -fx-text-fill: %s; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", "#FFFFFF");
        String btnHover = String.format("-fx-background-color: %s; -fx-font-size: 15px; -fx-text-fill: %s; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", "#0078D7", "#FFFFFF");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        for (Button b : Arrays.asList(homeBtn, graphBtn, suggestionBtn, profileBtn, adminBtn, logoutBtn)) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle(btnIdle);
            b.setOnMouseEntered(e -> b.setStyle(btnHover));
            b.setOnMouseExited(e -> b.setStyle(btnIdle));
        }

        sidebar.getChildren().addAll(title, profileBtn);

        boolean isAdmin = (currentUser != null && "ADMIN".equals(currentUser.getRole()));

        if (isAdmin) {
            sidebar.getChildren().add(adminBtn);
        } else {
            sidebar.getChildren().addAll(homeBtn, graphBtn, suggestionBtn);
        }
        sidebar.getChildren().addAll(spacer, logoutBtn);

        dashboard.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setAlignment(Pos.CENTER);
        dashboard.setCenter(contentArea);

        root.getChildren().setAll(dashboard);

        VBox homePage = createHomePage();
        VBox graphPage = createGraphPage();
        suggestionPage = createSuggestionPage();
        profilePage = createProfilePage();
        adminPage = createAdminPage();

        homeBtn.setOnAction(e -> switchDashboardPage(homePage));
        graphBtn.setOnAction(e -> switchDashboardPage(graphPage));
        suggestionBtn.setOnAction(e -> switchDashboardPage(suggestionPage));

        profileBtn.setOnAction(e -> {
            updateProfilePage();
            switchDashboardPage(profilePage);
        });

        adminBtn.setOnAction(e -> {
            adminPage = createAdminPage(); // Re-create admin panel to refresh data
            switchDashboardPage(adminPage);
        });

        logoutBtn.setOnAction(e -> {
            currentUser = null;
            FadeTransition fadeOutDashboard = new FadeTransition(Duration.millis(500), dashboard);
            fadeOutDashboard.setToValue(0.0);
            fadeOutDashboard.setOnFinished(event -> {
                if (loginPane == null) loginPane = createLoginScreen(primaryStage);
                loginPane.setOpacity(0.0);
                root.getChildren().setAll(loginPane);
                FadeTransition fadeInLogin = new FadeTransition(Duration.millis(500), loginPane);
                fadeInLogin.setToValue(1.0);
                fadeInLogin.play();
            });
            fadeOutDashboard.play();
        });

        if (isAdmin) {
            contentArea.getChildren().add(adminPage);
        } else {
            contentArea.getChildren().add(homePage);
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), dashboard);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }


    // ---------------- HOME PAGE ----------------
    private VBox createHomePage() {
        VBox box = new VBox(15); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(20));
        Label header = new Label("Home - Select District");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 26)); header.setTextFill(Color.WHITE); header.setPadding(new Insets(0, 0, 10, 0));
        TextField search = new TextField(); search.setPromptText("Search District..."); search.setMaxWidth(300);
        search.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-prompt-text-fill: %s; -fx-background-radius: 5; -fx-border-radius: 5;", "#1C2A3E", "#FFFFFF", "#3A506B", "#B0C4DE"));
        TableView<DistrictData> table = new TableView<>(); table.setPrefWidth(300); table.setPrefHeight(300);
        table.setStyle( "-fx-background-color: transparent;" + "-fx-border-color: #3A506B;" + "-fx-border-radius: 5;" + "-fx-background-radius: 5;");
        table.widthProperty().addListener((obs, oldVal, newVal) -> { Pane headerPane = (Pane) table.lookup(".column-header-background"); if (headerPane != null) { headerPane.setMaxHeight(0); headerPane.setMinHeight(0); headerPane.setPrefHeight(0); headerPane.setVisible(false); headerPane.setManaged(false); } });
        TableColumn<DistrictData, String> distCol = new TableColumn<>("District");
        distCol.setCellValueFactory(new PropertyValueFactory<>("district"));
        table.getColumns().add(distCol); distCol.prefWidthProperty().bind(table.widthProperty().subtract(2));
        distCol.setCellFactory(column -> new TableCell<DistrictData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setText(null); setStyle(""); }
                else {
                    setText(item); setAlignment(Pos.CENTER); setFont(Font.font("Arial", FontWeight.BOLD, 16)); setTextFill(Color.web("#B0C4DE"));
                    String base = "-fx-background-color: transparent; -fx-border-color: #3A506B; -fx-border-width: 0.5 0 0 0;";
                    String hover = "-fx-background-color: #243B55; -fx-border-color: #3A506B; -fx-border-width: 0.5 0 0 0;";
                    String sel = "-fx-background-color: #0078D7; -fx-border-color: #3A506B; -fx-border-width: 0.5 0 0 0;";
                    setStyle(base);
                    selectedProperty().addListener((o, ov, nv) -> setStyle(nv ? sel : (isHover() ? hover : base)));
                    hoverProperty().addListener((o, ov, nv) -> { if (!isSelected()) setStyle(nv ? hover : base); });
                    selectedProperty().addListener((o, ov, nv) -> setTextFill(nv ? Color.WHITE : (isHover() ? Color.WHITE : Color.web("#B0C4DE"))));
                    hoverProperty().addListener((o, ov, nv) -> { if (!isSelected()) setTextFill(nv ? Color.WHITE : Color.web("#B0C4DE")); });
                }
            }
        });

        List<DistrictData> all = readDataFromCSV();
        if (all != null) {
            table.getItems().addAll(all);
            search.textProperty().addListener((o, ov, nv) -> {
                if (nv == null || nv.isEmpty()) { table.getItems().setAll(all); }
                else { table.getItems().setAll(all.stream().filter(d->d.getDistrict().toLowerCase().contains(nv.toLowerCase())).collect(Collectors.toList())); }
            });
        }

        TextField amountField = new TextField();
        amountField.setPromptText("Enter Investment Amount"); amountField.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-prompt-text-fill: %s; -fx-background-radius: 5; -fx-border-radius: 5;", "#1C2A3E", "#FFFFFF", "#3A506B", "#B0C4DE"));
        Button submitBtn = new Button("Calculate Profit");
        String idle = "-fx-background-color: #00C853; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;";
        String hov = "-fx-background-color: #00A946; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;";
        submitBtn.setStyle(idle); submitBtn.setOnMouseEntered(e -> submitBtn.setStyle(hov)); submitBtn.setOnMouseExited(e -> submitBtn.setStyle(idle));
        HBox inputBox = new HBox(10, amountField, submitBtn); inputBox.setAlignment(Pos.CENTER);
        submitBtn.setOnAction(e -> {
            DistrictData sel = table.getSelectionModel().getSelectedItem(); String invTxt = amountField.getText();
            if (sel == null) { Alert a = new Alert(Alert.AlertType.WARNING, "Select district!"); a.showAndWait(); return; }
            if (invTxt.isEmpty()) { Alert a = new Alert(Alert.AlertType.WARNING, "Enter investment!"); a.showAndWait(); return; }
            try {
                int inv = Integer.parseInt(invTxt); String dist = sel.getDistrict();
                Main_calc calc = new Main_calc(inv, dist); calc.load();
                if (!calc.valid()) { Alert a = new Alert(Alert.AlertType.ERROR, "No data for " + dist); a.showAndWait(); if (pieChartData != null) { pieChartData.clear(); profitPieChart.setTitle("Profit"); } return; }

                if (currentUser != null) {
                    calc.logCalculation(currentUser.getEmail());
                }

                String s1 = calc.source_1(); String s2 = calc.source_2(); String s3 = calc.source_3(); String sug = calc.suggestion();
                if (reportDistrictLabel != null) { reportDistrictLabel.setText("District: " + dist); reportInvestmentLabel.setText("Investment: " + inv); suggestionTextLabel.setText(sug); source1DetailLabel.setText(s1); source2DetailLabel.setText(s2); source3DetailLabel.setText(s3); }
                List<Double> profits = calc.getMaxProfCurr();
                if (profitPieChart != null && profits != null && profits.size() == 3) {
                    pieChartData.clear(); double p1=profits.get(0)>0?profits.get(0):0.0; double p2=profits.get(1)>0?profits.get(1):0.0; double p3=profits.get(2)>0?profits.get(2):0.0;
                    if(p1>0) pieChartData.add(new PieChart.Data("Source 1",p1)); if(p2>0) pieChartData.add(new PieChart.Data("Source 2",p2)); if(p3>0) pieChartData.add(new PieChart.Data("Source 3",p3));
                    profitPieChart.setTitle("Profit for " + dist);
                }
                switchDashboardPage(suggestionPage); amountField.clear();
            } catch (NumberFormatException ex) { Alert a = new Alert(Alert.AlertType.ERROR, "Invalid number!"); a.showAndWait(); }
            catch (Exception ex) { Alert a = new Alert(Alert.AlertType.ERROR, "Calc error: " + ex.getMessage()); a.showAndWait(); ex.printStackTrace(); }
        });
        box.getChildren().addAll(header, search, table, inputBox);
        return box;
    }

    // ---------------- GRAPH PAGE ----------------
    private VBox createGraphPage() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.TOP_CENTER); box.setPadding(new Insets(25));
        Label lbl = new Label("📊 Profitability Graph"); lbl.setFont(Font.font("Arial", FontWeight.BOLD, 26)); lbl.setTextFill(Color.WHITE); VBox.setMargin(lbl, new Insets(0, 0, 10, 0));
        pieChartData = FXCollections.observableArrayList(); profitPieChart = new PieChart(pieChartData);
        profitPieChart.setTitle("Profit Breakdown"); profitPieChart.setLegendVisible(true); profitPieChart.setLabelsVisible(true);
        profitPieChart.getStylesheets().add( "data:text/css," + ".chart-pie-label { -fx-fill: white; -fx-font-size: 13px; }" + ".chart-legend { -fx-background-color: rgba(0,0,0,0.3); }" + ".chart-legend-item { -fx-text-fill: white; }" + ".chart-title { -fx-text-fill: white; -fx-font-size: 18px; }" + ".chart-no-data { -fx-text-fill: #B0C4DE; -fx-font-size: 16px; }" );
        box.getChildren().addAll(lbl, profitPieChart); VBox.setVgrow(profitPieChart, Priority.ALWAYS);
        return box;
    }


    // ---------------- SUGGESTION PAGE ---------------
    private VBox createSuggestionPage() {
        VBox pageLayout = new VBox(20); pageLayout.setAlignment(Pos.TOP_CENTER); pageLayout.setPadding(new Insets(25));
        Label pageTitle = new Label("💡 Profitability Report"); pageTitle.setFont(Font.font("Arial", FontWeight.BOLD, 26)); pageTitle.setTextFill(Color.WHITE); VBox.setMargin(pageTitle, new Insets(0, 0, 10, 0));
        reportDistrictLabel = new Label("District: (Please run a calculation)"); reportInvestmentLabel = new Label("Investment: (Please run a calculation)");
        for (Label l : Arrays.asList(reportDistrictLabel, reportInvestmentLabel)) { l.setFont(Font.font("Arial", FontWeight.NORMAL, 16)); l.setTextFill(Color.web("#B0C4DE")); }
        HBox inputsBox = new HBox(40, reportDistrictLabel, reportInvestmentLabel); inputsBox.setAlignment(Pos.CENTER);
        VBox suggestionBox = new VBox(10); suggestionBox.setAlignment(Pos.CENTER_LEFT); suggestionBox.setPadding(new Insets(20));
        suggestionBox.setStyle(String.format( "-fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 10; -fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: 10;", "#0078D7" ));
        Label suggestionTitle = new Label("Final Suggestion"); suggestionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18)); suggestionTitle.setTextFill(Color.web("#00C853"));
        suggestionTextLabel = new Label("Run a calculation from the 'Home' page to see results."); suggestionTextLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 15)); suggestionTextLabel.setTextFill(Color.WHITE); suggestionTextLabel.setWrapText(true);
        suggestionBox.getChildren().addAll(suggestionTitle, suggestionTextLabel);
        Label detailsHeader = new Label("Calculation Details"); detailsHeader.setFont(Font.font("Arial", FontWeight.BOLD, 18)); detailsHeader.setTextFill(Color.WHITE); VBox.setMargin(detailsHeader, new Insets(15, 0, 5, 0));
        source1DetailLabel = createDetailLabel(); source2DetailLabel = createDetailLabel(); source3DetailLabel = createDetailLabel();
        VBox box1 = createFloatingBox("Source 1", source1DetailLabel); VBox box2 = createFloatingBox("Source 2", source2DetailLabel); VBox box3 = createFloatingBox("Source 3", source3DetailLabel);
        HBox topBoxes = new HBox(20, box1, box2); topBoxes.setAlignment(Pos.CENTER); HBox.setHgrow(box1, Priority.ALWAYS); HBox.setHgrow(box2, Priority.ALWAYS);
        VBox detailsLayout = new VBox(20, topBoxes, box3); detailsLayout.setAlignment(Pos.CENTER); box3.setMaxWidth(Double.MAX_VALUE);
        VBox scrollContent = new VBox(15, inputsBox, suggestionBox, detailsHeader, detailsLayout); scrollContent.setAlignment(Pos.TOP_CENTER); scrollContent.setStyle("-fx-background-color: transparent;"); scrollContent.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(scrollContent); scrollPane.setFitToWidth(true); scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pageLayout.getChildren().addAll(pageTitle, scrollPane); VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return pageLayout;
    }

    private VBox createFloatingBox(String title, Label contentLabel) {
        VBox floatingBox = new VBox(10); floatingBox.setPadding(new Insets(15));
        floatingBox.setStyle(String.format( "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 1;", "#1C2A3E", "#3A506B" ));
        floatingBox.setMinWidth(250); Label boxTitle = new Label(title);
        boxTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16)); boxTitle.setTextFill(Color.WHITE);
        floatingBox.getChildren().addAll(boxTitle, contentLabel);
        return floatingBox;
    }

    private Label createDetailLabel() {
        Label label = new Label("-"); label.setTextFill(Color.web("#B0C4DE")); label.setFont(Font.font("Monospaced", FontWeight.NORMAL, 13)); label.setWrapText(true);
        return label;
    }

    // --- Profile Page Creation ---
    private VBox createProfilePage() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        Label header = new Label("My Profile");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        header.setTextFill(Color.WHITE);

        profilePageImageView = new ImageView();
        profilePageImageView.setFitHeight(120); profilePageImageView.setFitWidth(120);
        profilePageImageView.setPreserveRatio(true);
        Circle clip = new Circle(60, 60, 60);
        profilePageImageView.setClip(clip);

        GridPane detailsGrid = new GridPane();
        detailsGrid.setAlignment(Pos.CENTER); detailsGrid.setHgap(15); detailsGrid.setVgap(10);
        detailsGrid.setMaxWidth(400);

        Label nameLabel = createProfileLabel("Name:"); Label emailLabel = createProfileLabel("Email:");
        Label phoneLabel = createProfileLabel("Phone:"); Label locationLabel = createProfileLabel("Location:");

        profilePageNameValue = createProfileValueLabel();
        profilePageEmailValue = createProfileValueLabel();
        profilePagePhoneValue = createProfileValueLabel();
        profilePageLocationValue = createProfileValueLabel();

        detailsGrid.add(nameLabel, 0, 0); detailsGrid.add(profilePageNameValue, 1, 0);
        detailsGrid.add(emailLabel, 0, 1); detailsGrid.add(profilePageEmailValue, 1, 1);
        detailsGrid.add(phoneLabel, 0, 2); detailsGrid.add(profilePagePhoneValue, 1, 2);
        detailsGrid.add(locationLabel, 0, 3); detailsGrid.add(profilePageLocationValue, 1, 3);

        box.getChildren().addAll(header, profilePageImageView, detailsGrid);
        return box;
    }

    private void updateProfilePage() {
        if (currentUser != null) {
            profilePageNameValue.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            profilePageEmailValue.setText(currentUser.getEmail());
            profilePagePhoneValue.setText(currentUser.getPhone() != null && !currentUser.getPhone().isEmpty() ? currentUser.getPhone() : "-");
            profilePageLocationValue.setText(currentUser.getLocation() != null && !currentUser.getLocation().isEmpty() ? currentUser.getLocation() : "-");

            String photoPath = currentUser.getPhotoPath();
            Image profileImage = null;
            if (photoPath != null && !photoPath.isEmpty()) {
                try { profileImage = new Image(photoPath, 120, 120, true, true); }
                catch (Exception e) { System.err.println("Error loading profile image: " + e.getMessage()); }
            }

            Image defaultImage = null;
            try {
                InputStream defaultStream = getClass().getResourceAsStream("/icons/default_profile.png");
                if(defaultStream != null) defaultImage = new Image(defaultStream, 120, 120, true, true);
                else System.err.println("Default profile image not found.");
            } catch (Exception e) { System.err.println("Error loading default profile image.");}

            if (profileImage != null && !profileImage.isError()) {
                profilePageImageView.setImage(profileImage);
            } else {
                profilePageImageView.setImage(defaultImage);
            }
        }
    }


    private Label createProfileLabel(String text) {
        Label label = new Label(text); label.setFont(Font.font("Arial", FontWeight.BOLD, 14)); label.setTextFill(Color.web("#B0C4DE")); GridPane.setHalignment(label, javafx.geometry.HPos.RIGHT); return label;
    }
    private Label createProfileValueLabel() {
        Label label = new Label("-"); label.setFont(Font.font("Arial", FontWeight.NORMAL, 14)); label.setTextFill(Color.WHITE); label.setWrapText(true); GridPane.setHalignment(label, javafx.geometry.HPos.LEFT); return label;
    }

    // --- (FIXED) Admin Page (Calls AdminPanel class) ---
    private AdminPanel createAdminPage() {
        // Pass all required arguments
        adminPage = new AdminPanel(users, userFile, districts, currentUser);
        return adminPage;
    }


    // ---------------- CSV DATA HANDLING ----------------
    private List<DistrictData> readDataFromCSV() {
        List<DistrictData> list = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/data.csv")) {
            if (is == null) {
                System.err.println("CRITICAL ERROR: data.csv not found in resources!");
                return list;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        String district = parts[0]; String location = parts[1];
                        int amount = parts[2].isEmpty() ? 0 : Integer.parseInt(parts[2]);
                        list.add(new DistrictData(district, location, amount));
                    } catch (NumberFormatException e) { System.err.println("Skipping bad row in data.csv: " + line); }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ---------------- DATA CLASS ----------------
    public static class DistrictData {
        private String district; private String location; private int amount;
        public DistrictData(String district, String location, int amount) { this.district = district; this.location = location; this.amount = amount; }
        public String getDistrict() { return district; }
        public String getLocation() { return location; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}