package main;

import Connction.HttpConnection;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import guiController.FirstGuiController;
import nodes.Document;

import java.io.IOException;

public class Main extends Application
{
	@Override
	public void start(Stage primaryStage)
	{
		BorderPane root = new BorderPane();
		root.setCenter(new FirstGuiController());
		Scene scene = new Scene(root, 1200, 700);
		primaryStage.setTitle("I Browser");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	public static void main(String[] args) { launch(args); }
}
