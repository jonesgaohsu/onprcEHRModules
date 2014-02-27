/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.onprc_ehr.demographics;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.ehr.demographics.AbstractListDemographicsProvider;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: bimber
 * Date: 7/14/13
 * Time: 10:29 AM
 */
public class AssignedVetDemographicsProvider extends AbstractListDemographicsProvider
{
    public AssignedVetDemographicsProvider(Module owner)
    {
        super("study", "demographicsAssignedVet", "assignedVet", owner);
        _supportsQCState = false;
    }

    protected Set<FieldKey> getFieldKeys()
    {
        Set<FieldKey> keys = new HashSet<FieldKey>();
        keys.add(FieldKey.fromString("Id"));
        keys.add(FieldKey.fromString("assignedVet"));
        keys.add(FieldKey.fromString("assignedVetId"));
        keys.add(FieldKey.fromString("assignmentType"));

        return keys;
    }

    @Override
    public boolean requiresRecalc(String schema, String query)
    {
        return "study".equalsIgnoreCase(schema) && ("housing".equalsIgnoreCase(query) || "assignment".equalsIgnoreCase(query));
    }
}
