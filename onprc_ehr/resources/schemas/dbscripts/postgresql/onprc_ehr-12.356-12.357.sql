/*
 * Copyright (c) 2014 LabKey Corporation
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
CREATE TABLE onprc_ehr.housing_transfer_requests (
  Id varchar(100),
  date timestamp,
  room varchar(200),
  cage varchar(100),
  reason varchar(100),
  remark varchar(4000),
  qcstate int,

  requestid entityid,
  objectid entityid NOT NULL,
  container entityid,
  created timestamp,
  createdby int,
  modified timestamp,
  modifiedby int,

  CONSTRAINT PK_housing_transfer_requests PRIMARY KEY (objectid)
);