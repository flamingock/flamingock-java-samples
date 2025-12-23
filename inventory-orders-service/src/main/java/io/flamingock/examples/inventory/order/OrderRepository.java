package io.flamingock.examples.inventory.order;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Repository
public class OrderRepository {

    private static final String DATABASE_NAME = "inventory";
    private static final String COLLECTION_NAME = "orders";

    private final MongoCollection<Document> collection;

    public OrderRepository(MongoClient mongoClient) {
        this.collection = mongoClient.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
    }

    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                orders.add(toOrder(cursor.next()));
            }
        }
        return orders;
    }

    public Optional<Order> findById(String orderId) {
        Document doc = collection.find(eq("orderId", orderId)).first();
        return Optional.ofNullable(doc).map(this::toOrder);
    }

    private Order toOrder(Document doc) {
        List<OrderItem> items = new ArrayList<>();
        List<Document> itemDocs = doc.getList("items", Document.class);
        if (itemDocs != null) {
            for (Document itemDoc : itemDocs) {
                items.add(new OrderItem(
                        itemDoc.getString("productId"),
                        itemDoc.getInteger("quantity"),
                        itemDoc.getDouble("price")
                ));
            }
        }

        return new Order(
                doc.getString("orderId"),
                doc.getString("customerId"),
                items,
                doc.getDouble("total"),
                doc.getString("status"),
                doc.getString("createdAt"),
                doc.getString("discountCode"),
                doc.getBoolean("discountApplied", false)
        );
    }
}
