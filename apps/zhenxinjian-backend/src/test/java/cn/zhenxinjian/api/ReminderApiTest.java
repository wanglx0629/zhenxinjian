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

/**
 * 三餐提醒 API 测试 — 验证查询/保存/时间校验/鉴权
 * 作者: wanglx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReminderApiTest extends ApiTestSupport {

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
        String user = "rmuser";
        ensureUser(user);
        Response captchaResp = given().contentType("application/json").get("/auth/captcha");
        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);
        Response loginResp = given().contentType("application/json")
                .body(String.format(
                        "{\"username\":\"%s\",\"password\":\"Test@123456\",\"captcha\":\"%s\",\"captchaUuid\":\"%s\"}",
                        user, captchaCode, uuid))
                .post("/auth/login");
        token = loginResp.jsonPath().getString("data.token");
    }

    /** 场景：首次查询 → 默认值落库并返回 200 */
    @Test
    @Order(1)
    void get_returnsDefaults() {
        ensureLogin();

        given()
                .header("Authorization", "Bearer " + token)
                .get("/reminder")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.masterSwitch", equalTo(1))
                .body("data.breakfastTime", equalTo("08:30"))
                .body("data.lunchTime", equalTo("12:00"))
                .body("data.dinnerTime", equalTo("18:30"));
    }

    /** 场景：合法设置保存 → 200 且回显新值 */
    @Test
    @Order(2)
    void save_valid_returnsUpdatedSettings() {
        ensureLogin();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\"masterSwitch\":1,\"breakfastSwitch\":0,\"breakfastTime\":\"07:15\","
                        + "\"lunchSwitch\":1,\"lunchTime\":\"12:30\","
                        + "\"dinnerSwitch\":0,\"dinnerTime\":\"19:00\"}")
                .put("/reminder")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.breakfastTime", equalTo("07:15"))
                .body("data.lunchTime", equalTo("12:30"))
                .body("data.dinnerTime", equalTo("19:00"));
    }

    /** 场景：时间格式非法（25:99）→ HTTP 200 包裹业务码 40701 */
    @Test
    @Order(3)
    void save_invalidTime_returnsBusinessCode40701() {
        ensureLogin();

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\"masterSwitch\":1,\"breakfastSwitch\":1,\"breakfastTime\":\"25:99\","
                        + "\"lunchSwitch\":1,\"lunchTime\":\"12:00\","
                        + "\"dinnerSwitch\":1,\"dinnerTime\":\"18:30\"}")
                .put("/reminder")
                .then().statusCode(200)
                .body("code", equalTo(40701));
    }

    /** 场景：无 Token → 401 */
    @Test
    @Order(4)
    void get_withoutToken_returns401() {
        given()
                .get("/reminder")
                .then().statusCode(401);
    }
}
