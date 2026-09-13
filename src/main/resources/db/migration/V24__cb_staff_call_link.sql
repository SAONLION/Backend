ALTER TABLE `pending_action` ADD COLUMN `sku` BIGINT NULL;
ALTER TABLE `staff_call` ADD COLUMN `pending_action_id` BIGINT NULL;
ALTER TABLE `staff_call` ADD CONSTRAINT `staff_call_pending_action_uk` UNIQUE (`pending_action_id`);
