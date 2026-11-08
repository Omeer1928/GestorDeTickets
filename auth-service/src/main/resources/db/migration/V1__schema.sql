-- BD Autenticación (exclusiva del Authentication Service): credenciales, roles y sesiones (refresh tokens)

CREATE TABLE role_catalog (
    code        VARCHAR(20)  NOT NULL PRIMARY KEY,
    description VARCHAR(255) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE credential (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id        VARCHAR(20)  NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    last_login_at  DATETIME(6)  NULL,
    CONSTRAINT uk_credential_user  UNIQUE (user_id),
    CONSTRAINT uk_credential_email UNIQUE (email),
    CONSTRAINT fk_credential_role  FOREIGN KEY (role) REFERENCES role_catalog (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Refresh tokens opacos: solo se guarda su hash SHA-256. family_id agrupa las rotaciones
-- para detectar reutilización (si un token ya rotado se vuelve a usar, se revoca toda la familia).
CREATE TABLE refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token_hash  CHAR(64)     NOT NULL,
    user_id     VARCHAR(20)  NOT NULL,
    email       VARCHAR(150) NOT NULL,
    family_id   CHAR(36)     NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    revoked_at  DATETIME(6)  NULL,
    CONSTRAINT uk_refresh_hash UNIQUE (token_hash),
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_family (family_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
