ALTER TABLE `pending_action` ADD COLUMN `trigger_tag_scan_log_id` BIGINT NULL;
ALTER TABLE `pending_action` ADD COLUMN `trigger_interaction_log_id` BIGINT NULL;
ALTER TABLE `pending_action` ADD COLUMN `trigger_id` VARCHAR(20) NULL;
