package org.agoncal.application.petstore.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Exports the catalog as an Excel workbook with Apache POI (seed for JEE-T11, issue #377 of the
 * version upgrade toolkit): a merged, bold, centered title, a header row with a fill, a 0.00 price
 * format, a date cell and SUM formulas. These are the cell values and styles an upgrade can change
 * silently.
 */
public final class CatalogExcelExport
{
   private CatalogExcelExport()
   {
   }

   public static byte[] export(List<CatalogRow> rows, Date asOf) throws IOException
   {
      Workbook wb = new XSSFWorkbook();
      try
      {
         CreationHelper helper = wb.getCreationHelper();
         Sheet sheet = wb.createSheet("Catalog");

         Font bold = wb.createFont();
         bold.setBold(true);

         CellStyle title = wb.createCellStyle();
         title.setFont(bold);
         title.setAlignment(HorizontalAlignment.CENTER);

         CellStyle header = wb.createCellStyle();
         header.setFont(bold);
         header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
         header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

         CellStyle money = wb.createCellStyle();
         money.setDataFormat(helper.createDataFormat().getFormat("0.00"));
         money.setAlignment(HorizontalAlignment.RIGHT);

         CellStyle date = wb.createCellStyle();
         date.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));

         Row titleRow = sheet.createRow(0);
         Cell titleCell = titleRow.createCell(0);
         titleCell.setCellValue("Petstore catalog");
         titleCell.setCellStyle(title);
         sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

         Row asOfRow = sheet.createRow(1);
         asOfRow.createCell(0).setCellValue("As of");
         Cell asOfCell = asOfRow.createCell(1);
         asOfCell.setCellValue(asOf);
         asOfCell.setCellStyle(date);

         String[] headers = {"Category", "Item", "Unit cost", "Quantity", "Line total"};
         Row headerRow = sheet.createRow(3);
         for (int c = 0; c < headers.length; c++)
         {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(header);
         }

         int r = 4;
         for (CatalogRow row : rows)
         {
            Row xl = sheet.createRow(r);
            xl.createCell(0).setCellValue(row.getCategory());
            xl.createCell(1).setCellValue(row.getItem());
            Cell cost = xl.createCell(2);
            cost.setCellValue(row.getUnitCost());
            cost.setCellStyle(money);
            xl.createCell(3).setCellValue(row.getQuantity());
            Cell total = xl.createCell(4);
            total.setCellFormula("C" + (r + 1) + "*D" + (r + 1));
            total.setCellStyle(money);
            r++;
         }

         Row totalRow = sheet.createRow(r);
         Cell label = totalRow.createCell(3);
         label.setCellValue("Total");
         label.setCellStyle(header);
         Cell sum = totalRow.createCell(4);
         sum.setCellFormula("SUM(E5:E" + r + ")");
         sum.setCellStyle(money);

         ByteArrayOutputStream out = new ByteArrayOutputStream();
         wb.write(out);
         return out.toByteArray();
      }
      finally
      {
         wb.close();
      }
   }

   static String formatDate(Date d)
   {
      SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
      f.setTimeZone(TimeZone.getTimeZone("UTC"));
      return f.format(d);
   }
}
