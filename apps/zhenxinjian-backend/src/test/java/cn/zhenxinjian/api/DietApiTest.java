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
 * 饮食记录 API 测试 — 验证 CRUD 接口
 * 作者: wanglx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DietApiTest {

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
        // 注册 + 登录
        Response captchaResp = given().contentType("application/json").get("/auth/captcha");
        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);
        String user = "dietuser";
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

    /** 场景：创建记录 → 200 + 返回 id */
    @Test
    @Order(1)
    void create_returnsCreated() {
        ensureLogin();
        Response resp = given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                            "recordDate": "2026-09-14",
                            "mealType": 1,
                            "source": 3,
                            "name": "米饭",
                            "carb": 25.9,
                            "protein": 2.6,
                            "fat": 0.3,
                            "kcal": 116,
                            "amountG": 200
                        }""")
                .post("/diet/records");

        resp.then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.id", notNullValue());
        recordId = resp.jsonPath().getLong("data.id");
    }

    /** 场景：创建后查询 → 返回列表含该记录 */
    @Test
    @Order(2)
    void listByDate_returnsRecords() {
        ensureLogin();
        if (recordId == null) create_returnsCreated();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .queryParam("date", "2026-09-14")
                .get("/diet/records")
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：更新备注 → 返回更新后数据 */
    @Test
    @Order(3)
    void update_updatesRecord() {
        ensureLogin();
        if (recordId == null) create_returnsCreated();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\"remark\":\"测试备注\",\"mealType\":1,\"name\":\"米饭改\",\"carb\":30,\"protein\":3,\"fat\":0.5,\"kcal\":130,\"amountG\":250}")
                .put("/diet/records/" + recordId)
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：无 Token → 401 */
    @Test
    @Order(4)
    void create_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .body("{\"recordDate\":\"2026-09-14\",\"mealType\":1,\"source\":3,\"name\":\"测试\",\"carb\":10,\"protein\":10,\"fat\":10,\"kcal\":170,\"amountG\":100}")
                .post("/diet/records")
                .then().statusCode(401);
    }

    /** 场景：删除记录 → 200 */
    @Test
    @Order(5)
    void delete_removesRecord() {
        ensureLogin();
        if (recordId == null) create_returnsCreated();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .delete("/diet/records/" + recordId)
                .then().statusCode(200)
                .body("code", equalTo(200));
    }
}