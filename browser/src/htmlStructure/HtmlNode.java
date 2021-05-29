package htmlStructure;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlNode implements HtmlNodeInterface
{

    private String name;
    private HtmlNodeInterface parent;
    private List<HtmlNodeInterface> children  = new ArrayList<>();
    private Map<String, String> styleElements = new HashMap<>();
    private Map<String, String> props = new HashMap<>();

    protected static double x = 0;
    protected static double y = 0;

    protected double ySpace = 15;

    public HtmlNode(){}

    public HtmlNode(String name){
        this.name = name;
    }

    protected void drawColorValue(GraphicsContext gc)
    {
        if(this.getStyleElements().containsKey("color")){
            gc.setFill(getColorByName(getStyleElements().get("color")));
        }else{
            gc.setFill(Color.BLACK);
        }
    }

    protected double setCurrentY(double currentY, double fontSize){
        if(this.ySpace < fontSize)
            this.ySpace = fontSize;

        currentY += this.ySpace;
        this.y = currentY;

        return currentY;
    }

    protected void drawAlignment(double canvasWidth, double stringWidth){

        double canvasCenter = canvasWidth / 2.0;
        double stringCenter = stringWidth / 2.0;
        if(this.getStyleElements().containsKey("text-align")){
            String align = getStyleElements().get("text-align");
            if(align.equalsIgnoreCase("left")){
                this.x = 0;
            }
            if(align.equalsIgnoreCase("center")){

                this.x = (canvasCenter - stringCenter < 0 ) ? 0 : canvasCenter - stringCenter;
            }
            if(align.equalsIgnoreCase("right")){
                x = (canvasWidth - stringWidth < 0) ? 0 : canvasWidth - stringWidth;
            }
        }
    }

    protected Font getFontObj(){

        if(getStyleElements().containsKey("font-family") && getStyleElements().containsKey("font-size")) {
            String size = getStyleElements().get("font-size").replaceAll("px","");

            return Font.font(getStyleElements().get("font-family"), Double.parseDouble(size));
        }

        if(getStyleElements().containsKey("font-family") && !getStyleElements().containsKey("font-size"))
            return Font.font(getStyleElements().get("font-family"));


        if(getStyleElements().containsKey("font-size") && !getStyleElements().containsKey("font-family")) {
            String size = getStyleElements().get("font-size").replaceAll("px","");
            return Font.font(Double.parseDouble(size));
        }

        return Font.font("Arial", ySpace - 10);

    }

    protected double addSpace(double currentY, double fontSize){
        currentY += fontSize /2;
        return currentY;
    }

    protected double getStringWidth(String text, Font font){

        Text textObj = new Text(text);
        textObj.setFont(font);
        return textObj.getLayoutBounds().getWidth();
    }

    protected Color getColorByName(String color)
    {
        if(color != null && !color.trim().isEmpty()){
            if(color.equalsIgnoreCase("red"))
                return Color.RED;

            if(color.equalsIgnoreCase("black"))
                return Color.BLACK;

            if(color.equalsIgnoreCase("blue"))
                return Color.BLUE;

            if(color.equalsIgnoreCase("pink"))
                return Color.PINK;

            if(color.equalsIgnoreCase("green"))
                return Color.GREEN;
        }
        return null;
    }

    @Override
    public void drawElement(GraphicsContext gc) {

    }

    //setter and getter
    @Override
    public Map<String, String> getStyleElements() {
        return styleElements;
    }

    @Override
    public void setStyleElements(Map<String, String> styleElements) {
        this.styleElements = styleElements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<HtmlNodeInterface> getChildren() {
        return children;
    }

    public void setChildren(List<HtmlNodeInterface> children) {
        this.children = children;
    }

    public HtmlNodeInterface getParent() {
        return parent;
    }

    public void setParent(HtmlNodeInterface parent) {
        this.parent = parent;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void setProps(Map<String, String> props) {
        this.props = props;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    @Override
    public void setTitle(String title) {

    }
}
