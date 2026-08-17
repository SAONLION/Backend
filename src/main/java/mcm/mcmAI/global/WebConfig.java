package mcm.mcmAI.global;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "https://*.vercel.app",       // Vercel 프리뷰/변경 대응
                        "https://www.tagonai.site",   // 확정된 정식 도메인
                        "https://tagonai.site",        // www 없는 버전도 혹시 몰라
                        "https://api.tagonai.site",    // Swagger UI가 자기 자신(API 서버)을 호출할 때의 origin
                        "http://localhost:5173"        // 로컬 프론트 개발 서버
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
