\set ON_ERROR_STOP on

BEGIN;

TRUNCATE TABLE transactions;
TRUNCATE TABLE wallet;

WITH wallet_seed AS (
    SELECT
        gs AS idx,
        (
            substr(md5('wallet-' || gs::text), 1, 8) || '-' ||
            substr(md5('wallet-' || gs::text), 9, 4) || '-' ||
            substr(md5('wallet-' || gs::text), 13, 4) || '-' ||
            substr(md5('wallet-' || gs::text), 17, 4) || '-' ||
            substr(md5('wallet-' || gs::text), 21, 12)
        )::uuid AS wallet_id,
        (
            substr(md5('user-' || gs::text), 1, 8) || '-' ||
            substr(md5('user-' || gs::text), 9, 4) || '-' ||
            substr(md5('user-' || gs::text), 13, 4) || '-' ||
            substr(md5('user-' || gs::text), 17, 4) || '-' ||
            substr(md5('user-' || gs::text), 21, 12)
        )::uuid AS user_id
    FROM generate_series(1, :users) AS gs
)
INSERT INTO wallet (wallet_id, user_id, balance)
SELECT
    wallet_id,
    user_id,
    round((100000 + random() * 900000)::numeric, 2)
FROM wallet_seed;

WITH tx_seed AS (
    SELECT
        gs AS tx_idx,
        ((gs - 1) % :users) + 1 AS wallet_idx,
        random() AS type_roll,
        random() AS status_roll,
        now() - (random() * interval '30 days') AS created_at_raw
    FROM generate_series(1, :txns) AS gs
),
typed_tx AS (
    SELECT
        tx_idx,
        wallet_idx,
        CASE
            WHEN type_roll < 0.35 THEN 'TOPUP'
            WHEN type_roll < 0.75 THEN 'PAYMENT'
            WHEN type_roll < 0.90 THEN 'REFUND'
            ELSE 'WITHDRAW'
        END AS tx_type,
        CASE
            WHEN status_roll < 0.80 THEN 'SUCCESS'
            WHEN status_roll < 0.95 THEN 'FAILED'
            ELSE 'PENDING'
        END AS tx_status,
        created_at_raw
    FROM tx_seed
),
wallet_ref AS (
    SELECT
        row_number() OVER (ORDER BY user_id) AS idx,
        wallet_id
    FROM wallet
)
INSERT INTO transactions (
    transaction_id,
    wallet_id,
    amount,
    type,
    status,
    description,
    created_at,
    updated_at
)
SELECT
    (
        substr(md5('tx-' || t.tx_idx::text), 1, 8) || '-' ||
        substr(md5('tx-' || t.tx_idx::text), 9, 4) || '-' ||
        substr(md5('tx-' || t.tx_idx::text), 13, 4) || '-' ||
        substr(md5('tx-' || t.tx_idx::text), 17, 4) || '-' ||
        substr(md5('tx-' || t.tx_idx::text), 21, 12)
    )::uuid AS transaction_id,
    w.wallet_id,
    round((1000 + random() * 499000)::numeric, 2) AS amount,
    t.tx_type::varchar,
    t.tx_status::varchar,
    CASE
        WHEN t.tx_type = 'PAYMENT' THEN 'ORDER-' || lpad(t.tx_idx::text, 8, '0')
        WHEN t.tx_type = 'REFUND' THEN 'ORDER-' || lpad(((t.tx_idx % :txns) + 1)::text, 8, '0')
        WHEN t.tx_type = 'TOPUP' THEN 'TOPUP-' || lpad(t.tx_idx::text, 8, '0')
        ELSE 'WITHDRAW-' || lpad(t.tx_idx::text, 8, '0')
    END AS description,
    t.created_at_raw AS created_at,
    t.created_at_raw + (random() * interval '30 minutes') AS updated_at
FROM typed_tx t
JOIN wallet_ref w ON w.idx = t.wallet_idx;

COMMIT;

SELECT
    (SELECT count(*) FROM wallet) AS wallet_count,
    (SELECT count(*) FROM transactions) AS transaction_count;
