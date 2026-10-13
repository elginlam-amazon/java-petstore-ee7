package org.agoncal.application.petstore.export.poi;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import static org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT;

/**
 * JEE-T10 (#376) seed: Apache POI 3.x style calls that POI 3.17+ no longer has.
 * Every seeded call carries a trailing "T10:<site>" marker; the toolkit's container
 * test reads each marked line before and after the upgrade, and reads the written
 * workbooks back cell by cell (row label = site id, column 1 = the styled cell).
 */
public class ReportStyles {

    /** An application constant passed where POI expects a short code (6 = CENTER_SELECTION). */
    public static final short TOTAL_ALIGN = 6;

    private int next;

    /** Styled cells for the HSSF (.xls) workbook. */
    public void buildHssf(Workbook wb, short bodyAlign, short bodyFill) {
        Sheet sheet = wb.createSheet("styles");
        next = 0;

        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER); // T10:A1
        put(sheet, "A1", s);

        s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.GENERAL); // T10:A2
        put(sheet, "A2", s);

        s = wb.createCellStyle();
       s.setAlignment(HorizontalAlignment.forInt((short) 2)); // T10:A3
        put(sheet, "A3", s);

        s = wb.createCellStyle();
       s.setAlignment(HorizontalAlignment.forInt(bodyAlign)); // T10:A4
        put(sheet, "A4", s);

        s = wb.createCellStyle();
       s.setAlignment(HorizontalAlignment.forInt(TOTAL_ALIGN)); // T10:A5
        put(sheet, "A5", s);

        HSSFCellStyle hs = (HSSFCellStyle) wb.createCellStyle();
        hs.setAlignment(HorizontalAlignment.JUSTIFY); // T10:A6
        put(sheet, "A6", hs);

        hs = (HSSFCellStyle) wb.createCellStyle();
       hs.setAlignment(HorizontalAlignment.forInt((short) 4)); // T10:A7
        put(sheet, "A7", hs);

        s = wb.createCellStyle();
        s.setAlignment(LEFT); // T10:A8
        put(sheet, "A8", s);

        s = wb.createCellStyle();
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND); // T10:F1
        put(sheet, "F1", s);

        s = wb.createCellStyle();
        s.setFillPattern(FillPatternType.FINE_DOTS); // T10:F2
        put(sheet, "F2", s);

        s = wb.createCellStyle();
       s.setFillPattern(FillPatternType.forInt((short) 1)); // T10:F3
        put(sheet, "F3", s);

        s = wb.createCellStyle();
       s.setFillPattern(FillPatternType.forInt(bodyFill)); // T10:F4
        put(sheet, "F4", s);

        hs = (HSSFCellStyle) wb.createCellStyle();
       hs.setFillPattern(FillPatternType.forInt((short) 18)); // T10:F5
        put(sheet, "F5", hs);

        // Control sites: rewrite-apache's own recipes (ReplaceSetBoldweightWithSetBold, ReplaceSetCellType).
        s = wb.createCellStyle();
        Font bold = wb.createFont();
       bold.setBold(true); // T10:C1
        s.setFont(bold);
        put(sheet, "C1", s);

        Row text = sheet.createRow(next++);
        text.createCell(0).setCellValue("C2");
        Cell c2 = text.createCell(1);
       c2.setCellType(CellType.STRING); // T10:C2

        Row number = sheet.createRow(next++);
        number.createCell(0).setCellValue("C3");
        Cell c3 = number.createCell(1);
       c3.setCellType(CellType.NUMERIC); // T10:C3
    }

    /** Styled cells for the XSSF (.xlsx) workbook. */
    public void buildXssf(Workbook wb) {
        Sheet sheet = wb.createSheet("styles");
        next = 0;

        XSSFCellStyle xs = (XSSFCellStyle) wb.createCellStyle();
       xs.setAlignment(HorizontalAlignment.forInt((short) 3)); // T10:X1
        put(sheet, "X1", xs);

        xs = (XSSFCellStyle) wb.createCellStyle();
       xs.setFillPattern(FillPatternType.forInt((short) 1)); // T10:X2
        put(sheet, "X2", xs);

        xs = (XSSFCellStyle) wb.createCellStyle();
        xs.setAlignment(HorizontalAlignment.CENTER); // T10:X3
        put(sheet, "X3", xs);
    }

    private void put(Sheet sheet, String site, CellStyle style) {
        Row row = sheet.createRow(next++);
        row.createCell(0).setCellValue(site);
        row.createCell(1).setCellStyle(style);
    }
}
