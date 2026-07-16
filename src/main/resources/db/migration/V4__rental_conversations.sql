create table rental_conversations (
    id varchar(36) primary key,
    space_id bigint not null references spaces(id),
    customer_id bigint not null references users(id),
    owner_id bigint not null references users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_rental_conversation unique (space_id, customer_id),
    constraint chk_rental_participants check (customer_id <> owner_id)
);

create index idx_rental_conversation_customer on rental_conversations(customer_id, updated_at desc);
create index idx_rental_conversation_owner on rental_conversations(owner_id, updated_at desc);

create table rental_messages (
    id bigserial primary key,
    conversation_id varchar(36) not null references rental_conversations(id) on delete cascade,
    sender_id bigint not null references users(id),
    body varchar(2000) not null,
    created_at timestamptz not null default now()
);

create index idx_rental_messages_conversation on rental_messages(conversation_id, created_at);
