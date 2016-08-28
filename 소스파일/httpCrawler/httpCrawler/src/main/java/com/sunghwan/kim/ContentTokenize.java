package com.sunghwan.kim;

import com.twitter.penguin.korean.KoreanTokenJava;
import com.twitter.penguin.korean.TwitterKoreanProcessorJava;
import com.twitter.penguin.korean.tokenizer.KoreanTokenizer;
import scala.collection.Seq;

import java.sql.*;
import java.util.List;



/**
 * Created by kim on 2015-10-24.
 *  수집한 뉴스의 내용을 twitter 한글 형태소 분석기를 통해 nouns들로 나눈 후 DB에 저장
 */
public class ContentTokenize {
    Connection con = null;

    public ContentTokenize(Connection con) {
        this.con = con;
    }

    public void contentTokening(int yesterday) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String nsSQL = "SELECT NEWS_NO, NEWS_TITLE, NEWS_CONTENT_MOD FROM NEWS WHERE NEWS_YYYYDDMM=?";
        String isSQL = "INSERT INTO NEWS_NOUN(NEWS_NO, NEWS_NOUN_CONTENT) VALUES(?, ?)";
        pstmt = con.prepareStatement(nsSQL);
        pstmt.setString(1, String.valueOf(yesterday));

        rs = pstmt.executeQuery();              //sql문을 수행하여 결과를 RsultSet 객체에 저장

        String tempString;
        while(rs.next()){           //결과를 한 행씩 돌아가며 가져옴.
            //rs = NEWS_NO, NEWS_CONTENT_MOD
            tempString = "";
            String target = rs.getString("NEWS_TITLE") + " " + rs.getString("NEWS_CONTENT_MOD");
            CharSequence normalized = TwitterKoreanProcessorJava.normalize(target);
            Seq<KoreanTokenizer.KoreanToken> tokens = TwitterKoreanProcessorJava.tokenize(normalized);
            List<String> tokenList = TwitterKoreanProcessorJava.tokensToJavaStringList(tokens);
            List<KoreanTokenJava> tokenListInfo = TwitterKoreanProcessorJava.tokensToJavaKoreanTokenList(tokens);

            for (int i = 0; i<tokenListInfo.size(); i++){
                if(tokenListInfo.get(i).toString().contains("Noun")){
                    String word = tokenListInfo.get(i).toString().split("[(]")[0];
                    if(word.contains("*")){
                        //끝에 *가 붙어있으면 떼어냄.
                       word = word.substring(0,word.length()-2);
                    }
                    if(word.length() >1){
                        // 두글자 이상인 단어만 저장.
                        tempString = tempString.concat(word);
                        tempString = tempString.concat(" ");
                    }
                }
            }
            pstmt = con.prepareStatement(isSQL);
            pstmt.setString(1, String.valueOf(rs.getInt("NEWS_NO")));
            pstmt.setString(2,tempString);
            pstmt.executeUpdate();
        }
    }
}
