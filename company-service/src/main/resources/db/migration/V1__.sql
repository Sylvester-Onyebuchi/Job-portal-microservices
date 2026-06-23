CREATE TABLE company
(
    id          VARCHAR(255) NOT NULL,
    owner_id    VARCHAR(255),
    name        VARCHAR(255),
    email       VARCHAR(255),
    industry    VARCHAR(255),
    location    VARCHAR(255),
    phone       VARCHAR(255),
    website     VARCHAR(255),
    description VARCHAR(255),
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_company PRIMARY KEY (id)
);

ALTER TABLE company
    ADD CONSTRAINT uc_company_email UNIQUE (email);

ALTER TABLE company
    ADD CONSTRAINT uc_company_name UNIQUE (name);

ALTER TABLE company
    ADD CONSTRAINT uc_company_phone UNIQUE (phone);

ALTER TABLE company
    ADD CONSTRAINT uc_company_website UNIQUE (website);

CREATE INDEX idx_company_name ON company (name);