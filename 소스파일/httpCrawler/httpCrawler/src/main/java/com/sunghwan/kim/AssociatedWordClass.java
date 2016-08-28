package com.sunghwan.kim;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by kim on 2015-10-29.
 *
 *  랭킹에 표시될 토픽들의 연관어 찾기
 *
 *
 */
public class AssociatedWordClass {
    Connection con;
    int yesterday;
    static final int RANK_SIZE = 20;
    public AssociatedWordClass(Connection con, int yesterday){
        this.con = con;
        this.yesterday = yesterday;
    }

    //topicRank는 오른쪽부터 1순위
    public void topicAssociated(LinkedList<String> topicRank) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs  = null;
        ResultSet rs2 = null;
        //전체 문서 불러오기.
        String taSQL = "SELECT NEWS_NOUN.NEWS_NOUN_CONTENT FROM NEWS_NOUN, NEWS WHERE NEWS.NEWS_NO=NEWS_NOUN.NEWS_NO AND NEWS.NEWS_YYYYDDMM=?";
        String tnSQL = "SELECT TOPIC_NO FROM TOPIC WHERE TOPIC=? AND TOPIC_YYYYDDMM=?";
        //db에 insert해야함
        String taiSQL = "INSERT INTO TOPIC_ASSOCIATED(TOPIC, TOPIC_ASSOCIATED, ASSOCIATED_RANK, TOPIC_NO) VALUES(?,?,?,?)";
        int topicSize = topicRank.size();

        pstmt = con.prepareStatement(taSQL);
        pstmt.setString(1, String.valueOf(yesterday));

        //rs에는 전체 문서의 형태소 분석 결과가 있음.
        rs = pstmt.executeQuery();
        for(int i = 0; i<RANK_SIZE; i++){
            //연관어들 저장됨.
            //연관어는 20개씩 저장.
            LinkedList<String> associatedTopics = associatedRank(rs, topicRank.get(i));
            int associatedSize = associatedTopics.size();
            //topic associated는 여러번
            pstmt = con.prepareStatement(tnSQL);
            pstmt.setString(1, topicRank.get(topicSize - (i + 1)));
            pstmt.setString(2,  String.valueOf(yesterday));
            rs2 = pstmt.executeQuery();
            rs2.next();
            String topicNum = rs2.getString(1);
            for(int j =1; j < RANK_SIZE+1; j++){
                pstmt = con.prepareStatement(taiSQL);
                pstmt.setString(1, topicRank.get(topicSize-(i+1)));
                pstmt.setString(2, associatedTopics.get(associatedSize-j));
                pstmt.setInt(3,j);
                pstmt.setInt(4, Integer.parseInt(topicNum));
                pstmt.executeUpdate();
            }
        }
    }

    //해당 토픽의 연관어들 리스트를 반환
    private LinkedList<String> associatedRank(ResultSet rs, String topic) throws SQLException {
        LinkedList<String> containTopicRs = new LinkedList<String>();
        HashMap<String, Integer> containTopics = new HashMap<String, Integer>();
        LinkedList<String> containTopicRank = new LinkedList<String>();
        TopicClass tc = new TopicClass();
        while(rs.next()){
            String nounContent = rs.getString(1);
            if(nounContent.contains(topic)){
                containTopicRs.add(nounContent);
                //containTopicsRS에는 연관어를 찾으려는 토픽이 포함되는 문서들이 저장됨.
            }
        }
        tc.setTopics(containTopicRs, containTopics, containTopicRank);
        tc.quickSort(containTopicRank, containTopics, 0, containTopicRank.size() - 1);
        rs.beforeFirst();
        return containTopicRank;
    }
}
