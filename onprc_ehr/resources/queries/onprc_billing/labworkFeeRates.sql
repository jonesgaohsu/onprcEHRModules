/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
SELECT
  p.*,
  p.chargeId.name as item,
  p.chargeId.category as category,
  coalesce(e.unitCost, cr.unitCost) as unitCost,
  1 as quantity,
  coalesce(e.unitCost, cr.unitCost) as totalcost,
  ce.account as creditAccount,
  ce.rowid as creditAccountId,
  CASE
    WHEN e.unitCost IS NOT NULL THEN 'Y'
    ELSE null
  END as isExemption,
  CASE
    WHEN (e.unitCost IS NULL AND cr.unitCost IS NULL) THEN 'Y'
    ELSE null
  END as lacksRate,
  e.rowid as exemptionId,
  CASE WHEN e.rowid IS NULL THEN cr.rowid ELSE null END as rateId,

--find assignment on this date
  CASE
  WHEN p.project IS NULL THEN false
  WHEN p.project.alwaysavailable = true THEN true
  WHEN (SELECT count(*) as projects FROM study.assignment a WHERE
    p.Id = a.Id AND
    p.project = a.project AND
    (cast(p.date AS DATE) < a.enddateCoalesced OR a.enddate IS NULL) AND
    p.date >= a.dateOnly
  ) > 0 THEN true ELSE false END as matchesProject,
  'N' as isMiscCharge

FROM onprc_billing.labworkFees p

LEFT JOIN onprc_billing.chargeRates cr ON (
    p.date >= cr.startDate AND
    p.date <= cr.enddateTimeCoalesced AND
    p.chargeId = cr.chargeId
)

LEFT JOIN onprc_billing.chargeRateExemptions e ON (
    p.date >= e.startDate AND
    p.date <= e.enddateTimeCoalesced AND
    p.chargeId = e.chargeId AND
    p.project = e.project
)

LEFT JOIN onprc_billing.creditAccount ce ON (
    p.date >= ce.startDate AND
    p.date <= ce.enddateTimeCoalesced AND
    p.chargeId = ce.chargeId
)