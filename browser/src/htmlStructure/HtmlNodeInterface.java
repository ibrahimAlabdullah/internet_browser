package htmlStructure;

import javafx.scene.canvas.GraphicsContext;

import java.util.List;
import java.util.Map;

public interface HtmlNodeInterface
{

    public String getName();
    public void setName(String name);

    public void setParent(HtmlNodeInterface parent);
    public HtmlNodeInterface getParent();

    public void setChildren(List<HtmlNodeInterface> children);
    public List<HtmlNodeInterface> getChildren();

    public void setStyleElements(Map<String, String> styleElements);
    public Map<String, String> getStyleElements();

    public void drawElement(GraphicsContext gc);

    public Map<String, String> getProps();
    public void setProps(Map<String, String> props);

    public void setX(double x);
    public void setY(double y);
    public double getX();
    public double getY();

    public void setTitle(String title);
}
