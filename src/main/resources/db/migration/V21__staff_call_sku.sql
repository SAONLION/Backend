ALTER TABLE `staff_call` ADD COLUMN `sku` BIGINT NULL;
ALTER TABLE `staff_call` ADD CONSTRAINT `staff_call_sku_fk` FOREIGN KEY (`sku`) REFERENCES `sku` (`sku`);
ALTER TABLE `staff_call` DROP COLUMN `product_id`;
