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
ALTER TABLE onprc_billing.grants ADD rowid serial;
ALTER TABLE onprc_billing.grants ADD container entityid;

ALTER TABLE onprc_billing.grants DROP CONSTRAINT PK_grants;
ALTER TABLE onprc_billing.grants ADD CONSTRAINT PK_grants PRIMARY KEY (rowid);
ALTER TABLE onprc_billing.grants ADD CONSTRAINT UNIQUE_grants UNIQUE (container, grantNumber);

ALTER TABLE onprc_billing.grants DROP COLUMN totalDCBudget;
ALTER TABLE onprc_billing.grants DROP COLUMN totalFABudget;