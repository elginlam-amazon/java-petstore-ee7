package org.agoncal.application.petstore.security;

import org.agoncal.application.petstore.util.ConfigProperty;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * @author blep
 *         Date: 16/02/12
 *         Time: 07:28
 */

public class LoginContextProducer
{

   // ======================================
   // =             Attributes             =
   // ======================================

   @Inject
   private SimpleCallbackHandler callbackHandler;

   @Inject
   private Logger log;

   // ======================================
   // =          Business methods          =
   // ======================================

   @Produces
   public LoginContext produceLoginContext(@ConfigProperty("loginConfigFile") String loginConfigFileName,
                                           @ConfigProperty("loginModuleName") String loginModuleName) throws LoginException, URISyntaxException
   {

      //System.setProperty("java.security.auth.login.config", new File(LoginContextProducer.class.getResource(loginConfigFileName).toURI()).getPath());

      try
      {
         return new LoginContext(loginModuleName, callbackHandler);
      }
      catch (Exception e)
      {
         log.severe("/!\\ Could not create a LoginContext for login module " + loginModuleName + " - " + e.getMessage());
         return null;
      }
   }

}
