package makeHtmlTree;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tab;
import htmlStructure.HtmlNodeInterface;
import tags.Head_Tag;
import tags.Title_Tag;
import htmlStructure.HtmlNode;

import nodes.Attribute;
import nodes.Document;
import nodes.Element;

import java.util.*;

public class MakeHtmlTree
{

    private static final String STYLE = "style";
    private static final String CLASS = "class";
    private static Map<String, Object> htmlPageClasses = new HashMap<>() ;

    public static double yLine =10;

    public static Canvas drowTree(HtmlNodeInterface tree, Canvas canvas, Tab currentTab)
    {
        HtmlNodeInterface pointer = tree;

        pointer.drawElement(canvas.getGraphicsContext2D());

        for (HtmlNodeInterface child: pointer.getChildren())
        {
            canvas = drowTree(child, canvas, currentTab);
        }
        return canvas;
    }


    public static HtmlNodeInterface generateHtmlTree(Document document)
    {
        HtmlNodeInterface htmlRoot = new HtmlNode("html");
        Element body = document.body();
        Element head = document.head();

        HtmlNodeInterface headTag = new Head_Tag("head");

        headTag.setParent(htmlRoot);

        htmlRoot.getChildren().add(headTag);

        for (Element docEle : head.children())
        {
            if(docEle.tagName() != null && !docEle.tagName().trim().isEmpty())
            {
                if(docEle.tagName().equalsIgnoreCase("style"))
                    parseClassesWithinStyleTag(docEle.html());

                if(docEle.tagName().equalsIgnoreCase("title"))
                {
                    HtmlNodeInterface titleTag = new Title_Tag("title");
                    titleTag.setParent(headTag);
                    headTag.getChildren().add(titleTag);
                    setHtmlProperties(titleTag, docEle);
                    if(titleTag.getProps().containsKey("text"))
                    {
                        htmlRoot.setTitle(titleTag.getProps().get("text"));
                    }
                }
            }
        }
        generation(htmlRoot, body);

        return htmlRoot;
    }

    public static void generation(HtmlNodeInterface htmlParentElement, Element currentElement)
    {
        HtmlNodeInterface newHtmlElement = MakeNodeTags.createElement(currentElement.tagName());
        setHtmlProperties(newHtmlElement, currentElement);
        setHtmlStyleElements(newHtmlElement, currentElement);
        newHtmlElement.setParent(htmlParentElement);
        htmlParentElement.getChildren().add(newHtmlElement);
        for (Element subElement : currentElement.children())
        {
            if(subElement.tagName() != null && !subElement.tagName().trim().isEmpty())
            {
                generation(newHtmlElement, subElement);
            }
        }
    }

    public static void setHtmlProperties(HtmlNodeInterface htmlElement, Element element)
    {
        if(elementHasAttribute(element))
        {
            Iterator<Attribute> attrIterator = element.attributes().iterator();
            while(attrIterator.hasNext())
            {
                Attribute attr = attrIterator.next();
                if(!checkStyleProperty(attr.getKey()))
                    htmlElement.getProps().put(attr.getKey(), attr.getValue());
            }
        }
        if(element.children().size() == 0 && element.hasText())
        {
            String text = element.text();
            htmlElement.getProps().put("text", text);
        }
    }

    public static void setHtmlStyleElements(HtmlNodeInterface htmlElement, Element element)
    {
        if(elementHasAttribute(element))
        {
            if(element.attributes().hasKey(CLASS))
            {
                setClassProperty(htmlElement, element.attributes().get(CLASS));
            }
            if(element.attributes().hasKey(STYLE))
            {
                setStyleProperties(htmlElement, element.attributes().get(STYLE));
            }
        }
    }

    private static void setClassProperty(HtmlNodeInterface htmlElement, String propertyName)
    {
        String [] props = propertyName.split(" ");
        List<Map<String,String>> classesList = new ArrayList<>();
        for (int i=0; i<props.length; i++)
        {
            classesList.add((Map<String, String>) htmlPageClasses.get(props[i]));
        }
        // add class property to style map
        for (Map<String, String> classItem : classesList)
        {
            htmlElement.getStyleElements().putAll(classItem);
        }
    }

    private static void setStyleProperties(HtmlNodeInterface htmlElement, String propertyName)
    {
        htmlElement.getStyleElements().putAll(parseStyleProperty(propertyName));
    }

    private static void parseClassesWithinStyleTag(String styleTagValue)
    {
        if(styleTagValue == null || styleTagValue.trim().isEmpty())
            return;

        styleTagValue = styleTagValue.replaceAll("\\\n","")
                .replaceAll("\\\t","").replaceAll("\\\r","");
        String [] classes = styleTagValue.split("\\.");



        for (int i = 0; i < classes.length; i++)
        {
            String classItem = classes[i];
            if(classItem == null || classItem.trim().isEmpty())
                continue;
            htmlPageClasses.putAll(parseCssClass(classItem));
        }
    }

    private static Map<String,Object> parseCssClass(String classItem)
    {
        Map<String,Object> result = new HashMap<>();
        classItem = classItem.replace(".","").replace("}","");



        int index = classItem.indexOf("{");
        String className = classItem.substring(0,index);
        String classStyleProperties = classItem.substring(index+1, classItem.length());
        result.put(className, parseStyleProperty(classStyleProperties));
        return  result;
    }

    private static Map<String,String> parseStyleProperty(String propertyValue)
    {


        Map<String,String> result = new HashMap<>();
        String [] props = propertyValue.split("\\;");
        for (int i=0; i<props.length; i++)
        {
            String item = props[i];
            String itemParts [] = item.split("\\:");
            result.put(itemParts[0].trim(), itemParts[1].trim());
        }
        return result;
    }

    private static boolean checkStyleProperty(String propertyName)
    {
        return propertyName.equalsIgnoreCase(STYLE) || propertyName.equalsIgnoreCase(CLASS);
    }

    private static boolean elementHasAttribute(Element element)
    {
        return element != null && element.attributes() != null && element.attributes().size()>0 ;
    }
}