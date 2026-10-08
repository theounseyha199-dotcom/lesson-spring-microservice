CREATE TABLE IF NOT EXISTS customers (
    customer_id UUID PRIMARY KEY,
    username TEXT NOT NULL,
    family_name TEXT NOT NULL,
    given_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    status TEXT NOT NULL
);
