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
 * 认证 API 测试 — 验证登录/当前用户接口
 * 作者: wanglx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApiTest extends ApiTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static String token;
    private static final String TEST_USER = "apitest1";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    /** 场景：插库用户 → 验证码 → 登录 → 完整认证流程 */
    @Test
    @Order(1)
    void loginAndMe_fullFlow() {
        ensureUser(TEST_USER);

        // 1. 获取验证码
        Response captchaResp = given()
                .contentType("application/json")
                .get("/auth/captcha");
        captchaResp.then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.uuid", notNullValue());

        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);

        // 2. 登录
        Response loginResp = given()
                .contentType("application/json")
                .body(String.format("""
                        {
                            "username": "%s",
                            "password": "%s",
                            "captcha": "%s",
                            "captchaUuid": "%s"
                        }""", TEST_USER, TEST_PASS, captchaCode, uuid))
                .post("/auth/login");

        loginResp.then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.token", notNullValue());

        token = loginResp.jsonPath().getString("data.token");
    }

    /** 场景：无效密码 → 返回错误 */
    @Test
    @Order(2)
    void login_badCredentials_returnsError() {
        // 先获取验证码
        Response captchaResp = given()
                .contentType("application/json")
                .get("/auth/captcha");
        String uuid = captchaResp.jsonPath().getString("data.uuid");
        String captchaCode = redisTemplate.opsForValue().get("zhenxinjian:captcha:" + uuid);

        given()
                .contentType("application/json")
                .body(String.format("""
                        {
                            "username": "%s",
                            "password": "WrongPassword123",
                            "captcha": "%s",
                            "captchaUuid": "%s"
                        }""", TEST_USER, captchaCode, uuid))
                .post("/auth/login")
                .then().statusCode(200)
                .body("code", equalTo(401));
    }

    /** 场景：Token 认证 → 获取当前用户 */
    @Test
    @Order(3)
    void me_withValidToken_returnsUser() {
        if (token == null) {
            loginAndMe_fullFlow();
        }
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .get("/auth/me")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.username", equalTo(TEST_USER));
    }

    /** 场景：无 Token → 401 */
    @Test
    @Order(4)
    void me_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .get("/auth/me")
                .then().statusCode(401);
    }
}