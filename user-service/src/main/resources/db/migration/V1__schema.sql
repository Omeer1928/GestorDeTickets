-- BD Usuarios (exclusiva del User Service): clientes, técnicos, especialidades y disponibilidad

CREATE TABLE client (
    seq           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       VARCHAR(20)  NULL,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(20)  NULL,
    company_name  VARCHAR(150) NULL,
    ruc           CHAR(11)     NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_client_user  UNIQUE (user_id),
    CONSTRAINT uk_client_email UNIQUE (email),
    CONSTRAINT ck_client_status CHECK (status IN ('ACTIVO', 'INACTIVO', 'SUSPENDIDO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE technician (
    seq                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id                 VARCHAR(20)  NULL,
    first_name              VARCHAR(60)  NOT NULL,
    last_name               VARCHAR(80)  NOT NULL,
    email                   VARCHAR(150) NOT NULL,
    phone                   VARCHAR(20)  NULL,
    support_level           VARCHAR(2)   NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    is_available            BOOLEAN      NOT NULL DEFAULT TRUE,
    current_tickets         INT          NOT NULL DEFAULT 0,
    max_concurrent_tickets  INT          NOT NULL DEFAULT 5,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_tech_user  UNIQUE (user_id),
    CONSTRAINT uk_tech_email UNIQUE (email),
    CONSTRAINT ck_tech_level CHECK (support_level IN ('N1', 'N2', 'N3')),
    CONSTRAINT ck_tech_load  CHECK (current_tickets >= 0 AND max_concurrent_tickets > 0),
    INDEX idx_tech_dispatch (status, is_available, current_tickets)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE specialty (
    code        VARCHAR(40)  NOT NULL PRIMARY KEY,
    description VARCHAR(255) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE technician_specialty (
    technician_seq BIGINT      NOT NULL,
    specialty      VARCHAR(40) NOT NULL,
    PRIMARY KEY (technician_seq, specialty),
    CONSTRAINT fk_ts_tech FOREIGN KEY (technician_seq) REFERENCES technician (seq) ON DELETE CASCADE,
    CONSTRAINT fk_ts_spec FOREIGN KEY (specialty) REFERENCES specialty (code),
    INDEX idx_ts_specialty (specialty)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
