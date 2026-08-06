--liquibase formatted sql

--changeset pavel:004-create-orders-user-deleted-index
CREATE INDEX idx_orders_user_id_deleted
    ON orders (user_id, deleted);

--rollback DROP INDEX IF EXISTS idx_orders_user_id_deleted;


--changeset pavel:005-create-orders-status-index
CREATE INDEX idx_orders_status
    ON orders (status);

--rollback DROP INDEX IF EXISTS idx_orders_status;


--changeset pavel:006-create-order-items-order-index
CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);

--rollback DROP INDEX IF EXISTS idx_order_items_order_id;


--changeset pavel:007-create-order-items-item-index
CREATE INDEX idx_order_items_item_id
    ON order_items (item_id);

--rollback DROP INDEX IF EXISTS idx_order_items_item_id;


--changeset pavel:008-create-items-name-index
CREATE INDEX idx_items_name
    ON items (name);

--rollback DROP INDEX IF EXISTS idx_items_name;
