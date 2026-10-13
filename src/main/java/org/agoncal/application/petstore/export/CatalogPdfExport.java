package org.agoncal.application.petstore.export;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Exports the catalog as a PDF with OpenPDF (the com.lowagie iText 2 API; seed for JEE-T11, issue
 * #377 of the version upgrade toolkit). The document carries a creation date, so two exports of the
 * same catalog are byte-different PDFs with the same text.
 */
public final class CatalogPdfExport
{
   private CatalogPdfExport()
   {
   }

   public static byte[] export(List<CatalogRow> rows, Date asOf) throws DocumentException
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, out);
      document.addTitle("Petstore catalog");
      document.addAuthor("Petstore");
      document.addCreationDate();
      document.open();

      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
      Paragraph title = new Paragraph("Petstore catalog", titleFont);
      title.setAlignment(Element.ALIGN_CENTER);
      document.add(title);
      document.add(new Paragraph("As of " + CatalogExcelExport.formatDate(asOf), cellFont));
      document.add(new Paragraph(" ", cellFont));

      PdfPTable table = new PdfPTable(new float[] {3, 4, 2, 2, 2});
      table.setWidthPercentage(100);
      for (String h : new String[] {"Category", "Item", "Unit cost", "Quantity", "Line total"})
      {
         table.addCell(new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10))));
      }
      double grandTotal = 0;
      int items = 0;
      for (CatalogRow row : rows)
      {
         double lineTotal = row.getUnitCost() * row.getQuantity();
         grandTotal += lineTotal;
         items += row.getQuantity();
         table.addCell(new Phrase(row.getCategory(), cellFont));
         table.addCell(new Phrase(row.getItem(), cellFont));
         table.addCell(right(money(row.getUnitCost()), cellFont));
         table.addCell(right(String.valueOf(row.getQuantity()), cellFont));
         table.addCell(right(money(lineTotal), cellFont));
      }
      document.add(table);
      document.add(new Paragraph("Total: " + items + " items, " + money(grandTotal), titleFont));
      document.close();
      return out.toByteArray();
   }

   private static PdfPCell right(String text, Font font)
   {
      PdfPCell cell = new PdfPCell(new Phrase(text, font));
      cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
      return cell;
   }

   private static String money(double v)
   {
      return String.format(Locale.ROOT, "%.2f", v);
   }
}
