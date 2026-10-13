package org.agoncal.application.petstore.export;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;

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
      if (value == null)
      {
         return null;
      }
      // Hibernate 7 returns java.time types for native queries without a result mapping;
      // Hibernate 6 and older returned java.sql.Timestamp. Accept both.
      Timestamp timestamp = value instanceof LocalDateTime localDateTime ? Timestamp.valueOf(localDateTime) : (Timestamp) value;
      return new Date(timestamp.getTime());
   }

   public CatalogRow reload(Item item)
   {
      // Jakarta Persistence / Hibernate 7: refresh() of a detached entity throws IllegalArgumentException,
      // so load the managed instance first when the item comes from a view.
      Item managed = em.contains(item) ? item : em.find(Item.class, item.getId());
      if (managed == null)
      {
         throw new EntityNotFoundException("Item " + item.getId() + " no longer exists");
      }
      em.refresh(managed);
      return new CatalogRow(managed.getProduct().getCategory().getName(), managed.getName(), managed.getUnitCost(), 1);
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
