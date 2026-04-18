CREATE TABLE tb_order_items (
    id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES tb_orders (id),
    CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES tb_products (id)
);