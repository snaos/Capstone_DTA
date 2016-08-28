package com.sunghwan.kim;

import org.imgscalr.Scalr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 특정 단어를 주었을 경우, 그에 해당하는 이미지 링크를 제공해주는 클래스
 * - Naver Open 이미지검색 API 이용
 *
 * Created by khjk310 on 2015-11-29.
 */
public class GetImage {
    /*
    * variables
    * */
    private String api_url = "http://openapi.naver.com/search"; //  open API URL
    private String key = "b710fe591322877fd96d0dc20ddbf2a9";    //  검색 API 발급 키
    private String query = "";          //  검새어
    private String target = "image";    //  이미지 검색 API
    private int start = 2;              //  검색위치
    private int display = 1;             //  보여줄 갯수
    private String ImageURL = "";       //  검색할 전체 URL
    int yesterday=0;
    Connection con = null;
    /*
    * methods
    * */
    public GetImage(Connection con, int yesterday){
        this.yesterday = yesterday;
        this.con = con;
    }

    public void setQuery(String query){
        this.query = query;
    }

    public String getQuery(){
        return this.query;
    }

    public void setURL(){
        ImageURL = api_url+"?key="+key+"&query="+getQuery()+"&target="+target+"&start="+start+"&display="+display;
    }

    public String getURL(){
        return this.ImageURL;
    }

    //  실제 이미지의 URL으르 반환해주는 함수
    /**
     * 예시 : http://openapi.naver.com/search?key=b710fe591322877fd96d0dc20ddbf2a9&query=pudding&target=image&start=1&display=2
     * rss 태그가 root element이고
     * 자식첫번째 노드는 channel 태그인데, 이 태그는 한개만 존재하며 (1)
     * 자식노드를 한번 더 들어가 item 태그에 실제 원하는 이미지 검색결과가 존재한다. (2)
     * 이 item 태그에 존재하는 link 혹은 thumbnail 태그를 사용하여 이미지의 url을 알 수 있다. (3)
     *
     * @return
     * @throws SQLException
     * @throws IOException
     */
    //사진 받아오기
    public String getImageLink() throws SQLException, IOException {
        String imgLink = "";

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xml;

            xml = documentBuilder.parse(getURL());

            Element element = xml.getDocumentElement();
            Node channelNode = element.getElementsByTagName("channel").item(0); //  (1)
            NodeList list = channelNode.getChildNodes();

            for(int i=0; i<list.getLength(); i++){
                if(list.item(i).getNodeName().equals(("item"))){    //  (2)
                    NodeList list2 = list.item(i).getChildNodes();  //  <title>, <link>, <thumbnail>, <sizeheight>, <sizewidth> 등의 자식태그들이 존재

                    for(int j=0; j<list2.getLength(); j++){         //  실제 url 검색시, display=1로 설정하기 때문에 list2의 길이는 항상1이지만 확장성을 위해 다음과 같이 코드 작성
                        if(list2.item(j).getNodeName().equals("link")){    //  (3)
                            String content = list2.item(j).getTextContent();
                            //        System.out.println("name : "+list2.item(j).getNodeName());  //  다음 코드를 통해서 node 의 이름을 알 수 있다.
                            imgLink = content;
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imgLink;
    }

    //리사이즈
    public static void imageResize(String orgFilePath, String targetFilePath, String imageType) throws Exception, IOException {
        //  [파일 경로로부터 이미지를 읽어올 경우 아래의 코드 사용]
        //  BufferedImage originalImage = ImageIO.read(new File(orgFilePath));

        URL url = new URL(orgFilePath);
        BufferedImage originalImage = ImageIO.read(url);

        //  Index.html 의 사진이 뿌려지는 곳에 이미지를 삽입하기 위하여 해당 사진이 놓여지는 사이즈에 맞게 높이와 넓이 조정
        int width = 950;
        int height = 450;

        //    [사진 중심부를 기준으로 crop 하여 사용할 경우 아래의 코드 추가]
        //    int imgwidth = Math.min(originalImage.getHeight(), originalImage.getWidth());
        //    int imgheight = imgwidth;

        //    BufferedImage scaledImage = Scalr.crop(originalImage, (originalImage.getWidth() - imgwidth)/2, (originalImage.getHeight() - imgheight)/2, imgwidth, imgheight, null);

        //  GetImage proportion 에 상관없이 크기를 조정하기 위해 Scalr.Mode.FIT_EXACT mode 를 사용
        BufferedImage resizedImage = Scalr.resize(originalImage, Scalr.Mode.FIT_EXACT, width, height, null);
        ImageIO.write(resizedImage, imageType, new File(targetFilePath));
    }

    public void getMainImages() throws Exception {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String tnSQL = "SELECT TOPIC FROM TOPIC WHERE TOPIC_YYYYDDMM=? ORDER BY TOPIC_RANK LIMIT 10";
        pstmt = con.prepareStatement(tnSQL);
        pstmt.setString(1, String.valueOf(yesterday));
        rs = pstmt.executeQuery();
        int rankNum = 1;
        while(rs.next()) {
            String topic = rs.getString("TOPIC");
            String imageNM = yesterday + "_" + rankNum + ".jpg";
            setQuery(topic);
            setURL();
            imageResize(getImageLink(), imageNM, "jpg");
            rankNum++;
        }
        System.out.println("사진 저장 완료");

    }

}