/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.labkey.api.ehr.demographics.AbstractListDemographicsProvider;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//Created: 3-10-2017  R.Blasa
public class FosterChildDemographicsProvider extends AbstractListDemographicsProvider
{
    public FosterChildDemographicsProvider(Module module)
    {
        super(module, "study", "FosterChildInfo", "fosterchild");
        _supportsQCState = false;
    }

    protected Collection<FieldKey> getFieldKeys()
    {
        Set<FieldKey> keys = new HashSet<>();
        keys.add(FieldKey.fromString("Id"));
        keys.add(FieldKey.fromString("date"));
        keys.add(FieldKey.fromString("FosterChild"));
        keys.add(FieldKey.fromString("relationship"));
        keys.add(FieldKey.fromString("method"));

        return keys;
    }

    @Override
    public boolean requiresRecalc(String schema, String query)
    {
        return ("study".equalsIgnoreCase(schema) && "Parentage".equalsIgnoreCase(query)) ||
                ("study".equalsIgnoreCase(schema) && "Birth".equalsIgnoreCase(query)) ||
                ("study".equalsIgnoreCase(schema) && "Demographics".equalsIgnoreCase(query));
    }

}
