CREATE TABLE files (
    id SERIAL PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    system_file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);