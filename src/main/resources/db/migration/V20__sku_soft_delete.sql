-- delete.md 기준 이미지/스타일 삭제를 물리 삭제 대신 숨김 처리하기 위한 플래그.
-- sku.sku(PK)는 매장에 부착된 실물 태그 ID라 재배번하면 안 되고, sku_image.image_id도
-- 다른 테이블이 참조하지 않으므로 물리 삭제해도 되지만 되돌릴 수 있도록 동일하게 soft delete로 맞춘다.
ALTER TABLE `sku` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `sku_image` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
