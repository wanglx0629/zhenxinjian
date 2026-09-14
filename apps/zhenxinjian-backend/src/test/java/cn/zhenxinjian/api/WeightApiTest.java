package cn.zhenxinjian.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 体重记录 API 测试 — 验证记录/查询/删除接口
 * 作者: wanglx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeightApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static String token;
    private static Long recordId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    private void ensureLogin() {
        if (token != null) return;
        Response captchaResp = given().contentType("application/json").get("/auth/captcha");
        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);
        String user = "wtuser";
        given().contentType("application/json")
                .body(String.format("{\"username\":\"%s\",\"password\":\"Test@123456\",\"captcha\":\"%s\",\"captchaUuid\":\"%s\"}", user, captchaCode, uuid))
                .post("/auth/register");

        captchaResp = given().contentType("application/json").get("/auth/captcha");
        uuid = captchaResp.jsonPath().getString("data.uuid");
        captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);
        Response loginResp = given().contentType("application/json")
                .body(String.format("{\"username\":\"%s\",\"password\":\"Test@123456\",\"captcha\":\"%s\",\"captchaUuid\":\"%s\"}", user, captchaCode, uuid))
                .post("/auth/login");
        token = loginResp.jsonPath().getString("data.token");
    }

    /** 场景：保存体重记录 → 200 */
    @Test
    @Order(1)
    void save_returnsOk() {
        ensureLogin();
        Response resp = given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\"recordDate\":\"2026-09-14\",\"weight\":70.5}")
                .put("/weight")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.id", notNullValue())
                .extract().response();

        recordId = resp.jsonPath().getLong("data.id");
    }

    /** 场景：查询体重列表 → 200 */
    @Test
    @Order(2)
    void list_returnsRecords() {
        ensureLogin();
        if (recordId == null) save_returnsOk();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .get("/weight")
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：删除 → 200 */
    @Test
    @Order(3)
    void delete_returnsOk() {
        ensureLogin();
        if (recordId == null) save_returnsOk();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .delete("/weight/" + recordId)
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：无 Token → 401 */
    @Test
    @Order(4)
    void save_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .body("{\"recordDate\":\"2026-09-14\",\"weight\":70.5}")
                .put("/weight")
                .then().statusCode(401);
    }
}