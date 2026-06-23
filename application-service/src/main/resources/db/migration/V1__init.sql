CREATE TABLE application
(
    id                VARCHAR(255) NOT NULL,
    job_id            VARCHAR(255),
    candidate_user_id VARCHAR(255),
    company_id        VARCHAR(255),
    full_name         VARCHAR(255),
    email             VARCHAR(255),
    phone_number      VARCHAR(255),
    cover_letter      TEXT,
    portfolio_url     VARCHAR(255),
    linked_in_url     VARCHAR(255),
    github_url        VARCHAR(255),
    status            VARCHAR(255),
    cv                OID,
    cv_filename       VARCHAR(255),
    cv_content        VARCHAR(255),
    applied_at        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_application PRIMARY KEY (id)
);

CREATE TABLE application_education
(
    application_id VARCHAR(255) NOT NULL,
    school         VARCHAR(255),
    degree         VARCHAR(255),
    field_of_study VARCHAR(255),
    start_year     INTEGER,
    end_year       INTEGER
);

CREATE TABLE application_experience
(
    application_id VARCHAR(255) NOT NULL,
    company        VARCHAR(255),
    position       VARCHAR(255),
    description    VARCHAR(255),
    start_year     INTEGER,
    end_year       INTEGER,
    still_working  BOOLEAN
);

CREATE TABLE application_languages
(
    application_id VARCHAR(255) NOT NULL,
    languages      VARCHAR(255)
);

CREATE TABLE application_skills
(
    application_id VARCHAR(255) NOT NULL,
    skills         VARCHAR(255)
);

ALTER TABLE application
    ADD CONSTRAINT uc_application_email UNIQUE (email);

ALTER TABLE application_education
    ADD CONSTRAINT fk_application_education_on_application FOREIGN KEY (application_id) REFERENCES application (id);

ALTER TABLE application_experience
    ADD CONSTRAINT fk_application_experience_on_application FOREIGN KEY (application_id) REFERENCES application (id);

ALTER TABLE application_languages
    ADD CONSTRAINT fk_application_languages_on_application FOREIGN KEY (application_id) REFERENCES application (id);

ALTER TABLE application_skills
    ADD CONSTRAINT fk_application_skills_on_application FOREIGN KEY (application_id) REFERENCES application (id);