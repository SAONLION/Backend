ALTER TABLE `sku` ADD COLUMN `style_number` VARCHAR(20) NULL;
ALTER TABLE `sku` ADD CONSTRAINT `UQ_SKU_STYLE_NUMBER` UNIQUE (`style_number`);

CREATE TABLE `sku_image` (
                             `image_id`      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                             `style_number`  VARCHAR(20)  NOT NULL,
                             `position`      INT          NOT NULL,
                             `image_url`     VARCHAR(500) NOT NULL,
                             `shot_type`     VARCHAR(20)  NOT NULL,
                             `has_person`    BOOLEAN      NOT NULL DEFAULT FALSE,
                             `created_at`    DATETIME     NULL,
                             `updated_at`    DATETIME     NULL,
                             CONSTRAINT `UQ_SKU_IMAGE_STYLE_POSITION` UNIQUE (`style_number`, `position`)
);

CREATE INDEX `IDX_SKU_IMAGE_STYLE_SHOT` ON `sku_image` (`style_number`, `shot_type`);