-- Sett "Diverse" på alle ingredienser som mangler kategori
update ingredient
set category_id = (select id from category where name = 'Diverse')
where category_id is null;

alter table ingredient
    alter column category_id set not null;