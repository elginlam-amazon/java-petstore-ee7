package org.agoncal.application.petstore.security;

import org.agoncal.application.petstore.view.shopping.CredentialsBean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * @author blep
 *         Date: 12/02/12
 *         Time: 12:29
 */

@Named
public class SimpleCallbackHandler implements CallbackHandler
{

   // ======================================
   // =             Attributes             =
   // ======================================

   @Inject
   //@RequestScoped
   private CredentialsBean credentials;

   // ======================================
   // =          Business methods          =
   // ======================================

   @Override
   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
   {
      for (Callback callback : callbacks)
      {
         switch (callback)
         {
            case NameCallback nameCallback -> nameCallback.setName(credentials.getLogin());
            case PasswordCallback passwordCallback -> passwordCallback.setPassword(credentials.getPassword().toCharArray());
            case null, default -> throw new UnsupportedCallbackException(callback);
         }
      }
   }
}
