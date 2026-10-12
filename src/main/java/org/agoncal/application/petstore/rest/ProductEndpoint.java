package org.agoncal.application.petstore.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.agoncal.application.petstore.model.Product;
import org.agoncal.application.petstore.util.Loggable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import java.util.List;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Path("/products")
@Loggable
@Tag(name = "Product")
public class ProductEndpoint
{

   // ======================================
   // =             Attributes             =
   // ======================================

   @PersistenceContext(unitName = "applicationPetstorePU")
   private EntityManager em;

   // ======================================
   // =          Business methods          =
   // ======================================

   @POST
   @Consumes( {"application/xml", "application/json"})
   @Operation(summary = "Creates new product")
   public Response create(Product entity)
   {
      em.persist(entity);
      return Response.created(UriBuilder.fromResource(ProductEndpoint.class).path(String.valueOf(entity.getId())).build()).build();
   }

   @DELETE
   @Path("/{id:[0-9][0-9]*}")
   @Operation(summary = "Deletes a product by id")
   public Response deleteById(@PathParam("id") Long id)
   {
      Product entity = em.find(Product.class, id);
      if (entity == null)
      {
         return Response.status(Status.NOT_FOUND).build();
      }
      em.remove(entity);
      return Response.noContent().build();
   }

   @GET
   @Path("/{id:[0-9][0-9]*}")
   @Produces( {"application/xml", "application/json"})
   @Operation(summary = "Finds a product by id")
   public Response findById(@PathParam("id") Long id)
   {
      TypedQuery<Product> findByIdQuery = em.createQuery("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :entityId ORDER BY p.id", Product.class);
      findByIdQuery.setParameter("entityId", id);
      Product entity;
      try
      {
         entity = findByIdQuery.getSingleResult();
      }
      catch (NoResultException _)
      {
         entity = null;
      }
      if (entity == null)
      {
         return Response.status(Status.NOT_FOUND).build();
      }
      return Response.ok(entity).build();
   }

   @GET
   @Produces( {"application/xml", "application/json"})
   @Operation(summary = "Lists all products")
   public List<Product> listAll(@QueryParam("start") Integer startPosition, @QueryParam("max") Integer maxResult)
   {
      TypedQuery<Product> findAllQuery = em.createQuery("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category ORDER BY p.id", Product.class);
      if (startPosition != null)
      {
         findAllQuery.setFirstResult(startPosition);
      }
      if (maxResult != null)
      {
         findAllQuery.setMaxResults(maxResult);
      }
      final List<Product> results = findAllQuery.getResultList();
      return results;
   }

   @PUT
   @Path("/{id:[0-9][0-9]*}")
   @Consumes( {"application/xml", "application/json"})
   @Operation(summary = "Updates a product")
   public Response update(Product entity)
   {
      try
      {
         entity = em.merge(entity);
      }
      catch (OptimisticLockException e)
      {
         return Response.status(Response.Status.CONFLICT).entity(e.getEntity()).build();
      }

      return Response.noContent().build();
   }
}
