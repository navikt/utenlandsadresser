ALTER TABLE feed
DROP COLUMN hendelsestype;

ALTER TABLE feed
ADD COLUMN hendelsestype INTEGER;

UPDATE feed
SET hendelsestype = 1;

ALTER TABLE feed
ALTER COLUMN hendelsestype SET NOT NULL;
