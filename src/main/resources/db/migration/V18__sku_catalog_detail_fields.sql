-- finale_backdata/data/catalog.sqlite의 products 테이블(스타일 단위 원본 카탈로그 데이터)에는
-- 있지만 sku 테이블로 옮겨지지 않았던 필드들. 색상/사이즈 조합인 sku(=style_number) 단위로
-- 정확히 매칭되는 값이라 product(변형 그룹 대표 스타일)가 아닌 sku에 붙인다.
-- storage_text/lining_care_text는 원본 features 배열에서 "포켓/수납"·"안감" 키워드가 들어간
-- 문장만 골라 로더(scripts/load_products.py)가 적재 시점에 미리 뽑아둔 값이다.
ALTER TABLE `sku`
    ADD COLUMN `description`        LONGTEXT      NULL AFTER `style_number`,
    ADD COLUMN `short_description`  TEXT          NULL AFTER `description`,
    ADD COLUMN `body_material`      VARCHAR(255)  NULL AFTER `short_description`,
    ADD COLUMN `trim_material`      VARCHAR(255)  NULL AFTER `body_material`,
    ADD COLUMN `country_of_origin`  VARCHAR(100)  NULL AFTER `trim_material`,
    ADD COLUMN `dimensions_text`    VARCHAR(255)  NULL AFTER `country_of_origin`,
    ADD COLUMN `storage_text`       TEXT          NULL AFTER `dimensions_text`,
    ADD COLUMN `lining_care_text`   TEXT          NULL AFTER `storage_text`,
    ADD COLUMN `strap_length`       VARCHAR(64)   NULL AFTER `lining_care_text`,
    ADD COLUMN `handle_drop`        VARCHAR(64)   NULL AFTER `strap_length`;
