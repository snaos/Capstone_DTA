package com.sunghwan.kim;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.awt.CharsetString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by kim on 2015-11-02.
 */
public class NewsCrawler {
    //각 뉴스사 정보 저장할 hashMap
    static LinkedList<String> newsCorp;
    static final int NEWS_CONTENT_LENGTH = 10000;
    static final int NEWS_TITLE_LENGTH = 100;
    Connection con = null;
    int yesterday;
    int minDate;

    public NewsCrawler(int minDate, int yesterday, Connection con){
        this.con = con;
        this.yesterday = yesterday;
        this.minDate = minDate;
        newsCorp = new LinkedList<String>();
    }

    public void newsCrawler() throws SQLException, IOException {

        HttpClient httpClient = new DefaultHttpClient();
        // 받아올 뉴스 리스트 url
        String newsListPageUrl = "http://news.nate.com/recent?mid=n0100&type=c&date="+yesterday+"&page=";
        int newsPage =1;
        boolean pageListbool = true;
        setNewsCorp();          //뉴스사 설정
        PreparedStatement pstmt = null;

        String INSQL = "INSERT INTO NEWS(NEWS_TITLE, NEWS_TIME, NEWS_URL, NEWS_CONTENT, NEWS_CO,NEWS_CONTENT_MOD, NEWS_YYYYDDMM) VALUES(?,?,?,?,?,?,?) ";
        while(pageListbool) {
            HttpGet httpGet = new HttpGet(newsListPageUrl + newsPage);
            //  소스 받아올 페이지 주소
            //  받아올 페이지 주소는 계속 증가

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if( entity != null) {
                String html = "";               //  전체 소스코드를 저장할 변수
                String line = "";               //  한 줄씩 읽어 저장하고 있는 임시 변수

                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),"euc-kr"));

                while((line = rd.readLine()) != null) {
                    //          System.out.println("1 : "+line);               //그냥 출력
                    html += line+"\n";
                    //  소스 페이지에서 보이는대로 문자열로 저장
                }

                // HTML Parser인 Jsoup이용
                Document doc = Jsoup.parse(html);       //  html을 저장하고 jsoup document 객체로 변환한다.
                Elements eachNews = doc.getElementsByClass("mlt01");        //뉴스 기사리스트
                //              Elements eachNews = doc.getElementsByClass("mduSubjectList");        //뉴스 기사리스트

                    /*
                    //  여기서 이전페이지랑 현재 페이지랑 비교해서 겹치면 넘어가도록 설정했는데 안되는건가..?
                    if( pastEachNews != null ) {
                        //System.out.println(pastEachNews.get(0));
                       // System.out.println(eachNews.get(0));
                        //이전페이지들 저장한 변수가 empty가 될 때까지 반복
                        while(!pastEachNews.isEmpty()){
                            //각 첫번째가 같으면 현재페이지와 이전페이지 모두 1씩 증가
                            if(pastEachNews.get(0) == eachNews.get(0)){
                                pastEachNews.remove(0);
                                eachNews.remove(0);
                                System.out.printf("!");
                            } else {
                                //같지 않으면 이전 페이지만 1증가
                                pastEachNews.remove(0);
                                System.out.println(eachNews.get(0));
                                System.out.printf("?");
                            }
                        }//이런식으로 이전페이지들과 현재페이지들을 비교해서 셋팅..인데 안되는듯?
                    }
*/
                //뉴스가 존재하지 않는 페이지 => 최대 페이지를 넘어감
                if(eachNews.isEmpty()){
                    break;
                }
                // 받아온 뉴스 리스트
                while (!eachNews.isEmpty()) {
                    String[] content = contentsExtraction(eachNews.get(0).getAllElements().attr("href"));

                    //  해당 뉴스 내용 첫번째 배열요소에는 웹페이지에서 보여지는 텍스트, 두번째 배열요소에는 본문내용의 전체 html 저장
                    //  contentsExtraction() 함수를 사용함으로써 뉴스에 해당하는 기사를 content 변수에 저장
                    String[] medium = eachNews.get(0).getElementsByClass("medium").text().split(" ");               //  예) <span class="medium">정책브리핑<em>10-23 16:31</em></span>
                    String newsTitle = eachNews.get(0).getElementsByClass("tit").text();                            //  뉴스 제목
                    String newsCo = medium[0];                                                                      //  뉴스사
                    String newsTime = medium[1] +" "+ medium[2];                                                    //  뉴스 날짜와 시간
                    String newsUrl = eachNews.get(0).getAllElements().attr("href");                                 //뉴스 url

                    //  뉴스사가 target인 [종합]+[인터넷]에 속해있지 않은 뉴스사라면 분석할 뉴스에서 삭제시킴
                    if(!newsCorp.contains(newsCo)){
                        eachNews.remove(0);
                        continue;
                    }

                    //끝에 광고글 자르기.
                    //       String newsContentMod = lastSpot(content[0], newsCo);
                    String newsContentMod = tLastSpot(content[0]);
                    //       System.out.println("newsContent : "+newsContentMod+"\n");

                    //  DB 'NEWS_CONTENT' column의 최대 길이가 10000이므로 해당 조건에 맞게 뉴스 길이 정리
                    //  DB 'NEWS_TITLE' column의 최대 길이는 100
                    content[0] = stringSizeLimit(content[0], NEWS_CONTENT_LENGTH);
                    newsContentMod = stringSizeLimit(newsContentMod, NEWS_CONTENT_LENGTH);
                    newsTitle = stringSizeLimit(newsTitle, NEWS_TITLE_LENGTH);

                    //  지정한 날짜가 되면 저장 종료
//                    if(Integer.parseInt(medium[1].split("-")[0]) == minDate){
//                        httpGet.abort();
//                        httpClient.getConnectionManager().shutdown();
//                        break;
//                    }
                    //db에 입력
                    pstmt = con.prepareStatement(INSQL);
                    pstmt.setString(1, newsTitle);
                    pstmt.setString(2, newsTime);
                    pstmt.setString(3, newsUrl);
                    pstmt.setString(4, content[0]);

                    //pstmt.setString(5, content[1]);
                    pstmt.setString(5, newsCo);
                    pstmt.setString(6, newsContentMod);
                    pstmt.setString(7, String.valueOf(yesterday));
                    pstmt.executeUpdate();

                    //  현재 news기사 정보 삭제 => 다음 기사가 eachNews의 첫번째 element로 옴
                    eachNews.remove(0);
                }
                //현재의 페이지들을 과거페이지 변수에 저장
                //    pastEachNews = doc.getElementsByClass("mlt01");
            }
            //페이지 증가
            newsPage++;
            System.out.println("page = " + newsPage);
            httpGet.abort();
            //지정한 날짜 외에도 db에서 url을 unique key로 설정했기 때문에 같은기사는 저장되지 않고 catch로 넘어가서 종료되게 된다.
        }
    }

    //뉴스 리스트에서 해당 뉴스에 접근하여 내용을 추출
    private static String[] contentsExtraction(String url) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();

        String[] article = new String[2];

        for(int i=0; i<article.length; i++){
            article[i] = "";
        }

        if( entity != null) {
            String html = "";
            String line = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),"euc-kr"));

            while((line = rd.readLine()) != null) {
                //         System.out.println("2: "+line);
                html += line+"\n";
            }

            Document doc = Jsoup.parse(html);
            Element articleContents = doc.getElementById("articleContetns");
            //    System.out.println(articleContents.html());
            //   System.out.println(articleContents.getElementById("realArtcContents").html().replaceAll("<[^>]*>", " ").toString());

            //뉴스 내용 저장. realArtcContents인 경우도 있고, 아닌 경우 articleContetns으로 찾음. Contentns는 내 오타인것 같지만 아니다.
            try {
                article[0] = articleContents.getElementById("realArtcContents").html().replaceAll("<[^>]*>", " ").toString();
                //  <br/>, <p> tag 등을 제거
                article[1] = articleContents.getElementById("realArtcContents").html();
            } catch(NullPointerException ne) {
                // To do?
                article[0] = articleContents.getElementById("articleContetns").html().replaceAll("<[^>]*>", " ").toString();
                article[1] = articleContents.getElementById("articleContetns").html();
            } finally {
                return article;
            }

        }
        return article;
    }

    /*
        //  nate에서 수집의 범위로 잡은 news에 대해 기사의 끝을 key-value 쌍으로 설정
        private static void setNewsCo(){
            //  meta name=nate:site_name 을 찾으면 뉴스사를 알 수 있다.
            //  1. 종합 뉴스
            newsCoMap.put("국민일보", "메일");            // '김의구 기자 egkim@kmib.co.kr' 가끔 기자 이름 형태가 달라짐.
            newsCoMap.put("경향신문", "[오늘의");               // '<이윤주 기자 runyj@kyunghyang.com>'으로 끝나나 안적는 기자도 있음. 하지만 '경향신문 [오늘의 인기뉴스]'는 무조건 적힘.
            newsCoMap.put("노컷뉴스", "●");               // 'park@cbs.co.kr' 하지만 아무것도 없이 끝나는 경우도 있음. 그땐 바로 광고
            newsCoMap.put("뉴스1", "관련뉴스");                  // '뉴스1 관련뉴스'
            newsCoMap.put("뉴시스", "메일");                 // 노컷과 같음
            newsCoMap.put("동아일보", "메일");               // '동아경제 기사제보 eco@donga.com' '동아닷컴 디지털뉴스팀 기사제보 dnews@donga.com'       '소속' 기사제보 메일
            newsCoMap.put("머니투데이", "[머니투데이");             // '[머니투데이 핫뉴스]'
            newsCoMap.put("문화일보", "[관련기사/많이본기사]");               // '[관련기사/많이본기사]'
            newsCoMap.put("서울신문","보러가기]");               // '[서울신문 다른기사 보러가기]'
            newsCoMap.put("세계일보","메일");               // '이슈팀 ent@segye.com'  or '김경호 기자 stillcut@segye.com'
            newsCoMap.put("아시아투데이","ⓒ");           // 'ⓒ "젊은 파워, 모바일 넘버원 아시아투데이"'
            newsCoMap.put("연합뉴스","(끝)");               // (끝)
            newsCoMap.put("조선일보", "ㆍ");               // 바로 광고
            newsCoMap.put("중앙일보","[J-Hot]");               // [J-Hot]
            newsCoMap.put("쿠키뉴스", "메일");               // 제일 거지 같네... 바로 광고... 없을 때도 있음. 메일 있을때도 있고.. 일단 마지막은 '갓 구워낸 바삭바삭한 뉴스 ⓒ 쿠키뉴스(www.kukinews.com), 무단전재 및 재배포금지'
            newsCoMap.put("한겨례", "<한겨레");                 // <한겨레 인기기사>
            newsCoMap.put("한국일보","관련기사");               // 관련기사

            //  2. 인터넷 뉴스
            newsCoMap.put("뉴데일리"," 자유민주·시장경제의");                // - 정재훈 기자 -    자유민주·시장경제의 파수꾼 - 뉴데일리    Copyrights ⓒ 2005 뉴데일리뉴스 - 무단전재, 재배포 금지
            newsCoMap.put("더자유", "[주간자유]");                  // 김정일 기자 (kji@daily-liberty.com) [주간자유] 제공     주간자유(http://www.daily-liberty.com) ⓒ 무단전재 및 재배포금지
            newsCoMap.put("더팩트", "[인기기사]");                  // [인기기사]
            newsCoMap.put("데일리안", "Copyrights");                // - Copyrights ⓒ (주)데일리안, 무단 전재-재배포 금지 -
            newsCoMap.put("오마이뉴스", "저작권자(c)");              // 저작권자(c) 오마이뉴스(시민기자), 무단 전재 및 재배포 금지
            newsCoMap.put("팝뉴스", "리포터");                  // 이정 리포터
            newsCoMap.put("헤럴드POP", "[베스트");               // [베스트 클릭! 헤럴드 생생 얼리어답터 뉴스]

            return;
        }
    */
    private static void setNewsCorp(){
        //  1. 종합뉴스
        newsCorp.add("국민일보");
        newsCorp.add("경향신문");
        newsCorp.add("노컷뉴스");
        newsCorp.add("뉴스1");
        newsCorp.add("뉴시스");
        newsCorp.add("동아일보");
        newsCorp.add("머니투데이");
        newsCorp.add("문화일보");
        newsCorp.add("서울신문");
        newsCorp.add("세계일보");
        newsCorp.add("아시아투데이");
        newsCorp.add("연합뉴스");
        newsCorp.add("조선일보");
        newsCorp.add("중앙일보");
        newsCorp.add("쿠키뉴스");
        newsCorp.add("한겨례");
        newsCorp.add("한국일보");

        //  2. 인터넷
        newsCorp.add("뉴데일리");
        newsCorp.add("더자유");
        newsCorp.add("더팩트");
        newsCorp.add("데일리안");
        newsCorp.add("오마이뉴스");
        newsCorp.add("팝뉴스");
        newsCorp.add("헤럴드POP");
    }

    /*
    private static boolean emailCheck(String email) {
        boolean err = false;

        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9._]+\\.[a-zA-Z]{2,4}";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(email);

        if(m.matches()) {
            err = true;
        }
        return err;
    }
*/

    //  뉴스기사 본문에서 불필요한 광고 부분을 제거하기 위한 함수
    //  email을 기준으로 제거
    private static String tLastSpot(String content){
        String retStr = content;
        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9._]+\\.[a-zA-Z]{2,4}";
        //     System.out.println("tLastSpot");
        if(content.contains("@")){
            retStr = content.split(regex)[0];
            //  email 이전까지를 본문으로 취급
        }

        return retStr;
    }
    private String stringSizeLimit(String str, int limit){
        if(str.length() > limit){
            str = str.substring(0, limit);
        }
        return str;
    }

    /*
    //  뉴스기사 본문에서 불필요한 광고 부분을 제거하기 위한 함수
    private static String lastSpot(String content, String newsCo) {
    //    System.out.println("newsCo = " + newsCo);
        String endIndexValue = newsCoMap.get(newsCo);
        // 마지막 인덱스
        // endIndxValue가 무엇인가에 따라서 다름
        System.out.println("newsCoMap value = "+ endIndexValue);
        String returnString = "";

        //  구분자가 email의 경우
        if(endIndexValue.equals("메일")){
            //  이메일일 경우 본문을 " "을 구분자로 잘라서 각 token마다 다 이메일형식과 비교해서 이메일 형식이면 종료. 이메일이 안나오면 끝까지.
            String[] contentToken = content.split(" ");
            int emailIndex;
            for(emailIndex =0; emailIndex<contentToken.length; emailIndex++){
                if(emailCheck(contentToken[emailIndex])){
                    break;
                }
            }

            System.out.println("contentToken.length = " + contentToken.length);
            System.out.println("emailIndex = "+ emailIndex);
            System.out.println("contentToken[emailIndex] = " + contentToken[emailIndex-1]);
            for(int i = 0; i< emailIndex-1; i++){
                returnString = returnString.concat(contentToken[i]);
                returnString = returnString.concat(" ");
            }
        } else {    //구분자가 메일이 아니면
            //단어와 비교해서 해당 단어가 나오면 종료
            int endIndx = content.indexOf(endIndexValue);
            if(newsCo.equals("서울신문")){
                endIndx = endIndx-2;
            }
            returnString = content.substring(0, endIndx-1);
        }
        return returnString;
    }
    */
}


