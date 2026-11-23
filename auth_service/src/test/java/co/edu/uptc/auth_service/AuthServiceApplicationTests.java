package co.edu.uptc.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "seguridad.jwt.secret=secreto-de-prueba-con-mas-de-treinta-y-dos-bytes-0123456789")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
