-- ============================================================
-- BH Group PMS — deterministic (but non-reversible) email fingerprint
-- ============================================================
-- masked_email alone (e.g. "g***@example.com") isn't unique enough to
-- prove which exact address a compliance record covers - two different
-- addresses can share the same first character and domain. This adds an
-- HMAC-SHA256 fingerprint (keyed with an application secret, so it can't
-- be brute-forced from a wordlist of common emails) that lets an exact
-- match be verified without ever storing or logging the email itself.

ALTER TABLE gdpr_requests
    ADD COLUMN email_fingerprint VARCHAR(64);

CREATE INDEX ix_gdpr_requests_email_fingerprint ON gdpr_requests (email_fingerprint);
