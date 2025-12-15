-- 1) Ny tabell for kategorier
create table if not exists category (
    id bigserial primary key,
    name varchar(100) not null unique
);

-- 2) Sørg for at "Annet" alltid finnes
insert into category(name) values ('Annet')
on conflict (name) do nothing;

-- 3) Ta med eksisterende kategorier fra ingredient (hvis det er fritekst der)
insert into category(name)
select distinct trim(category)
from ingredient
where category is not null and trim(category) <> ''
on conflict (name) do nothing;

-- 4) Legg til FK-kolonne på ingredient
alter table ingredient
add column if not exists category_id bigint;

-- 5) Sett category_id basert på gammel fritekst (fallback til 'Annet')
update ingredient i
set category_id = c.id
from category c
where c.name = coalesce(nullif(trim(i.category), ''), 'Annet');

-- 6) Gjør den NOT NULL + FK
alter table ingredient
alter column category_id set not null;

alter table ingredient
add constraint fk_ingredient_category
foreign key (category_id) references category(id);

create index if not exists idx_ingredient_category_id on ingredient(category_id);

alter table ingredient
drop column if exists category;