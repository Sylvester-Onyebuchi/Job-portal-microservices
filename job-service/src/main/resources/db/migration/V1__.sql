CREATE TABLE jobs
(
    id               VARCHAR(255) NOT NULL,
    company_id       VARCHAR(255),
    title            VARCHAR(255),
    location         VARCHAR(255),
    employment_type  VARCHAR(255),
    work_mode        VARCHAR(255),
    description      TEXT,
    responsibilities TEXT,
    qualifications   TEXT,
    benefits         TEXT,
    why_join_us      TEXT,
    posted_at        TIMESTAMP WITHOUT TIME ZONE,
    expires_at       TIMESTAMP WITHOUT TIME ZONE,
    status           VARCHAR(255),
    CONSTRAINT pk_jobs PRIMARY KEY (id)
);

CREATE INDEX idx_job_name ON jobs (title);