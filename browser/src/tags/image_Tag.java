package tags;

import htmlStructure.HtmlNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;

import java.io.File;

public class image_Tag extends HtmlNode
{
    private Image image;
    public image_Tag(String name)

    {
        super(name);
    }
    @Override
    public void  drawElement(GraphicsContext gc)
    {
        if(getImage("imag.png")==null)
                System.out.println("the image is null");
        else
            gc.drawImage(getImage("imag.png"),this.x,this.y+20);

    }

    private Image getImage(String path)
    {

        File file = new File(path);
        Image img=null;
        try {
            img = new Image("imag.png");

        } catch (Exception e) {

        }
        return img;
    }
}
