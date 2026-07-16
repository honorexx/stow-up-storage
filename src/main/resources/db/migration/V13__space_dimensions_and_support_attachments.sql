alter table spaces add column height_meters numeric(8, 2);
alter table spaces add column length_meters numeric(8, 2);
alter table spaces add column depth_meters numeric(8, 2);

update spaces
set height_meters = 2.00,
    length_meters = greatest(1.00, sqrt(size::numeric)),
    depth_meters = greatest(1.00, sqrt(size::numeric))
where height_meters is null;

alter table spaces alter column height_meters set not null;
alter table spaces alter column length_meters set not null;
alter table spaces alter column depth_meters set not null;
alter table spaces add constraint ck_space_height_positive check (height_meters > 0);
alter table spaces add constraint ck_space_length_positive check (length_meters > 0);
alter table spaces add constraint ck_space_depth_positive check (depth_meters > 0);

alter table support_messages add column attachment_url varchar(500);
alter table support_messages add column attachment_name varchar(240);
alter table support_messages add column attachment_content_type varchar(120);
alter table support_messages add column attachment_size bigint;
