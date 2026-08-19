-- finale_backdata/data/catalog.sqlite의 products 테이블에서 아직 sku로 옮겨지지 않은 나머지 두 필드.
-- hardware_text는 V18과 같은 방식(features 배열에서 "하드웨어" 키워드가 들어간 문장만 골라
-- 로더가 적재 시점에 미리 뽑아둔 값)이고, sustainability_certification은
-- product_attributes_json의 attribute_key="sustainability_certification" 값을 그대로 옮긴 것이다.
ALTER TABLE `sku`
    ADD COLUMN `hardware_text`               TEXT          NULL AFTER `handle_drop`,
    ADD COLUMN `sustainability_certification` VARCHAR(255) NULL AFTER `hardware_text`;
