package makeHtmlTree;

import htmlStructure.HtmlNode;
import htmlStructure.HtmlNodeInterface;
import tags.*;

public class MakeNodeTags {

    public static HtmlNodeInterface createElement(String elementName){

        if(elementName.equalsIgnoreCase("p"))
            return new P_Tag("p");

        if(elementName.equalsIgnoreCase("h1"))
            return new H1_Tag("h1");

        if(elementName.equalsIgnoreCase("h2"))
            return new H2_Tag("h2");
        if(elementName.equalsIgnoreCase("select"))
        {
            System.out.println("yes it is tag input");
            return new Select_Tag("select");
        }

        if(elementName.equalsIgnoreCase("src"))
            return new H2_Tag("src");
        if(elementName.equalsIgnoreCase("h3"))
            return new H3_Tag("h3");
        if(elementName.equalsIgnoreCase("h4"))
            return new H4_Tag("h4");

        if(elementName.equalsIgnoreCase("h5"))
            return new H5_Tag("h5");

        if(elementName.equalsIgnoreCase("h6"))
            return new H6_Tag("h6");

        if(elementName.equalsIgnoreCase("input"))
        {
            System.out.println("yes");
            return new Input_Tag("input");
        }


        if(elementName.equalsIgnoreCase("img"))
            return new image_Tag("img");

        if(elementName.equalsIgnoreCase("div"))
            return new Div_Tag("div");

        if(elementName.equalsIgnoreCase("a"))
            return new A_Tag("a");

        if(elementName.equalsIgnoreCase("body"))
            return new Body_Tag("body");

        if(elementName.equalsIgnoreCase("head"))
            return new Head_Tag("head");

        if(elementName.equalsIgnoreCase("html"))
            return new HtmlNode("html");

        return null;
    }
}
