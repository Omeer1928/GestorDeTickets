-- BD Tickets (exclusiva del Ticket Service): incidencias, historial de escalamiento y bitácora

CREATE TABLE ticket (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id            VARCHAR(20)  NULL,
    client_id            VARCHAR(20)  NOT NULL,
    company_name         VARCHAR(150) NULL,
    title                VARCHAR(150) NOT NULL,
    description          TEXT         NOT NULL,
    category             VARCHAR(40)  NOT NULL,
    priority             VARCHAR(10)  NOT NULL,
    priority_rank        INT          NOT NULL,
    status               VARCHAR(15)  NOT NULL,
    required_level       VARCHAR(2)   NOT NULL,
    assigned_tech_id     VARCHAR(20)  NULL,
    assigned_tech_name   VARCHAR(150) NULL,
    assigned_tech_level  VARCHAR(2)   NULL,
    queue_name           VARCHAR(20)  NULL,
    resolution_log       TEXT         NULL,
    resolution_code      VARCHAR(40)  NULL,
    registered_at        DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    assigned_at          DATETIME(6)  NULL,
    resolved_at          DATETIME(6)  NULL,
    closed_at            DATETIME(6)  NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_ticket_code UNIQUE (ticket_id),
    CONSTRAINT ck_ticket_priority CHECK (priority IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_ticket_status CHECK (status IN ('REGISTRADO', 'ASIGNADO', 'EN_PROCESO', 'ESCALADO',
                                                  'REASIGNADO', 'RESUELTO', 'CERRADO')),
    INDEX idx_ticket_client (client_id),
    INDEX idx_ticket_tech (assigned_tech_id),
    INDEX idx_ticket_status (status),
    INDEX idx_ticket_queue (queue_name, priority_rank, registered_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 AUTO_INCREMENT = 102001;

-- Historial de escalamiento y reasignación (preserva la bitácora al transferir el caso)
CREATE TABLE escalation_record (
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_pk          BIGINT       NOT NULL,
    type               VARCHAR(15)  NOT NULL,
    previous_tech_id   VARCHAR(20)  NULL,
    new_tech_id        VARCHAR(20)  NULL,
    reason             VARCHAR(500) NOT NULL,
    previous_category  VARCHAR(40)  NULL,
    new_category       VARCHAR(40)  NULL,
    previous_level     VARCHAR(2)   NULL,
    new_level          VARCHAR(2)   NULL,
    transfer_date      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_escalation_ticket FOREIGN KEY (ticket_pk) REFERENCES ticket (id) ON DELETE CASCADE,
    INDEX idx_escalation_ticket (ticket_pk)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Bitácora inmutable de eventos del ticket (trazabilidad y auditoría)
CREATE TABLE ticket_event (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_pk    BIGINT       NOT NULL,
    event_type   VARCHAR(40)  NOT NULL,
    from_status  VARCHAR(15)  NULL,
    to_status    VARCHAR(15)  NULL,
    actor_id     VARCHAR(40)  NOT NULL,
    actor_role   VARCHAR(20)  NOT NULL,
    detail       VARCHAR(500) NULL,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT fk_event_ticket FOREIGN KEY (ticket_pk) REFERENCES ticket (id) ON DELETE CASCADE,
    INDEX idx_event_ticket (ticket_pk, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
