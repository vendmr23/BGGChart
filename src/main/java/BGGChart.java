import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class download a collection from BGG and renders a set of consolidated images
 *
 * @author <a href="mailto:vendmr@hotmail.com">mrobinson</a>, 4/12/2018
 */
public class BGGChart {
    public static void main(String[] args) throws IOException {

        String userId = args[0];
        int baseSize = Integer.parseInt(args[1]);
        int imagesAcross = Integer.parseInt(args[2]);
        int imagesDown = Integer.parseInt(args[3]);

        BGGChart bggChart = new BGGChart();

        String collectionXmlString = bggChart.fetchCollectionXml(userId);

        List<String> imageUrls = new ArrayList<>();
        bggChart.extractImageUrls(collectionXmlString, imageUrls);

        //    writeToSingleFiles(baseSize, imageUrls);

        writeToConsolidatedFiles(baseSize, imageUrls, imagesAcross, imagesDown);
    }

    /**
     * Downloads each image individually.
     * @param baseSize
     * @param imageUrls
     */
    private static void writeToSingleFiles(int baseSize, List<String> imageUrls) {
        int i = 0;
        for (String imageUrl : imageUrls) {
            i++;
            System.out.println("writing image " + i + "/" + imageUrls.size());
            Image image = null;
            try {
                URL url = new URL(imageUrl);
                image = ImageIO.read(url);
                Image scaledInstance = null;

                if (image.getHeight(null) < image.getWidth(null)) {
                    scaledInstance = image.getScaledInstance(baseSize, -1, Image.SCALE_DEFAULT);
                } else {
                    scaledInstance = image.getScaledInstance(-1, baseSize, Image.SCALE_DEFAULT);
                }

                RenderedImage resizedImage = convertToBufferedImage(scaledInstance, baseSize);
                ImageIO.write(resizedImage, "png", new File("myimage" + i + ".png"));
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    /**
     * Creates the compound images (called pages here)
     * @param baseSize
     * @param imageUrls
     * @param inAcross
     * @param inDown
     */
    private static void writeToConsolidatedFiles(int baseSize, List<String> imageUrls, int inAcross, int inDown) {

        int imageCount = 0;
        int size = imageUrls.size();
        int pages = size / (inAcross * inDown) + 1;

        for (int j = 1; j <= pages; j++) {
            BufferedImage newImage = new BufferedImage(baseSize * inAcross, baseSize * inDown, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = newImage.createGraphics();

            try {
                for (int y = 0; y < inDown; y++) {
                    for (int x = 0; x < inAcross; x++) {

                        if (imageCount < size) {
                            System.out.println("writing image " + (imageCount+1) + "/" + imageUrls.size());
                            String imageUrl = imageUrls.get(imageCount++);
                            URL url = new URL(imageUrl);
                            Image image = ImageIO.read(url);
                            Image scaledInstance = null;

                            if (image.getHeight(null) < image.getWidth(null)) {
                                scaledInstance = image.getScaledInstance(baseSize, -1, Image.SCALE_SMOOTH);
                            } else {
                                scaledInstance = image.getScaledInstance(-1, baseSize, Image.SCALE_SMOOTH);
                            }

                            int horizontalPadding = (baseSize - scaledInstance.getWidth(null)) / 2;
                            int verticalPadding = (baseSize - scaledInstance.getHeight(null)) / 2;

                            g.drawImage(scaledInstance, x * baseSize+ horizontalPadding, y * baseSize+ verticalPadding, null);
                        }
                    }
                }
                g.dispose();


                ImageIO.write(newImage, "png", new File("fullImage" + j + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public static BufferedImage convertToBufferedImage(Image image, int baseSize) {
        BufferedImage newImage = new BufferedImage(
                baseSize, baseSize,
                BufferedImage.TYPE_INT_ARGB);

        int horizontalPadding = (baseSize - image.getWidth(null)) / 2;
        int verticalPadding = (baseSize - image.getHeight(null)) / 2;
        Graphics2D g = newImage.createGraphics();
        g.setPaint(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, baseSize, baseSize);
        g.drawImage(image, horizontalPadding, verticalPadding, null);
        g.dispose();
        return newImage;
    }

    /**
     * oarse the collection xml file
     * @param inCollectionXmlString
     * @param inImageUrls
     * @throws IOException
     */
    private void extractImageUrls(String inCollectionXmlString, List<String> inImageUrls) throws IOException {
        try {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(inCollectionXmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList nList = doc.getElementsByTagName("item");
            System.out.println("----------------------------");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    System.out.println("\nCurrent Element :" + eElement.getNodeName());
                    NodeList childNodes = nNode.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node child = childNodes.item(i);
                        if (child != null && child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName() == "image") {
                            inImageUrls.add(child.getTextContent());
                            System.out.println("Image URL=" + child.getTextContent());
                            break;
                        }
                    }

                }
            }

        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the collection from BGG
     * @param inUserId
     * @return
     * @throws IOException
     */
    String fetchCollectionXml(String inUserId) throws IOException {
        String collectionXmlString = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://www.boardgamegeek.com/xmlapi2/collection?username=" + inUserId + "&own=1&excludesubtype=boardgameexpansion");
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                collectionXmlString = EntityUtils.toString(entity);
            }
        }
        return collectionXmlString;
    }

}
