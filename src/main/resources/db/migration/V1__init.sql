CREATE TABLE `session` (
                           `session_id`	VARCHAR(64)	NOT NULL,
                           `nickname`	VARCHAR(50)	NULL,
                           `language`	VARCHAR(5)	NULL,
                           `intent_score`	INT	NULL,
                           `last_activity_at`	DATETIME	NULL,
                           `ended_at`	DATETIME	NULL,
                           `end_reason`	VARCHAR(20)	NULL,
                           `created_at`	DATETIME	NULL,
                           `updated_at`	DATETIME	NULL
);

CREATE TABLE `product` (
                           `product_id`	BIGINT	NOT NULL,
                           `name`	VARCHAR(100)	NULL,
                           `category`	VARCHAR(50)	NULL,
                           `material_desc`	TEXT	NULL,
                           `heritage_desc`	TEXT	NULL,
                           `created_at`	DATETIME	NULL,
                           `updated_at`	DATETIME	NULL
);

CREATE TABLE `product_translation` (
                                       `id`	BIGINT	NOT NULL,
                                       `product_id`	BIGINT	NULL,
                                       `language`	VARCHAR(5)	NULL,
                                       `name`	VARCHAR(100)	NULL,
                                       `material_desc`	TEXT	NULL,
                                       `heritage_desc`	TEXT	NULL,
                                       `created_at`	DATETIME	NULL,
                                       `updated_at`	DATETIME	NULL
);

CREATE TABLE `sku` (
                       `sku`	BIGINT	NOT NULL,
                       `product_id`	BIGINT	NULL,
                       `color`	VARCHAR(30)	NULL,
                       `size`	VARCHAR(20)	NULL,
                       `price`	INT	NULL,
                       `stock_qty`	INT	NULL,
                       `created_at`	DATETIME	NULL,
                       `updated_at`	DATETIME	NULL
);

CREATE TABLE `product_image` (
                                 `image_id`	BIGINT	NOT NULL,
                                 `sku`	BIGINT	NULL,
                                 `image_url`	VARCHAR(500)	NULL,
                                 `sort_order`	INT	NULL,
                                 `created_at`	DATETIME	NULL,
                                 `updated_at`	DATETIME	NULL
);

CREATE TABLE `tag_scan_log` (
                                `scan_id`	BIGINT	NOT NULL,
                                `session_id`	VARCHAR(64)	NULL,
                                `sku`	BIGINT	NULL,
                                `scan_order`	INT	NULL,
                                `scanned_at`	DATETIME	NULL,
                                `created_at`	DATETIME	NULL,
                                `updated_at`	DATETIME	NULL
);

CREATE TABLE `interaction_log` (
                                   `interaction_id`	BIGINT	NOT NULL,
                                   `session_id`	VARCHAR(64)	NULL,
                                   `sku`	BIGINT	NULL,
                                   `interest_type`	VARCHAR(30)	NULL,
                                   `sub_option`	VARCHAR(50)	NULL,
                                   `created_at`	DATETIME	NULL,
                                   `updated_at`	DATETIME	NULL
);

CREATE TABLE `inquiry` (
                           `inquiry_id`	BIGINT	NOT NULL,
                           `session_id`	VARCHAR(64)	NULL,
                           `sku`	BIGINT	NULL,
                           `inquiry_type`	VARCHAR(30)	NULL,
                           `is_blocked`	BOOLEAN	NULL,
                           `created_at`	DATETIME	NULL,
                           `updated_at`	DATETIME	NULL
);

CREATE TABLE `sa_call` (
                           `call_id`	BIGINT	NOT NULL,
                           `session_id`	VARCHAR(64)	NULL,
                           `inquiry_id`	BIGINT	NULL,
                           `trigger_reason`	VARCHAR(30)	NULL,
                           `status`	VARCHAR(20)	NULL,
                           `wait_time_sec`	INT	NULL,
                           `created_at`	DATETIME	NULL,
                           `updated_at`	DATETIME	NULL
);

CREATE TABLE `visit_purpose` (
                                 `purpose_id`	BIGINT	NOT NULL,
                                 `session_id`	VARCHAR(64)	NULL,
                                 `purpose_type`	VARCHAR(30)	NULL,
                                 `confirmed_at`	DATETIME	NULL,
                                 `created_at`	DATETIME	NULL,
                                 `updated_at`	DATETIME	NULL
);

CREATE TABLE `potential_customer` (
                                      `pc_id`	BIGINT	NOT NULL,
                                      `session_id`	VARCHAR(64)	NULL,
                                      `email`	VARCHAR(100)	NULL,
                                      `language`	VARCHAR(5)	NULL,
                                      `trigger_type`	VARCHAR(20)	NULL,
                                      `consent_marketing`	BOOLEAN	NULL,
                                      `sent_status`	VARCHAR(20)	NULL,
                                      `sent_at`	DATETIME	NULL,
                                      `created_at`	DATETIME	NULL,
                                      `updated_at`	DATETIME	NULL
);

CREATE TABLE `potential_customer_product` (
                                              `id`	BIGINT	NOT NULL,
                                              `pc_id`	BIGINT	NULL,
                                              `sku`	BIGINT	NULL,
                                              `created_at`	DATETIME	NULL,
                                              `updated_at`	DATETIME	NULL
);

ALTER TABLE `session` ADD CONSTRAINT `PK_SESSION` PRIMARY KEY (
                                                               `session_id`
    );

ALTER TABLE `product` ADD CONSTRAINT `PK_PRODUCT` PRIMARY KEY (
                                                               `product_id`
    );

ALTER TABLE `product_translation` ADD CONSTRAINT `PK_PRODUCT_TRANSLATION` PRIMARY KEY (
                                                                                       `id`
    );

ALTER TABLE `sku` ADD CONSTRAINT `PK_SKU` PRIMARY KEY (
                                                       `sku`
    );

ALTER TABLE `product_image` ADD CONSTRAINT `PK_PRODUCT_IMAGE` PRIMARY KEY (
                                                                           `image_id`
    );

ALTER TABLE `tag_scan_log` ADD CONSTRAINT `PK_TAG_SCAN_LOG` PRIMARY KEY (
                                                                         `scan_id`
    );

ALTER TABLE `interaction_log` ADD CONSTRAINT `PK_INTERACTION_LOG` PRIMARY KEY (
                                                                               `interaction_id`
    );

ALTER TABLE `inquiry` ADD CONSTRAINT `PK_INQUIRY` PRIMARY KEY (
                                                               `inquiry_id`
    );

ALTER TABLE `sa_call` ADD CONSTRAINT `PK_SA_CALL` PRIMARY KEY (
                                                               `call_id`
    );

ALTER TABLE `visit_purpose` ADD CONSTRAINT `PK_VISIT_PURPOSE` PRIMARY KEY (
                                                                           `purpose_id`
    );

ALTER TABLE `potential_customer` ADD CONSTRAINT `PK_POTENTIAL_CUSTOMER` PRIMARY KEY (
                                                                                     `pc_id`
    );

ALTER TABLE `potential_customer_product` ADD CONSTRAINT `PK_POTENTIAL_CUSTOMER_PRODUCT` PRIMARY KEY (
                                                                                                     `id`
    );

