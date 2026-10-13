package org.agoncal.application.petstore.export;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.agoncal.application.petstore.model.Item;

/**
 * Builds the catalog export from the database (seed for JEE-T11, issue #377 of the version upgrade
 * toolkit). The native query's date column is read as java.sql.Timestamp, which Hibernate 7 returns
 * as java.time.LocalDateTime instead (a silent break the runtime checklist names), and the reload of
 * an item handed in from a view is a refresh() on a possibly detached entity.
 */
@Stateless
@LocalBean
public class CatalogExportService
{
   @PersistenceContext(unitName = "applicationPetstorePU")
   private EntityManager em;

   public Date lastCatalogChange()
   {
      Object value = em.createNativeQuery("select max(CURRENT_TIMESTAMP) from Item").getSingleResult();
      return value == null ? null : new Date(((Timestamp) value).getTime());
   }

   public CatalogRow reload(Item item)
   {
      em.refresh(item);
      return new CatalogRow(item.getProduct().getCategory().getName(), item.getName(), item.getUnitCost(), 1);
   }

   public List<CatalogRow> catalog()
   {
      List<CatalogRow> rows = new ArrayList<CatalogRow>();
      for (Item item : em.createNamedQuery(Item.FIND_ALL, Item.class).getResultList())
      {
         rows.add(new CatalogRow(item.getProduct().getCategory().getName(), item.getName(), item.getUnitCost(), 1));
      }
      return rows;
   }
}
