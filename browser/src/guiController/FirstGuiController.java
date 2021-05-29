package guiController;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

public class FirstGuiController extends StackPane
{

	private final Logger logger = Logger.getLogger(getClass().getName());

	@FXML
	private TabPane tabPane;
	@FXML
	private Button addFileTab;

	public FirstGuiController()
	{
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/FirstGui.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		
		try {
			loader.load();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "", ex);
		}
	}
	

	@FXML
	private void initialize()
	{
		tabPane.getTabs().clear();

		addNewFileTab();
		addFileTab.setOnAction(Event->
		{
			addNewFileTab();
		});
	}

	public void addNewFileTab()
	{
		SecondGuiController webBrowserTab = createNewTab();

		tabPane.getTabs().add(webBrowserTab.getTab());

		//return webBrowserTab;
	}


	public SecondGuiController createNewTab()
	{
		Tab tab = new Tab("New Tab");
		tab.setStyle("-fx-background-color:red;");
		SecondGuiController webBrowserTab = new SecondGuiController(this, tab);
		tab.setOnClosed(c ->
		{
			if (tabPane.getTabs().isEmpty())
				addNewFileTab();
		});

		return webBrowserTab;
	}

	public TabPane getTabPane() {
		return tabPane;
	}
}
