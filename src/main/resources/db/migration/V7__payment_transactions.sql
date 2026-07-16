create table payment_transactions (
    id varchar(36) primary key,
    booking_id varchar(20) not null references bookings(id),
    provider varchar(30) not null,
    provider_preference_id varchar(120),
    provider_payment_id varchar(120),
    status varchar(30) not null,
    checkout_url varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_payment_transactions_booking on payment_transactions(booking_id, created_at desc);
create unique index uk_payment_provider_payment on payment_transactions(provider, provider_payment_id) where provider_payment_id is not null;
