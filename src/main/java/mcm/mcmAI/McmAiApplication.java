package mcm.mcmAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class McmAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(McmAiApplication.class, args);
	}

}
