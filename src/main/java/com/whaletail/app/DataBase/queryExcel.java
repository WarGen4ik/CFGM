package com.whaletail.app.DataBase;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

/**
 * Created by Admin on 29.05.2017.
 */
public class queryExcel {
    private ArrayList<String> queries;
    private static final String QUERIES = "Translate.xls";

    public queryExcel(String lang, String last_query) {
        queries = new ArrayList<>();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(QUERIES);
            Workbook workbook = new HSSFWorkbook(fis);
            String str = null;
            int i = 0;
            do {
                try {
                    str = workbook.getSheetAt(0).getRow(0).getCell(i).getStringCellValue();
                } catch (NullPointerException e){ str = null; }
                if (str != null){
                    i = 0;
                    break;
                }
                if (str.equals(lang)) {
                    break;
                }
                i++;
            } while (true);
            int j = 1;
            boolean isAfterLastQuery = false;
            do {
                if (last_query.equals("START")) {
                    try {
                        str = workbook.getSheetAt(0).getRow(j).getCell(i).getStringCellValue();
                        queries.add(str);
                    } catch (NullPointerException e) { str = null; }
                } else {
                    try {
                        str = workbook.getSheetAt(0).getRow(j).getCell(i).getStringCellValue();
                        if (str.equals(last_query)){
                            isAfterLastQuery = true;
                        }
                    } catch (NullPointerException e) { str = null; }
                    if (isAfterLastQuery){
                        if (str != null)
                            queries.add(str);
                    }
                }
                j++;
            } while (str != null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public ArrayList<String> getList(){ return queries; }
}
