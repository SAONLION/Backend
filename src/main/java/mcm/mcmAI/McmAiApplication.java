package mcm.mcmAI;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class McmAiApplication {

	public static void main(String[] args) {
		// 실행 환경(Docker TZ, JVM 옵션 등)에 의존하지 않고 LocalDateTime.now()가 항상 KST로 채워지도록 명시적으로 고정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(McmAiApplication.class, args);
	}

}
