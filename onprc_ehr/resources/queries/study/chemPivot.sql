/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
b.Id,
b.date,
b.method,
group_concat(distinct b.remark, chr(10)) as remark,
b.testId,
group_concat(b.result) as results

FROM (

SELECT
  b.Id,
  b.date,
  b.testId,
  coalesce(b.runId, b.objectid) as runId,
  b.method,
  b.remark,
  b.resultoorindicator,
  CASE
  WHEN b.result IS NULL THEN  b.qualresult
    ELSE CAST(CAST(b.result AS float) AS VARCHAR)
  END as result
FROM study."Chemistry Results" b
WHERE b.testId.includeInPanel = true and b.qcstate.publicdata = true

) b

GROUP BY b.id, b.date, b.runId, b.testId, b.method, b.remark
PIVOT results BY testId IN
(select testid from ehr_lookups.chemistry_tests t WHERE t.includeInPanel = true order by sort_order)

