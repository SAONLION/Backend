package mcm.mcmAI.global;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 메일 발송처럼 응답 지연 없이 백그라운드로 넘겨도 되는 작업을 위한 비동기 실행 설정.
 * SMTP 호출은 초 단위로 걸릴 수 있어, 요청 스레드(톰캣 워커)를 잡아두지 않기 위해 전용
 * 스레드풀을 둔다. 기본 SimpleAsyncTaskExecutor(호출마다 새 스레드 생성)를 그대로 쓰면
 * 발송량이 몰릴 때 스레드가 무한정 늘어날 수 있어 풀 크기를 제한한다.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "mailExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // @Async void 메서드는 예외가 호출자에게 전달되지 않는다. 각 비동기 메서드가
        // 실패를 자체적으로 처리(FAILED 기록)하는 것이 정책이므로, 여기까지 올라오는 예외는
        // 그 처리 로직 자체의 버그일 가능성이 높아 놓치지 않도록 별도로 남긴다.
        return (throwable, method, params) ->
                log.error("비동기 작업 처리 중 처리되지 않은 예외가 발생했습니다 - method={}", method.getName(), throwable);
    }
}
