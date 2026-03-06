package com.anthem_traffic;

import com.anthem_traffic.view.TrafficDashboardController;
import com.anthem_traffic.presenter.ITrafficDashboardPresenter;
import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();

        TrafficDashboardController view = loader.getController();
        
        // --- THE DYNAMIC MOCK PRESENTER ---
        ITrafficDashboardPresenter stubPresenter = new ITrafficDashboardPresenter() {
            private Timeline mockDataTimer;
            private Random random = new Random();
            private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            @Override
            public void onStartStreamClicked() {
                System.out.println("Presenter: Starting Stream...");
                view.setStreamStatus(true);
                view.updateSystemStatus(true, true, true);
                
                // If a timer already exists, stop it first
                if (mockDataTimer != null) mockDataTimer.stop();

                // Create a loop that runs every 2 seconds to simulate live AI data
                mockDataTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                    // Generate fake random data
                    int cars = 30 + random.nextInt(25);
                    int trucks = 5 + random.nextInt(15);
                    int bikes = 10 + random.nextInt(20);
                    int total = cars + trucks + bikes;
                    
                    // Update numbers
                    view.updateVehicleCounts(cars, trucks, bikes);
                    view.updateTotalVehicleCount(total);
                    
                    // Update Pie Chart (calculate percentages)
                    view.updateCategoryDistribution(
                        (cars * 100.0) / total, 
                        (trucks * 100.0) / total, 
                        (bikes * 100.0) / total
                    );
                    
                    // Update Line Chart with current time
                    String now = LocalTime.now().format(timeFormatter);
                    view.updateTrafficTrend(now, cars, trucks, bikes);
                    
                    // Randomly change the alert status based on total volume
                    if (total > 75) {
                        view.showCongestionAlert("heavy");
                    } else if (total > 50) {
                        view.showCongestionAlert("moderate");
                    } else {
                        view.showCongestionAlert("smooth");
                    }
                }));
                
                mockDataTimer.setCycleCount(Timeline.INDEFINITE); // Run forever
                mockDataTimer.play(); // Start the loop!
            }

            @Override
            public void onStopStreamClicked() {
                System.out.println("Presenter: Stopping Stream...");
                if (mockDataTimer != null) mockDataTimer.stop();
                view.setStreamStatus(false);
                view.showCongestionAlert("smooth");
                view.updateVehicleCounts(0, 0, 0);
                view.updateTotalVehicleCount(0);
            }

            @Override
            public void onFetchHistoricalDataClicked() {
                System.out.println("Presenter: Fetching historical data...");
            }

            @Override
            public void onGenerateReportClicked() {
                System.out.println("Presenter: Generating report...");
            }
        };

        view.setPresenter(stubPresenter);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Anthem AI - Live Traffic Control Center");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}