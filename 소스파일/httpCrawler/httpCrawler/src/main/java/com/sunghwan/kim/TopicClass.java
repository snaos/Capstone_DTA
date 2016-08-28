package com.sunghwan.kim;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by kim on 2015-11-02.
 *  뉴스들의 주제에 따라서 랭킹을 메긴다.
 *
 */
public class TopicClass {
    Connection con;
    HashMap<String, Integer> topics;    //  ex) 기사1의 주제 : 김영삼, 기사2의 주제 : 김영삼, 기사3의 주제 : 문재인, 기사 4의 주제 : 김영삼
    //      [김영삼, 3], [문재인,1], [...] 의 형태로 저장
    LinkedList<String> topicRank;       //      [김영삼, 문재인, ... ] 의 형태로 저장
    int yesterday;
    public TopicClass(Connection con, int yesterday){
        this.con = con;
        this.yesterday = yesterday;
        topics = new HashMap<String, Integer>();
        topicRank = new LinkedList<String>();
    }
    public TopicClass(){
    }

    //주제 랭킹별로 역순으로 정렬된 LinkedList를 반환
    public LinkedList<String> makeTopic() throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs  = null;

        String nsSQL = "SELECT NEWS_ANALYSIS.NEWS_TOPIC FROM NEWS_ANALYSIS, NEWS WHERE NEWS.NEWS_YYYYDDMM=? AND NEWS.NEWS_NO = NEWS_ANALYSIS.NEWS_NO";
        String tiSQL = "INSERT INTO TOPIC(TOPIC, TOPIC_RANK, TOPIC_YYYYDDMM) VALUES(?,?,?)";
        pstmt = con.prepareStatement(nsSQL);
        pstmt.setInt(1, yesterday);
        rs = pstmt.executeQuery();      //주제들만 가져옴.

        setTopics(rs, topics, topicRank);
        quickSort(topicRank, topics, 0, topicRank.size()-1);
        pstmt = con.prepareStatement(tiSQL);
        for(int i = 1; i<(topicRank.size()+1); i++){
            pstmt.setString(1, topicRank.get(topicRank.size()-i));
            pstmt.setInt(2, i);
            pstmt.setString(3, String.valueOf(yesterday));
            pstmt.executeUpdate();
        }

        return topicRank;           //주제들의 순위가 반환됨.
    }
    //  topic 과 해당 topic 이 주제인 뉴스의 갯수를 HashMap 에 저장
    //  topic 을 linkedList 에 저장
    private void setTopics(ResultSet rs, HashMap<String, Integer> hashMap,  LinkedList<String> linkedList) throws SQLException {
        while(rs.next()) {
            String topic = rs.getString("NEWS_TOPIC");
            //  AnalysisClass의 Line 91~93을 추가하면 아래의 null 검사를 제거할 수 있음
            if (topic != null) {
                if (hashMap.containsKey(topic)) {
                    //  이미 한번이상 제시된 topic 이라면 그 count 값을 증가 시킴
                    int value = hashMap.get(topic);
                    value++;
                    hashMap.put(topic, value);
                } else {
                    hashMap.put(topic, 1);
                    linkedList.add(topic);
                    //  해당 topic 이 나온 횟수를 1로 초기화
                    //  처음 나온 주제라면 LinkedList 에 추가
                }
            }
        }
    }
    // linkedList에 포함된 단어들,
    // hashMap에 해당 단어들의 출현 빈도 저장.
    public void setTopics(LinkedList<String> rs, HashMap<String, Integer> hashMap, LinkedList<String> linkedList){
        String[] nouns;
        for(int i =0; i<rs.size(); i++){
            nouns = rs.get(i).split(" ");
            for(int j = 0; j<nouns.length; j++){
                if(hashMap.containsKey(nouns[j])){
                    int value = hashMap.get(nouns[j]);
                    value++;
                    hashMap.put(nouns[j],value);
                } else {
                    hashMap.put(nouns[j], 1);
                    linkedList.add(nouns[j]);
                }
            }
        }
    }

    private int partition(LinkedList<String> arr, HashMap<String, Integer> hashMap, int left, int right)
    {
        int i = left, j = right;
        String tmp;
        int pivot = hashMap.get(arr.get((right+left) / 2));

        while (i <= j) {
            while (hashMap.get(arr.get(i)) < pivot)
                i++;
            while (hashMap.get(arr.get(j)) > pivot)
                j--;
            if (i <= j) {
                tmp = arr.get(i);
                arr.set(i, arr.get(j));
                arr.set(j, tmp);
                i++;
                j--;
            }
        };

        return i;
    }
    //  [퀵정렬]
    //  퀵정렬에 사용하기 위한 pivot 을 기준으로 left, right 를 나누는 partition 함수
    public void quickSort(LinkedList<String> linkedList, HashMap<String, Integer> hashMap, int left, int right) {
        int index = partition(linkedList, hashMap, left, right);
        if (left < index - 1)
            quickSort(linkedList, hashMap, left, index - 1);
        if (index < right)
            quickSort(linkedList, hashMap, index, right);
    }
}
