package com.profitlense;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class AdminPanel extends VBox {

    // --- Style Constants ---
    private final String adminBaseColor = "#2F3E4E";
    private final String contentColor = "#394A5D";
    private final String tableHeaderColor = "#4A5F7A";
    private final String borderColor = "#556A86";
    private final String textColor = "#FFFFFF";
    private final String textMutedColor = "#CFD8DC";
    private final String greenButton = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;";
    private final String blueButton = "-fx-background-color: #0078D7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;";
    private final String orangeButton = "-fx-background-color: #fd7e14; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;";
    private final String redButton = "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;";
    private final String grayButton = "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;";

    private final String CSV_HEADER = "email,password,firstName,lastName,phone,location,photoPath,role";
    private Map<String, UserData> usersMap;
    private UserData adminUser;
    private File userFile;
    private ObservableList<UserData> usersObservableList;
    private TableView<UserData> userTable;

    // --- For Settings ---
    private Properties appSettings = new Properties();
    private final File settingsFile = new File("src/main/resources/settings.properties");
    private CheckBox enableRegistrationCheck;
    private CheckBox maintenanceModeCheck;
    private TextField defaultPasswordField;

    // --- For Activity Log ---
    private ObservableList<ActivityLogger.UserActivity> activityLogList;

    // --- For Data Management ---
    private final File dataCalcFile = new File("src/main/resources/data_calc.csv");
    private TableView<CsvDataRow> dataCalcTable;
    private ObservableList<CsvDataRow> dataCalcList = FXCollections.observableArrayList();
    private List<String> dataCalcHeaders = new ArrayList<>();
    private Label dataCalcLastModifiedLabel;

    private String[] districts;

    public AdminPanel(Map<String, UserData> users, File userFile, String[] districts, UserData adminUser) {
        this.usersMap = users;
        this.userFile = userFile;
        this.districts = districts;
        this.adminUser = adminUser;

        if (users != null) {
            this.usersObservableList = FXCollections.observableArrayList(users.values());
        } else {
            System.err.println("AdminPanel Error: The user map provided was null. Initializing empty list.");
            this.usersObservableList = FXCollections.observableArrayList();
            this.usersMap = new HashMap<>();
        }

        this.activityLogList = FXCollections.observableArrayList();
        loadSettings();
        loadActivityLog();
        loadDataCalcCsv();

        this.setPadding(new Insets(20));
        this.setSpacing(20);
        this.setAlignment(Pos.TOP_CENTER);
        this.setStyle(String.format("-fx-background-color: %s;", adminBaseColor));

        Label header = new Label("⚙️ Admin Panel");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        header.setTextFill(Color.WHITE);

        TabPane tabPane = new TabPane();
        Tab dashboardTab = new Tab("Dashboard", createAdminDashboard());
        Tab userManagementTab = new Tab("User Management", createUsersPanel());
        Tab dataManagementTab = new Tab("Data Management", createDataPanel());
        Tab activityLogTab = new Tab("Activity Log", createActivityLogPanel());
        Tab settingsTab = new Tab("Software Settings", createSettingsPanel());

        dashboardTab.setClosable(false);
        userManagementTab.setClosable(false);
        dataManagementTab.setClosable(false);
        activityLogTab.setClosable(false);
        settingsTab.setClosable(false);

        tabPane.getTabs().addAll(dashboardTab, userManagementTab, dataManagementTab, activityLogTab, settingsTab);

        // --- (FIXED) Updated TabPane Styling to remove white background ---
        // We add .tab-content-area styling to force transparency/dark color
        String tabPaneStyle = String.format(
                "-fx-background-color: %s;" +
                        "-fx-control-inner-background: %s;" +
                        "-fx-tab-min-width: 120px;" +
                        "-fx-tab-max-height: 40px;" +
                        "-fx-tab-min-height: 40px;",
                adminBaseColor, adminBaseColor
        );

        // This inline stylesheet is the most reliable way to target the inner content area
        tabPane.getStylesheets().add("data:text/css," +
                ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: " + adminBaseColor + "; } " +
                ".tab-pane .tab { -fx-background-color: " + contentColor + "; -fx-background-insets: 0; } " +
                ".tab-pane .tab:selected { -fx-background-color: " + adminBaseColor + "; } " +
                ".tab-pane .tab-label { -fx-text-fill: " + textMutedColor + "; } " +
                ".tab-pane .tab:selected .tab-label { -fx-text-fill: " + textColor + "; } " +
                ".tab-pane .tab-content-area { -fx-background-color: " + adminBaseColor + "; -fx-padding: 20; }" // This fixes the white box
        );

        tabPane.setStyle(tabPaneStyle);
        // --- (END OF FIX) ---

        tabPane.getSelectionModel().select(dashboardTab);

        this.getChildren().addAll(header, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
    }

    private void loadSettings() {
        if (!settingsFile.exists()) {
            appSettings.setProperty("enable.registration", "true");
            appSettings.setProperty("default.reset.password", "password123");
            appSettings.setProperty("maintenance.mode", "false");
            saveSettings(false);
        } else {
            try (InputStream input = new FileInputStream(settingsFile)) {
                appSettings.load(input);
            } catch (IOException e) { System.err.println("Error loading settings.properties: " + e.getMessage()); }
        }
    }

    private void saveSettings(boolean showAlert) {
        try (OutputStream output = new FileOutputStream(settingsFile)) {
            appSettings.setProperty("enable.registration", String.valueOf(enableRegistrationCheck.isSelected()));
            appSettings.setProperty("maintenance.mode", String.valueOf(maintenanceModeCheck.isSelected()));
            appSettings.setProperty("default.reset.password", defaultPasswordField.getText());
            appSettings.store(output, "ProfitLense Application Settings");

            ActivityLogger.log(adminUser.getEmail(), "Settings Change", "Admin updated software settings.");

            if (showAlert) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings Saved", ButtonType.OK);
                alert.setHeaderText(null);
                alert.setContentText("Your settings have been saved successfully.");
                alert.showAndWait();
            }
        } catch (IOException e) {
            System.err.println("Error saving settings.properties: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving settings: " + e.getMessage(), ButtonType.OK);
            alert.setHeaderText("Could not save settings.");
            alert.showAndWait();
        }
    }

    private void loadActivityLog() {
        activityLogList.setAll(ActivityLogger.readActivityLog());
    }

    private Node createAdminDashboard() {
        VBox dashboardContainer = new VBox(20);
        dashboardContainer.setPadding(new Insets(20));
        dashboardContainer.setAlignment(Pos.TOP_LEFT);
        dashboardContainer.setStyle("-fx-background-color: transparent;");

        HBox statCardsLayout = new HBox(20);
        statCardsLayout.setAlignment(Pos.CENTER_LEFT);

        long totalUsers = usersMap.size();
        long adminUsers = usersMap.values().stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        long regularUsers = totalUsers - adminUsers;

        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        long calcsLast24h = activityLogList.stream()
                .filter(log -> log.getAction().equals("Calculation Run") && log.getTimestampObject().isAfter(yesterday))
                .count();

        statCardsLayout.getChildren().addAll(
                createStatCard("Total Users", String.valueOf(totalUsers)),
                createStatCard("Admins", String.valueOf(adminUsers)),
                createStatCard("Regular Users", String.valueOf(regularUsers)),
                createStatCard("Calculations (24h)", String.valueOf(calcsLast24h))
        );

        Label locationHeader = new Label("User Locations (Top 5)");
        locationHeader.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        locationHeader.setTextFill(Color.WHITE);

        VBox locationBox = new VBox(10);
        locationBox.setPadding(new Insets(15));
        locationBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 1;", contentColor, borderColor));

        Map<String, Long> locationCounts = usersMap.values().stream()
                .filter(u -> u.getLocation() != null && !u.getLocation().isEmpty())
                .collect(Collectors.groupingBy(UserData::getLocation, Collectors.counting()));

        if (locationCounts.isEmpty()) {
            locationBox.getChildren().add(createMutedLabel("No location data available from users."));
        } else {
            locationCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> locationBox.getChildren().add(createMutedLabel(entry.getKey() + ": " + entry.getValue() + " user(s)")));
        }

        Label activityHeader = new Label("Recent Activity (Top 5)");
        activityHeader.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        activityHeader.setTextFill(Color.WHITE);
        VBox activityBox = new VBox(10);
        activityBox.setPadding(new Insets(15));
        activityBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 1;", contentColor, borderColor));

        if (activityLogList.isEmpty()) {
            activityBox.getChildren().add(createMutedLabel("No activity logged yet."));
        } else {
            activityLogList.stream().limit(5).forEach(activity -> {
                String logText = String.format("[%s] %s: %s", activity.getTimestamp(), activity.getEmail(), activity.getAction());
                activityBox.getChildren().add(createMutedLabel(logText));
            });
        }

        dashboardContainer.getChildren().addAll(statCardsLayout, locationHeader, locationBox, activityHeader, activityBox);
        return dashboardContainer;
    }

    private VBox createStatCard(String title, String value) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(180);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 1;", contentColor, borderColor));
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        titleLabel.setTextFill(Color.web(textMutedColor));
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(textColor));
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private Node createUsersPanel() {
        VBox userPanel = new VBox(15);
        userPanel.setPadding(new Insets(10));
        userPanel.setStyle("-fx-background-color: transparent;");

        Button createUserButton = new Button("Create New User");
        createUserButton.setStyle(greenButton);
        createUserButton.setOnAction(e -> showCreateUserDialog());
        HBox topBar = new HBox(createUserButton);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(0, 0, 5, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or email...");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-prompt-text-fill: %s; -fx-background-radius: 5; -fx-border-radius: 5;", contentColor, textColor, borderColor, textMutedColor));

        userTable = new TableView<>();
        FilteredList<UserData> filteredData = new FilteredList<>(usersObservableList, p -> true);
        userTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                if (user.getEmail() != null && user.getEmail().toLowerCase().contains(filter)) return true;
                if (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(filter)) return true;
                return user.getLastName() != null && user.getLastName().toLowerCase().contains(filter);
            });
        });

        TableColumn<UserData, String> emailCol = createColumn("Email", "email", 200);
        TableColumn<UserData, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName()));
        nameCol.setPrefWidth(150);
        styleColumn(nameCol);
        TableColumn<UserData, String> roleCol = createColumn("Role", "role", 80);
        TableColumn<UserData, Void> actionsCol = createActionsColumn();

        userTable.getColumns().addAll(emailCol, nameCol, roleCol, actionsCol);
        userTable.setStyle(String.format(
                "-fx-background-color: %s;" +
                        "-fx-control-inner-background: %s;" +
                        "-fx-border-color: %s;" +
                        "-fx-table-cell-border-color: %s;",
                adminBaseColor, contentColor, borderColor, borderColor
        ));
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        userPanel.getChildren().addAll(topBar, searchField, userTable);
        return userPanel;
    }

    private <T> void styleColumn(TableColumn<UserData, T> col) {
        col.setStyle(String.format("-fx-font-weight: bold; -fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 0 1 1 0;", tableHeaderColor, textColor, borderColor));
        col.setCellFactory(column -> new TableCell<UserData, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 1 1 0;", contentColor, borderColor));
                } else {
                    setText(item.toString());
                    setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-alignment: CENTER_LEFT; -fx-border-color: %s; -fx-border-width: 0 1 1 0;",
                            contentColor, textMutedColor, borderColor));
                }
            }
        });
    }

    private <T> TableColumn<UserData, T> createColumn(String title, String property, double width) {
        TableColumn<UserData, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        styleColumn(col);
        return col;
    }

    private TableColumn<UserData, Void> createActionsColumn() {
        TableColumn<UserData, Void> col = new TableColumn<>("Actions");
        col.setPrefWidth(300);

        col.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button roleButton = new Button();
            private final Button resetPassButton = new Button("Reset Pass");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttonBox = new HBox(5, viewButton, editButton, roleButton, resetPassButton, deleteButton);
            {
                viewButton.setStyle(blueButton); editButton.setStyle(grayButton); deleteButton.setStyle(redButton); resetPassButton.setStyle(orangeButton);
                buttonBox.setAlignment(Pos.CENTER);
                viewButton.setOnAction(e -> { UserData user = (UserData) getTableRow().getItem(); if (user != null) showUserProfile(user); });
                editButton.setOnAction(e -> { UserData user = (UserData) getTableRow().getItem(); if (user != null) showEditUserDialog(user); });
                roleButton.setOnAction(e -> { UserData user = (UserData) getTableRow().getItem(); if (user != null) { user.setRole("ADMIN".equals(user.getRole()) ? "USER" : "ADMIN"); ActivityLogger.log(adminUser.getEmail(), "Role Change", user.getEmail() + " set to " + user.getRole()); rewriteUserCsvFile(); userTable.refresh(); } });
                resetPassButton.setOnAction(e -> { UserData user = (UserData) getTableRow().getItem(); if (user != null) resetUserPassword(user); });
                deleteButton.setOnAction(e -> { UserData user = (UserData) getTableRow().getItem(); if (user != null) deleteUser(user); });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(String.format("-fx-background-color: %s; -fx-alignment: CENTER; -fx-border-color: %s; -fx-border-width: 0 1 1 0;", contentColor, borderColor));
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); }
                else { UserData user = (UserData) getTableRow().getItem(); if ("ADMIN".equals(user.getRole())) { roleButton.setText("Demote"); roleButton.setStyle(orangeButton); } else { roleButton.setText("Promote"); roleButton.setStyle(greenButton); } setGraphic(buttonBox); }
            }
        });
        return col;
    }

    private void deleteUser(UserData user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete user: " + user.getEmail() + "?");
        alert.setContentText("This action is permanent and cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            usersMap.remove(user.getEmail());
            usersObservableList.remove(user);
            rewriteUserCsvFile();
            ActivityLogger.log(adminUser.getEmail(), "User Deleted", "Email: " + user.getEmail());
            loadActivityLog();
        }
    }

    private void resetUserPassword(UserData user) {
        String newPass = appSettings.getProperty("default.reset.password", "password123");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Password");
        alert.setHeaderText("Reset password for " + user.getEmail() + "?");
        alert.setContentText("This will set their password to '" + newPass + "'.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            user.setPassword(newPass);
            rewriteUserCsvFile();
            ActivityLogger.log(adminUser.getEmail(), "Password Reset", "User: " + user.getEmail());
            loadActivityLog();
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Success"); info.setHeaderText(null);
            info.setContentText(user.getEmail() + "'s password has been reset to '" + newPass + "'.");
            info.showAndWait();
        }
    }

    private void showUserProfile(UserData user) {
        Stage profileStage = new Stage(); profileStage.initModality(Modality.APPLICATION_MODAL); profileStage.setTitle("User Profile - " + user.getFirstName());
        VBox box = new VBox(20); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(30));
        box.setStyle(String.format("-fx-background-color: %s;", adminBaseColor));
        ImageView profileImageView = new ImageView();
        profileImageView.setFitHeight(100); profileImageView.setFitWidth(100); profileImageView.setPreserveRatio(true);
        Circle clip = new Circle(50, 50, 50); profileImageView.setClip(clip);
        String photoPath = user.getPhotoPath();
        Image profileImage = null;
        if (photoPath != null && !photoPath.isEmpty()) { try { profileImage = new Image(photoPath, 100, 100, true, true); } catch (Exception e) { System.err.println("Error loading profile image: " + e.getMessage()); } }
        Image defaultImage = null;
        try { InputStream defaultStream = getClass().getResourceAsStream("/icons/default_profile.png"); if(defaultStream != null) defaultImage = new Image(defaultStream, 100, 100, true, true); } catch (Exception e) { System.err.println("Error loading default profile image.");}
        if (profileImage != null && !profileImage.isError()) { profileImageView.setImage(profileImage); } else { profileImageView.setImage(defaultImage); }
        GridPane detailsGrid = new GridPane();
        detailsGrid.setAlignment(Pos.CENTER); detailsGrid.setHgap(15); detailsGrid.setVgap(10);
        detailsGrid.add(createProfileLabel("Name:"), 0, 0); detailsGrid.add(createProfileValueLabel(user.getFirstName() + " " + user.getLastName()), 1, 0);
        detailsGrid.add(createProfileLabel("Email:"), 0, 1); detailsGrid.add(createProfileValueLabel(user.getEmail()), 1, 1);
        detailsGrid.add(createProfileLabel("Phone:"), 0, 2); detailsGrid.add(createProfileValueLabel(user.getPhone()), 1, 2);
        detailsGrid.add(createProfileLabel("Location:"), 0, 3); detailsGrid.add(createProfileValueLabel(user.getLocation()), 1, 3);
        detailsGrid.add(createProfileLabel("Role:"), 0, 4); detailsGrid.add(createProfileValueLabel(user.getRole()), 1, 4);
        box.getChildren().addAll(profileImageView, detailsGrid);
        Scene scene = new Scene(box, 400, 450); profileStage.setScene(scene); profileStage.showAndWait();
    }

    private void showEditUserDialog(UserData user) {
        Stage editStage = new Stage(); editStage.initModality(Modality.APPLICATION_MODAL); editStage.setTitle("Edit User");
        GridPane grid = new GridPane(); grid.setVgap(10); grid.setHgap(10); grid.setPadding(new Insets(20));
        TextField firstNameField = new TextField(user.getFirstName());
        TextField lastNameField = new TextField(user.getLastName());
        TextField phoneField = new TextField(user.getPhone());
        ComboBox<String> locationBox = new ComboBox<>(FXCollections.observableArrayList(districts));
        locationBox.setValue(user.getLocation()); locationBox.setPrefWidth(200);
        grid.add(new Label("First Name:"), 0, 0); grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2); grid.add(phoneField, 1, 2);
        grid.add(new Label("Location:"), 0, 3); grid.add(locationBox, 1, 3);
        Button saveButton = new Button("Save Changes"); saveButton.setStyle(greenButton);
        saveButton.setOnAction(e -> {
            user.setFirstName(firstNameField.getText()); user.setLastName(lastNameField.getText());
            user.setPhone(phoneField.getText()); user.setLocation(locationBox.getValue());
            rewriteUserCsvFile(); userTable.refresh();
            ActivityLogger.log(adminUser.getEmail(), "User Edited", "Email: " + user.getEmail());
            loadActivityLog();
            editStage.close();
        });
        VBox layout = new VBox(20, grid, saveButton); layout.setAlignment(Pos.CENTER); layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout, 350, 250); editStage.setScene(scene); editStage.showAndWait();
    }

    private void showCreateUserDialog() {
        Stage createStage = new Stage(); createStage.initModality(Modality.APPLICATION_MODAL); createStage.setTitle("Create New User");
        GridPane grid = new GridPane(); grid.setVgap(10); grid.setHgap(10); grid.setPadding(new Insets(20));
        TextField emailField = new TextField();
        PasswordField passField = new PasswordField();
        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField phoneField = new TextField();
        ComboBox<String> locationBox = new ComboBox<>(FXCollections.observableArrayList(districts));
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("USER", "ADMIN"));
        roleBox.setValue("USER");
        grid.add(new Label("Email:"), 0, 0); grid.add(emailField, 1, 0);
        grid.add(new Label("Password:"), 0, 1); grid.add(passField, 1, 1);
        grid.add(new Label("First Name:"), 0, 2); grid.add(firstNameField, 1, 2);
        grid.add(new Label("Last Name:"), 0, 3); grid.add(lastNameField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4); grid.add(phoneField, 1, 4);
        grid.add(new Label("Location:"), 0, 5); grid.add(locationBox, 1, 5);
        grid.add(new Label("Role:"), 0, 6); grid.add(roleBox, 1, 6);
        Button createButton = new Button("Create User"); createButton.setStyle(greenButton);
        createButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty() || passField.getText().isEmpty() || firstNameField.getText().isEmpty()) { Alert a = new Alert(Alert.AlertType.ERROR, "Email, Password, and First Name are required."); a.showAndWait(); return; }
            if (usersMap.containsKey(email)) { Alert a = new Alert(Alert.AlertType.ERROR, "A user with this email already exists."); a.showAndWait(); return; }
            UserData newUser = new UserData( email, passField.getText(), firstNameField.getText(), lastNameField.getText(), phoneField.getText(), locationBox.getValue(), "", roleBox.getValue() );
            usersMap.put(email, newUser); usersObservableList.add(newUser);
            rewriteUserCsvFile();
            ActivityLogger.log(adminUser.getEmail(), "User Created", "Email: " + newUser.getEmail());
            loadActivityLog();
            createStage.close();
        });
        VBox layout = new VBox(20, grid, createButton); layout.setAlignment(Pos.CENTER); layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout, 350, 400); createStage.setScene(scene); createStage.showAndWait();
    }


    private Label createProfileLabel(String text) {
        Label label = new Label(text); label.setFont(Font.font("Arial", FontWeight.BOLD, 14)); label.setTextFill(Color.web(textMutedColor)); GridPane.setHalignment(label, javafx.geometry.HPos.RIGHT); return label;
    }
    private Label createProfileValueLabel(String text) {
        Label label = new Label(text); label.setFont(Font.font("Arial", FontWeight.NORMAL, 14)); label.setTextFill(Color.WHITE); label.setWrapText(true); GridPane.setHalignment(label, javafx.geometry.HPos.LEFT); return label;
    }

    // --- TAB 3: Software Settings ---
    private Node createSettingsPanel() {
        VBox settingsPanel = new VBox(20);
        settingsPanel.setPadding(new Insets(20));
        settingsPanel.setAlignment(Pos.TOP_LEFT);
        settingsPanel.setStyle(String.format("-fx-background-color: %s;", adminBaseColor));

        Label header = new Label("Software Settings");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        header.setTextFill(Color.WHITE);

        enableRegistrationCheck = new CheckBox("Enable New User Registration");
        enableRegistrationCheck.setSelected(Boolean.parseBoolean(appSettings.getProperty("enable.registration", "true")));
        enableRegistrationCheck.setFont(Font.font("Arial", 14));
        enableRegistrationCheck.setTextFill(Color.web(textMutedColor)); // <-- FIX

        maintenanceModeCheck = new CheckBox("Enable Maintenance Mode");
        maintenanceModeCheck.setSelected(Boolean.parseBoolean(appSettings.getProperty("maintenance.mode", "false")));
        maintenanceModeCheck.setFont(Font.font("Arial", 14));
        maintenanceModeCheck.setTextFill(Color.web(textMutedColor)); // <-- FIX
        Label maintenanceLabel = new Label("When enabled, only Admins can log in.");
        maintenanceLabel.setFont(Font.font("Arial", 12));
        maintenanceLabel.setTextFill(Color.web(textMutedColor)); // <-- FIX
        maintenanceLabel.setPadding(new Insets(0, 0, 0, 25));
        VBox maintenanceBox = new VBox(5, maintenanceModeCheck, maintenanceLabel);

        Label passLabel = new Label("Default Reset Password:");
        passLabel.setFont(Font.font("Arial", 14));
        passLabel.setTextFill(Color.web(textMutedColor)); // <-- FIX
        defaultPasswordField = new TextField(appSettings.getProperty("default.reset.password", "password123"));
        defaultPasswordField.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-prompt-text-fill: %s;",
                contentColor, textColor, borderColor, textMutedColor));
        HBox passBox = new HBox(10, passLabel, defaultPasswordField);
        passBox.setAlignment(Pos.CENTER_LEFT);

        Button saveChanges = new Button("Save Settings");
        saveChanges.setStyle(greenButton);
        saveChanges.setOnAction(e -> saveSettings(true));

        settingsPanel.getChildren().addAll(header, enableRegistrationCheck, maintenanceBox, passBox, saveChanges);
        return settingsPanel;
    }

    // --- TAB 4: Data Management ---
    private Node createDataPanel() {
        VBox dataPanel = new VBox(20);
        dataPanel.setPadding(new Insets(20));
        dataPanel.setAlignment(Pos.TOP_LEFT);
        dataPanel.setStyle(String.format("-fx-background-color: %s;", adminBaseColor));

        Label header = new Label("Calculation Data Management");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        header.setTextFill(Color.WHITE);

        Label infoLabel = new Label("Manage the `data_calc.csv` file. Double-click any cell to edit.");
        infoLabel.setFont(Font.font("Arial", 14));
        infoLabel.setTextFill(Color.web(textMutedColor)); // <-- FIX

        HBox fileInfoBox = new HBox(10);
        fileInfoBox.setAlignment(Pos.CENTER_LEFT);
        Label fileLabel = new Label("Current File:");
        fileLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        fileLabel.setTextFill(Color.web(textColor)); // <-- FIX

        dataCalcLastModifiedLabel = new Label(getFileTimestamp(dataCalcFile));
        dataCalcLastModifiedLabel.setFont(Font.font("Arial", 14));
        dataCalcLastModifiedLabel.setTextFill(Color.web(textMutedColor)); // <-- FIX
        fileInfoBox.getChildren().addAll(fileLabel, dataCalcLastModifiedLabel);

        Button uploadButton = new Button("Upload New `data_calc.csv`");
        uploadButton.setStyle(blueButton);
        uploadButton.setOnAction(e -> uploadNewDataFile());

        Button saveButton = new Button("Save Changes to CSV");
        saveButton.setStyle(greenButton);
        saveButton.setOnAction(e -> saveDataCalcCsv());

        HBox buttonBar = new HBox(10, uploadButton, saveButton);

        dataCalcTable = new TableView<>(dataCalcList);
        dataCalcTable.setEditable(true);

        if (!dataCalcHeaders.isEmpty()) {
            for (int i = 0; i < dataCalcHeaders.size(); i++) {
                final int colIndex = i;
                TableColumn<CsvDataRow, String> column = new TableColumn<>(dataCalcHeaders.get(i));

                column.setCellValueFactory(param -> {
                    if (param.getValue() != null && colIndex < param.getValue().getProperties().size()) {
                        return param.getValue().getProperty(colIndex);
                    } else {
                        return new SimpleStringProperty("");
                    }
                });

                column.setCellFactory(TextFieldTableCell.forTableColumn());
                column.setOnEditCommit(event -> {
                    CsvDataRow row = event.getRowValue();
                    row.setData(colIndex, event.getNewValue());
                });

                styleDataCalcColumn(column); // Style the column
                dataCalcTable.getColumns().add(column);
            }
        }
        dataCalcTable.setStyle(String.format(
                "-fx-background-color: %s;" +
                        "-fx-control-inner-background: %s;" +
                        "-fx-border-color: %s;" +
                        "-fx-table-cell-border-color: %s;",
                adminBaseColor, contentColor, borderColor, borderColor
        ));
        VBox.setVgrow(dataCalcTable, Priority.ALWAYS);

        dataPanel.getChildren().addAll(header, infoLabel, fileInfoBox, buttonBar, dataCalcTable);
        return dataPanel;
    }

    private String getFileTimestamp(File file) {
        if (file.exists()) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
            return "`data_calc.csv` (Last modified: " + sdf.format(file.lastModified()) + ")";
        }
        return "`data_calc.csv` (File not found)";
    }

    private void uploadNewDataFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select new data_calc.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(this.getScene().getWindow());

        if (selectedFile != null) {
            try {
                java.nio.file.Path targetPath = Paths.get(dataCalcFile.toURI());
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                dataCalcLastModifiedLabel.setText(getFileTimestamp(dataCalcFile));
                loadDataCalcCsv(); // <-- Reload data into table

                ActivityLogger.log(adminUser.getEmail(), "Data Upload", "Uploaded new data_calc.csv");
                loadActivityLog();

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Data file updated successfully.", ButtonType.OK);
                alert.setHeaderText("Upload Complete");
                alert.showAndWait();

            } catch (Exception e) {
                System.err.println("Error uploading new data file: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not upload file: " + e.getMessage(), ButtonType.OK);
                alert.setHeaderText("Upload Failed");
                alert.showAndWait();
            }
        }
    }


    // --- TAB 5: Activity Log ---
    private Node createActivityLogPanel() {
        VBox activityPanel = new VBox(15);
        activityPanel.setPadding(new Insets(10));
        activityPanel.setStyle(String.format("-fx-background-color: %s;", adminBaseColor)); // <-- FIX
        activityPanel.setAlignment(Pos.TOP_CENTER); // <-- FIX

        Label header = new Label("User Activity Log");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        header.setTextFill(Color.WHITE);

        Button refreshButton = new Button("Refresh Log");
        refreshButton.setStyle(blueButton);
        refreshButton.setOnAction(e -> loadActivityLog());

        Button clearButton = new Button("Clear Log");
        clearButton.setStyle(redButton);
        clearButton.setOnAction(e -> clearActivityLog());

        HBox headerBox = new HBox(10, refreshButton, clearButton);
        headerBox.setAlignment(Pos.CENTER); // <-- FIX

        VBox topControls = new VBox(10, header, headerBox); // <-- FIX
        topControls.setAlignment(Pos.CENTER); // <-- FIX

        TableView<ActivityLogger.UserActivity> activityTable = new TableView<>(activityLogList);

        TableColumn<ActivityLogger.UserActivity, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timestampCol.setPrefWidth(150);
        styleActivityColumn(timestampCol);

        TableColumn<ActivityLogger.UserActivity, String> emailCol = new TableColumn<>("User Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(200);
        styleActivityColumn(emailCol);

        TableColumn<ActivityLogger.UserActivity, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionCol.setPrefWidth(120);
        styleActivityColumn(actionCol);

        TableColumn<ActivityLogger.UserActivity, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));
        detailsCol.setPrefWidth(300);
        styleActivityColumn(detailsCol);

        activityTable.getColumns().addAll(timestampCol, emailCol, actionCol, detailsCol);
        activityTable.setStyle(String.format(
                "-fx-background-color: %s;" +
                        "-fx-control-inner-background: %s;" +
                        "-fx-border-color: %s;" +
                        "-fx-table-cell-border-color: %s;",
                adminBaseColor, contentColor, borderColor, borderColor
        ));
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(activityTable, Priority.ALWAYS);

        activityPanel.getChildren().addAll(topControls, activityTable); // <-- FIX
        return activityPanel;
    }

    private void clearActivityLog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Clear Log");
        alert.setHeaderText("Clear all activity?");
        alert.setContentText("This action is permanent and cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ActivityLogger.log(adminUser.getEmail(), "Log Cleared", "Cleared activity_log.csv");
            ActivityLogger.clearLog();
            loadActivityLog();
        }
    }

    private <T> void styleActivityColumn(TableColumn<ActivityLogger.UserActivity, T> col) {
        col.setStyle(String.format("-fx-font-weight: bold; -fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 0 1 1 0;", tableHeaderColor, textColor, borderColor));
        col.setCellFactory(column -> new TableCell<ActivityLogger.UserActivity, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 1 1 0;", contentColor, borderColor));
                } else {
                    setText(item.toString());
                    setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-alignment: CENTER_LEFT; -fx-border-color: %s; -fx-border-width: 0 1 1 0;",
                            contentColor, textMutedColor, borderColor));
                }
            }
        });
    }

    private void rewriteUserCsvFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile, false))) {
            writer.write(CSV_HEADER);
            writer.newLine();
            for (UserData user : usersMap.values()) {
                writer.write(user.toCsvString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error rewriting user.csv file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- (FIXED) This helper method was missing ---
    private Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.web(textMutedColor)); // Use Color.web()
        return label;
    }

    // --- (FIXED) Helper method for loading CSV data ---
    private void loadDataCalcCsv() {
        dataCalcList.clear();
        dataCalcHeaders.clear();
        if (!dataCalcFile.exists()) {
            System.err.println("CRITICAL ERROR: data_calc.csv not found in resources!");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(dataCalcFile))) {
            String line;
            // Read Header
            if ((line = br.readLine()) != null) {
                dataCalcHeaders.addAll(Arrays.asList(line.split(",")));
            }
            // Read Data
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                dataCalcList.add(new CsvDataRow(line.split(",")));
            }
        } catch (IOException e) {
            System.err.println("Error reading data_calc.csv: " + e.getMessage());
        }
    }

    // --- (FIXED) Helper method to save CSV data ---
    private void saveDataCalcCsv() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataCalcFile, false))) {
            // Write header
            writer.write(String.join(",", dataCalcHeaders));
            writer.newLine();
            // Write data rows
            for (CsvDataRow row : dataCalcList) {
                writer.write(row.toCsvString());
                writer.newLine();
            }
            // Log and alert
            ActivityLogger.log(adminUser.getEmail(), "Data Edit", "Admin saved changes to data_calc.csv");
            loadActivityLog(); // Refresh logs
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Changes to data_calc.csv saved.", ButtonType.OK);
            alert.setHeaderText("Save Successful");
            alert.showAndWait();
        } catch (IOException e) {
            System.err.println("Error saving data_calc.csv: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not save file: " + e.getMessage(), ButtonType.OK);
            alert.setHeaderText("Save Failed");
            alert.showAndWait();
        }
    }

    // --- (FIXED) Helper class for the CSV TableView ---
    public static class CsvDataRow {
        private final List<StringProperty> properties = new ArrayList<>();

        public CsvDataRow(String[] values) {
            for (String value : values) {
                properties.add(new SimpleStringProperty(value));
            }
        }

        public StringProperty getProperty(int index) {
            if (index >= 0 && index < properties.size()) {
                return properties.get(index);
            }
            return new SimpleStringProperty(""); // Return empty property if out of bounds
        }

        public List<StringProperty> getProperties() {
            return properties;
        }

        public void setData(int index, String value) {
            if (index >= 0 && index < properties.size()) {
                properties.get(index).set(value);
            }
        }

        public String toCsvString() {
            // Collect all property values into a list of strings
            return properties.stream()
                    .map(StringProperty::get)
                    .collect(Collectors.joining(","));
        }
    }

    // --- (FIXED) Helper for CsvDataRow Table ---
    private <T> void styleDataCalcColumn(TableColumn<CsvDataRow, T> col) {
        col.setStyle(String.format("-fx-font-weight: bold; -fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 0 1 1 0;", tableHeaderColor, textColor, borderColor));

        col.setCellFactory(new Callback<TableColumn<CsvDataRow, T>, TableCell<CsvDataRow, T>>() {
            @Override
            public TableCell<CsvDataRow, T> call(TableColumn<CsvDataRow, T> param) {
                // Create a TextFieldTableCell
                TableCell<CsvDataRow, T> cell = (TableCell<CsvDataRow, T>) (Object) new TextFieldTableCell<CsvDataRow, String>();

                cell.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 0 1 1 0;",
                        contentColor, textMutedColor, borderColor));

                cell.editingProperty().addListener((obs, wasEditing, isEditing) -> {
                    if (isEditing) {
                        cell.setTextFill(Color.BLACK);
                    } else {
                        cell.setTextFill(Color.web(textMutedColor));
                    }
                });

                cell.setAlignment(Pos.CENTER_LEFT);
                return cell;
            }
        });
    }
}