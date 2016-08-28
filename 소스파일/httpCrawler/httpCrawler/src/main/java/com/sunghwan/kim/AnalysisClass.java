package com.sunghwan.kim;

import org.apache.poi.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by kim on 2015-10-29.
 *
 *  tf(t,d)=log⁡(f(t,d)+1) {f(t,d) : d에서의 t 출현 빈도}
 *
 *  idf (t,D)=log⁡(|D|/(n(d))) {d : 단어 t가 포함된 문서 수, D : 전체 문서 수}
 *
 *  TF-IDF(t, d, D) = tf(t,d) * idf(t, D)
 *
 *  ---------------------------------------------------------
 *  2015 - 11 - 09 수정
 *      1. 제목에 포함된 단어만 주제로 설정.
 *      2. 그 중에서 단어길이가 1보다 긴 것들만 설정.
 *
 *      제목에 포함된 단어만 추출하는 이유 중 하나는
 *      우리가 내용을 메일로 끊게 되는데 이 경우 광고 글도 포함이 되게 된다.
 *      근데 주제를 이 광고 글을 가져오는 경우도 있지만 제목에서 추출하면 이러한 경우는 사라진다.
 *
 */
public class AnalysisClass {
    HashMap<String, String> preDF;
    HashMap<String, Integer> TF;
    //  특정 단어와 문서 번호의 쌍으로 구성
    int yesterday;
    Connection con = null;
    public AnalysisClass(int yesterday, Connection con){
        preDF = new HashMap<String, String>();      // 해당 단어가 포함된 문서들
                        // key : noun , value : DF value
        TF = new HashMap<String, Integer>();        // 해당 단어의 TF 값(log를 씌우기 전)
                        // key : newsNo_noun , value : TF value
        this.yesterday = yesterday;     // 분석할 뉴스 날짜
        this.con = con;                 //DB connection
    }

    public void newsAnalysis() throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs  = null;
        int IDF_D = 0, newsNo;          //IDF_D : IDF에서 쓰이는 전체 D
        String[] nouns, analysisResult;
        String title;

        // select sql qeury
        String ntSQL = "SELECT NEWS_NOUN.NEWS_NO, NEWS_TITLE AS NEWS_NO, NEWS_NOUN.NEWS_NOUN_CONTENT AS NEWS_NOUN_CONTENT, NEWS.NEWS_TITLE AS NEWS_TITLE FROM NEWS_NOUN,NEWS WHERE NEWS.NEWS_YYYYDDMM=? AND NEWS.NEWS_NO=NEWS_NOUN.NEWS_NO";
        // insert sql query
        String ntiSQL = "INSERT INTO NEWS_ANALYSIS(NEWS_NO, NEWS_TOPIC, NEWS_TOPIC_TFIDF) VALUES(?,?,?)";

        pstmt = con.prepareStatement(ntSQL);
        pstmt.setString(1, String.valueOf(yesterday));
        rs = pstmt.executeQuery();              //rs에 sql 결과 저장.
        //각 뉴스에 대한 TF를 설정하고 전체에 대한 DF값을 설정
        while(rs.next()){
            //rs = NEWS_NO, NEWS_NOUN_CONTENT
            newsNo = rs.getInt("NEWS_NO");                                  //뉴스 번호
            nouns = rs.getString("NEWS_NOUN_CONTENT").split(" ");       //뉴스가 포함한 단어들
            for(String nounToken : nouns){          //각 단어들 마다 tf와 df 세팅
                setTF(newsNo, nounToken);
                setDF(newsNo, nounToken);
            }
            IDF_D++;        //전체 문서수 카운트
        }
        rs.beforeFirst();       //iterator 처음으로 돌아가기
        while(rs.next()){
            newsNo = rs.getInt("NEWS_NO");                                  //뉴스 번호
            nouns = rs.getString("NEWS_NOUN_CONTENT").split(" ");       //뉴스가 포함한 단어들
            title = rs.getString("NEWS_TITLE");
            analysisResult = newsTF_IDF(newsNo, nouns, IDF_D, title);              //해당 문서에서의 [단어, tf-idf값]

            //  newsTF_IDF() 함수내에서 조건에 만족하지 않는 결과가 나올경우, null(noun) 0(tf-idf)가 저장되어지기 때문에 다음 조건문을 이용하여 거름
            /*
            if(analysisResult[0] == null || analysisResult[1].equals("0")){
                continue;
            }
            */

            //  해당 뉴스에 대해 주제와 그에 해당하는 tf-idf 값을 DB에 저장
            pstmt = con.prepareStatement(ntiSQL);
            pstmt.setInt(1, newsNo);
            //토픽이 null값이나 공란을 저장하지 않도록 한다.
            if(analysisResult[0] == null || analysisResult[0].equals(" ")){
                continue;
            }
            pstmt.setString(2, analysisResult[0]);
            pstmt.setString(3, analysisResult[1]);
            pstmt.executeUpdate();
        }

    }
    //  DF값(특정 단어가 나타난 문서의 수) 설정
    //  전체적인 문서에 대해서 해당 단어가 출현했나 안했나 확인
    private void setDF(int newsNo, String noun){
        boolean isContain = false;

        if(preDF.containsKey(noun)){
            String value = preDF.get(noun);
            String[] valueToken = value.split(" ");         //valueToken은 해당 noun이 포함 됐던 뉴스 번호들
            for(String token : valueToken){
                if(token.equals(newsNo)){                   //현재의 news에서 이미 카운트를 를 했음.
                    isContain = true;
                }
            }
            if(isContain == false){                         // 이전에 카운트를 안했으면 포함해서 preDF에 저장
                value = value.concat(" ");
                value = value.concat(String.valueOf(newsNo));
                preDF.put(noun, value);                     // preDF에 추가
            }
        } else {
            preDF.put(noun, String.valueOf(newsNo));
        }
    }

    //  TF값(특정 단어가 문서내에 얼마나 자주 등장하는지를 나타내는 값) 설정
    //  각 문서번호와 단어로 저장하여 각 문서에서 해당 단어의 빈도수를 구함
    //  newsNo : 문서번호,    noun : 특정단어
    private void setTF(int newsNo, String noun){
        String TFKey = "";
        TFKey = TFKey.concat(String.valueOf(newsNo));
        TFKey = TFKey.concat("_");
        TFKey = TFKey.concat(noun);
        int tempValue;
        if(TF.containsKey(TFKey)){
            tempValue = TF.get(TFKey)+1;
            TF.put(TFKey, tempValue);
        } else {
            TF.put(TFKey,1);
        }
    }

    //  해당 뉴스의 noun 마다 TF-IDF 값을 구해서 가장 큰 noun 을 return
    //  nouns : 해당 뉴스의 단어들, IDF_D : 전체 문서 수
    private String[] newsTF_IDF(int newsNo, String[] nouns, int IDF_D, String title){
        String newsList, TF_IDF_noun = null;
        int d;                                  //포함된 전체 문서 수
        double TFValue, IDFValue, maxTF_IDF = 0;
        for(String noun : nouns){           // 해당 문서 단어들 마다 tf-idf를 구함
            newsList = preDF.get(noun);     //해당 단어 포함된 뉴스들 번호 받아옴.
            d = newsList.split(" ").length;         // 구분자가 " "이므로 포함된 전체 문서 수 구해옴
            TFValue = Math.log10(d+1);               // tf값 구함.
            IDFValue = Math.log10(IDF_D/d);          // idf값 구함
            //제목에 포함된 단어만 생각.
            //단어는 1글자보다 긴 것들만.
            if( (title.contains(noun)) && (maxTF_IDF <(TFValue*IDFValue)) && (noun.length() >1) ){      //가장 큰 tf-idf가 해당 뉴스 주제
                maxTF_IDF = (TFValue*IDFValue);     //tf-idf값
                TF_IDF_noun = noun;                 //해당 단어
            }
        }
        return new String[]{TF_IDF_noun, String.valueOf(maxTF_IDF)};        //tf-idf와 해당 단어 반환
    }
}
