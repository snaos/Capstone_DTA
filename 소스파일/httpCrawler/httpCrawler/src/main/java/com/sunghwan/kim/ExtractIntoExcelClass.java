package com.sunghwan.kim;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kim on 2015-11-07.
 */
public class ExtractIntoExcelClass {

    Connection con = null;
    int yesterday=0;
    public ExtractIntoExcelClass(Connection con, int yesterday){
        this.con = con;
        this.yesterday = yesterday;
    }

    public void extractIntoExcel() throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String snSQL = "SELECT NEWS.NEWS_NO, NEWS.NEWS_TITLE, NEWS.NEWS_CONTENT, NEWS.NEWS_CO, NEWS_ANALYSIS.NEWS_TOPIC FROM NEWS, NEWS_ANALYSIS WHERE NEWS.NEWS_NO=NEWS_ANALYSIS.NEWS_NO AND NEWS.NEWS_YYYYDDMM=?";
        pstmt = con.prepareStatement(snSQL);
        pstmt.setString(1, String.valueOf(yesterday));
        rs = pstmt.executeQuery();

        //엑셀 객체
        Workbook xlsxWb = new XSSFWorkbook();

        //시트 생성
        Sheet sheet1 = xlsxWb.createSheet();

        //시트 설정
        sheet1.setColumnWidth(0, 10000);

        Row row = null;
        Cell cell = null;
        int rowNum = 0;
        int cellNum;

        //불러온 결과 저장
        while(rs.next()){
            cellNum = 0;
            row = sheet1.createRow(rowNum);
            cell = row.createCell(cellNum);
            cell.setCellValue(rs.getString("NEWS_NO"));
            cellNum++;

            cell = row.createCell(cellNum);
            cell.setCellValue(rs.getString("NEWS_TITLE"));
            cellNum++;

            cell = row.createCell(cellNum);
            cell.setCellValue(rs.getString("NEWS_CONTENT"));
            cellNum++;

            cell = row.createCell(cellNum);
            cell.setCellValue(rs.getString("NEWS_CO"));
            cellNum++;

            cell = row.createCell(cellNum);
            cell.setCellValue(rs.getString("NEWS_TOPIC"));

            rowNum++;
        }
        try {
            System.out.println("엑셀 파일 저장");
            File xlsFile = new File("./testExcel.xls");
            FileOutputStream fileOut = new FileOutputStream(xlsFile);
            xlsxWb.write(fileOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
