ALTER TABLE app_users
    ADD COLUMN google_subject VARCHAR(255);

CREATE UNIQUE INDEX uq_app_users_google_subject
    ON app_users (google_subject)
    WHERE google_subject IS NOT NULL;
