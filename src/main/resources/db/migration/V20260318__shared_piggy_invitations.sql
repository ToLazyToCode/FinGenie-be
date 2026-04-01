create table if not exists shared_piggy_invitation (
    id bigserial primary key,
    version bigint,
    is_deleted boolean not null default false,
    is_active boolean not null default true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    inviter_id bigint not null,
    invitee_id bigint not null,
    wallet_id bigint not null,
    piggy_title varchar(255) not null,
    goal_amount numeric(15, 2) not null,
    lock_until date,
    status varchar(20) not null,
    expires_at timestamp not null,
    responded_at timestamp,
    accepted_at timestamp,
    created_piggy_id bigint
);

create index if not exists idx_shared_piggy_invitee_status
    on shared_piggy_invitation (invitee_id, status);

create index if not exists idx_shared_piggy_inviter_status
    on shared_piggy_invitation (inviter_id, status);

create index if not exists idx_shared_piggy_wallet_status
    on shared_piggy_invitation (wallet_id, status);

create index if not exists idx_shared_piggy_expires
    on shared_piggy_invitation (expires_at);
