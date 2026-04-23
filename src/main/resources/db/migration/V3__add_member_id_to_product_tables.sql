ALTER TABLE product
    ADD COLUMN IF NOT EXISTS member_id BIGINT;

ALTER TABLE product_draft
    ADD COLUMN IF NOT EXISTS member_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_product_member_id ON product (member_id);
CREATE INDEX IF NOT EXISTS idx_product_draft_member_id ON product_draft (member_id);
