
--liquibase formatted sql

--changeset ashish:2
CREATE TABLE invoices (
    invoice_id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_date TIMESTAMP WITH TIME ZONE NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(100),
    vendor_name VARCHAR(100) NOT NULL,
    vendor_gst_number VARCHAR(15),
    subtotal NUMERIC(10, 2) NOT NULL,
    gst_rate NUMERIC(5, 2) NOT NULL,
    gst_amount NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    invoice_data BYTEA NOT NULL,
    file_path VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

ALTER TABLE orders
ADD COLUMN invoice_id BIGINT,
ADD CONSTRAINT fk_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
