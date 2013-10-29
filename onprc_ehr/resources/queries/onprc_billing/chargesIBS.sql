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
  i.servicecenter,
  i.rowid as transactionNumber,
  'N' as transactionType,
  i.date as transactionDate,
  i.item as transactionDescription,
  i.lastName,
  i.firstName,
  i.faid,
  i.faid.faid as fiscalAuthorityNumber,
  i.faid.lastName as fiscalAuthorityName,
  i.department,
  'L584' as mailcode,
  i.contactPhone,
  i.itemCode,
  i.quantity,
  i.unitCost as price,
  i.debitedaccount as OSUAlias,
  --i.creditedaccount,
  i.totalcost,
  i.invoiceDate,
  i.invoiceId

FROM onprc_billing.invoicedItems i
WHERE i.totalcost != 0

UNION ALL

SELECT
  i.servicecenter,
  i.rowid as transactionNumber,
  'N' as transactionType,
  i.date as transactionDate,
  i.item as transactionDescription,
  i.lastName,
  i.firstName,
  i.faid,
  i.faid.faid as fiscalAuthorityNumber,
  i.faid.lastName as fiscalAuthorityName,
  i.department,
  'L584' as mailcode,
  i.contactPhone,
  (i.itemCode || 'C') as itemCode,
  i.quantity,
  i.unitCost as price,
  --i.debitedaccount as OSUAlias,
  i.creditedaccount as OSUAlias,
  (i.totalcost * -1) as totalcost,
  i.invoiceDate,
  i.invoiceId as invoiceNumber

FROM onprc_billing.invoicedItems i
WHERE i.totalcost != 0
