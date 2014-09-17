/*
 * Copyright (c) 2012-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var console = require("console");
var LABKEY = require("labkey");
var ETL = require("onprc_ehr/etl").EHR.ETL;
var onprc_utils = require("onprc_ehr/utils").ONPRC_EHR.Utils;
var triggerHelper = new org.labkey.onprc_ehr.query.ONPRC_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

exports.init = function(EHR){
    EHR.ETL = ETL;

    EHR.Server.TriggerManager.registerHandler(EHR.Server.TriggerManager.Events.INIT, function(event, helper){
        helper.setScriptOptions({
            cacheAccount: false,
            //NOTE: this deliberately omits assignment, since it will be handled separately
            datasetsToClose: ['Cases', 'Housing', 'Treatment Orders', 'Notes', 'Problem List', 'Animal Record Flags', 'Diet', 'Animal Group Members']
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'Treatment Orders', function(event, helper){
        helper.setScriptOptions({
            announceAllModifiedParticipants: true
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'Birth', function(event, helper){
        //NOTE: births are sometimes not entered until processing, meaning they could be significantly in the past
        helper.setScriptOptions({
            allowDatesInDistantPast: true
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'Clinpath Runs', function(event, helper){
        helper.setScriptOptions({
            allowDeadIds: false,
            //this was changed to allow merge records to insert, even if we lack a demographics record
            allowAnyId: true
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'Assignment', function(event, helper){
        helper.setScriptOptions({
            doStandardProtocolCountValidation: false
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'Deaths', function(event, helper){
        helper.setScriptOptions({
            allowDatesInDistantPast: true
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Tissue Samples', function(helper, scriptErrors, row, oldRow){
        if (!row.weight && !row.noWeight){
            EHR.Server.Utils.addError(scriptErrors, 'weight', 'A weight is required unless \'No Weight\' is checked', 'WARN');
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Clinical Encounters', function(helper, scriptErrors, row, oldRow){
        if (row.chargetype == 'Research Staff' && !row.assistingstaff && row.procedureid && triggerHelper.requiresAssistingStaff(row.procedureid)){
            EHR.Server.Utils.addError(scriptErrors, 'chargetype', 'If choosing Research Staff, you must enter the assisting staff.', 'WARN');
        }

        if (row.chargetype != 'Research Staff' && row.assistingstaff){
            EHR.Server.Utils.addError(scriptErrors, 'assistingstaff', 'This field will be ignored unless Research Staff is selected, and should be blank.', 'WARN');
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'Animal Record Flags', function(helper, scriptErrors, row, oldRow){
        if (row.flag && row.Id && !row.enddate){
            var msg = triggerHelper.validateHousingConditionInsert(row.Id, row.flag, row.objectid);
            if (msg){
                EHR.Server.Utils.addError(scriptErrors, 'flag', msg, 'ERROR');
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'Assignment', function(helper, scriptErrors, row, oldRow){
        //check number of allowed animals at assign/approve time.  use different behavior than core EHR
        if (!helper.isETL() && !helper.isQuickValidation() &&
            //this is designed to always perform the check on imports, but also updates where the Id was changed
                !(oldRow && oldRow.Id && oldRow.Id==row.Id) &&
                row.Id && row.project && row.date
        ){
            var assignmentsInTransaction = helper.getProperty('assignmentsInTransaction');
            assignmentsInTransaction = assignmentsInTransaction || [];

            var msgs = triggerHelper.verifyProtocolCounts(row.Id, row.project, assignmentsInTransaction);
            if (msgs){
                msgs = msgs.split("<>");
                for (var i=0;i<msgs.length;i++){
                    EHR.Server.Utils.addError(scriptErrors, 'project', msgs[i], 'INFO');
                }
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'ehr', 'project', function(helper, scriptErrors, row){
        if (!row.project){
            row.project = triggerHelper.getNextProjectId();
            console.log('setting projectId: ' + row.project);
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'ehr', 'protocol', function(helper, scriptErrors, row){
        if (!row.protocol){
            row.protocol = triggerHelper.getNextProtocolId().toString();
            console.log('setting protocol: ' + row.protocol);
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Assignment', function(helper, scriptErrors, row, oldRow){
        if (row.enddate && !row.releaseCondition){
            EHR.Server.Utils.addError(scriptErrors, 'releaseCondition', 'Must provide the release condition when the release date is set', 'WARN');
        }

        if (row.enddate && !row.releaseType){
            EHR.Server.Utils.addError(scriptErrors, 'releaseType', 'Must provide the release type when the release date is set', 'WARN');
        }

        //update condition on release
        if (!helper.isETL() && helper.getEvent() == 'update' && oldRow){
            if (EHR.Server.Security.getQCStateByLabel(row.QCStateLabel).PublicData && EHR.Server.Security.getQCStateByLabel(oldRow.QCStateLabel).PublicData){
                if (row.releaseCondition && row.enddate){
                    triggerHelper.updateAnimalCondition(row.Id, row.enddate, row.releaseCondition);
                }
            }
        }

        // we want to record the date a record was marked endded, in addition to the actual end itself
        // NOTE: we only do this when both enddate and releaseType are entered
        if (!row.enddatefinalized && row.enddate && row.releaseCondition && EHR.Server.Security.getQCStateByLabel(row.QCStateLabel).PublicData){
            //note: if ended in the future, defer to that date
            row.enddatefinalized = new Date();
            if (row.enddate.getTime() > row.enddatefinalized.getTime()){
                row.enddatefinalized = row.enddate;
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Assignment', function(scriptErrors, helper, row, oldRow){
        if (!helper.isETL() && row.Id && row.assignCondition){
            triggerHelper.updateAnimalCondition(row.Id, row.date, row.assignCondition);
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'animal_group_members', function(helper, scriptErrors, row, oldRow){
        if (!helper.isETL() && row.Id && row.groupid && !row.enddate){
            var msg = triggerHelper.getOverlappingGroupAssignments(row.Id, row.objectid);
            if (msg){
                EHR.Server.Utils.addError(scriptErrors, 'groupId', msg, 'INFO');
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPSERT, 'study', 'Birth', function(helper, errors, row, oldRow) {
        //NOTE: we want to perform the birth updates if this row is becoming public, or if we're updating to set the dam for the first time
        if (!helper.isValidateOnly() && (row._becomingPublicData || (oldRow && !oldRow.dam && EHR.Server.Security.getQCStateByLabel(row.QCStateLabel).PublicData && row.dam))) {
            triggerHelper.doBirthTriggers(row.Id, row.date, row.dam || null, row.cond || null, !!row._becomingPublicData);
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Birth', function(helper, scriptErrors, row, oldRow) {
        //also allow changes here to update the demographics record
        //this should only occur if updating an already public record, not just for onBecomePublic, which is a one-time call
        if (!helper.isValidateOnly() && row.Id && !helper.isGeneratedByServer() && oldRow && EHR.Server.Security.getQCStateByLabel(oldRow.QCStateLabel).PublicData && EHR.Server.Security.getQCStateByLabel(row.QCStateLabel).PublicData){
            EHR.Server.Utils.findDemographics({
                participant: row.Id,
                helper: helper,
                scope: this,
                callback: function (data) {
                    if (!data)
                        return;

                    var obj = {};
                    var hasUpdates = false;

                    if (row.gender && row.gender != data.gender) {
                        obj.gender = row.gender;
                        hasUpdates = true;
                    }

                    if (row['Id/demographics/species'] && row['Id/demographics/species'] != data.species) {
                        obj.species = row['Id/demographics/species'];
                        hasUpdates = true;
                    }

                    if (row.dam && !data.species && !obj.species){
                        var damSpecies = triggerHelper.getSpeciesForDam(row.dam);
                        if (damSpecies){
                            obj.species = damSpecies;
                            hasUpdates = true;
                        }
                    }

                    if (row['Id/demographics/geographic_origin'] && row['Id/demographics/geographic_origin'] != data.geographic_origin){
                        obj.geographic_origin = row['Id/demographics/geographic_origin'];
                        hasUpdates = true;
                    }

                    if (row.date && row.date.getTime() != (data.birth ? data.birth.getTime() : 0)) {
                        obj.birth = row.date;
                        hasUpdates = true;
                    }

                    //update death date in demographics if born dead
                    if (row.Id && row.date && !data.death && row.cond && ['Born Dead', 'Terminated At Birth'].indexOf(row.cond) > -1){
                        obj.death = row.date;
                        hasUpdates = true;

                        //if this is the first time a birth condition was entered, treat this the same as when a death is entered
                        if (oldRow && !oldRow.cond){
                            helper.onDeathDeparture(row.Id, row.date);
                        }
                    }

                    if (hasUpdates){
                        obj.Id = row.Id;
                        helper.getJavaHelper().updateDemographicsRecord(row.Id, obj);
                    }
                }
            });
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Deaths', function(scriptErrors, helper, row, oldRow){
        if (row.Id && row.date){
            //close assignment records.
            triggerHelper.closeActiveAssignmentRecords(row.Id, row.date, (row.cause || null));
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Drug Administration', function(helper, scriptErrors, row, oldRow){
        if (row.outcome && row.outcome != 'Normal' && !row.remark){
            EHR.Server.Utils.addError(scriptErrors, 'remark', 'A remark is required if a non-normal outcome is reported', 'WARN');
        }

        if (!row.code){
            EHR.Server.Utils.addError(scriptErrors, 'code', 'Must enter a treatment', 'WARN');
        }
        else if ((row.code == 'E-70590' || row.code == 'E-YY928') && (row.amount || row.volume) && (!row.amount || !row.amount_units || row.amount_units.toLowerCase() != 'mg')){
            EHR.Server.Utils.addError(scriptErrors, 'amount_units', 'When entering ketamine or telazol, amount must be in mg', 'WARN');
        }

        if (!row.amount && !row.volume){
            EHR.Server.Utils.addError(scriptErrors, 'amount', 'Must enter an amount or volume', 'WARN');
            EHR.Server.Utils.addError(scriptErrors, 'volume', 'Must enter an amount or volume', 'WARN');
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'Treatment Orders', function(helper, scriptErrors, row){
        if (row.date && ((new Date()).getTime() - row.date.getTime()) > 43200000){  //12 hours in past
            EHR.Server.Utils.addError(scriptErrors, 'date', 'You are ordering a treatment that starts in the past', 'INFO');
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'Blood Draws', function(helper, scriptErrors, row){
        if (!helper.isETL() && row.project && row.chargetype){
            if (row.chargetype == 'No Charge' && !helper.getJavaHelper().isDefaultProject(row.project))
                EHR.Server.Utils.addError(scriptErrors, 'chargetype', 'You have chosen \'No Charge\' with a project that does not support this.  You may need to choose \'Research\' instead (which is not billed)', 'INFO');
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Housing', function(helper, scriptErrors, row, oldRow){
        onprc_utils.doHousingCheck(EHR, helper, scriptErrors, triggerHelper, row, oldRow);

        //also attempt to update dividers
        var msgs = onprc_utils.doUpdateDividers(EHR, row, helper, scriptErrors, triggerHelper, true);
        if (msgs){
            msgs = msgs.split("<>");
            for (var i=0;i<msgs.length;i++){
                EHR.Server.Utils.addError(scriptErrors, 'divider', msgs[i], 'INFO');
            }
        }

        //dont allow open ended housing for non-living animals
        if (!row.enddate && row.Id){
            EHR.Server.Utils.findDemographics({
                participant: row.Id,
                helper: helper,
                scope: this,
                callback: function(data){
                    if (data){
                        if (data.calculated_status == 'Unknown' || !data.calculated_status){
                            EHR.Server.Utils.addError(scriptErrors, 'enddate', 'There is no record of this animal, cannot enter an open ended housing record', 'WARN');
                        }
                        else if (data.calculated_status != 'Alive'){
                            EHR.Server.Utils.addError(scriptErrors, 'enddate', 'This animal is not listed as alive, cannot enter an open ended housing record', 'WARN');
                        }
                    }
                }
            });
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Housing', function(scriptErrors, helper, row, oldRow){
        if (!helper.isETL() && row){
            //mark associated requests complete
            if (row.parentid){
                triggerHelper.markHousingTransferRequestsComplete(row.parentid);
            }

            //also attempt to update dividers
            var msgs = onprc_utils.doUpdateDividers(EHR, row, helper, scriptErrors, triggerHelper, helper.isValidateOnly());
            if (msgs){
                msgs = msgs.split("<>");
                for (var i=0;i<msgs.length;i++){
                    EHR.Server.Utils.addError(scriptErrors, 'divider', msgs[i], 'INFO');
                }
            }

            if (row.room){
                row.housingCondition = row.housingCondition || triggerHelper.getHousingCondition(row.room);
                row.housingType = row.housingType || triggerHelper.getHousingType(row.room);
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'onprc_ehr', 'housing_transfer_requests', function(helper, scriptErrors, row, oldRow){
        onprc_utils.doHousingCheck(EHR, helper, scriptErrors, triggerHelper, row, oldRow);
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Arrival', function(helper, scriptErrors, row, oldRow){
        onprc_utils.doHousingCheck(EHR, helper, scriptErrors, triggerHelper, row, oldRow, 'initialRoom', 'initialCage', false);
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Arrival', function(scriptErrors, helper, row, oldRow) {
        if (row.Id && row.date) {
            triggerHelper.ensureQuarantineFlag(row.Id, row.date);
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Birth', function(helper, scriptErrors, row, oldRow){
        onprc_utils.doHousingCheck(EHR, helper, scriptErrors, triggerHelper, row, oldRow, null, null, false);
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Treatment Orders', function(scriptErrors, helper, row, oldRow){
        var fieldPairs = [
            ['dosage_units', 'dosage'],
            ['conc_units', 'concentration'],
            ['vol_units', 'volume'],
            ['amount_units', 'amount']
        ];

        //if we have a value entered for units, but no value in the corresponding numeric field, clear units
        for (var i=0;i<fieldPairs.length;i++){
            var pair = fieldPairs[i];
            if (row[pair[0]] && LABKEY.ExtAdapter.isEmpty(row[pair[1]])){
                row[pair[0]] = null;
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Drug Administration', function(scriptErrors, helper, row, oldRow){
        var fieldPairs = [
            ['dosage_units', 'dosage'],
            ['conc_units', 'concentration'],
            ['vol_units', 'volume'],
            ['amount_units', 'amount']
        ];

        //if we have a value entered for units, but no value in the corresponding numeric field, clear units
        for (var i=0;i<fieldPairs.length;i++){
            var pair = fieldPairs[i];
            if (row[pair[0]] && LABKEY.ExtAdapter.isEmpty(row[pair[1]])){
                row[pair[0]] = null;
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Treatment Orders', function(helper, scriptErrors, row, oldRow){
        if (!row.code){
            EHR.Server.Utils.addError(scriptErrors, 'code', 'Must enter a treatment', 'WARN');
        }
        else if ((row.code == 'E-70590' || row.code == 'E-YY928') && (row.amount || row.volume) && (!row.amount || !row.amount_units || row.amount_units.toLowerCase() != 'mg')){
            EHR.Server.Utils.addError(scriptErrors, 'amount_units', 'When entering ketamine or telazol, amount must be in mg', 'WARN');
        }

        if (!row.amount && !row.volume){
            EHR.Server.Utils.addError(scriptErrors, 'amount', 'Must enter an amount or volume', 'WARN');
            EHR.Server.Utils.addError(scriptErrors, 'volume', 'Must enter an amount or volume', 'WARN');
        }

        if (row.frequency){
            if (!triggerHelper.isTreatmentFrequencyActive(row.frequency)){
                EHR.Server.Utils.addError(scriptErrors, 'frequency', 'This frequency has been disabled.  Please select a different option', 'INFO');
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPSERT, 'study', 'Treatment Orders', function(helper, errors, row, oldRow){
        if (row.frequency && row.objectid){
            triggerHelper.cleanTreatmentFrequencyTimes(row.frequency, row.objectid);
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPDATE, 'study', 'Treatment Orders', function(helper, scriptErrors, row, oldRow){
        if (helper.isETL() || helper.isValidateOnly()){
            return;
        }

        if (row && oldRow && row.category != 'Research'){
            EHR.Assert.assertNotEmpty('triggerHelper is null', triggerHelper);

            var date = triggerHelper.onTreatmentOrderChange(row, oldRow);
            if (date){
                row.date = date;
            }
        }
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'Clinical Remarks', function(scriptErrors, helper, row, oldRow){
        if (helper.isETL() || helper.isValidateOnly()){
            return;
        }

        if (row && row.category == 'Replacement SOAP' && row.parentid){
            EHR.Assert.assertNotEmpty('triggerHelper is null', triggerHelper);

            triggerHelper.replaceSoap(row.parentid);
        }
    });
};