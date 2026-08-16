CREATE TABLE `pending_action` (
                                  `action_id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                  `session_id`	VARCHAR(64)	NULL,
                                  `blocker_type`	VARCHAR(20)	NOT NULL,
                                  `product_id`	BIGINT	NULL,
                                  `popup_title`	VARCHAR(200)	NOT NULL,
                                  `popup_body`	TEXT	NULL,
                                  `options`	TEXT	NOT NULL,
                                  `status`	VARCHAR(20)	NOT NULL DEFAULT 'PENDING',
                                  `created_at`	DATETIME	NULL,
                                  `updated_at`	DATETIME	NULL
);