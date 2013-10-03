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
package org.labkey.onprc_ehr;

import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: bimber
 * Date: 9/21/13
 * Time: 9:55 AM
 */
public class ONPRC_EHRManager
{
    private static ONPRC_EHRManager _instance = new ONPRC_EHRManager();

    private ONPRC_EHRManager()
    {

    }

    public static ONPRC_EHRManager get()
    {
        return _instance;
    }

    public List<String> deleteBillingRuns(User user, Collection<String> pks, boolean testOnly)
    {
        TableInfo invoiceRuns = ONPRC_EHRSchema.getInstance().getBillingSchema().getTable(ONPRC_EHRSchema.TABLE_INVOICE_RUNS);
        TableInfo invoicedItems = ONPRC_EHRSchema.getInstance().getBillingSchema().getTable(ONPRC_EHRSchema.TABLE_INVOICED_ITEMS);
        TableInfo miscCharges = ONPRC_EHRSchema.getInstance().getBillingSchema().getTable(ONPRC_EHRSchema.TABLE_MISC_CHARGES);

        //create filters
        SimpleFilter invoiceRunFilter = new SimpleFilter(FieldKey.fromString("invoiceId"), pks, CompareType.IN);

        SimpleFilter adjustmentFilter = new SimpleFilter(FieldKey.fromString("invoiceId"), pks, CompareType.IN);
        adjustmentFilter.addCondition(FieldKey.fromString("invoicedItemId"), null, CompareType.NONBLANK);

        SimpleFilter miscChargesFilter = new SimpleFilter(FieldKey.fromString("invoiceId"), pks, CompareType.IN);
        miscChargesFilter.addCondition(FieldKey.fromString("invoicedItemId"), null, CompareType.ISBLANK);

        //perform the work
        List<String> ret = new ArrayList<>();
        if (testOnly)
        {
            TableSelector tsRuns = new TableSelector(invoicedItems, invoiceRunFilter, null);
            ret.add(tsRuns.getRowCount() + " records from invoiced items");

            TableSelector tsMiscCharges = new TableSelector(miscCharges, adjustmentFilter, null);
            ret.add(tsMiscCharges.getRowCount() + " records of adjustments or reversals that will be deleted.");

            TableSelector tsMiscCharges2 = new TableSelector(miscCharges, miscChargesFilter, null);
            ret.add(tsMiscCharges2.getRowCount() + " records from misc charges will be removed from the deleted invoice, which means they will be picked up by the next invoice.  They are not deleted.");
        }
        else
        {
            ExperimentService.get().ensureTransaction();
            try
            {
                long deleted1 = Table.delete(invoicedItems, invoiceRunFilter);

                //TODO: should I actually update these and set invoiceItemId to null instead?
                long deleted2 = Table.delete(miscCharges, adjustmentFilter);


                TableSelector tsMiscCharges2 = new TableSelector(miscCharges, Collections.singleton("objectid"), miscChargesFilter, null);
                String[] miscChargesIds = tsMiscCharges2.getArray(String.class);
                for (String objectid : miscChargesIds)
                {
                    Map<String, Object> map = new CaseInsensitiveHashMap<>();
                    map.put("invoiceId", null);
                    map = Table.update(user, miscCharges, map, objectid);
                }

                long deleted3 = Table.delete(invoiceRuns, new SimpleFilter(FieldKey.fromString("objectid"), pks, CompareType.IN));

                ExperimentService.get().commitTransaction();
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
            finally
            {
                ExperimentService.get().closeTransaction();
            }
        }

        return ret;
    }

}

