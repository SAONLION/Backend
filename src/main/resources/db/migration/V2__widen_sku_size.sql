-- 크롤링 데이터의 sku.size는 스타일(색상)별로 선택 가능한 사이즈 목록을 콤마로 합쳐 저장한다.
-- (예: "XS,S,M,L,XL") 기존 VARCHAR(20)로는 부족해 최대 관측치(98자) 기준으로 넓힌다.
ALTER TABLE `sku` MODIFY COLUMN `size` VARCHAR(255) NULL;
