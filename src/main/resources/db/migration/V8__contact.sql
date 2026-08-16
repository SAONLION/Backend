CREATE TABLE `contact` (
                          `contact_id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          `session_id`	VARCHAR(64)	NULL,
                          `action_id`	BIGINT	NULL,
                          `product_id`	BIGINT	NULL,
                          `email`	VARCHAR(100)	NOT NULL,
                          `content_topic`	VARCHAR(200)	NULL,
                          `content_sent`	BOOLEAN	NOT NULL DEFAULT FALSE,
                          `sent_at`	DATETIME	NULL,
                          `created_at`	DATETIME	NULL,
                          `updated_at`	DATETIME	NULL
);