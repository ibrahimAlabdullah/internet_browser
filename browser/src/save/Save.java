package save;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Save extends StackPane implements Serializable
{

    String page;

    public Save(String page)
    {
        this.page = page;
    }
}
