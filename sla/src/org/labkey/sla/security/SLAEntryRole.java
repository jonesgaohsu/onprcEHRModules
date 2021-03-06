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
package org.labkey.sla.security;

import org.labkey.api.data.Container;
import org.labkey.api.ehr.security.EHRDataEntryPermission;
import org.labkey.api.ehr.security.EHRSurgeryEntryPermission;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.security.SecurableResource;
import org.labkey.api.security.SecurityPolicy;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.AbstractModuleScopedRole;
import org.labkey.api.security.roles.AbstractRole;
import org.labkey.sla.SLAModule;

/**
 * User: bimber
 * Date: 1/17/13
 * Time: 7:42 PM
 */
public class SLAEntryRole extends AbstractModuleScopedRole
{
    public SLAEntryRole()
    {
        super("SLA Data Entry", "This role is used to track which users can enter SLA data.", SLAModule.class,
            ReadPermission.class,
            InsertPermission.class,
            UpdatePermission.class,
            DeletePermission.class,
            SLAEntryPermission.class
        );
    }
}
