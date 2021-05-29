package tags;

import htmlStructure.HtmlNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import makeHtmlTree.MakeHtmlTree;

public class H1_Tag extends HtmlNode
{

    public H1_Tag(String name){
        super(name);
        ySpace = 45;
    }

    @Override
    public void drawElement(GraphicsContext gc)
    {

        if(this.getProps().containsKey("text")) {
            Font fontObj = getFontObj();
            drawColorValue(gc);
            gc.setFont(Font.font(fontObj.getFamily(),fontObj.getSize()));


            double textWidth = getStringWidth(getProps().get("text"), fontObj);
            double canvasWidth = gc.getCanvas().getWidth();

            if(textWidth > canvasWidth){
                String [] words = getProps().get("text").split(" ");
                String newLine = "";
                String line = "";
                MakeHtmlTree.yLine = addSpace(MakeHtmlTree.yLine, fontObj.getSize());
                for (String word : words)
                {
                    newLine += " " + word;
                    double newLineWidth = getStringWidth(newLine, fontObj);
                    if(newLineWidth < canvasWidth)
                    {
                        line = newLine;
                        continue;
                    }
                    MakeHtmlTree.yLine = setCurrentY(MakeHtmlTree.yLine, fontObj.getSize());
                    drawAlignment(canvasWidth, getStringWidth(line, fontObj));
                    gc.fillText(line, this.x, this.y);
                    newLine = word;
                    line = "";
                }
                if(! newLine.trim().isEmpty()){
                    MakeHtmlTree.yLine = setCurrentY(MakeHtmlTree.yLine, fontObj.getSize());
                    drawAlignment(canvasWidth, getStringWidth(line, fontObj));
                    gc.fillText(line, this.x, this.y);
                }
                MakeHtmlTree.yLine = addSpace(MakeHtmlTree.yLine, fontObj.getSize());
            }
            if(textWidth == canvasWidth){
                this.x = 0;
            }
            if(textWidth < canvasWidth){
                MakeHtmlTree.yLine = addSpace(MakeHtmlTree.yLine, fontObj.getSize());
                MakeHtmlTree.yLine = setCurrentY(MakeHtmlTree.yLine, fontObj.getSize());
                drawAlignment(canvasWidth, textWidth);
                gc.fillText(getProps().get("text"), this.x, this.y);
                MakeHtmlTree.yLine = addSpace(MakeHtmlTree.yLine, fontObj.getSize());
            }
        }

    }
}
