alter table users add column phone_normalized varchar(20);

update users
set phone_normalized = case
    when length(regexp_replace(phone, '[^0-9]', '', 'g')) in (10, 11)
        then '55' || regexp_replace(phone, '[^0-9]', '', 'g')
    else regexp_replace(phone, '[^0-9]', '', 'g')
end
where phone is not null and trim(phone) <> '';

create unique index uk_users_phone_normalized
    on users(phone_normalized)
    where phone_normalized is not null;
