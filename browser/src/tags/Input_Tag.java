package tags;

import guiController.SecondGuiController;
import htmlStructure.HtmlNode;
import javafx.scene.canvas.GraphicsContext;
import static guiController.SecondGuiController.drawingTextFilde;
import static guiController.SecondGuiController.hbx1;


public class Input_Tag extends HtmlNode
{
    public Input_Tag(String name)
    {
        super(name);
    }

    @Override
     public void drawElement(GraphicsContext gc)
     {
         hbx1=true;
        drawingTextFilde(this.getX(),this.getY());
     }

}
