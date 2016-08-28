package com.sunghwan.kim;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by khjk310 on 2015-10-23.
 *
 * 어제 날짜 구하는 class
 *
 */
public class DateCalc {
    Calendar cal;

    public DateCalc(){
        cal = new GregorianCalendar();
    }

    public int getYesterdayDate(){
        cal.add(Calendar.DATE, -1); //  전날 날짜 계산
        return cal.get(Calendar.DATE);
    }

    public int getYesterday(){
        cal.add(Calendar.DATE, 0); //  전날 날짜 계산
        //위에서 -1을 또하면 getYesterdayDate를 부르고 또 getYesterday를 부르면서 날짜가 -2로 됨.
        String year = Integer.toString(cal.get(Calendar.YEAR));
        String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
        String day;
        if(cal.get(Calendar.DATE) < 10){
            day = "0" + Integer.toString(cal.get(Calendar.DATE));
        } else {
            day = Integer.toString(cal.get(Calendar.DATE));

        }
        int yesterday = Integer.parseInt(year+month+day);

        return yesterday;
    }
}
