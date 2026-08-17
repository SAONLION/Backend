CREATE TABLE `tryon_request` (
                          `tryon_request_id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          `session_id`	VARCHAR(64)	NULL,
                          `sku`	BIGINT	NOT NULL,
                          `size`	VARCHAR(255)	NOT NULL,
                          `color`	VARCHAR(30)	NOT NULL,
                          `requested_at`	DATETIME	NULL,
                          `created_at`	DATETIME	NULL,
                          `updated_at`	DATETIME	NULL
);

CREATE TABLE `purchase_inquiry` (
                          `purchase_inquiry_id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          `session_id`	VARCHAR(64)	NULL,
                          `sku`	BIGINT	NOT NULL,
                          `inquired_at`	DATETIME	NULL,
                          `created_at`	DATETIME	NULL,
                          `updated_at`	DATETIME	NULL
);
