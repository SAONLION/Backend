package mcm.mcmAI.domain.purchaseinquiry.service;

import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.purchaseinquiry.dto.PurchaseInquiryRequest;
import mcm.mcmAI.domain.purchaseinquiry.dto.PurchaseInquiryResponse;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
import mcm.mcmAI.domain.purchaseinquiry.repository.PurchaseInquiryRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseInquiryService {

    private final PurchaseInquiryRepository purchaseInquiryRepository;
    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public PurchaseInquiryResponse createPurchaseInquiry(String sessionId, PurchaseInquiryRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Sku sku = skuRepository.findById(request.sku())
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

        PurchaseInquiry purchaseInquiry = PurchaseInquiry.builder()
                .session(session)
                .sku(sku)
                .build();

        return PurchaseInquiryResponse.from(purchaseInquiryRepository.save(purchaseInquiry));
    }
}
