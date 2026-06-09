CREATE TABLE items (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       category VARCHAR(100),
                       price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                       quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0)
);

CREATE INDEX idx_items_name ON items(name);