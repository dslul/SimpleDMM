package org.dslul.simpledmm;
	
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage stage) {
		try {
			stage.setTitle("Simple DMM");
			stage.setMinHeight(600);
			stage.setMinWidth(800);
			stage.sizeToScene();
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
			Scene scene = new Scene(root,800,600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			stage.setScene(scene);
			stage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		stage.setOnCloseRequest((e) -> System.exit(0));
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
