package org.agoncal.application.petstore.security;

import org.agoncal.application.petstore.view.shopping.CredentialsBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
         switch (callback) {
            case NameCallback nameCallback:
                nameCallback -> __P__./*__p3__*/voidp();
            case PasswordCallback passwordCallback:
                passwordCallback -> __P__./*__p6__*/voidp();
            case null:
               , default
                -> __P__.<java.lang.Object>/*__p7__*/p()/*__TEMPLATE_STOP__*/
         }
      }
   }
}
