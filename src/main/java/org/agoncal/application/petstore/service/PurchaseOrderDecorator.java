package org.agoncal.application.petstore.service;

import org.agoncal.application.petstore.service.ComputablePurchaseOrder;
import jakarta.decorator.Decorator;
import jakarta.inject.Inject;
import jakarta.decorator.Delegate;

@Decorator
public abstract class PurchaseOrderDecorator implements ComputablePurchaseOrder
{

   @Inject
   @Delegate
   private ComputablePurchaseOrder delegate;
}