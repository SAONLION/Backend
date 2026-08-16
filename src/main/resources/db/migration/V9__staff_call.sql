CREATE TABLE `staff_call` (
                          `call_id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          `session_id`	VARCHAR(64)	NULL,
                          `product_id`	BIGINT	NOT NULL,
                          `reason`	VARCHAR(200)	NOT NULL,
                          `status`	VARCHAR(20)	NOT NULL DEFAULT 'REQUESTED',
                          `requested_at`	DATETIME	NULL,
                          `created_at`	DATETIME	NULL,
                          `updated_at`	DATETIME	NULL
);