package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.example.model.Item;
import org.example.network.http.JwtAuthenticator;
import org.example.network.http.StoreHttpServer;
import org.example.repository.ItemRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.ItemServiceImpl;
import org.example.service.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class HttpTest extends BasePostgresqlTest {
    private static StoreHttpServer server;

    @BeforeAll
    static void startServer() throws Exception {
        UserRepository userRepository = new UserRepository(connectionProvider);
        JwtService jwtService = new JwtService();
        AuthService authService = new AuthService(userRepository, jwtService);
        JwtAuthenticator authenticator = new JwtAuthenticator(jwtService);
        ItemRepository itemRepository = new ItemRepository(connectionProvider);
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository);

        authService.registerUser("user", "My SUPER secret cybersecure password! cyberops analyst!");
        int port = 8080;
        server = new StoreHttpServer(port, Executors.newFixedThreadPool(10), authService, itemService, authenticator);
        server.start();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @BeforeEach
    void clearItems() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE items RESTART IDENTITY");
        }
    }

    private String getValidToken() {
        String loginJson = """
                {
                    "username": "user",
                    "password": "My SUPER secret cybersecure password! cyberops analyst!"
                }
                """;

        return given()
                .contentType(ContentType.JSON)
                .body(loginJson)
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test
    void testLoginReturnsToken() {
        String loginJson = """
                {
                    "username": "user",
                    "password": "My SUPER secret cybersecure password! cyberops analyst!"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void testInvalidLoginShouldReturn401() {
        String loginJson = """
                {
                    "username": "admin",
                    "password": "wrong_password_haha"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/login")
        .then()
            .statusCode(401)
            .body("error", equalTo("Invalid credentials"));
    }

    @Test
    void testAccessWithoutTokenShouldReturn401() {
        given()
        .when()
            .get("/products/1")
        .then()
            .statusCode(401);
    }

    @Test
    void testCreateProduct() {
        String token = getValidToken();
        
        Item newItem = new Item(null, "Phone", "Electronics", 999.99, 10);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(newItem)
        .when()
            .post("/products")
        .then()
            .statusCode(201)
            .body("id", equalTo(1))
            .body("message", equalTo("Created successfully"));
    }

    @Test
    void testCreatingDuplicateProductShouldReturn409() {
        String token = getValidToken();
        Item item = new Item(null, "Laptop", "PC", 1000.0, 5);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(item)
            .post("/products")
        .then()
            .statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(item)
        .when()
            .post("/products")
        .then()
            .statusCode(409)
            .body("error", equalTo("Item name already exists"));
    }

    @Test
    void testUpdateProduct() {
        String token = getValidToken();
        
        Item item = new Item(null, "name1", "Category", 10.0, 1);
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(item)
            .post("/products");

        Item updateItem = new Item(null, "name2", "Category", 20.0, 5);
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(updateItem)
        .when()
            .put("/products/1")
        .then()
            .statusCode(200)
            .body("message", equalTo("Updated successfully"));

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/products/1")
        .then()
            .statusCode(200)
            .body("name", equalTo("name2"))
            .body("price", equalTo(20.0f));
    }

    @Test
    void testUpdateNonExistentProductShouldReturn404() {
        String token = getValidToken();
        Item updateItem = new Item(null, "who", "None", 10.0, 1);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(updateItem)
                .when()
                .put("/products/9999")
                .then()
                .statusCode(404)
                .body("error", containsString("not found for update"));
    }

    @Test
    void testUpdateProductWithNegativePriceShouldReturn400() {
        String token = getValidToken();

        Item item = new Item(null, "ValidItem", "Category", 10.0, 1);
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(item)
                .post("/products");

        Item invalidUpdate = new Item(null, "ValidItem", "Category", -50.0, 1);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(invalidUpdate)
                .when()
                .put("/products/1")
                .then()
                .statusCode(400)
                .body("error", equalTo("Price cannot be negative or null."));
    }

    @Test
    void testUpdateProductWithDuplicateNameShouldReturn409() {
        String token = getValidToken();

        Item itemA = new Item(null, "Name_A", "Category", 10.0, 1);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(itemA).post("/products");

        Item itemB = new Item(null, "Name_B", "Category", 20.0, 2);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(itemB).post("/products");

        Item conflictingUpdate = new Item(null, "Name_A", "Category", 25.0, 2);
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(conflictingUpdate)
                .when()
                .put("/products/2")
                .then()
                .statusCode(409)
                .body("error", equalTo("Item name already exists"));
    }

    @Test
    void testDeleteProduct() {
        String token = getValidToken();
        
        Item item = new Item(null, "ToDelete", "Category", 10.0, 1);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(item).post("/products");

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/products/1")
        .then()
            .statusCode(200)
            .body("message", equalTo("Deleted successfully"));

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/products/1")
        .then()
            .statusCode(404)
            .body("error", containsString("not found"));
    }

    @Test
    void testDeleteNonExistentProductShouldReturn404() {
        String token = getValidToken();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/products/9999")
                .then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }
}