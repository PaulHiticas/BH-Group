-- ============================================================
-- BH Group PMS — Register LATE_CHECKOUT_REQUEST as a notification type
--
-- The late checkout feature (V29) added NotificationType.LATE_CHECKOUT_REQUEST
-- on the Java side, but never extended the Postgres-native notification_type
-- enum to match - inserts failed with "invalid input value for enum
-- notification_type" whenever a late checkout notification fired. Same
-- pattern as V21's DOCUMENT_EXPIRING addition.
-- ============================================================

ALTER TYPE notification_type ADD VALUE 'LATE_CHECKOUT_REQUEST';
