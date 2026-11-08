-- =====================================================================
-- ObraIA - V6: values of the sentences (block 12)
-- The frontend shows the site in Spanish or English. Notifications and
-- budget rows keep the values of their sentence (project, city, days...)
-- and the frontend writes the sentence in the language of the user. The
-- English text stays as it is for older rows and for other clients.
-- =====================================================================

ALTER TABLE notifications
    ADD COLUMN params jsonb;

COMMENT ON COLUMN notifications.params IS
    'Values of the message as a JSON object of texts; null for the notifications made before V6';

ALTER TABLE budget_items
    ADD COLUMN params jsonb;

COMMENT ON COLUMN budget_items.params IS
    'Values of the description as a JSON object of texts; null when the description has none';

-- The rows made before V6 get the values back from their English text.
UPDATE budget_items
SET params = jsonb_build_object('items', substring(description FROM '\((\d+) items\)$'))
WHERE item_type = 'materials'
  AND description ~ '^Materials for the measured elements \(\d+ items\)$';

UPDATE budget_items
SET params = jsonb_build_object('city', substring(description FROM '^Transport of the materials to the site in (.+)$'))
WHERE item_type = 'transport'
  AND description LIKE 'Transport of the materials to the site in %';

UPDATE budget_items
SET params = jsonb_build_object(
        'days', substring(description FROM '^Crew for (\d+) working days'),
        'rate', replace(substring(description FROM 'at COP ([0-9,]+) a day$'), ',', ''))
WHERE item_type = 'labor'
  AND description ~ '^Crew for \d+ working days at COP [0-9,]+ a day$';

-- Same for the notifications about budgets; the risk ones keep only their English text.
UPDATE notifications
SET params = jsonb_build_object(
        'version', m[1],
        'project', m[2],
        'amount', replace(m[3], ',', ''))
FROM (SELECT id AS notification_id,
             regexp_match(message, '^Version (\d+) of the budget of "(.+)" was approved for COP ([0-9,]+)\.$') AS m
      FROM notifications
      WHERE type = 'budget_approved') AS found
WHERE notifications.id = found.notification_id
  AND found.m IS NOT NULL;

UPDATE notifications
SET params = jsonb_build_object(
        'project', m[1],
        'spent', replace(m[2], ',', ''),
        'budget', replace(m[3], ',', ''))
FROM (SELECT id AS notification_id,
             regexp_match(message, '^The expenses of "(.+)" passed the approved budget: COP ([0-9,]+) spent of COP ([0-9,]+)\.$') AS m
      FROM notifications
      WHERE type = 'budget_deviation') AS found
WHERE notifications.id = found.notification_id
  AND found.m IS NOT NULL;
