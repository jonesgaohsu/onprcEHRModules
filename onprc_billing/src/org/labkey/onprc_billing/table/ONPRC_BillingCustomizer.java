package org.labkey.onprc_billing.table;

import org.apache.log4j.Logger;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.EHRService;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.table.AbstractTableCustomizer;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.onprc_billing.ONPRC_BillingManager;
import org.labkey.onprc_billing.ONPRC_BillingSchema;

/**
 * User: bimber
 * Date: 1/7/14
 * Time: 5:57 PM
 */
public class ONPRC_BillingCustomizer extends AbstractTableCustomizer
{
    private static final Logger _log = Logger.getLogger(ONPRC_BillingCustomizer.class);

    // consider moving this helper to AbstractTableCustomizer
    private QueryForeignKey.Builder qfk(AbstractTableInfo ti)
    {
        // default to useRawFkValue if displayField==null
        return QueryForeignKey.from(ti.getUserSchema(), ti.getContainerFilter()).raw(true);
    }

    public void customize(TableInfo table)
    {
        if (table instanceof AbstractTableInfo)
        {
            customizeSharedColumns((AbstractTableInfo)table);

            if (matches(table, "onprc_billing", "invoicedItems"))
            {
                customizeInvoicedItems((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "invoiceRuns"))
            {
                customizeInvoiceRuns((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "miscCharges"))
            {
                customizeMiscCharges((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "dataAccess"))
            {
                customizeDataAccess((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "chargeableItems"))
            {
                customizeChargeableItems((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing_public", "chargeableItems"))
            {
                customizeChargeableItems((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "chargeUnits"))
            {
                customizeChargeUnits((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing_public", "chargeUnits"))
            {
                customizeChargeUnits((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "aliases"))
            {
                customizeAliases((AbstractTableInfo) table);
            }
            else if (matches(table, "onprc_billing", "projectAccountHistory"))
            {
                customizeProjectAccountHistory((AbstractTableInfo) table);
            }
            else if(matches(table, "ehr", "project"))
            {
                customizeProjects((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "labworkFeeRates"))
            {
                addTotalCost((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "leaseFeeRates"))
            {
                addTotalCost((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "miscChargesWithRates"))
            {
                addTotalCost((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "medicationFeeDefinition"))
            {
                customizeMedicationFeeDefinition((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "perDiemRates"))
            {
                addTotalCost((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "procedureFeeRates"))
            {
                addTotalCost((AbstractTableInfo) table);
            }
            else if(matches(table, "onprc_billing", "slaPerDiemRates"))
            {
                addTotalCost((AbstractTableInfo) table);
            }
            else if(matches(table, "study", "Blood Draws") || matches(table, "study", "blood"))
            {
                addChargeUnitLookup((AbstractTableInfo) table, "bloodChargeType");
            }
            else if(matches(table, "study", "Clinical Encounters") || matches(table, "study", "encounters"))
            {
                addChargeUnitLookup((AbstractTableInfo) table, "procedureChargeType");
            }
            else if(matches(table, "study", "Drug Administration") || matches(table, "study", "drug"))
            {
                addChargeUnitLookup((AbstractTableInfo) table, "medicationChargeType");
            }
        }
    }

    private UserSchema _billingUserSchema = null;

    private void addChargeUnitLookup(AbstractTableInfo table, String queryName)
    {
        if (table.getColumn(FieldKey.fromString("chargetype")) != null)
        {
            UserSchema us = getUserSchema(table, "ehr_lookups");
            if (us != null)
            {
                table.getMutableColumn(FieldKey.fromString("chargetype")).setFk( qfk(table).schema(us).to(queryName, "value", null) );
            }
        }

        if (table.getColumn(FieldKey.fromString("assistingstaff")) != null)
        {
            UserSchema us = getUserSchema(table, "onprc_billing_public");
            if (us != null)
            {
                table.getMutableColumn(FieldKey.fromString("assistingstaff")).setFk( qfk(table).schema(us).to("chargeUnits", "chargetype", null) );
            }
        }
    }

    private void customizeMedicationFeeDefinition(AbstractTableInfo ti)
    {
        if (ti.getColumn("code") != null)
        {
            Container ehrContainer = EHRService.get().getEHRStudyContainer(ti.getUserSchema().getContainer());
            if (ehrContainer != null)
            {
                UserSchema us = getUserSchema(ti, "ehr_lookups", ehrContainer);
                if (us != null)
                {
                    ti.getMutableColumn(FieldKey.fromString("code")).setFk( qfk(ti).schema(us).to("snomed", "code", "meaning") );
                }
            }
        }
    }

    private void addTotalCost(AbstractTableInfo ti)
    {
        var unitCost = ti.getMutableColumn("unitCost");
        if (unitCost != null)
        {
            unitCost.setFormat("$###,##0.00");
        }

        if (ti.getColumn("totalCost") == null && unitCost != null && ti.getColumn("quantity") != null)
        {
            SQLFragment sql = new SQLFragment("(" + ExprColumn.STR_TABLE_ALIAS + ".unitCost * " + ExprColumn.STR_TABLE_ALIAS + ".quantity)");
            ExprColumn totalCost = new ExprColumn(ti, "totalCost", sql, JdbcType.DOUBLE, ti.getColumn("unitCost"), ti.getColumn("quantity"));
            totalCost.setLabel("Total Cost");
            totalCost.setFormat("$###,##0.00");
            ti.addColumn(totalCost);
        }

        if (ti.getColumn("nihRate") != null)
        {
            ti.getMutableColumn("nihRate").setLabel("NIH Rate");
            ti.getMutableColumn("nihRate").setFormat("$###,##0.00");
        }
    }

    private UserSchema getBillingUserSchema(AbstractTableInfo table)
    {
        if (_billingUserSchema != null)
        {
            return _billingUserSchema;
        }

        //first try to actual container
        Container c = ONPRC_BillingManager.get().getBillingContainer(table.getUserSchema().getContainer());
        if (c != null && c.hasPermission(table.getUserSchema().getUser(), ReadPermission.class))
        {
            UserSchema us = QueryService.get().getUserSchema(table.getUserSchema().getUser(), c, ONPRC_BillingSchema.NAME);
            if (us != null)
            {
                _billingUserSchema = us;
                return us;
            }
        }

        //then a linked schema
        UserSchema us = QueryService.get().getUserSchema(table.getUserSchema().getUser(), table.getUserSchema().getContainer(), "onprc_billing_public");
        if (us != null)
        {
            _billingUserSchema = us;
            return us;
        }

        return null;
    }

    private void customizeDataAccess(AbstractTableInfo table)
    {
        Container billingContainer = ONPRC_BillingManager.get().getBillingContainer(table.getUserSchema().getContainer());
        if (billingContainer == null)
        {
            return;
        }

        Container publicContainer = billingContainer.getChild("Public");
        if (publicContainer == null)
        {
            return;
        }

        UserSchema us = getUserSchema(table, "core", publicContainer);
        if (us == null)
        {
            return;
        }

        table.getMutableColumn("userid").setFk(new QueryForeignKey(us, publicContainer, "UsersAndGroups", "UserId", "DisplayName"));
    }

    private void customizeMiscCharges(AbstractTableInfo table)
    {
        UserSchema us = getBillingUserSchema(table);
        if (us == null)
        {
            return;
        }

        var invoicedItemId = table.getMutableColumn("invoicedItemId");
        if (invoicedItemId != null)
        {
            invoicedItemId.setFk( qfk(table).schema(us).to("invoicedItems", "objectid", "rowid"));
        }

        var sourceInvoicedItem = table.getMutableColumn("sourceInvoicedItem");
        if (sourceInvoicedItem != null)
        {
            sourceInvoicedItem.setFk( qfk(table).schema(us).to("invoicedItems", "objectid", "transactionNumber") );
        }

        var invoiceId = table.getMutableColumn("invoiceId");
        if (invoiceId != null)
        {
            invoiceId.setFk( qfk(table).schema(us).to("invoiceRuns", "objectid", "rowid") );
        }

        var chargeType = table.getMutableColumn("chargeType");
        if (chargeType != null)
        {
            chargeType.setFk( qfk(table).schema(us).to("chargeUnits", "chargeType", null) );
        }

        addAliasLookup(table, "debitedaccount");
        addAliasLookup(table, "creditedaccount");
    }

    private void customizeInvoicedItems(AbstractTableInfo table)
    {
        table.getButtonBarConfig().setAlwaysShowRecordSelectors(true);
        table.setDetailsURL(DetailsURL.fromString("/onprc_billing/invoicedItemDetails.view?invoicedItem=${objectid}"));

        var col = table.getMutableColumn("invoicedItemId");
        if (col != null)
        {
            UserSchema us = getBillingUserSchema(table);
            if (us != null)
            {
                col.setFk( qfk(table).schema(us).to("invoicedItems", "objectid", "rowid") );
            }
        }

        var idCol = table.getMutableColumn("Id");
        if (idCol != null)
        {
            Container ehrContainer = EHRService.get().getEHRStudyContainer(table.getUserSchema().getContainer());
            if (ehrContainer != null)
            {
                idCol.setFk( qfk(table).schema("study", ehrContainer).to("animal", "Id", "Id") );
                EHRService.get().appendCalculatedIdCols(table, "date");
            }
        }

        addAliasLookup(table, "debitedaccount");
        addAliasLookup(table, "creditedaccount");

        Container ehrContainer = EHRService.get().getEHRStudyContainer(table.getUserSchema().getContainer());
        if (ehrContainer != null)
        {
            String pendingAdjustments = "pendingAdjustments";
            if (table.getColumn(pendingAdjustments) == null)
            {
                SQLFragment sql = new SQLFragment("(SELECT count(*) FROM " + ONPRC_BillingSchema.NAME + "." + ONPRC_BillingSchema.TABLE_MISC_CHARGES + " mc WHERE mc.invoiceId IS NULL AND mc.container = ? AND mc.sourceInvoicedItem = " + ExprColumn.STR_TABLE_ALIAS + ".objectid)", ehrContainer.getId());
                ExprColumn newCol = new ExprColumn(table, pendingAdjustments, sql, JdbcType.INTEGER, table.getColumn("objectid"));
                newCol.setLabel("Pending Adjustments");
                newCol.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=onprc_billing&query.queryName=miscChargesWithRates&query.viewName=Adjustment Detail&query.invoiceId~isblank&query.sourceInvoicedItem/transactionNumber~eq=${transactionNumber}", ehrContainer));
                table.addColumn(newCol);
            }

            String totalAdjustments = "totalAdjustments";
            if (table.getColumn(totalAdjustments) == null)
            {
                SQLFragment sql = new SQLFragment("(SELECT count(*) FROM " + ONPRC_BillingSchema.NAME + "." + ONPRC_BillingSchema.TABLE_MISC_CHARGES + " mc WHERE mc.container = ? AND mc.sourceInvoicedItem = " + ExprColumn.STR_TABLE_ALIAS + ".objectid)", ehrContainer.getId());
                ExprColumn newCol = new ExprColumn(table, totalAdjustments, sql, JdbcType.INTEGER, table.getColumn("objectid"));
                newCol.setLabel("All Adjustments");
                newCol.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=onprc_billing&query.queryName=miscChargesWithRates&query.viewName=Adjustment Detail&query.sourceInvoicedItem/transactionNumber~eq=${transactionNumber}", ehrContainer));
                table.addColumn(newCol);
            }
        }
    }

    private void addAliasLookup(AbstractTableInfo table, String sourceColName)
    {
        var sourceCol = table.getMutableColumn(sourceColName);
        if (sourceCol != null && sourceCol.getFk() == null)
        {
            UserSchema us = getUserSchema(table, "onprc_billing_public");
            if (us == null)
            {
                us = getUserSchema(table, "onprc_billing");
            }

            if (us != null)
            {
                sourceCol.setFk( qfk(table).schema(us).to("aliases", "alias", "alias") );
                sourceCol.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=onprc_billing_public&query.queryName=aliases&query.alias~eq=${" + sourceColName + "}"));
            }
        }
    }

    private void customizeSharedColumns(AbstractTableInfo ti)
    {
        boolean found = false;
        for (String field : new String[]{"grant", "grantNumber"})
        {
            if (found)
                continue; //a table should never contain both of these anyway

            var grant = ti.getMutableColumn(field);
            if (grant != null)
            {
                found = true;
                if (!ti.getName().equalsIgnoreCase("grants") && grant.getFk() == null)
                {
                    UserSchema us = getUserSchema(ti, "onprc_billing_public");
                    if (us != null)
                        grant.setFk(QueryForeignKey.from(us, ti.getContainerFilter())
                                .table("grants")
                                .key("grantNumber")
                                .display("grantNumber"));
                }
            }
        }

        var account = ti.getMutableColumn("account");
        if (account != null && !ti.getName().equalsIgnoreCase("accounts"))
        {
            account.setLabel("Alias");
            if (account.getFk() == null)
            {
                UserSchema us = getUserSchema(ti, "onprc_billing_public");
                if (us != null)
                {
                    account.setFk( qfk(ti).schema(us).to("aliases", "alias", "alias") );
                    account.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=onprc_billing_public&query.queryName=aliases&query.alias~eq=${account}", us.getContainer()));
                }
            }
        }

        var projectNumber = ti.getMutableColumn("projectNumber");
        if (projectNumber != null && !ti.getName().equalsIgnoreCase("grantProjects"))
        {
            UserSchema us = getUserSchema(ti, "onprc_billing_public");
            if (us != null)
            {
                projectNumber.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=onprc_billing_public&query.queryName=aliases&query.projectNumber~eq=${projectNumber}", us.getContainer()));
            }
        }

        var chargeId = ti.getMutableColumn("chargeId");
        if (chargeId != null)
        {
            UserSchema us = getUserSchema(ti, "onprc_billing_public");
            if (us != null){
                chargeId.setFk(QueryForeignKey.from(us, ti.getContainerFilter())
                        .table("chargeableItems")
                        .key("rowid")
                        .display("name"));
            }
            chargeId.setLabel("Charge Name");
        }

        var rateId = ti.getMutableColumn("rateId");
        if (rateId != null)
        {
            UserSchema us = getUserSchema(ti, "onprc_billing_public");
            if (us != null){
                rateId.setFk(QueryForeignKey.from(us, ti.getContainerFilter())
                        .table("chargeableRates")
                        .key("rowid")
                        .display("rowid"));
            }
            rateId.setLabel("Rate");
        }

        var exemptionId = ti.getMutableColumn("exemptionId");
        if (exemptionId != null)
        {
            UserSchema us = getUserSchema(ti, "onprc_billing_public");
            if (us != null){
                exemptionId.setFk(QueryForeignKey.from(us, ti.getContainerFilter())
                        .table("chargeableRateExemptions")
                        .key("rowid")
                        .display("rowid"));
            }
            exemptionId.setLabel("Rate Exemption");
        }

        var chargeType = ti.getMutableColumn("chargetype");
        if (chargeType != null)
        {
            chargeType.setLabel("Charge Unit");
        }

        var creditAccountType = ti.getMutableColumn("creditAccountType");
        if (creditAccountType != null)
        {
            creditAccountType.setLabel("Credit Alias Based On");
        }

        var currentActiveAlias = ti.getMutableColumn("currentActiveAlias");
        if (currentActiveAlias != null)
        {
            currentActiveAlias.setLabel("Currently Active Alias For Project");
            currentActiveAlias.setDescription("This shows the alias that is currently active for this project as of today's date, which is not necessarily the same alias as the one used for this transaction.  The latter will use the alias active on the transaction date.");
        }
    }

    private void customizeChargeUnits(AbstractTableInfo ti)
    {
        String activeAccount = "activeAccount";
        if (ti.getColumn(activeAccount) == null)
        {
            SQLFragment sql = new SQLFragment("(SELECT max(account) as expr FROM " + ONPRC_BillingSchema.NAME + "." + ONPRC_BillingSchema.TABLE_CHARGE_UNIT_ACCOUNT + " cr WHERE cr.chargetype = " + ExprColumn.STR_TABLE_ALIAS + ".chargetype AND (cr.enddate IS NULL OR cr.enddate > {fn curdate()}) AND cr.startdate <= {fn curdate()})");
            ExprColumn col = new ExprColumn(ti, activeAccount, sql, JdbcType.VARCHAR, ti.getColumn("chargetype"));
            col.setLabel("Active Alias");
            col.setIsUnselectable(true);
            ti.addColumn(col);
        }

        //NOTE: this is separated to allow linked schemas to use the same column
        UserSchema us = getUserSchema(ti, "onprc_billing_public");
        if (us == null)
        {
            us = getUserSchema(ti, "onprc_billing");
        }

        if (us != null)
        {
            ti.getMutableColumn(activeAccount).setFk(QueryForeignKey.from(us, ti.getContainerFilter())
                    .table(ONPRC_BillingSchema.TABLE_ALIASES)
                    .key("alias")
                    .display("alias"));
        }
    }

    private void customizeChargeableItems(AbstractTableInfo ti)
    {
        String activeRate = "activeRate";
        if (ti.getColumn(activeRate) == null)
        {
            SQLFragment sql = new SQLFragment("(SELECT max(rowid) as expr FROM " + ONPRC_BillingSchema.NAME + "." + ONPRC_BillingSchema.TABLE_CHARGE_RATES + " cr WHERE cr.chargeid = " + ExprColumn.STR_TABLE_ALIAS + ".rowid AND (cr.enddate IS NULL OR cr.enddate > {fn curdate()}) AND cr.startdate <= {fn curdate()})");
            ExprColumn col = new ExprColumn(ti, activeRate, sql, JdbcType.INTEGER, ti.getColumn("rowid"));
            col.setLabel("Active Rate");
            col.setIsUnselectable(true);
            ti.addColumn(col);
        }
        //NOTE: this is separated to allow linked schemas to use the same column
        ti.getMutableColumn(activeRate).setFk(QueryForeignKey.from(ti.getUserSchema(), ti.getContainerFilter())
                .table(ONPRC_BillingSchema.TABLE_CHARGE_RATES)
                .key("rowid")
                .display("rowid"));

        String totalExemptions = "totalExemptions";
        if (ti.getColumn(totalExemptions) == null)
        {
            SQLFragment sql = new SQLFragment("(SELECT count(rowid) as expr FROM " + ONPRC_BillingSchema.NAME + "." + ONPRC_BillingSchema.TABLE_CHARGE_RATE_EXEMPTIONS + " cr WHERE cr.chargeid = " + ExprColumn.STR_TABLE_ALIAS + ".rowid AND (cr.enddate IS NULL OR cr.enddate > {fn curdate()}) AND cr.startdate <= {fn curdate()})");
            ExprColumn col = new ExprColumn(ti, totalExemptions, sql, JdbcType.INTEGER, ti.getColumn("rowid"));
            col.setLabel("# Active Exemptions");
            col.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=" + ONPRC_BillingSchema.NAME + "&query.queryName=" + ONPRC_BillingSchema.TABLE_CHARGE_RATE_EXEMPTIONS + "&query.chargeId~eq=${rowid}"));
            ti.addColumn(col);
        }

        String activeCreditAccount = "activeCreditAccount";
        if (ti.getColumn(activeCreditAccount) == null)
        {
            SQLFragment sql = new SQLFragment("(SELECT max(rowid) as expr FROM " + ONPRC_BillingSchema.NAME + "." + ONPRC_BillingSchema.TABLE_CREDIT_ACCOUNT + " cr WHERE cr.chargeid = " + ExprColumn.STR_TABLE_ALIAS + ".rowid AND (cr.enddate IS NULL OR cr.enddate > {fn curdate()}) AND cr.startdate <= {fn curdate()})");
            ExprColumn col = new ExprColumn(ti, activeCreditAccount, sql, JdbcType.INTEGER, ti.getColumn("rowid"));
            col.setLabel("Active Credit Alias");
            col.setIsUnselectable(true);
            ti.addColumn(col);
        }
        //NOTE: this is separated to allow linked schemas to use the same column
        ti.getMutableColumn(activeCreditAccount).setFk(QueryForeignKey.from(ti.getUserSchema(), ti.getContainerFilter())
                .table(ONPRC_BillingSchema.TABLE_CREDIT_ACCOUNT)
                .key("rowid")
                .display("rowid"));
    }

    private void customizeProjectAccountHistory(AbstractTableInfo ti)
    {
        String name = "isActive";
        if (ti.getColumn(name) == null)
        {
            SQLFragment sql = getIsActiveSql(ti);
            ExprColumn col = new ExprColumn(ti, name, sql, JdbcType.BOOLEAN, ti.getColumn("startdate"), ti.getColumn("enddate"));
            col.setLabel("Is Active?");
            ti.addColumn(col);
        }
    }

    private void customizeProjects(AbstractTableInfo ti)
    {
        ColumnInfo accountCol = ti.getColumn("account");
        ti.removeColumn(accountCol);
        SQLFragment sql4 = new SQLFragment("(SELECT ").append(ti.getSqlDialect().getGroupConcat(new SQLFragment("pa.account"), true, true)).append(" as expr FROM onprc_billing.projectAccountHistory pa WHERE pa.project = " + ExprColumn.STR_TABLE_ALIAS + ".project AND (").append(getIsActiveSql(ti, "pa")).append(") = " + ti.getSqlDialect().getBooleanTRUE() + ")");
        ExprColumn newAccountCol = new ExprColumn(ti, "account", sql4, JdbcType.VARCHAR, ti.getColumn("project"));
        newAccountCol.setLabel("Alias");
        if (newAccountCol.getFk() == null)
        {
            UserSchema us = getUserSchema(ti, "onprc_billing_public");
            if (us != null)
            {
                newAccountCol.setFk( qfk(ti).schema(us).to("aliases", "alias", "alias") );
                newAccountCol.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=onprc_billing_public&query.queryName=aliases&query.alias~eq=${account}", us.getContainer()));
            }
        }

        ti.addColumn(newAccountCol);

        //add column showing highest alias expiration
        String maxAliasEnd = "maxAliasEnd";
        if (ti.getColumn(maxAliasEnd) == null)
        {
            SQLFragment sql = new SQLFragment("(SELECT max(pa.enddate) as expr FROM onprc_billing.projectAccountHistory pa WHERE pa.project = " + ExprColumn.STR_TABLE_ALIAS + ".project AND pa.enddate IS NOT NULL)");
            ExprColumn maxAliasEndCol = new ExprColumn(ti, maxAliasEnd, sql, JdbcType.TIMESTAMP, ti.getColumn("project"));
            maxAliasEndCol.setLabel("Max Alias Expiration");
            maxAliasEndCol.setHidden(true);
            maxAliasEndCol.setDescription("This columns shows the highest date where an alias is associated with this account, which is a proxy for when the alias association will expire.  This is not perfect, since this does not test if there is an active alias today (that other alias could start in the future)");
            ti.addColumn(maxAliasEndCol);
        }

    }

    private SQLFragment getIsActiveSql(AbstractTableInfo ti)
    {
        return getIsActiveSql(ti, ExprColumn.STR_TABLE_ALIAS);
    }

    //NOTE: this will consider the record to be active on the enddate itself
    private SQLFragment getIsActiveSql(AbstractTableInfo ti, String tableAlias)
    {
        return new SQLFragment("(CASE " +
                // when the start is in the future, using whole-day increments, it is not active
                " WHEN (CAST(" + tableAlias + ".startdate as DATE) > {fn curdate()}) THEN " + ti.getSqlDialect().getBooleanFALSE() + "\n" +
                // when enddate is null, it is active
                " WHEN (" + tableAlias + ".enddate IS NULL) THEN " + ti.getSqlDialect().getBooleanTRUE() + "\n" +
                // if enddate is in the future (whole-day increments), then it is active
                " WHEN (CAST(" + tableAlias + ".enddate AS DATE) >= {fn curdate()}) THEN " + ti.getSqlDialect().getBooleanTRUE() + "\n" +
                " ELSE " + ti.getSqlDialect().getBooleanFALSE() + "\n" +
                " END)");
    }

    private void customizeAliases(AbstractTableInfo ti)
    {
        LDKService.get().appendCalculatedDateColumns(ti, null, "budgetEndDate");

        var aliasType = ti.getMutableColumn(FieldKey.fromString("aliasType"));
        if (aliasType != null && aliasType.getFk() == null)
        {
            UserSchema us = getUserSchema(ti, "onprc_billing_public");
            if (us != null)
            {
                aliasType.setFk( qfk(ti).schema(us).to("aliasTypes", "aliasType", null) );
            }
        }
    }

    private void customizeInvoiceRuns(AbstractTableInfo table)
    {
        EHRService.get().customizeDateColumn(table, "billingPeriodStart");
    }
}
