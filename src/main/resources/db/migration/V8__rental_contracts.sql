create table rental_contracts (
    id varchar(36) primary key,
    booking_id varchar(20) not null references bookings(id),
    status varchar(30) not null,
    generated_path varchar(600) not null,
    signed_path varchar(600),
    original_signed_filename varchar(255),
    validator_report_reference varchar(500),
    review_notes varchar(1000),
    signer_name_matches boolean,
    reviewed_by bigint references users(id),
    created_at timestamptz not null default now(),
    signed_at timestamptz,
    reviewed_at timestamptz,
    constraint uk_rental_contract_booking unique (booking_id)
);

create index idx_rental_contract_status on rental_contracts(status, created_at desc);
