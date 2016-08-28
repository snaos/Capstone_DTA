
    /*

        아직 형태소분석하는 곳 안 달아 두었음 언제든지 달면 됨

     */

    package com.sunghwan.kim;

    import java.sql.*;

    /**
     * Created by kim on 2015-09-12.
     *
     *  먼저 DateCalc 함수를 통해서 어제자 날자를 계산한다.
     *  NuewsCrawler 클래스를 통해서 날짜에 해당하는 네이트 뉴스를 수집하고
     *  ContentTokenize 클래스를 통해 수집한 뉴스 내용을 형태소 분석을 하여 단어들로 나눈다.
     *  이제 TF-IDF를 통해 뉴스의 주제를 찾아야 하는데 이는 AnalysisClass 클래스를 통해서 뉴스의 주제를 찾는다.
     *  주제를 찾은 후 주제들의 랭킹을 찾는데 TopicClass를 통해서 주제들의 랭킹을 구하고
     *  AssociatedWordClass를 통해 랭킹 주제들의 연관어를 찾아낸다.
     *
     *  엑셀로 주제들을 추출하고 싶은 경우, ExtractIntoExcelClass를 통해 엑셀을 추출한다.
     *
     *  DateCalc -> NewsCrawler -> ContentTokenize -> AnalysisClass (-> ExtractIntoExcelClass )-> TopicClass -> AssociatedWordClass
     */

    public class MainClass {
        public static void main(String[] args) throws Exception {

            Connection con = null;          //connection 객체 생성
            con = DriverManager.getConnection("jdbc:mysql://54.65.222.144/mydb", "root", "raviewme5");       //mysql 연결
            int yesterday = 0;
            int minDate = 0;
            if(args[0].equals("yesterday")){
                System.out.println("DateCalc 수행");
                DateCalc dc1 = new DateCalc();
                minDate = dc1.getYesterdayDate();           //긁어올 최소한의 날짜
                yesterday = dc1.getYesterday();
                System.out.println("yesterday = " + yesterday);
            } else  {
                yesterday = Integer.parseInt(args[0]);
                minDate = yesterday;
            }

            if(Integer.parseInt(args[1]) == 0){
                //         크롤러
                System.out.println("NewsCrawler 수행");
                NewsCrawler nc = new NewsCrawler(minDate, yesterday, con);
                nc.newsCrawler();
    //         토큰화
                System.out.println("ContentTokenize 수행");
                ContentTokenize ct = new ContentTokenize(con);
                ct.contentTokening(yesterday);

                System.out.println("AnalysisClass 수행");
                AnalysisClass ac = new AnalysisClass(yesterday, con);
                ac.newsAnalysis();

    //        엑셀로 추출
                System.out.println("ExtractIntoExcelClass 수행");
                ExtractIntoExcelClass ec = new ExtractIntoExcelClass(con, yesterday);
                ec.extractIntoExcel();

    //        yesterday = 20151115;
                System.out.println("topic 관련 클래스 수행");
                TopicClass tc = new TopicClass(con, yesterday);
                AssociatedWordClass awc = new AssociatedWordClass(con, yesterday);
                awc.topicAssociated(tc.makeTopic());

                System.out.println("topic 사진 클래스 수행");
                GetImage gi = new GetImage(con, yesterday);
                gi.getMainImages();
            } else if (Integer.parseInt(args[1]) == 1 ){
                System.out.println("NewsCrawler 수행");
                NewsCrawler nc = new NewsCrawler(minDate, yesterday, con);
                nc.newsCrawler();

            } else if (Integer.parseInt(args[1]) == 2){
                System.out.println("ContentTokenize 수행");
                ContentTokenize ct = new ContentTokenize(con);
                ct.contentTokening(yesterday);
            } else if (Integer.parseInt(args[1]) == 3 ){
                System.out.println("AnalysisClass 수행");
                AnalysisClass ac = new AnalysisClass(yesterday, con);
                ac.newsAnalysis();
            } else if (Integer.parseInt(args[1]) == 4 ){
    //            엑셀로 추출
                System.out.println("ExtractIntoExcelClass 수행");
                ExtractIntoExcelClass ec = new ExtractIntoExcelClass(con, yesterday);
                ec.extractIntoExcel();

            } else if (Integer.parseInt(args[1]) == 5 ) {
                System.out.println("topic 관련 클래스 수행");
                TopicClass tc = new TopicClass(con, yesterday);
                AssociatedWordClass awc = new AssociatedWordClass(con, yesterday);
                awc.topicAssociated(tc.makeTopic());
            } else if (Integer.parseInt(args[1]) == 6 ) {
                System.out.println("topic 사진 클래스 수행");
                GetImage gi = new GetImage(con, yesterday);
                gi.getMainImages();
            }

            con.close();
        }       //end main
    }



