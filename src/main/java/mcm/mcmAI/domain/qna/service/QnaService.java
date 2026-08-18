package mcm.mcmAI.domain.qna.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.qna.dto.QnaRequest;
import mcm.mcmAI.domain.qna.dto.QnaResponse;
import mcm.mcmAI.domain.qna.type.QnaFixedAnswers;
import mcm.mcmAI.domain.qna.type.QuestionType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.global.ai.OpenAiClient;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private static final String SYSTEM_PROMPT = """
            당신은 명품 매장의 AI 어시스턴트입니다. 고객이 자유롭게 남긴 질문에 답변하세요.
            반드시 다음 지침을 지키세요.
            1. 답변은 5문장(5줄) 이내로 간결하게 작성하세요.
            2. 아래 제공되는 상품 컨텍스트(소재 설명, 헤리티지 설명)에 없는 내용은 절대 지어내지 말고, \
            컨텍스트 안에서만 답변하세요. 컨텍스트에 없는 내용을 물으면 아는 범위 내에서만 답하거나 자연스럽게 \
            직원 상담으로 안내하세요.
            3. 가격, 재고, 할인, 배송/반품 세부 정책 등 민감하거나 부정확할 수 있는 질문에는 "모른다"거나 답변을 \
            회피하지 말고, "매장 직원이 더 정확하고 빠르게 도와줄 수 있다"는 긍정적인 톤으로 자연스럽게 직원 상담을 \
            안내하세요. 예를 들어 "그건 답변드리기 어려워요" 대신 "정확한 가격은 매장 직원이 바로 확인해드릴 수 \
            있어요! 직원 호출 버튼을 눌러보시겠어요?"처럼 답하세요.
            4. 답변은 반드시 지정된 고객 언어로 작성하세요.
            5. 다른 설명이나 포맷팅 없이 순수한 답변 텍스트만 출력하세요.""";

    private final ProductRepository productRepository;
    private final SessionRepository sessionRepository;
    private final OpenAiClient openAiClient;

    public QnaResponse answer(Long productId, String sessionId, QnaRequest request) {
        Session session = findSession(sessionId);
        Product product = findProduct(productId);

        QuestionType questionType = QuestionType.from(request.questionType());

        if (questionType == QuestionType.FREE_TEXT) {
            return QnaResponse.of(answerFreeText(product, session.getLanguage(), requireQuestion(request)));
        }

        if (questionType == QuestionType.SHIPPING_RETURN) {
            return QnaResponse.of(QnaFixedAnswers.shippingReturnAnswer(session.getLanguage()));
        }

        return QnaResponse.of(QnaFixedAnswers.fixedAnswer(questionType));
    }

    private String requireQuestion(QnaRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_QNA_QUESTION);
        }
        return request.question();
    }

    private String answerFreeText(Product product, String language, String question) {
        try {
            String userPrompt = buildUserPrompt(product, language, question);
            return openAiClient.requestChatCompletion(SYSTEM_PROMPT, userPrompt, false)
                    .map(String::trim)
                    .filter(answer -> !answer.isBlank())
                    .orElseGet(() -> QnaFixedAnswers.freeTextFallbackAnswer(language));
        } catch (Exception e) {
            log.warn("자유 질문 답변 생성에 실패해 fallback 답변을 반환합니다: {}", e.getMessage());
            return QnaFixedAnswers.freeTextFallbackAnswer(language);
        }
    }

    private String buildUserPrompt(Product product, String language, String question) {
        return "고객 언어 코드: " + normalizeLanguage(language) + "\n"
                + "상품 소재 설명: " + nullToEmpty(product.getMaterialDesc()) + "\n"
                + "상품 헤리티지 설명: " + nullToEmpty(product.getHeritageDesc()) + "\n"
                + "고객 질문: " + question;
    }

    private String nullToEmpty(String value) {
        return value == null || value.isBlank() ? "(정보 없음)" : value;
    }

    private String normalizeLanguage(String language) {
        return language == null || language.isBlank() ? QnaFixedAnswers.DEFAULT_LANGUAGE : language;
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
