create table users (
    id bigserial primary key,
    name varchar(120) not null,
    email varchar(180) not null,
    password_hash varchar(255) not null,
    phone varchar(24),
    role varchar(20) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    constraint uk_user_email unique (email)
);

create table spaces (
    id bigserial primary key,
    title varchar(140) not null,
    city varchar(80) not null,
    district varchar(80) not null,
    type varchar(30) not null,
    size integer not null check (size > 0),
    price numeric(10,2) not null check (price > 0),
    rating double precision not null default 0,
    reviews integer not null default 0,
    distance double precision not null default 0,
    latitude double precision,
    longitude double precision,
    verified boolean not null default false,
    available boolean not null default true,
    description varchar(1200) not null,
    features_csv varchar(600) not null,
    tone_a varchar(12) not null default '#D8E4EF',
    tone_b varchar(12) not null default '#68809C',
    owner_id bigint not null references users(id)
);

create index idx_spaces_location on spaces(city, district);
create index idx_spaces_available on spaces(available, verified);

create table bookings (
    id varchar(20) primary key,
    space_id bigint not null references spaces(id),
    customer_id bigint not null references users(id),
    start_date date not null,
    months integer not null check (months between 1 and 24),
    total numeric(10,2) not null,
    status varchar(20) not null,
    contract_code varchar(20) not null unique,
    payment_method varchar(30) not null,
    created_at timestamptz not null default now()
);

create index idx_bookings_customer on bookings(customer_id, created_at desc);
create index idx_bookings_space on bookings(space_id, created_at desc);
