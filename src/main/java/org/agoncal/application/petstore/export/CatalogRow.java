package org.agoncal.application.petstore.export;

import java.io.Serializable;

/**
 * One row of the catalog export (seed for JEE-T11, issue #377 of the version upgrade toolkit).
 */
public class CatalogRow implements Serializable
{
   private final String category;
   private final String item;
   private final double unitCost;
   private final int quantity;

   public CatalogRow(String category, String item, double unitCost, int quantity)
   {
      this.category = category;
      this.item = item;
      this.unitCost = unitCost;
      this.quantity = quantity;
   }

   public String getCategory()
   {
      return category;
   }

   public String getItem()
   {
      return item;
   }

   public double getUnitCost()
   {
      return unitCost;
   }

   public int getQuantity()
   {
      return quantity;
   }
}
