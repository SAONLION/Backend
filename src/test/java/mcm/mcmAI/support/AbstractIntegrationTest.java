package mcm.mcmAI.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * @SpringBootTest 클래스가 상속하면, JVM 전체에서 재사용되는 단일 MySQL 컨테이너에 격리된 상태로 연결된다.
 * application.yaml의 spring.datasource.* 를 컨테이너 값으로 덮어써 실제 개발/운영 DB에 연결되는 것을 막는다.
 */
public abstract class AbstractIntegrationTest {

    static final MySQLContainer MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer("mysql:8")
                .withDatabaseName("mcmai_test")
                .withUsername("test")
                .withPassword("test");
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
    }
}
