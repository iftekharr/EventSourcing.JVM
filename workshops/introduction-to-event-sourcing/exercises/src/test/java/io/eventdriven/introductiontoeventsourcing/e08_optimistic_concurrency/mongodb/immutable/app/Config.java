package io.eventdriven.introductiontoeventsourcing.e08_optimistic_concurrency.mongodb.immutable.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import io.eventdriven.eventstores.mongodb.MongoDBEventStore;
import io.eventdriven.eventstores.mongodb.config.NativeMongoConfig;
import io.eventdriven.introductiontoeventsourcing.e08_optimistic_concurrency.core.http.GlobalExceptionHandler;
import io.eventdriven.introductiontoeventsourcing.e08_optimistic_concurrency.core.serializer.DefaultSerializer;
import io.eventdriven.introductiontoeventsourcing.e08_optimistic_concurrency.mongodb.immutable.app.shoppingcarts.ShoppingCartStore;
import io.eventdriven.introductiontoeventsourcing.e08_optimistic_concurrency.mongodb.immutable.app.shoppingcarts.productItems.FakeProductPriceCalculator;
import io.eventdriven.introductiontoeventsourcing.e08_optimistic_concurrency.mongodb.immutable.app.shoppingcarts.productItems.ProductPriceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.annotation.ApplicationScope;

import static io.eventdriven.eventstores.mongodb.MongoDBEventStore.Storage;

@Configuration
class Config {
  @Bean
  ObjectMapper defaultJSONMapper() {
    return DefaultSerializer.mapper;
  }

  @Bean
  @Scope("singleton")
  MongoClient mongoDBClient(@Value("${mongodb.connectionstring}") String connectionString) {
    return NativeMongoConfig.createClient(connectionString);
  }

  @Bean
  @Scope("singleton")
  MongoDBEventStore eventStore(MongoClient mongoClient) {
    return MongoDBEventStore.with(Storage.EventAsDocument, mongoClient, "e08_optimistic_concurrency_mongodb_immutable");
  }

  @Bean
  @ApplicationScope
  ProductPriceCalculator productPriceCalculator() {
    return FakeProductPriceCalculator.returning(100);
  }


  @Bean
  @Scope("singleton")
  public static ShoppingCartStore shoppingCartStore(MongoDBEventStore eventStore) {
    return new ShoppingCartStore(eventStore);
  }

  @Primary
  @Bean
  public GlobalExceptionHandler restResponseEntityExceptionHandler() {
    return new GlobalExceptionHandler();
  }
}
