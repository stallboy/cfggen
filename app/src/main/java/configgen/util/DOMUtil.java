package configgen.util;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class DOMUtil {
    public static Element rootElement(File file) {
        try {
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(file)
                    .getDocumentElement();
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Element> elements(Element self, String name) {
        List<Element> res = new ArrayList<>();
        NodeList childNodes = self.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node node = childNodes.item(i);
            if (org.w3c.dom.Node.ELEMENT_NODE != node.getNodeType())
                continue;
            Element e = (Element) node;
            if (e.getNodeName().equals(name))
                res.add(e);
        }
        return res;
    }

    private static final Pattern stringArrayPattern = Pattern.compile("\\s*,\\s*");

    public static String[] parseStringArray(Element self, String attrName) {
        String attr = self.getAttribute(attrName).trim();
        if (!attr.isEmpty())
            return stringArrayPattern.split(attr);
        else
            return new String[0];
    }

}
