-- 개인화 추천 메일 수신자/발송 이력.
-- V1__init.sql에 테이블만 정의돼 있고 PK 자동 증가·조회 인덱스가 없어 실제 사용이 불가능했으므로 보완한다.

ALTER TABLE `potential_customer` MODIFY COLUMN `pc_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `potential_customer` ADD COLUMN `store_name` VARCHAR(100) NULL;
ALTER TABLE `potential_customer` ADD COLUMN `fail_reason` VARCHAR(255) NULL;

CREATE INDEX `IDX_POTENTIAL_CUSTOMER_SESSION` ON `potential_customer` (`session_id`);
CREATE INDEX `IDX_POTENTIAL_CUSTOMER_SENT_STATUS` ON `potential_customer` (`sent_status`, `created_at`);

ALTER TABLE `potential_customer_product` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

-- 어느 슬롯(고객이 태그한 PICK 4칸 / 추천 2칸)을 어떤 SKU로 채워 보냈는지 남겨,
-- 발송 후에도 메일 본문을 그대로 재현하고 추천 성과를 역추적할 수 있게 한다.
ALTER TABLE `potential_customer_product` ADD COLUMN `slot_type` VARCHAR(20) NOT NULL DEFAULT 'PICK';
ALTER TABLE `potential_customer_product` ADD COLUMN `slot_order` INT NOT NULL DEFAULT 1;
ALTER TABLE `potential_customer_product` ADD COLUMN `product_name` VARCHAR(100) NULL;
ALTER TABLE `potential_customer_product` ADD COLUMN `image_url` VARCHAR(500) NULL;

CREATE INDEX `IDX_POTENTIAL_CUSTOMER_PRODUCT_PC` ON `potential_customer_product` (`pc_id`, `slot_type`, `slot_order`);
