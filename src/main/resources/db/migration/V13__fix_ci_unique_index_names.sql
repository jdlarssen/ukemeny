-- V13__fix_ci_unique_index_names.sql

DO $$
BEGIN
    -- CATEGORY: rename typo-index hvis korrekt ikke finnes
    IF to_regclass('public.ux_catgory_name_ci') IS NOT NULL AND to_regclass('public.ux_category_name_ci') IS NULL THEN
        EXECUTE 'ALTER INDEX public.ux_catgory_name_ci RENAME TO ux_category_name_ci';
END IF;

    -- CATEGORY: hvis begge finnes, dropp typo
    IF to_regclass('public.ux_catgory_name_ci') IS NOT NULL AND to_regclass('public.ux_category_name_ci') IS NOT NULL THEN
        EXECUTE 'DROP INDEX public.ux_catgory_name_ci';
END IF;
END $$;