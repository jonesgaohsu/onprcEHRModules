/*
 * Copyright (c) 2015-2018 LabKey Corporation
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
package org.labkey.onprc_ehr.notification;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.security.User;
import org.labkey.api.ehr.notification.AbstractEHRNotification;

import java.util.Date;

//Added 1-12-2016  Blasa

public class MensesTMBNotification extends AbstractEHRNotification
{
    public MensesTMBNotification(Module owner)
    {
        super(owner);
    }

    @Override
    public String getName()
    {
        return "TMB Menses Notification";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "TMB Menses Notification: " + getDateTimeFormat(c).format(new Date());
    }

    @Override
    public String getCronString()
    {
        return null;
    }

    @Override
    public String getScheduleDescription()
    {
        return "Sent immediately when a TMB Menses Observation is Updated";
    }

    @Override
    public String getDescription()
    {
        return "The report sends an alert whenever an animal is added into Menses Observation record.";
    }

    @Override
    public String getMessageBodyHTML(Container c, User u)
    {
        //this is used as a placeholder so we can use it to track the list of subscribed users
        return null ;
    }
}
