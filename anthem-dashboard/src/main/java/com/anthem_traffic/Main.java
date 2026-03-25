package com.anthem_traffic;

import com.anthem_traffic.view.TrafficDashboardController;
import com.anthem_traffic.presenter.ITrafficDashboardPresenter;
import com.anthem_traffic.presenter.TrafficDashboardPresenter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();

        TrafficDashboardController view = loader.getController();

        // Create real presenter that connects to backend API
        ITrafficDashboardPresenter presenter = new TrafficDashboardPresenter(view);
        view.setPresenter(presenter);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Anthem AI - Live Traffic Control Center");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Cleanup on window close
        primaryStage.setOnCloseRequest(event -> {
            if (presenter instanceof TrafficDashboardPresenter) {
                ((TrafficDashboardPresenter) presenter).shutdown();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}