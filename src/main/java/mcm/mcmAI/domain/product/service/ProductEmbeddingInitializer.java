package mcm.mcmAI.domain.product.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.global.ai.EmbeddingCodec;
import mcm.mcmAI.global.ai.OpenAiClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 제품 벡터를 1회성으로 채우는 배치. 아직 임베딩이 없는 제품만 골라 채우고, 이미 있는 제품은 다시 호출하지
// 않는다. 매 애플리케이션 기동마다 자동으로 도는 대신 --backfill-product-embeddings 옵션이 있을 때만
// 동작한다 — 매번 기동할 때마다 아직 벡터가 없는 제품이 있으면 계속 호출을 시도하게 되고(예: 로컬에 키가
// 없는 개발/테스트 환경), 특히 테스트에서 OpenAiClient를 목으로 교체해 verifyNoInteractions로 "AI를
// 호출하지 않았다"를 검증하는 케이스와 충돌하기 때문이다. 최초 1회 배포 시 이 옵션을 켜서 실행하면 된다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEmbeddingInitializer implements ApplicationRunner {

    private static final String BACKFILL_OPTION = "backfill-product-embeddings";

    private final ProductRepository productRepository;
    private final OpenAiClient openAiClient;
    private final EmbeddingCodec embeddingCodec;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(BACKFILL_OPTION)) {
            return;
        }

        List<Product> pending = productRepository.findByEmbeddingIsNull();
        if (pending.isEmpty()) {
            log.info("임베딩이 없는 제품이 없어 배치를 건너뜁니다.");
            return;
        }

        log.info("제품 임베딩 생성을 시작합니다. 대상 {}건", pending.size());
        int successCount = 0;
        for (Product product : pending) {
            try {
                Optional<float[]> vector = openAiClient.requestEmbedding(buildEmbeddingText(product));
                if (vector.isEmpty()) {
                    continue;
                }
                product.updateEmbedding(embeddingCodec.encode(vector.get()));
                productRepository.save(product);
                successCount++;

                // 🟢 Rate Limit(429) 방지를 위한 250ms(0.25초)    대기
                Thread.sleep(250);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                log.warn("임베딩 백필 작업이 중단되었습니다.");
                break;
            } catch (Exception e) {
                log.warn("제품 임베딩 생성 실패: productId={}, error={}", product.getProductId(), e.getMessage());
            }
        }
        log.info("제품 임베딩 생성 완료: {}/{}", successCount, pending.size());
    }

    private String buildEmbeddingText(Product product) {
        StringBuilder text = new StringBuilder();
        text.append(product.getName()).append(' ').append(product.getCategory());
        if (product.getMaterialDesc() != null && !product.getMaterialDesc().isBlank()) {
            text.append(' ').append(product.getMaterialDesc());
        }
        if (product.getHeritageDesc() != null && !product.getHeritageDesc().isBlank()) {
            text.append(' ').append(product.getHeritageDesc());
        }
        return text.toString();
    }
}
