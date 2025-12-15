update ingredient
set category_id = (select id from category where name = 'Diverse')
where category_id = (select id from category where name = 'Annet');

delete from category
where name = 'Annet'
and not exists (
    select 1 from ingredient i
             where i.category_id = category.id
);