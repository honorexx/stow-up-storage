create table password_recovery_requests (
    id uuid primary key,
    account_id bigint not null references users(id) on delete cascade,
    channel varchar(10) not null,
    destination varchar(180) not null,
    code_hash varchar(255) not null,
    reset_token_hash varchar(255),
    failed_attempts integer not null default 0,
    expires_at timestamptz not null,
    resend_available_at timestamptz not null,
    verified_at timestamptz,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_password_recovery_account_created
    on password_recovery_requests(account_id, channel, created_at desc);
create index idx_password_recovery_expiry
    on password_recovery_requests(expires_at);
