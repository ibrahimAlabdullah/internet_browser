package tags;

import htmlStructure.HtmlNode;
import javafx.scene.canvas.GraphicsContext;
import static guiController.SecondGuiController.hbx2;
import static guiController.SecondGuiController.drawingComboBox;


public class Select_Tag extends HtmlNode {
    public Select_Tag(String name){
        super(name);

    }
    @Override
    public void drawElement(GraphicsContext gc)
    {
        hbx2=true;
        drawingComboBox(this.getX(),this.getY());
    }
}
