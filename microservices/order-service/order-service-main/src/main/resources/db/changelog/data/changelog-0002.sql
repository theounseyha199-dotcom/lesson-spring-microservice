--liquibase formatted sql
--changeset seyha:insert-order-addresses-0002

INSERT INTO public.order_addresses (id, street, zip_code, city)
VALUES
    (gen_random_uuid(), 'Street 271', '12000', 'Phnom Penh'),
    (gen_random_uuid(), 'Street 01', '17000', 'Siem Reap');
