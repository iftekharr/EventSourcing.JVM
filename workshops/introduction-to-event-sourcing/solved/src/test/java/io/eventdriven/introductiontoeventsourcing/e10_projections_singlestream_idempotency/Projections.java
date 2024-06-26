package io.eventdriven.introductiontoeventsourcing.e10_projections_singlestream_idempotency;

import io.eventdriven.introductiontoeventsourcing.e10_projections_singlestream_idempotency.ProjectionsTests.PricedProductItem;
import io.eventdriven.introductiontoeventsourcing.e10_projections_singlestream_idempotency.ProjectionsTests.ShoppingCartStatus;
import io.eventdriven.introductiontoeventsourcing.e10_projections_singlestream_idempotency.tools.Database;
import io.eventdriven.introductiontoeventsourcing.e10_projections_singlestream_idempotency.tools.EventEnvelopeBase.EventEnvelope;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.eventdriven.introductiontoeventsourcing.e10_projections_singlestream_idempotency.ProjectionsTests.ShoppingCartEvent.*;

public class Projections {
  public interface Versioned {
    long getVersion();

    void setVersion(long version);
  }

  public static class ShoppingCartDetails implements Versioned {
    private UUID id;
    private UUID clientId;
    private ShoppingCartStatus status;
    private List<PricedProductItem> productItems;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime canceledAt;
    private double totalAmount;
    private double totalItemsCount;
    private long version;

    public ShoppingCartDetails() {
    }

    public ShoppingCartDetails(UUID id, UUID clientId, ShoppingCartStatus status, List<PricedProductItem> productItems, OffsetDateTime confirmedAt, OffsetDateTime canceledAt, double totalAmount, double totalItemsCount, long version) {
      this.id = id;
      this.clientId = clientId;
      this.status = status;
      this.productItems = productItems;
      this.confirmedAt = confirmedAt;
      this.canceledAt = canceledAt;
      this.totalAmount = totalAmount;
      this.totalItemsCount = totalItemsCount;
      this.version = version;
    }

    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public UUID getClientId() {
      return clientId;
    }

    public void setClientId(UUID clientId) {
      this.clientId = clientId;
    }

    public ShoppingCartStatus getStatus() {
      return status;
    }

    public void setStatus(ShoppingCartStatus status) {
      this.status = status;
    }

    public List<PricedProductItem> getProductItems() {
      return productItems;
    }

    public void setProductItems(List<PricedProductItem> productItems) {
      this.productItems = productItems;
    }

    public OffsetDateTime getConfirmedAt() {
      return confirmedAt;
    }

    public void setConfirmedAt(OffsetDateTime confirmedAt) {
      this.confirmedAt = confirmedAt;
    }

    public OffsetDateTime getCanceledAt() {
      return canceledAt;
    }

    public void setCanceledAt(OffsetDateTime canceledAt) {
      this.canceledAt = canceledAt;
    }

    public double getTotalAmount() {
      return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
      this.totalAmount = totalAmount;
    }

    public void addTotalAmount(double totalAmount) {
      this.totalAmount += totalAmount;
    }

    public double getTotalItemsCount() {
      return totalItemsCount;
    }

    public void setTotalItemsCount(double totalItemsCount) {
      this.totalItemsCount = totalItemsCount;
    }

    public void addTotalItemsCount(double totalItemsCount) {
      this.totalItemsCount += totalItemsCount;
    }

    @Override
    public long getVersion() {
      return version;
    }

    @Override
    public void setVersion(long version) {
      this.version = version;
    }
  }

  public static class ShoppingCartDetailsProjection {
    private final Database database;

    public ShoppingCartDetailsProjection(Database database) {
      this.database = database;
    }

    public void handleOpened(EventEnvelope<ShoppingCartOpened> event) {
      database.store(
        ShoppingCartDetails.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        new ShoppingCartDetails(
          event.data().shoppingCartId(),
          event.data().clientId(),
          ShoppingCartStatus.Pending,
          new ArrayList<>(),
          null,
          null,
          0,
          0,
          event.metadata().streamPosition())
      );
    }

    public void handleProductAdded(EventEnvelope<ProductItemAddedToShoppingCart> event) {
      database.getAndUpdate(ShoppingCartDetails.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        item -> {
          var productItem = event.data().productItem();

          item.getProductItems().stream()
            .filter(pi -> pi.productId().equals(productItem.productId()))
            .findAny()
            .ifPresentOrElse(
              current -> item.getProductItems().set(
                item.getProductItems().indexOf(current),
                new PricedProductItem(current.productId(), current.quantity() + productItem.quantity(), current.unitPrice())
              ),
              () -> item.getProductItems().add(productItem)
            );

          item.addTotalAmount(productItem.totalAmount());
          item.addTotalItemsCount(productItem.quantity());

          return item;
        });
    }

    public void handleProductRemoved(EventEnvelope<ProductItemRemovedFromShoppingCart> event) {
      database.getAndUpdate(ShoppingCartDetails.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        item -> {
          var productItem = event.data().productItem();

          item.getProductItems().stream()
            .filter(pi -> pi.productId().equals(productItem.productId()))
            .findAny()
            .ifPresent(
              current -> item.getProductItems().set(
                item.getProductItems().indexOf(current),
                new PricedProductItem(current.productId(), current.quantity() - productItem.quantity(), current.unitPrice())
              )
            );

          item.addTotalAmount(-productItem.totalAmount());
          item.addTotalItemsCount(-productItem.quantity());

          return item;
        });
    }

    public void handleConfirmed(EventEnvelope<ShoppingCartConfirmed> event) {
      database.getAndUpdate(ShoppingCartDetails.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        item -> {
          item.setStatus(ShoppingCartStatus.Confirmed);
          item.setConfirmedAt(event.data().confirmedAt());

          return item;
        });
    }


    public void handleCanceled(EventEnvelope<ShoppingCartCanceled> event) {
      database.getAndUpdate(ShoppingCartDetails.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        item -> {
          item.setStatus(ShoppingCartStatus.Canceled);
          item.setCanceledAt(event.data().canceledAt());

          return item;
        });
    }
  }

  public static class ShoppingCartShortInfo implements Versioned {
    private UUID id;
    private UUID clientId;
    private double totalAmount;
    private double totalItemsCount;
    private long version;

    public ShoppingCartShortInfo() {
    }

    public ShoppingCartShortInfo(UUID id, UUID clientId, double totalAmount, double totalItemsCount, long version) {
      this.id = id;
      this.clientId = clientId;
      this.totalAmount = totalAmount;
      this.totalItemsCount = totalItemsCount;
      this.version = version;
    }

    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public UUID getClientId() {
      return clientId;
    }

    public void setClientId(UUID clientId) {
      this.clientId = clientId;
    }

    public double getTotalAmount() {
      return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
      this.totalAmount = totalAmount;
    }

    public void addTotalAmount(double totalAmount) {
      this.totalAmount += totalAmount;
    }

    public double getTotalItemsCount() {
      return totalItemsCount;
    }

    public void setTotalItemsCount(double totalItemsCount) {
      this.totalItemsCount = totalItemsCount;
    }

    public void addTotalItemsCount(double totalItemsCount) {
      this.totalItemsCount += totalItemsCount;
    }

    @Override
    public long getVersion() {
      return version;
    }

    @Override
    public void setVersion(long version) {
      this.version = version;
    }
  }

  public static class ShoppingCartShortInfoProjection {
    private final Database database;

    public ShoppingCartShortInfoProjection(Database database) {
      this.database = database;
    }

    public void handleOpened(EventEnvelope<ShoppingCartOpened> event) {
      database.store(ShoppingCartShortInfo.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        new ShoppingCartShortInfo(
          event.data().shoppingCartId(),
          event.data().clientId(),
          0,
          0,
          event.metadata().streamPosition()
        )
      );
    }

    public void handleProductAdded(EventEnvelope<ProductItemAddedToShoppingCart> event) {
      database.getAndUpdate(ShoppingCartShortInfo.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        item -> {
          var productItem = event.data().productItem();

          item.addTotalAmount(productItem.totalAmount());
          item.addTotalItemsCount(productItem.quantity());

          return item;
        });
    }

    public void handleProductRemoved(EventEnvelope<ProductItemRemovedFromShoppingCart> event) {
      database.getAndUpdate(ShoppingCartShortInfo.class, event.data().shoppingCartId(), event.metadata().streamPosition(),
        item -> {
          var productItem = event.data().productItem();

          item.addTotalAmount(-productItem.totalAmount());
          item.addTotalItemsCount(-productItem.quantity());

          return item;
        });
    }

    public void handleConfirmed(EventEnvelope<ShoppingCartConfirmed> event) {
      database.delete(ShoppingCartShortInfo.class, event.data().shoppingCartId());
    }


    public void handleCanceled(EventEnvelope<ShoppingCartCanceled> event) {
      database.delete(ShoppingCartShortInfo.class, event.data().shoppingCartId());
    }
  }
}
