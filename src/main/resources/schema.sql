CREATE TABLE IF NOT EXISTS lines (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(10)  NOT NULL UNIQUE,
    name             VARCHAR(100) NOT NULL,
    company_id       INT          NOT NULL,
    company_name     VARCHAR(100) NOT NULL,
    is_artesp_monitored BOOLEAN   NOT NULL,
    situation        VARCHAR(100) NOT NULL,
    descricao        TEXT         NOT NULL,
    classification   VARCHAR(50)  NOT NULL,
    is_normal        BOOLEAN      NOT NULL,
    updated_at       VARCHAR(50)  NOT NULL,
    source           VARCHAR(200) NOT NULL DEFAULT '' -- comma-separated list of provider names
);
