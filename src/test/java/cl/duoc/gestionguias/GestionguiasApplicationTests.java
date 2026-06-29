package cl.duoc.gestionguias;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://dummy.b2clogin.com/dummy/v2.0/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://dummy.b2clogin.com/dummy/discovery/v2.0/keys",
    "aws.accessKeyId=dummy",
    "aws.secretKey=dummy",
    "aws.sessionToken=dummy"
})
class GestionguiasApplicationTests {

    @Test
    void contextLoads() {
    }

}
