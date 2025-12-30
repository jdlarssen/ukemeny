-- Case-insensitive unique constraints (Postgres)
-- Bruker funksjonelle UNIQUE-indexer på lower(name)

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_name_ci
    ON category (lower(name));

CREATE UNIQUE INDEX IF NOT EXISTS ux_ingredient_name_ci
    ON ingredient (lower(name));
