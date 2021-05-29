package guiController;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import main.Main;
import parserr.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import Connction.HttpConnection;
import javafx.scene.canvas.Canvas;
import javafx.stage.FileChooser;
import makeHtmlTree.MakeHtmlTree;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import nodes.Document;
import htmlStructure.HtmlNodeInterface;
import save.Save;

public class SecondGuiController extends StackPane
{

	private final Logger logger = Logger.getLogger(getClass().getName());
	@FXML
	private BorderPane borderPane;
	@FXML
	private TextField searchBar;
	@FXML
	private Button openFile;
	@FXML
	private VBox errorPane;
	@FXML
	private Button tryAgain;
	@FXML
	private Button go;
	@FXML
	private BorderPane myCanvas;

	@FXML
	private ScrollPane scroll;
	@FXML
	private Button save;
	private Button tryA;
	public static Button ok = new Button("Ok");
    public static TextField tf = new TextField();
    public static Stage stage = new Stage();
	public static String savePage = null;

	HtmlNodeInterface tree;
	private final Tab tab;
	private final FirstGuiController firstGuiController;


	public static HBox hbox1 = new HBox();
	public static HBox hbox2 = new HBox();
	public static boolean hbx1=false;
	public static boolean hbx2=false;


	public SecondGuiController(FirstGuiController firstGuiController, Tab tab) {
		this.firstGuiController = firstGuiController;
		this.tab = tab;
		this.tab.setContent(this);

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/SecondGui.fxml"));
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
		startFileHtmlTab();
	}


	private void startFileHtmlTab()
	{

		this.borderPane.setStyle("-fx-background-color:#111111;");

		openFile.setOnAction(Event->
		{
			    FileChooser filechooser = new FileChooser();
			    FileChooser.ExtensionFilter fileExtension = new FileChooser.ExtensionFilter("web pages","*.html");
			    filechooser.getExtensionFilters().add(fileExtension);
			    File selectedFile = filechooser.showOpenDialog(null);


				this.tree = parseHtmlPage(selectedFile.getPath());
				Canvas canvas = new Canvas(1335, 625);
				canvas = MakeHtmlTree.drowTree(this.tree, canvas, this.tab);
				this.myCanvas.getChildren().clear();
				this.myCanvas.getChildren().add(canvas);

				if(hbx1)
				{
					//this.myCanvas.getChildren().add(hbox1);
					//this.firstGuiController.getChildren().add(hbox1);
					this.getChildren().add(hbox1);
				}
                if(hbx2)
                	this.getChildren().add(hbox2);

			    this.myCanvas.applyCss();
				this.borderPane.setCenter(myCanvas);
				//this.borderPane.setStyle("-fx-background-color:cyan;");
		});


		go.setOnAction(Event->
		{

			String path = searchBar.getText();

			if(path.equals(""))
			{
				System.out.println("the path is null");
			}
			else if(path.contains("http"))
			{
				// checkForInternetConnection();
				String url = searchBar.getText();//"https://www.whatsapp.com/";
		         Document doc=null;
		         try
		         {
			        doc=HttpConnection.connect(url).get();
			        savePage = doc.toString();
		          } catch (IOException e) {
			     e.printStackTrace();
		          }
		            System.out.println(doc);
		         Canvas canvas = new Canvas(1335,625);
		         canvas.getGraphicsContext2D().fillText(doc.toString(),0,0);
		         this.myCanvas.getChildren().clear();
		         this.myCanvas.getChildren().add(canvas);
		         scroll.setContent(canvas);
		         scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

			}
			else
			{

				this.borderPane.getChildren().remove(this.myCanvas);
				this.myCanvas.getChildren().clear();
			    this.tree = parseHtmlPage(path);
			    Canvas canvas = new Canvas(1335, 625);
			    canvas = MakeHtmlTree.drowTree(this.tree, canvas, this.tab);
			    this.myCanvas.getChildren().clear();
			    this.myCanvas.getChildren().add(canvas);
			    if(hbx1)
				    this.getChildren().add(hbox1);
			    if(hbx2)
			    	this.getChildren().add(hbox2);

			this.myCanvas.applyCss();
			this.borderPane.setCenter(myCanvas);
			//this.borderPane.setStyle("-fx-background-color:cyan;");
			}


		});

		save.setOnAction(Event->
		{
			HBox hbox = new HBox();
			Label label = new Label("Enter Name : ");
			hbox.getChildren().addAll(label,tf,ok);
			Scene scene = new Scene(hbox,275,35);
			stage.setScene(scene);
			stage.show();

		});

		ok.setOnAction(Event->
		{
			File saveFile= new File(tf.getText()+".html");
			Save saveObj = new Save(savePage);

			FileOutputStream f;
			ObjectOutputStream o ;
			try
			{
				f = new FileOutputStream(saveFile);
				o = new ObjectOutputStream(f);
				o.writeObject(saveObj);
				o.close();
				f.close();
			} catch (IOException ex) {
				System.out.println("!Error ....... ");
			}
			stage.close();
		});


	}
	private HtmlNodeInterface parseHtmlPage(String pageName)
	{
		MakeHtmlTree.yLine = 20;
		HtmlNodeInterface tree = null;
		try {
			File page = new File(pageName);
			Document doc = HttpConnection.parseInputStream(new FileInputStream(page), "UTF-8", page.getAbsolutePath(), Parser.htmlParser());;//load(page, "UTF-8", page.getAbsolutePath());
			tree = MakeHtmlTree.generateHtmlTree(doc);

		}catch (Exception e){
			e.printStackTrace();
		}
		return tree;
	}


	public static void  drawingTextFilde(double x,double y)
	{
		TextField tf = new TextField("Enter your text");
		tf.setTranslateX(x);
		tf.setTranslateY(y);
		hbox1.getChildren().add(tf);
	}


	public static void  drawingComboBox(double x,double y)
	{

		ComboBox comboBox = new ComboBox();
		comboBox.getItems().add("first");
		comboBox.getItems().add("second");
		comboBox.getItems().add("third");

		comboBox.getSelectionModel().select(0);

		comboBox.setTranslateX(x);
		comboBox.setTranslateY(y);
		comboBox.setPrefWidth(200);
		hbox2.getChildren().add(comboBox);
	}


	public Tab getTab()
	{
		return tab;
	}


	public VBox getErrorPane()
	{
		return errorPane;
	}


/*
	private String getHostName(String urlInput) {
		try {
			URL url = new URL(urlInput);
			return url.getProtocol() + "://" + url.getHost() + "/";
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
*/
/*
	public String getSearchEngineHomeUrl(String searchProvider) {
		//Find
		switch (searchProvider.toLowerCase()) {
			/*case "bing":
				return "http://www.bing.com";
			case "duckduckgo":
				return "https://duckduckgo.com";
			case "yahoo":
				return "https://search.yahoo.com";
			default: //then google
				return "https://www.google.com";
		}
	}


	public String getSelectedEngineHomeUrl() {
		return getSearchEngineHomeUrl(searchEngineComboBox.getSelectionModel().getSelectedItem());
	}
	*/
/*
	public void loadWebSite(boolean runModem, String webSite) {
		
		//Check null or empty
		//		if (webSite == null || webSite.isEmpty())
		//			return;

		if(!runModem){
			//Search if it is a valid WebSite url
			String load = !new UrlValidator().isValid(webSite) ? null : webSite;

			//Load
			try {

				//First Part
				String finalWebsiteFristPart = ( load != null ) ? load : getSelectedEngineHomeUrl();

				//Second Part
				String finalWebsiteSecondPart = "";
				if (searchBar.getText().isEmpty())
					finalWebsiteSecondPart = "";
				else {
					switch (searchEngineComboBox.getSelectionModel().getSelectedItem().toLowerCase()) {
						/*case "bing":
							finalWebsiteSecondPart = "//?q=" + URLEncoder.encode(searchBar.getText(), "UTF-8");
							break;
						case "duckduckgo":
							finalWebsiteSecondPart = "//?q=" + URLEncoder.encode(searchBar.getText(), "UTF-8");
							break;
						case "yahoo":
							finalWebsiteSecondPart = "//?q=" + URLEncoder.encode(searchBar.getText(), "UTF-8");
							break;
						default: //then google
							finalWebsiteSecondPart = "//search?q=" + URLEncoder.encode(searchBar.getText(), "UTF-8");
							break;
					}

				}

				//Load it
				webEngine.load(finalWebsiteFristPart + finalWebsiteSecondPart);
			} catch (UnsupportedEncodingException ex) {
				ex.printStackTrace();
			}
		}
		else{
			System.out.println("public void loadWebSite(boolean runModem, String webSite)");
		}

		
	}
	
*/
/*
	public void loadDefaultWebSite() {
		webEngine.load(getSelectedEngineHomeUrl());
	}
*/
/*
	public void reloadWebSite(boolean runModem) {

		if(!runModem){
			if (!getHistory().getEntries().isEmpty())
				webEngine.reload();
			else
				loadDefaultWebSite();
		}
		else
			{
			String fileName = searchBar.getText() ;
			loadHtmlFile(fileName, true);
		}

	}
*/
/*
	private void loadHtmlFile(String fileName, boolean isNew)
	{
		if(fileName.trim().isEmpty())
			return;

		if(isNew) {
			if(!fileName.endsWith("html"))
				fileName += ".html";
			this.tree = parseHtmlPage(this.constPathPage + fileName);
			pagesName.add(this.constPathPage + fileName);
		}
		else{
			this.tree = parseHtmlPage(fileName);
		}
		System.out.println("public void reloadWebSite(boolean runModem)");

		this.borderPane.getChildren().remove(this.webView);
		this.borderPane.getChildren().remove(this.myCanvas);

		Canvas canvas = new Canvas(1335,625);
		canvas= MakeHtmlTree.drowTree(this.tree, canvas, this.tab);
		this.myCanvas.getChildren().clear();
		this.myCanvas.getChildren().add(canvas);
		this.myCanvas.applyCss();
		this.borderPane.setCenter(myCanvas);
		this.borderPane.applyCss();
	}
*/
/*
	public void goBack(boolean runMode) {
		if(!runMode){
			getHistory().go(historyEntryList.size() > 1 && getHistory().getCurrentIndex() > 0 ? -1 : 0);
		}
		else{
			currentPage = currentPage -1;
			if(currentPage < 0)
				currentPage = 0;

			loadHtmlFile(pagesName.get(currentPage), false);
			System.out.println("public void goBack(boolean runMode)");

			String res = pagesName.get(currentPage).substring(constPathPage.length(), pagesName.get(currentPage).length());
			System.out.println("::::::::::::::::::::::");
			System.out.println(res);
			searchBar.setText(res);
		}
	}
*/
/*
	public void goForward(boolean runMode) {
		if(!runMode){
			getHistory().go(historyEntryList.size() > 1 && getHistory().getCurrentIndex() < historyEntryList.size() - 1 ? 1 : 0);
		}
		else{
			currentPage = currentPage +1;
			if(currentPage > pagesName.size() -1)
				currentPage = pagesName.size() -1;

			loadHtmlFile(pagesName.get(currentPage), false);
			System.out.println("public void goForward(boolean runMode)");
			String res = pagesName.get(currentPage).substring(constPathPage.length(), pagesName.get(currentPage).length());
			System.out.println("::::::::::::::::::::::");
			System.out.println(res);
			searchBar.setText(res);
		}
	}
*/




	void checkForInternetConnection() {
		
		//tryAgainIndicator
		//tryAgainIndicator.setVisible(true);
		
		//Check for internet connection
		Thread thread = new Thread(() -> {
			boolean hasInternet = isReachableByPing("www.google.com");
			Platform.runLater(() -> {
				
				//Visibility of error pane
				errorPane.setVisible(!hasInternet);
				
				//Visibility of Try Again Indicator
				//tryAgainIndicator.setVisible(false);
				
				//Reload the website if it has internet
				//if (hasInternet)
					//reloadWebSite(this.runMode);
			});
		}, "Internet Connection Tester Thread");
		thread.setDaemon(true);
		thread.start();
	}

	public static boolean isReachableByPing(String host)
	{
		try
		{
			// Start a new Process
			Process process = Runtime.getRuntime().exec("ping -" +
					( System.getProperty("os.name").toLowerCase().startsWith("windows") ? "n" : "c" ) + " 1 " + host);
			//Wait for it to finish
			process.waitFor();
			//Check the return value
			return process.exitValue() == 0;
		}
		catch (Exception ex)
		{
			Logger.getLogger(Main.class.getName()).log(Level.INFO, null, ex);
			return false;
		}
	}
/*
	public WebHistory getHistory() {
		return history;
	}
	

	public void setHistory(WebHistory history) {
		this.history = history;
	}
	

	/*public void setMovingTitleEnabled(boolean value) {
		movingTitleAnimation.setSelected(value);
	}*/

	/*
	///////////////////////////// INNER CLASSES ////////////////////////////////
	public class FavIconProvider implements ChangeListener<State> {
		
		@Override
		public void changed(ObservableValue<? extends State> observable , State oldState , State newState) {
			if (newState == Worker.State.SUCCEEDED) {
				try {
					if ("about:blank".equals(webEngine.getLocation()))
						return;
					
					//Determine the full url
					String favIconFullURL = getHostName(webEngine.getLocation()) + "favicon.ico";
					//System.out.println(favIconFullURL)
					
					//Create HttpURLConnection 
					HttpURLConnection httpcon = (HttpURLConnection) new URL(favIconFullURL).openConnection();
					httpcon.addRequestProperty("User-Agent", "Mozilla/5.0");
					List<BufferedImage> image = ICODecoder.read(httpcon.getInputStream());
					
					//Set the favicon
					facIconImageView.setImage(SwingFXUtils.toFXImage(image.get(0), null));
					
				} catch (Exception ex) {
					//ex.printStackTrace()
					facIconImageView.setImage(null);
				}
			}
		}
	}// FavIconProvider
	*/
	/*
	public class DownloadDetector implements ChangeListener<State> {
		
		@Override
		public void changed(ObservableValue<? extends State> observable , State oldState , State newState) {
			if (newState == Worker.State.CANCELLED) {
				// download detected
				String url = webEngine.getLocation();
				logger.info("download url: " + url);
				//             try{
				//                 Download download = new Download(webEngine.getLocation());
				//                 Thread t = new Thread(download);
				//                 t.start();
				//             }catch(Exception ex){
				//                 logger.log(Level.SEVERE, "download", ex);
				//             }
			}
		}
	}// DownloadDetector

	*/

}
