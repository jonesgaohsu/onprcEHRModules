/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
  cast(pp.Animalid1 as nvarchar(4000)) as Id,
--cast(pp.Animalid2 as nvarchar(4000)) as Id2,
  pp.Pairingdate as date,
--pp.SeparationDate as enddate,

  L1.Location as room,
  ltrim(rtrim(rtrim(r1.row) + convert(char(2), r1.Cage))) As Cage,
  CASE
  WHEN (ltrim(rtrim(rtrim(r1.row) + convert(char(2), r1.Cage))) < ltrim(rtrim(rtrim(r2.row) + convert(char(2), r2.Cage)))) THEN ltrim(rtrim(rtrim(r1.row) + convert(char(2), r1.Cage)))
  ELSE ltrim(rtrim(rtrim(r2.row) + convert(char(2), r2.Cage)))
  END as lowestCage,
  pp.objectid as pairid,

--L2.Location as room2,
--ltrim(rtrim(rtrim(r2.row) + convert(char(2), r2.Cage))) As Cage2,

  'Pair attempt' as eventType,
  s2.Value as housingType,
  s3.Value as outcome,

  s4.Value AS separationReason,

  case when pp.aggressor = pp.Animalid1 THEN 'Aggression' ELSE null END as observation,
  pp.Remarks as Remark,

  s5.Value AS PairingOrigin,

  case
  WHEN rt.LastName = 'Unassigned' or rt.FirstName = 'Unassigned' THEN
    'Unassigned'
  WHEN datalength(rt.LastName) > 0 AND datalength(rt.FirstName) > 0 AND datalength(rt.Initials) > 0 THEN
    rt.LastName + ', ' + rt.FirstName + ' (' + rt.Initials + ')'
  WHEN datalength(rt.LastName) > 0 AND datalength(rt.FirstName) > 0 THEN
    rt.LastName + ', ' + rt.FirstName
  WHEN datalength(rt.LastName) > 0 AND datalength(rt.Initials) > 0 THEN
    rt.LastName + ' (' + rt.Initials + ')'
  WHEN datalength(rt.Initials) = 0 OR rt.initials = ' ' OR rt.lastname = ' none' THEN
    null
  else
  rt.Initials
  END as performedBy,
  cast(pp.objectid as varchar(38)) + '1' as objectid

FROM Psych_Pairings PP
  left join Ref_Technicians rt on (PP.Technician = rt.ID)
  left join Sys_parameters s1 on (s1.Field = 'DepartmentCode' And s1.Flag = rt.DeptCode)
  left join Sys_parameters s2 on (s2.Field = 'PairingType' And s2.Flag = PP.PairingType)
  left join Sys_parameters s3 on (s3.Field = 'PairingOutcome' And s3.Flag = PP.PairingOutcome)
  left join  Sys_parameters s4 on (s4.Field = 'PairingSeparationReason' And s4.Flag = PP.SeparationReason)
  left join  Sys_parameters s5 on (s5.Field = 'PairingOrigin' And s5.Flag = PP.PairingOrigin)

  left join Ref_RowCage r1 on  (r1.CageID = PP.CageID1)
  left join Ref_Location L1 on (r1.LocationID = L1.LocationId)

  left join Ref_RowCage r2 on  (r2.CageID = PP.CageID2)
  left join Ref_Location L2 on (r2.LocationID = L2.LocationId)

where s5.Value != 'Autogenerated' and s5.Value != 'Unscheduled'
and pp.ts > ?

union all

SELECT
  --cast(pp.Animalid1 as nvarchar(4000)) as Id,
  cast(pp.Animalid2 as nvarchar(4000)) as Id,
  pp.Pairingdate as date,
  --pp.SeparationDate as enddate,

--   L1.Location as room,
--   ltrim(rtrim(rtrim(r1.row) + convert(char(2), r1.Cage))) As Cage,

  L2.Location as room,
  ltrim(rtrim(rtrim(r2.row) + convert(char(2), r2.Cage))) As Cage,
  CASE
  WHEN (ltrim(rtrim(rtrim(r1.row) + convert(char(2), r1.Cage))) < ltrim(rtrim(rtrim(r2.row) + convert(char(2), r2.Cage)))) THEN ltrim(rtrim(rtrim(r1.row) + convert(char(2), r1.Cage)))
  ELSE ltrim(rtrim(rtrim(r2.row) + convert(char(2), r2.Cage)))
  END as lowestCage,
  pp.objectid as pairid,

  'Pair attempt' as eventType,
  s2.Value as housingType,
  s3.Value as outcome,

  s4.Value AS separationReason,

  case when pp.aggressor = pp.Animalid2 THEN 'Aggression' ELSE null END as observation,
  pp.Remarks as Remark,

  s5.Value AS PairingOrigin,

  case
  WHEN rt.LastName = 'Unassigned' or rt.FirstName = 'Unassigned' THEN
    'Unassigned'
  WHEN datalength(rt.LastName) > 0 AND datalength(rt.FirstName) > 0 AND datalength(rt.Initials) > 0 THEN
    rt.LastName + ', ' + rt.FirstName + ' (' + rt.Initials + ')'
  WHEN datalength(rt.LastName) > 0 AND datalength(rt.FirstName) > 0 THEN
    rt.LastName + ', ' + rt.FirstName
  WHEN datalength(rt.LastName) > 0 AND datalength(rt.Initials) > 0 THEN
    rt.LastName + ' (' + rt.Initials + ')'
  WHEN datalength(rt.Initials) = 0 OR rt.initials = ' ' OR rt.lastname = ' none' THEN
    null
  else
  rt.Initials
  END as performedBy,
  cast(pp.objectid as varchar(38)) + '2' as objectid

FROM Psych_Pairings PP
  left join Ref_Technicians rt on (PP.Technician = rt.ID)
  left join Sys_parameters s1 on (s1.Field = 'DepartmentCode' And s1.Flag = rt.DeptCode)
  left join Sys_parameters s2 on (s2.Field = 'PairingType' And s2.Flag = PP.PairingType)
  left join Sys_parameters s3 on (s3.Field = 'PairingOutcome' And s3.Flag = PP.PairingOutcome)
  left join  Sys_parameters s4 on (s4.Field = 'PairingSeparationReason' And s4.Flag = PP.SeparationReason)
  left join  Sys_parameters s5 on (s5.Field = 'PairingOrigin' And s5.Flag = PP.PairingOrigin)

  left join Ref_RowCage r1 on  (r1.CageID = PP.CageID1)
  left join Ref_Location L1 on (r1.LocationID = L1.LocationId)

  left join Ref_RowCage r2 on  (r2.CageID = PP.CageID2)
  left join Ref_Location L2 on (r2.LocationID = L2.LocationId)

where s5.Value != 'Autogenerated' and s5.Value != 'Unscheduled'
and pp.ts > ?