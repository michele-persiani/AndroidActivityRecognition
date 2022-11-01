package umu.software.activityrecognition.shared.util.xml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * Class to parse XML files into objects
 * @param <T> type of the objects being parsed
 */
public abstract class XMLParser<T>
{

    /**
     * Parse a xml file
     * @param xml xml string
     * @return the parsed object of type T
     */
    public T parseXML(String xml) throws ParserConfigurationException, IOException, SAXException
    {
        InputSource is = new InputSource(new StringReader(xml));
        return parseXML(is);
    }

    /**
     * Parse a xml
     * @param is input source for an xml file
     * @return the parsed object of type T
     */
    public T parseXML(InputSource is) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        return parseDocument(doc);
    }

    /**
     * Parse a xml file
     * @param doc the xml document
     * @return the parsed object of type T
     */
    protected abstract T parseDocument(Document doc);


    protected <R> R castAttribute(Node node, String attrName, boolean required, @Nullable R defaultValue, Function<String, R> cast)
    {
        Node attrNode = node.getAttributes().getNamedItem(attrName);
        if (required && attrNode == null) throw new RuntimeException(String.format("Value for attribute '%s' is required", attrName));
        if (attrNode == null)
            return defaultValue;
        return cast.apply(attrNode.getNodeValue());
    }


    protected String getStringAttribute(Node node, String attrName, boolean required, @Nullable String defaultValue)
    {
        return castAttribute(node, attrName, required, defaultValue, attr -> attr);
    }


    protected <R> List<R> filterMap(NodeList nodeList, @Nullable Predicate<Node> filter, @Nullable Function<Node, R> map)
    {
        List<Node> nodes = Lists.newArrayList();
        for (int i =0; i < nodeList.getLength(); i++)
            nodes.add(nodeList.item(i));
        return nodes.stream()
                .filter( n -> filter == null || filter.test(n))
                .map(n -> (map == null)? (R) n : map.apply(n))
                .collect(Collectors.toList());
    }

    protected List<Node> filterChildren(Node parent, @Nullable Predicate<Node> filter)
    {
        NodeList nodeList = parent.getChildNodes();
        return filterMap(nodeList, filter, node -> node);
    }

    protected <R> List<R> filterMapChildren(Node parent, @Nullable Predicate<Node> filter, @Nullable Function<Node, R> map)
    {
        NodeList nodeList = parent.getChildNodes();
        return filterMap(nodeList, filter, map);
    }


    protected Node findChildren(Node parent, String name)
    {
        for (int i =0; i < parent.getChildNodes().getLength(); i++)
        {
            Node ch = parent.getChildNodes().item(i);
            if (ch.getNodeName().equals(name))
                return ch;
        }
        return null;
    }
}
