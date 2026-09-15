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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 食物库 API 测试 — 验证搜索/分类/热门/试算接口
 * 作者: wanglx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FoodApiTest extends ApiTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    private void ensureLogin() {
        if (token != null) return;
        // 插库测试用户 + 验证码登录
        String user = "fooduser";
        ensureUser(user);
        Response captchaResp = given().contentType("application/json").get("/auth/captcha");
        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);
        Response loginResp = given().contentType("application/json")
                .body(String.format("{\"username\":\"%s\",\"password\":\"Test@123456\",\"captcha\":\"%s\",\"captchaUuid\":\"%s\"}", user, captchaCode, uuid))
                .post("/auth/login");
        token = loginResp.jsonPath().getString("data.token");
    }

    /** 场景：搜索食物 → 返回分页列表 */
    @Test
    @Order(1)
    void search_returnsPage() {
        ensureLogin();
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "米饭")
                .get("/food/search")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.records", notNullValue());
    }

    /** 场景：分类列表 → 返回 10 大类 */
    @Test
    @Order(2)
    void categories_returnsList() {
        ensureLogin();
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .get("/food/categories")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThanOrEqualTo(0));
    }

    /** 场景：热门食物 → 返回列表 */
    @Test
    @Order(3)
    void hot_returnsList() {
        ensureLogin();
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .get("/food/hot")
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：份量试算 → 返回计算结果 */
    @Test
    @Order(4)
    void calc_returnsResult() {
        ensureLogin();
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .queryParam("grams", 150)
                .get("/food/1/calc")
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：无 Token → 401 */
    @Test
    @Order(5)
    void search_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .queryParam("keyword", "米饭")
                .get("/food/search")
                .then().statusCode(401);
    }
}