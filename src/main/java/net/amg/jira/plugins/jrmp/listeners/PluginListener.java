/*Copyright 2015 AMG.net

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package net.amg.jira.plugins.jrmp.listeners;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Klasa Listenera, która ustawia odpowiednie typy pól oraz zgłoszeń aby można było działać na odpowiednich danych.
 * @author Adam Król
 */
@Component
public class PluginListener implements LifecycleAware  {
    private Logger logger = LoggerFactory.getLogger(getClass());
    //Constants:
    public static final String CUSTOMFIELDTYPES_FLOAT = "com.atlassian.jira.plugin.system.customfieldtypes:select";
    public static final String CUSTOMFIELDTYPES_EXACTNUMBER = "com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher";

    public static final String RISK_CONSEQUENCE_TEXT_CF = "Risk Consequence";
    public static final String RISK_PROBABILITY_TEXT_CF = "Risk Probability";
    public static final String RISK_ISSUE_TYPE = "Risk";

    public Option addOptionToCustomField(CustomField customField, String value, OptionsManager optionsManager) {
        Option newOption = null;

        if (customField != null) {
            List<FieldConfigScheme> schemes = customField
                    .getConfigurationSchemes();
            if (schemes != null && !schemes.isEmpty()) {
                FieldConfigScheme sc = schemes.get(0);
                Map configs = sc.getConfigsByConfig();
                if (configs != null && !configs.isEmpty()) {
                    FieldConfig config = (FieldConfig) configs.keySet()
                            .iterator().next();

                    Options l = optionsManager.getOptions(config);
                    Option opt;

                    newOption = optionsManager.createOption(config, null,
                            new Long(value),
                            value);
                }
            }
        }

        return newOption;
    }

    @Override
    public void onStart() {
        logger.info("Starting JIRA Risk Management plugin configuration!");
        final IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
        final CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        final FieldScreenManager fieldScreenManager = ComponentAccessor.getFieldScreenManager();
        final ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
        final OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        IssueType riskIssueType = null;
        IssueConstant risk = constantsManager.getConstantByNameIgnoreCase(ConstantsManager.ISSUE_TYPE_CONSTANT_TYPE, RISK_ISSUE_TYPE);
        if(risk != null)
        {
            riskIssueType = constantsManager.getIssueTypeObject(risk.getId());
        } else {
            try {
                riskIssueType = constantsManager.insertIssueType(RISK_ISSUE_TYPE, 0L, null, "Risk in projects", "/images/icons/issuetypes/delete.png");
            } catch (CreateException e) {
                logger.info("Couldn't create Risk Issue type: " + e.getMessage(), e);
            }
        }

        final List<GenericValue> issueTypes = new ArrayList<GenericValue>();
        issueTypes.add(riskIssueType.getGenericValue());

        final List<JiraContextNode> contexts = new ArrayList<JiraContextNode>();
        contexts.add(GlobalIssueContext.getInstance());

        issueTypeSchemeManager.addOptionToDefault(riskIssueType.getId());
        final CustomField riskConsequenceCustomField;
        final CustomField riskProbabilityCustomField;
        try {
            FieldScreen defaultScreen = fieldScreenManager.getFieldScreen(FieldScreen.DEFAULT_SCREEN_ID);
            if(customFieldManager.getCustomFieldObjectByName(RISK_CONSEQUENCE_TEXT_CF) == null) {
                riskConsequenceCustomField = customFieldManager.createCustomField(RISK_CONSEQUENCE_TEXT_CF, RISK_CONSEQUENCE_TEXT_CF,
                        customFieldManager.getCustomFieldType(CUSTOMFIELDTYPES_FLOAT),
                        customFieldManager.getCustomFieldSearcher(CUSTOMFIELDTYPES_EXACTNUMBER),
                        contexts, issueTypes);
                if (!defaultScreen.containsField(riskConsequenceCustomField.getId())) {
                    FieldScreenTab firstTab = defaultScreen.getTab(0);
                    firstTab.addFieldScreenLayoutItem(riskConsequenceCustomField.getId());
                }
                for(int i = 1; i < 11; i++)
                {
                    addOptionToCustomField(riskConsequenceCustomField,String.valueOf(i),optionsManager);
                }

            }
            if(customFieldManager.getCustomFieldObjectByName(RISK_PROBABILITY_TEXT_CF) == null) {
                riskProbabilityCustomField = customFieldManager.createCustomField(RISK_PROBABILITY_TEXT_CF, RISK_PROBABILITY_TEXT_CF,
                        customFieldManager.getCustomFieldType(CUSTOMFIELDTYPES_FLOAT),
                        customFieldManager.getCustomFieldSearcher(CUSTOMFIELDTYPES_EXACTNUMBER),
                        contexts, issueTypes);
                if (!defaultScreen.containsField(riskProbabilityCustomField.getId())) {
                    FieldScreenTab firstTab = defaultScreen.getTab(0);
                    firstTab.addFieldScreenLayoutItem(riskProbabilityCustomField.getId());
                }
                for(int i = 1; i < 11; i++)
                {
                    addOptionToCustomField(riskProbabilityCustomField,String.valueOf(i),optionsManager);
                }
            }
        } catch (GenericEntityException e) {
            logger.info("Couldn't create risk Custom fields : " + e.getMessage(),e);
        } catch (NullPointerException e) {
            logger.info("Couldn't create risk Custom fields:" + e.getMessage(),e);
        }
    }
}
