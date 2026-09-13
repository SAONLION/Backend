ALTER TABLE `staff_call` ADD COLUMN `size` VARCHAR(255) NULL;
ALTER TABLE `staff_call` ADD COLUMN `tryon_request_id` BIGINT NULL;
ALTER TABLE `staff_call` ADD COLUMN `purchase_inquiry_id` BIGINT NULL;
ALTER TABLE `staff_call` ADD CONSTRAINT `staff_call_tryon_request_uk` UNIQUE (`tryon_request_id`);
ALTER TABLE `staff_call` ADD CONSTRAINT `staff_call_purchase_inquiry_uk` UNIQUE (`purchase_inquiry_id`);
