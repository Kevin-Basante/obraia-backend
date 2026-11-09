-- =====================================================================
-- ObraIA - V7: codes of the risk sentences saved before block 12
-- Since V6 every risk factor keeps a detail code and its values, and
-- every notification keeps the values of its sentence, so the frontend
-- writes them in Spanish or English. The risk assessments and the risk
-- notifications made before that only have their English text: this
-- migration reads that text and adds the same codes and values. A text
-- that does not match any known sentence is left as it is.
-- =====================================================================

-- Code and values of one English risk sentence:
-- {"detailCode": "budget_spent", "detailParams": {"spent": "104"}}, or null when it is not known.
CREATE FUNCTION pg_temp.risk_detail(detail text) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE AS
$$
DECLARE
    m text[];
BEGIN
    IF detail = 'There is no approved budget yet' THEN
        RETURN jsonb_build_object('detailCode', 'no_budget', 'detailParams', '{}'::jsonb);
    END IF;
    IF detail = 'The plan has no dates yet' THEN
        RETURN jsonb_build_object('detailCode', 'no_dates', 'detailParams', '{}'::jsonb);
    END IF;
    IF detail = 'No task is past its planned end' THEN
        RETURN jsonb_build_object('detailCode', 'no_late_tasks', 'detailParams', '{}'::jsonb);
    END IF;

    m := regexp_match(detail, '^(-?\d+)% of the approved budget is spent$');
    IF m IS NOT NULL THEN
        RETURN jsonb_build_object('detailCode', 'budget_spent', 'detailParams', jsonb_build_object('spent', m[1]));
    END IF;

    m := regexp_match(detail, '^(-?\d+)% of the budget is spent with (-?\d+)% of the work done$');
    IF m IS NOT NULL THEN
        RETURN jsonb_build_object('detailCode', 'spending_vs_progress',
                                  'detailParams', jsonb_build_object('spent', m[1], 'progress', m[2]));
    END IF;

    m := regexp_match(detail, '^(-?\d+)% of the planned time passed with (-?\d+)% of the work done$');
    IF m IS NOT NULL THEN
        RETURN jsonb_build_object('detailCode', 'time_vs_progress',
                                  'detailParams', jsonb_build_object('elapsed', m[1], 'progress', m[2]));
    END IF;

    m := regexp_match(detail, '^(\d+) tasks? (?:is|are) past their planned end; "(.*)" is (\d+) days? late$');
    IF m IS NOT NULL THEN
        RETURN jsonb_build_object('detailCode', 'late_tasks',
                                  'detailParams', jsonb_build_object('count', m[1], 'task', m[2], 'days', m[3]));
    END IF;

    RETURN NULL;
END;
$$;

-- Risk factors without a code get it from their sentence; the order of the factors does not change.
UPDATE risk_assessments
SET risk_factors = (
    SELECT jsonb_agg(
               CASE
                   WHEN factor ? 'detailCode' AND factor ->> 'detailCode' IS NOT NULL THEN factor
                   ELSE factor || coalesce(pg_temp.risk_detail(factor ->> 'detail'), '{}'::jsonb)
               END ORDER BY position)
    FROM jsonb_array_elements(risk_factors) WITH ORDINALITY AS factors(factor, position))
WHERE jsonb_typeof(risk_factors) = 'array'
  AND jsonb_array_length(risk_factors) > 0
  AND EXISTS (SELECT 1
              FROM jsonb_array_elements(risk_factors) AS factors(factor)
              WHERE factor ->> 'detailCode' IS NULL);

DROP FUNCTION pg_temp.risk_detail(text);
