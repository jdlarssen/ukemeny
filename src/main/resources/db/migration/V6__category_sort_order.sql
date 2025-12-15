ALTER TABLE category
ADD COLUMN sort_order INT NOT NULL DEFAULT 1000;

UPDATE category SET sort_order = 10 WHERE name = 'Frukt & grønt';
UPDATE category SET sort_order = 20 WHERE name = 'Meieri';
UPDATE category SET sort_order = 30 WHERE name = 'Kjøtt';
UPDATE category SET sort_order = 40 WHERE name = 'Fisk';
UPDATE category SET sort_order = 90 WHERE name = 'Annet';