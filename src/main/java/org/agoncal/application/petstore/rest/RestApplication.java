package org.agoncal.application.petstore.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@ApplicationPath("/rest")
@OpenAPIDefinition(
   info = @Info(title = "Petstore APIs", version = "1.0.0", description = "PetStore APIs exposed",
      contact = @Contact(name = "Antonio Goncalves", url = "https://antoniogoncalves.org/", email = "antonio.goncalves@gmail.com")),
   servers = {@Server(url = "http://localhost:8080/applicationPetstore/rest"), @Server(url = "https://localhost:8080/applicationPetstore/rest")})
public class RestApplication extends Application
{
   // ======================================
   // =          Business methods          =
   // ======================================

   // TODO Not sure this is still needed for portability in EE 7
//   @Override
//   public Set<Class<?>> getClasses()
//   {
//      Set<Class<?>> classes = new HashSet<>();
//      classes.add(CategoryEndpoint.class);
//      classes.add(CountryEndpoint.class);
//      classes.add(CustomerEndpoint.class);
//      classes.add(ItemEndpoint.class);
//      classes.add(ProductEndpoint.class);
//      return classes;
//   }
}