package org.agoncal.application.petstore.export.poi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Characterization driver for the JEE-T10 seed: writes styles.xls and styles.xlsx
 * into the directory given as the only argument. Run before and after the upgrade;
 * the same cells must come back with the same alignment and fill.
 */
public class WriteWorkbooks {

    void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        dir.mkdirs();
        ReportStyles styles = new ReportStyles();

        Workbook xls = new HSSFWorkbook();
        styles.buildHssf(xls, (short) 3, (short) 10);
        write(xls, new File(dir, "styles.xls"));

        Workbook xlsx = new XSSFWorkbook();
        styles.buildXssf(xlsx);
        write(xlsx, new File(dir, "styles.xlsx"));
    }

    private static void write(Workbook wb, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            wb.write(out);
        } finally {
            out.close();
        }
    }
}
