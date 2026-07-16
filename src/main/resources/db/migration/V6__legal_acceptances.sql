create table legal_acceptances (
    id bigserial primary key,
    user_id bigint not null references users(id),
    document_code varchar(40) not null,
    document_version varchar(30) not null,
    accepted_at timestamptz not null default now(),
    constraint uk_legal_acceptance unique (user_id, document_code, document_version)
);

create index idx_legal_acceptances_user on legal_acceptances(user_id, accepted_at desc);
