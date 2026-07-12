-- ============================================================
-- BH Group PMS — Owner Portal
--
-- BH Group manages properties on behalf of third-party owners under
-- contract (not a SaaS/multi-tenant product — a single company
-- managing many owners' properties). Adds the OWNER role and links
-- each property to the owner it is managed for, plus the commission
-- percentage BH Group takes on that property's revenue.
-- ============================================================

ALTER TYPE user_role ADD VALUE 'OWNER';
