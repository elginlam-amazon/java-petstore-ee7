package org.agoncal.application.petstore.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Characterization driver for the golden-master (seed for JEE-T11, issue #377 of the version
 * upgrade toolkit): writes catalog.xlsx and catalog.pdf for a FIXED catalog and a FIXED as-of date
 * into the folder given as the first argument, so the output of the pre-upgrade build and of the
 * upgraded build can be captured and compared structurally.
 */
public final class GoldenMasterOutputs
{
   private GoldenMasterOutputs()
   {
   }

   public static List<CatalogRow> fixedCatalog()
   {
      List<CatalogRow> rows = new ArrayList<CatalogRow>();
      rows.add(new CatalogRow("Fish", "Angelfish", 10.0, 3));
      rows.add(new CatalogRow("Fish", "Koi", 24.95, 1));
      rows.add(new CatalogRow("Dogs", "Bulldog", 115.5, 2));
      rows.add(new CatalogRow("Cats", "Persian", 89.0, 1));
      return rows;
   }

   public static Date fixedAsOf()
   {
      Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      c.clear();
      c.set(2024, Calendar.JANUARY, 31);
      return c.getTime();
   }

   public static void main(String[] args) throws Exception
   {
      File dir = new File(args.length > 0 ? args[0] : "target/golden-master");
      if (!dir.isDirectory() && !dir.mkdirs())
      {
         throw new IllegalStateException("cannot create " + dir);
      }
      write(new File(dir, "catalog.xlsx"), CatalogExcelExport.export(fixedCatalog(), fixedAsOf()));
      write(new File(dir, "catalog.pdf"), CatalogPdfExport.export(fixedCatalog(), fixedAsOf()));
      IO.println("golden-master outputs written to " + dir);
   }

   private static void write(File f, byte[] bytes) throws Exception
   {
      OutputStream out = new FileOutputStream(f);
      try
      {
         out.write(bytes);
      }
      finally
      {
         out.close();
      }
   }
}
