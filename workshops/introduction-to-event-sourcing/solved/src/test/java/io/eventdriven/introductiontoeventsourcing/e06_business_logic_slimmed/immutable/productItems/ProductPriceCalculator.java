package io.eventdriven.introductiontoeventsourcing.e06_business_logic_slimmed.immutable.productItems;

import static io.eventdriven.introductiontoeventsourcing.e06_business_logic_slimmed.immutable.productItems.ProductItems.PricedProductItem;
import static io.eventdriven.introductiontoeventsourcing.e06_business_logic_slimmed.immutable.productItems.ProductItems.ProductItem;

public interface ProductPriceCalculator {
  PricedProductItem calculate(ProductItem productItems);
}
