alter table users add column cpf_encrypted varchar(512);
alter table users add column cpf_hash varchar(64);
alter table users add column postal_code varchar(8);
alter table users add column address_line varchar(180);
alter table users add column address_number varchar(20);
alter table users add column address_complement varchar(80);
alter table users add column district varchar(80);
alter table users add column city varchar(80);
alter table users add column state varchar(2);
alter table users add column photo_url varchar(300);
create unique index uk_users_cpf_hash on users(cpf_hash) where cpf_hash is not null;

alter table spaces add column postal_code varchar(8);
alter table spaces add column address_line varchar(180);
alter table spaces add column address_number varchar(20);
alter table spaces add column address_complement varchar(80);
alter table spaces add column state varchar(2);

create table space_photos (
    id bigserial primary key,
    space_id bigint not null references spaces(id) on delete cascade,
    url varchar(300) not null,
    display_order integer not null default 0
);
create index idx_space_photos_space on space_photos(space_id, display_order);
