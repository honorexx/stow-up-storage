create table support_conversations (
    id varchar(36) primary key,
    customer_id bigint not null references users(id),
    status varchar(20) not null default 'OPEN',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_support_conversation_customer on support_conversations(customer_id, updated_at desc);

create table support_messages (
    id bigserial primary key,
    conversation_id varchar(36) not null references support_conversations(id),
    sender_id bigint references users(id),
    sender_type varchar(20) not null,
    body varchar(2000) not null,
    created_at timestamptz not null default now()
);

create index idx_support_messages_conversation on support_messages(conversation_id, created_at);
