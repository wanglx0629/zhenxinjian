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
 * 身体档案 API 测试 — 验证档案保存/查询接口
 * 作者: wanglx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BodyApiTest {

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
        Response captchaResp = given().contentType("application/json").get("/auth/captcha");
        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);
        String user = "bodyuser";
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

    /** 场景：保存身体档案 → 200 */
    @Test
    @Order(1)
    void saveProfile_returnsOk() {
        ensureLogin();
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                            "gender": 1,
                            "age": 28,
                            "height": 170,
                            "weight": 75,
                            "targetWeight": 65,
                            "activityLevel": 2,
                            "deficit": 300,
                            "cfc": 0.8
                        }""")
                .put("/body/profile")
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：查询已保存的档案 → 200 */
    @Test
    @Order(2)
    void getProfile_returnsProfile() {
        ensureLogin();
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .get("/body/profile")
                .then().statusCode(200)
                .body("code", equalTo(200));
    }

    /** 场景：无 Token → 401 */
    @Test
    @Order(3)
    void getProfile_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .get("/body/profile")
                .then().statusCode(401);
    }
}