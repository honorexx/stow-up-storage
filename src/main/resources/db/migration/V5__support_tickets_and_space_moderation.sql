alter table support_conversations add column ticket_number varchar(24);
alter table support_conversations add column category varchar(30);
alter table support_conversations add column subject varchar(140);

update support_conversations
set ticket_number = 'SUP-' || upper(substr(replace(id, '-', ''), 1, 8)),
    category = 'OTHER',
    subject = 'Atendimento anterior';

alter table support_conversations alter column ticket_number set not null;
alter table support_conversations alter column category set not null;
alter table support_conversations alter column subject set not null;
alter table support_conversations add constraint uk_support_ticket_number unique (ticket_number);

alter table spaces add column moderation_status varchar(20);
alter table spaces add column moderation_reason varchar(500);
alter table spaces add column moderated_by bigint references users(id);
alter table spaces add column moderated_at timestamptz;

update spaces set moderation_status = case when verified then 'APPROVED' else 'PENDING' end;
alter table spaces alter column moderation_status set not null;
create index idx_spaces_moderation_status on spaces(moderation_status, id desc);
