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
package org.labkey.onprc_ehr.history;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Results;
import org.labkey.api.util.PageFlowUtil;

import java.sql.SQLException;
import java.util.Set;

/**
 * User: bimber
 * Date: 2/17/13
 * Time: 4:52 PM
 */
public class DefaultPairingDataSource extends AbstractEHRDataSource
{
    public DefaultPairingDataSource()
    {
        super("study", "pairings", "Pairing", "Behavior");
    }

    @Override
    protected String getHtml(Results rs, boolean redacted) throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        sb.append(safeAppend(rs, "Event Type", "eventtype"));
        sb.append(safeAppend(rs, "Housing Type", "housingtype"));
        sb.append(safeAppend(rs, "Observation", "observation"));
        sb.append(safeAppend(rs, "Outcome", "outcome"));

        String room1 = rs.getString("room");
        if (!StringUtils.isEmpty(room1))
        {
            sb.append("Location: ").append(room1);
            String cage = rs.getString("cage");
            if (!StringUtils.isEmpty(cage))
            {
                sb.append(" / ").append(cage);
            }
            sb.append("\n");
        }

        sb.append(safeAppend(rs, "Remark", "remark"));
        return sb.toString();
    }

    @Override
    protected Set<String> getColumnNames()
    {
        return PageFlowUtil.set("Id", "date", "enddate", "eventtype", "housingtype", "observation", "outcome", "room", "cage", "remark");
    }
}
