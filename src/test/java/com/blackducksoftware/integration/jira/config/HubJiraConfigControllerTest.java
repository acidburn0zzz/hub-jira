/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.jira.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.blackducksoftware.integration.atlassian.utils.HubConfigKeys;
import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.api.HubVersionRestService;
import com.blackducksoftware.integration.hub.api.policy.PolicyRestService;
import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.api.project.ProjectItem;
import com.blackducksoftware.integration.hub.api.project.version.SourceEnum;
import com.blackducksoftware.integration.hub.encryption.PasswordEncrypter;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.meta.MetaAllowEnum;
import com.blackducksoftware.integration.hub.meta.MetaInformation;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.jira.common.HubJiraConfigKeys;
import com.blackducksoftware.integration.jira.common.HubJiraConstants;
import com.blackducksoftware.integration.jira.common.HubProject;
import com.blackducksoftware.integration.jira.common.HubProjectMapping;
import com.blackducksoftware.integration.jira.common.JiraProject;
import com.blackducksoftware.integration.jira.common.PolicyRuleSerializable;
import com.blackducksoftware.integration.jira.mocks.GroupPickerSearchServiceMock;
import com.blackducksoftware.integration.jira.mocks.HttpServletRequestMock;
import com.blackducksoftware.integration.jira.mocks.PluginSchedulerMock;
import com.blackducksoftware.integration.jira.mocks.PluginSettingsFactoryMock;
import com.blackducksoftware.integration.jira.mocks.ProjectManagerMock;
import com.blackducksoftware.integration.jira.mocks.TransactionTemplateMock;
import com.blackducksoftware.integration.jira.mocks.UserManagerUIMock;
import com.blackducksoftware.integration.jira.task.HubMonitor;
import com.blackducksoftware.integration.jira.task.JiraSettingsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HubJiraConfigControllerTest {

    private List<ProjectItem> getHubProjects() {
        final List<ProjectItem> hubProjects = new ArrayList<>();

        final List<MetaAllowEnum> allows = new ArrayList<>();
        allows.add(MetaAllowEnum.GET);
        allows.add(MetaAllowEnum.PUT);
        final MetaInformation metaInfo1 = new MetaInformation(allows, "projectURL1", null);
        final ProjectItem project1 = new ProjectItem(metaInfo1, "HubProject1", "HubProject1", false, 1, SourceEnum.KB);

        final MetaInformation metaInfo2 = new MetaInformation(allows, "projectURL2", null);
        final ProjectItem project2 = new ProjectItem(metaInfo2, "HubProject2", "HubProject2", false, 1, SourceEnum.KB);

        final MetaInformation metaInfo3 = new MetaInformation(allows, "projectURL3", null);
        final ProjectItem project3 = new ProjectItem(metaInfo3, "HubProject3", "HubProject3", false, 1, SourceEnum.KB);

        final MetaInformation metaInfo4 = new MetaInformation(null, "projectURL4", null);
        final ProjectItem project4 = new ProjectItem(metaInfo4, "HubProject4", "HubProject4", false, 1, SourceEnum.KB);

        hubProjects.add(project1);
        hubProjects.add(project2);
        hubProjects.add(project3);
        hubProjects.add(project4);
        return hubProjects;
    }

    private List<PolicyRule> getHubPolicies() {
        final List<PolicyRule> policyRules = new ArrayList<>();
        final MetaInformation metaInfo1 = new MetaInformation(null, "policyURL1", null);
        final PolicyRule rule1 = new PolicyRule(metaInfo1, "PolicyRule1", "1TestDescription", true, null, null, null,
                null, null, null);

        final MetaInformation metaInfo2 = new MetaInformation(null, "policyURL2", null);
        final PolicyRule rule2 = new PolicyRule(metaInfo2, "PolicyRule2", "2TestDescription", true, null, null, null,
                null, null, null);

        final MetaInformation metaInfo3 = new MetaInformation(null, "policyURL3", null);
        final PolicyRule rule3 = new PolicyRule(metaInfo3, "PolicyRule3", "3TestDescription", false, null, null, null,
                null, null, null);

        policyRules.add(rule1);
        policyRules.add(rule2);
        policyRules.add(rule3);
        return policyRules;
    }

    private List<PolicyRuleSerializable> getJiraPolicies() {
        final List<PolicyRuleSerializable> newPolicyRules = new ArrayList<>();

        final PolicyRuleSerializable rule1 = new PolicyRuleSerializable();
        rule1.setName("PolicyRule1");
        rule1.setPolicyUrl("policyURL1");
        rule1.setDescription("1TestDescription");
        rule1.setChecked(true);

        final PolicyRuleSerializable rule2 = new PolicyRuleSerializable();
        rule2.setName("PolicyRule2");
        rule2.setPolicyUrl("policyURL2");
        rule2.setDescription("2TestDescription");
        rule2.setChecked(true);

        final PolicyRuleSerializable rule3 = new PolicyRuleSerializable();
        rule3.setName("PolicyRule3");
        rule3.setPolicyUrl("policyURL3");
        rule3.setDescription("3TestDescription");
        rule3.setChecked(true);

        newPolicyRules.add(rule1);
        newPolicyRules.add(rule2);
        newPolicyRules.add(rule3);
        return newPolicyRules;
    }

    private Set<HubProjectMapping> getMappings() {
        final JiraProject jiraProject1 = new JiraProject();
        jiraProject1.setProjectName("Project1");
        jiraProject1.setProjectKey("ProjectKey");
        jiraProject1.setProjectId(0L);

        final HubProject hubProject1 = new HubProject();
        hubProject1.setProjectName("HubProject1");
        hubProject1.setProjectUrl("projectURL1");

        final JiraProject jiraProject2 = new JiraProject();
        jiraProject2.setProjectName("Project2");
        jiraProject2.setProjectKey("ProjectKey");
        jiraProject2.setProjectId(153L);

        final HubProject hubProject2 = new HubProject();
        hubProject2.setProjectName("HubProject2");
        hubProject2.setProjectUrl("projectURL2");

        final HubProjectMapping mapping1 = new HubProjectMapping();
        mapping1.setHubProject(hubProject1);
        mapping1.setJiraProject(jiraProject1);

        final HubProjectMapping mapping2 = new HubProjectMapping();
        mapping2.setHubProject(hubProject1);
        mapping2.setJiraProject(jiraProject2);

        final HubProjectMapping mapping3 = new HubProjectMapping();
        mapping3.setHubProject(hubProject2);
        mapping3.setJiraProject(jiraProject1);

        final HubProjectMapping mapping4 = new HubProjectMapping();
        mapping4.setHubProject(hubProject2);
        mapping4.setJiraProject(jiraProject2);

        final Set<HubProjectMapping> mappings = new HashSet<>();
        mappings.add(mapping1);
        mappings.add(mapping2);
        mappings.add(mapping3);
        mappings.add(mapping4);

        return mappings;
    }

    @Test
    public void testGetIntervalNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetIntervalNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetIntervalEmpty() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertEquals(JiraConfigErrors.NO_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetIntervalInvalid() {
        final String intervalBetweenChecks = "intervalBetweenChecks";

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertEquals("The String : " + intervalBetweenChecks + " , is not an Integer.",
                config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetIntervalNegative() {
        final String intervalBetweenChecks = "-30";

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertEquals(JiraConfigErrors.INVALID_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetIntervalZero() {
        final String intervalBetweenChecks = "0";

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertEquals(JiraConfigErrors.INVALID_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetIntervalValid() {
        final String intervalBetweenChecks = "30";

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetIntervalValidInGroup() {
        final String intervalBetweenChecks = "30";

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Group1");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, "Group1,Group2");

        final Response response = controller.getInterval(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetHubPoliciesNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubPoliciesNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubPoliciesWithNoServerConfig() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = spyControllerRealRestConnection(controller, "3.1.0", true);

        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);
        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertTrue(config.getPolicyRules().isEmpty());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertEquals(JiraConfigErrors.HUB_CONFIG_PLUGIN_MISSING, config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetHubPoliciesWithPartialServerConfig() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "Test Server Url");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);
        controller = spyControllerRealRestConnection(controller, "3.1.0", true);

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertTrue(config.getPolicyRules().isEmpty());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertEquals(JiraConfigErrors.HUB_SERVER_MISCONFIGURATION + JiraConfigErrors.CHECK_HUB_SERVER_CONFIGURATION,
                config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetHubPoliciesNoPolicyRulesOldHub() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = spyControllerMockRestConnection(controller, "2.5.0", false);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        final List<PolicyRule> emptyPolicyRules = new ArrayList<>();

        Mockito.doReturn(emptyPolicyRules).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertTrue(config.getPolicyRules().isEmpty());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertEquals(JiraConfigErrors.HUB_SERVER_NO_POLICY_SUPPORT_ERROR + " : "
                + JiraConfigErrors.NO_POLICY_RULES_FOUND_ERROR, config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetHubPoliciesNoPolicyRules() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = Mockito.spy(controller);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        final List<PolicyRule> emptyPolicyRules = new ArrayList<>();

        Mockito.doReturn(emptyPolicyRules).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        // TODO remove this crap
        // VersionComparison versionComparison = new VersionComparison("3.1.0", "3.1.0", 1, "=");
        // Mockito.when(hubVersionRestService.getHubVersionComparison(Mockito.anyString())).thenReturn(versionComparison);
        Mockito.doReturn(true).when(hubVersionRestService).isConsumerVersionLessThanOrEqualToServerVersion(Mockito.anyString());
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));
        // Mockito.when(controller.getRestConnection(Mockito.any(PluginSettings.class),
        // Mockito.any(HubJiraConfigSerializable.class)))
        // .thenReturn(restConnectionMock);
        // Mockito.when(controller.getHubVersionRestService(restConnectionMock)).thenReturn(hubVersionRestService);
        Mockito.doReturn(hubVersionRestService).when(controller).getHubVersionRestService(restConnectionMock);
        // ClientResource clientResource = Mockito.mock(ClientResource.class);
        // Mockito.when(restConnectionMock.createClientResource()).thenReturn(clientResource);
        // Mockito.when(restConnectionMock.createClientResource(Mockito.anyString())).thenReturn(clientResource);
        // Mockito.doReturn(restConnectionMock).when(controller).getRestConnection(Mockito.any(PluginSettings.class),
        // Mockito.any(HubJiraConfigSerializable.class));
        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertTrue(config.getPolicyRules().isEmpty());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertEquals(JiraConfigErrors.NO_POLICY_RULES_FOUND_ERROR, config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetHubPoliciesWithPolicyRules() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = Mockito.spy(controller);

        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn(true).when(hubVersionRestService).isConsumerVersionLessThanOrEqualToServerVersion(Mockito.anyString());
        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(hubVersionRestService).when(controller).getHubVersionRestService(restConnectionMock);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertFalse(config.getPolicyRules().isEmpty());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetHubPoliciesWithPolicyRulesInGroup() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Group1");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, "Group1,Group2");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final Response response = controller.getHubPolicies(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertTrue(!config.getPolicyRules().isEmpty());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    private HubJiraConfigController spyControllerMockRestConnection(HubJiraConfigController controller, String hubVersion,
            boolean consumerVersionLessThanOrEqualToServerVersion)
            throws IOException, BDRestException, URISyntaxException, ResourceDoesNotExistException {
        controller = Mockito.spy(controller);
        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));

        mockCommonServices(controller, hubVersion, consumerVersionLessThanOrEqualToServerVersion);
        return controller;
    }

    private HubJiraConfigController spyControllerRealRestConnection(HubJiraConfigController controller, String hubVersion,
            boolean consumerVersionLessThanOrEqualToServerVersion)
            throws IOException, BDRestException, URISyntaxException, ResourceDoesNotExistException {
        controller = Mockito.spy(controller);

        mockCommonServices(controller, hubVersion, consumerVersionLessThanOrEqualToServerVersion);
        return controller;
    }

    private void mockCommonServices(HubJiraConfigController controller, String hubVersion, boolean consumerVersionLessThanOrEqualToServerVersion)
            throws IOException, BDRestException, URISyntaxException, ResourceDoesNotExistException {
        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn(hubVersionRestService).when(controller).getHubVersionRestService(Mockito.any(RestConnection.class));
        Mockito.doReturn(hubVersion).when(hubVersionRestService).getHubVersion();
        Mockito.doReturn(consumerVersionLessThanOrEqualToServerVersion).when(hubVersionRestService)
                .isConsumerVersionLessThanOrEqualToServerVersion(Mockito.anyString());

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));
    }

    @Test
    public void testGetJiraProjectsNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getJiraProjects(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetJiraProjectsNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getJiraProjects(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetJiraProjectsNone() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getJiraProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertTrue(config.getJiraProjects().isEmpty());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
        assertEquals(JiraConfigErrors.NO_JIRA_PROJECTS_FOUND, config.getJiraProjectsError());
    }

    @Test
    public void testGetJiraProjectsMultipleProjects() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getJiraProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertTrue(!config.getJiraProjects().isEmpty());
        for (final JiraProject proj : config.getJiraProjects()) {
            assertNull(proj.getProjectError());
        }
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetJiraProjectsMultipleProjectsInGroup() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Group1");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        settingsFactory.createGlobalSettings().put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, "Group1,Group2");

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getJiraProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;
        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertTrue(!config.getJiraProjects().isEmpty());
        for (final JiraProject proj : config.getJiraProjects()) {
            assertNull(proj.getProjectError());
        }
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetHubProjectsNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubProjects(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubProjectsNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubProjects(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubProjectsPartialServerConfig() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "Test Server Url");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertTrue(config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());

        assertEquals(JiraConfigErrors.HUB_SERVER_MISCONFIGURATION + JiraConfigErrors.CHECK_HUB_SERVER_CONFIGURATION,
                config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
    }

    @Test
    public void testGetHubProjectsNoHubProjects() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        final List<ProjectItem> emptyHubProjects = new ArrayList<>();
        Mockito.doReturn(emptyHubProjects).when(restServiceMock).getProjectMatches(Mockito.anyString());
        // Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.getHubProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertTrue(config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(config.hasErrors());
        assertEquals(JiraConfigErrors.NO_HUB_PROJECTS_FOUND, config.getHubProjectsError());
    }

    @Test
    public void testGetHubProjectsHasHubProjects() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.getHubProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertTrue(!config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetHubProjectsHasHubProjectsInGroup() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Group1");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, "Group1,Group2");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        controller = Mockito.spy(controller);

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));
        final Response response = controller.getHubProjects(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertTrue(!config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetMappingsNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getMappings(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetMappingsNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getMappings(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetMappingsNoMappings() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getMappings(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertNull(config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetMappingsWithMappings() {
        final Set<HubProjectMapping> mappings = getMappings();

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final Gson gson = new GsonBuilder().create();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getMappings(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertTrue(!config.getHubProjectMappings().isEmpty());

        assertEquals(mappings, config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testGetMappingsWithMappingsInGroup() {
        final Set<HubProjectMapping> mappings = getMappings();

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Group1");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final Gson gson = new GsonBuilder().create();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, "Group1,Group2");

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getMappings(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubJiraConfigSerializable config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getIntervalBetweenChecks());
        assertNull(config.getPolicyRules());
        assertNull(config.getJiraProjects());
        assertNull(config.getHubProjects());
        assertTrue(!config.getHubProjectMappings().isEmpty());

        assertEquals(mappings, config.getHubProjectMappings());

        assertNull(config.getErrorMessage());
        assertNull(config.getIntervalBetweenChecksError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getHubProjectMappingError());
        assertTrue(!config.hasErrors());
    }

    @Test
    public void testSaveConfigNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final HubJiraConfigSerializable config = new HubJiraConfigSerializable();

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testSaveConfigNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final HubJiraConfigSerializable config = new HubJiraConfigSerializable();

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testSaveConfigEmptyNoServerConfig() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);
        controller = Mockito.spy(controller);
        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));
        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);
        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));
        HubJiraConfigSerializable config = new HubJiraConfigSerializable();

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        config = (HubJiraConfigSerializable) configObject;

        assertEquals(JiraConfigErrors.NO_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertNull(config.getHubProjectMappingError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getIntervalBetweenChecks());
        assertTrue(config.getJiraProjects().isEmpty());
        assertTrue(config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());
        assertNull(config.getPolicyRules());
        assertTrue(config.hasErrors());
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());
    }

    @Test
    public void testSaveConfigEmpty() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);

        HubJiraConfigSerializable config = new HubJiraConfigSerializable();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getErrorMessage());
        assertEquals(JiraConfigErrors.NO_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertNull(config.getHubProjectMappingError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getIntervalBetweenChecks());
        assertTrue(!config.getJiraProjects().isEmpty());
        assertTrue(!config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());
        assertNull(config.getPolicyRules());
        assertTrue(config.hasErrors());
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());
    }

    @Test
    public void testSaveConfigResetToBlank() throws Exception {
        final String intervalBetweenChecks = "30";

        final Set<HubProjectMapping> mappings = getMappings();

        final List<PolicyRuleSerializable> jiraPolices = getJiraPolicies();

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);

        HubJiraConfigSerializable config = new HubJiraConfigSerializable();

        final Gson gson = new GsonBuilder().create();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON, gson.toJson(jiraPolices));
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getErrorMessage());
        assertEquals(JiraConfigErrors.NO_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertNull(config.getHubProjectMappingError());
        assertNull(config.getPolicyRulesError());
        assertNull(config.getIntervalBetweenChecks());
        assertTrue(!config.getJiraProjects().isEmpty());
        assertTrue(!config.getHubProjects().isEmpty());
        assertNull(config.getHubProjectMappings());
        assertNull(config.getPolicyRules());
        assertTrue(config.hasErrors());
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());
    }

    @Test
    public void testSaveConfigNoUpdate() throws Exception {
        final String intervalBetweenChecks = "30";

        final Set<HubProjectMapping> mappings = getMappings();

        final List<PolicyRuleSerializable> jiraPolices = getJiraPolicies();

        for (final PolicyRuleSerializable policyRule : jiraPolices) {
            policyRule.setChecked(false);
        }

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);
        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final HubJiraConfigSerializable config = new HubJiraConfigSerializable();
        config.setIntervalBetweenChecks(intervalBetweenChecks);
        config.setHubProjectMappings(mappings);
        config.setPolicyRules(jiraPolices);

        final Gson gson = new GsonBuilder().create();
        PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON, gson.toJson(jiraPolices));
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNull(configObject);

        settings = settingsFactory.createGlobalSettings();
        assertEquals(intervalBetweenChecks, settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS));
        assertEquals(gson.toJson(mappings), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON));
        assertEquals(gson.toJson(jiraPolices), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON));
        assertEquals("User", settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_USER));
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());
    }

    @Test
    public void testSaveConfigNoUpdateInGroup() throws Exception {
        final String intervalBetweenChecks = "30";

        final Set<HubProjectMapping> mappings = getMappings();

        final List<PolicyRuleSerializable> jiraPolices = getJiraPolicies();

        for (final PolicyRuleSerializable policyRule : jiraPolices) {
            policyRule.setChecked(false);
        }

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Group1");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);

        final HubJiraConfigSerializable config = new HubJiraConfigSerializable();
        config.setIntervalBetweenChecks(intervalBetweenChecks);
        config.setHubProjectMappings(mappings);
        config.setPolicyRules(jiraPolices);

        final Gson gson = new GsonBuilder().create();
        PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, intervalBetweenChecks);
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON, gson.toJson(jiraPolices));
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, "Group1,Group2");

        controller = Mockito.spy(controller);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));
        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNull(configObject);

        settings = settingsFactory.createGlobalSettings();
        assertEquals(intervalBetweenChecks, settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS));
        assertEquals(gson.toJson(mappings), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON));
        assertEquals(gson.toJson(jiraPolices), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON));
        assertEquals("User", settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_USER));
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());
    }

    @Test
    public void testSaveConfigEmptyInterval() throws Exception {
        final String intervalBetweenChecks = "";

        final Set<HubProjectMapping> mappings = getMappings();

        final List<PolicyRuleSerializable> jiraPolices = getJiraPolicies();

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);

        HubJiraConfigSerializable config = new HubJiraConfigSerializable();
        config.setIntervalBetweenChecks(intervalBetweenChecks);
        config.setHubProjectMappings(mappings);
        config.setPolicyRules(jiraPolices);

        final Gson gson = new GsonBuilder().create();
        PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, "30");
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON, gson.toJson(jiraPolices));
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getErrorMessage());
        assertEquals(JiraConfigErrors.NO_INTERVAL_FOUND_ERROR, config.getIntervalBetweenChecksError());
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getHubProjectMappingError());
        assertNull(config.getPolicyRulesError());
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertTrue(!config.getJiraProjects().isEmpty());
        assertTrue(!config.getHubProjects().isEmpty());
        assertTrue(!config.getHubProjectMappings().isEmpty());
        assertTrue(!config.getPolicyRules().isEmpty());
        assertEquals(mappings, config.getHubProjectMappings());
        assertTrue(config.hasErrors());
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());

        settings = settingsFactory.createGlobalSettings();
        assertEquals(intervalBetweenChecks, settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS));
        assertEquals(gson.toJson(mappings), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON));
        assertEquals(gson.toJson(jiraPolices), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON));
        assertEquals("User", settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_USER));
    }

    @Test
    public void testSaveConfigInvalidInterval() throws Exception {
        final String intervalBetweenChecks = "intervalBetweenChecks";

        final Set<HubProjectMapping> mappings = getMappings();

        final List<PolicyRuleSerializable> jiraPolices = getJiraPolicies();

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);

        HubJiraConfigSerializable config = new HubJiraConfigSerializable();
        config.setIntervalBetweenChecks(intervalBetweenChecks);
        config.setHubProjectMappings(mappings);
        config.setPolicyRules(jiraPolices);

        final Gson gson = new GsonBuilder().create();
        PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, "30");
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON, gson.toJson(jiraPolices));
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        controller = Mockito.spy(controller);
        RestConnection restConnectionMock = Mockito.mock(RestConnection.class);
        Mockito.doReturn(restConnectionMock)
                .when(controller).getRestConnection(Mockito.any(PluginSettings.class), Mockito.any(HubJiraConfigSerializable.class));
        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        config = (HubJiraConfigSerializable) configObject;

        assertNull(config.getErrorMessage());
        assertEquals("The String : " + intervalBetweenChecks + " , is not an Integer.",
                config.getIntervalBetweenChecksError());
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertNull(config.getHubProjectMappingError());
        assertNull(config.getPolicyRulesError());
        assertEquals(intervalBetweenChecks, config.getIntervalBetweenChecks());
        assertTrue(!config.getJiraProjects().isEmpty());
        assertTrue(!config.getHubProjects().isEmpty());
        assertTrue(!config.getHubProjectMappings().isEmpty());
        assertTrue(!config.getPolicyRules().isEmpty());
        assertEquals(mappings, config.getHubProjectMappings());
        assertTrue(config.hasErrors());
        assertTrue(!pluginScheduler.isJobUnScheduled());
        assertTrue(!pluginScheduler.isJobScheduled());

        settings = settingsFactory.createGlobalSettings();
        assertEquals(intervalBetweenChecks, settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS));
        assertEquals(gson.toJson(mappings), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON));
        assertEquals(gson.toJson(jiraPolices), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON));
        assertEquals("User", settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_USER));
    }

    @Test
    public void testSaveConfigUpdate() throws Exception {
        final String intervalBetweenChecks = "30";

        final Set<HubProjectMapping> mappings = getMappings();

        final List<PolicyRuleSerializable> jiraPolices = getJiraPolicies();

        for (final PolicyRuleSerializable policyRule : jiraPolices) {
            policyRule.setChecked(false);
        }

        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final PluginSchedulerMock pluginScheduler = new PluginSchedulerMock();
        final HubMonitor hubMonitor = new HubMonitor(pluginScheduler, settingsFactory);

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, hubMonitor, groupPickerSearchServiceMock);

        final HubJiraConfigSerializable config = new HubJiraConfigSerializable();
        config.setIntervalBetweenChecks(intervalBetweenChecks);
        config.setHubProjectMappings(mappings);
        config.setPolicyRules(jiraPolices);

        final Gson gson = new GsonBuilder().create();
        PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS, "560");
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON, gson.toJson(mappings));
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON, gson.toJson(jiraPolices));
        settings.put(HubConfigKeys.CONFIG_HUB_URL, "http://www.google.com");
        settings.put(HubConfigKeys.CONFIG_HUB_USER, "Test User");
        settings.put(HubConfigKeys.CONFIG_HUB_PASS, PasswordEncrypter.encrypt("Test"));
        settings.put(HubConfigKeys.CONFIG_HUB_PASS_LENGTH, "4");
        settings.put(HubConfigKeys.CONFIG_HUB_TIMEOUT, "300");

        for (final PolicyRuleSerializable policyRule : jiraPolices) {
            policyRule.setChecked(true);
        }

        final JiraProject jiraProject = new JiraProject();
        jiraProject.setProjectName("Project");
        jiraProject.setProjectKey("ProjectKey");
        jiraProject.setProjectId(0L);
        jiraProject.setProjectError("");

        final HubProject hubProject = new HubProject();
        hubProject.setProjectName("HubProject");
        hubProject.setProjectUrl("projectURL");

        final HubProjectMapping newMapping = new HubProjectMapping();
        newMapping.setHubProject(hubProject);
        newMapping.setJiraProject(jiraProject);
        mappings.add(newMapping);

        controller = spyControllerMockRestConnection(controller, "3.1.0", true);

        final PolicyRestService policyServiceMock = Mockito.mock(PolicyRestService.class);

        Mockito.doReturn(getHubPolicies()).when(policyServiceMock).getAllPolicyRules();

        Mockito.doReturn(policyServiceMock).when(controller).getPolicyService(Mockito.any(RestConnection.class));

        final HubIntRestService restServiceMock = Mockito.mock(HubIntRestService.class);

        Mockito.doReturn(getHubProjects()).when(restServiceMock).getProjectMatches(Mockito.anyString());
        final HubVersionRestService hubVersionRestService = Mockito.mock(HubVersionRestService.class);
        Mockito.doReturn("3.1.0").when(hubVersionRestService).getHubVersion();

        Mockito.doReturn(restServiceMock).when(controller).getHubRestService(Mockito.any(RestConnection.class),
                Mockito.any(HubJiraConfigSerializable.class));

        final Response response = controller.put(config, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNull(configObject);

        settings = settingsFactory.createGlobalSettings();
        assertEquals(intervalBetweenChecks, settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_INTERVAL_BETWEEN_CHECKS));
        assertEquals(gson.toJson(mappings), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_PROJECT_MAPPINGS_JSON));
        assertEquals(gson.toJson(jiraPolices), settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_POLICY_RULES_JSON));
        assertEquals("User", settings.get(HubJiraConfigKeys.HUB_CONFIG_JIRA_USER));
        assertTrue(pluginScheduler.isJobUnScheduled());
        assertTrue(pluginScheduler.isJobScheduled());
    }

    @Test
    public void testNullUserTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraTicketErrors(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testUserNotAdminOrInGroupTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraTicketErrors(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testNullTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraTicketErrors(requestMock);
        assertNotNull(response);
        final Object creationErrorsObject = response.getEntity();
        assertNotNull(creationErrorsObject);
        final TicketCreationErrorSerializable creationErrors = (TicketCreationErrorSerializable) creationErrorsObject;

        assertNull(creationErrors.getHubJiraTicketErrors());
    }

    @Test
    public void testEmptyTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final HashMap<String, String> ticketCreationErrors = new HashMap<>();

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, ticketCreationErrors);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraTicketErrors(requestMock);
        assertNotNull(response);
        final Object creationErrorsObject = response.getEntity();
        assertNotNull(creationErrorsObject);
        final TicketCreationErrorSerializable creationErrors = (TicketCreationErrorSerializable) creationErrorsObject;

        assertNull(creationErrors.getHubJiraTicketErrors());
    }

    @Test
    public void testWithTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final HashMap<String, String> ticketCreationErrors = new HashMap<>();

        ticketCreationErrors.put("Test Error Message", DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 2 \n line 2 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message3 \n line 2 of error \n line 3 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, ticketCreationErrors);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraTicketErrors(requestMock);
        assertNotNull(response);
        final Object creationErrorsObject = response.getEntity();
        assertNotNull(creationErrorsObject);
        final TicketCreationErrorSerializable creationErrors = (TicketCreationErrorSerializable) creationErrorsObject;

        assertNotNull(creationErrors.getHubJiraTicketErrors());
        assertTrue(creationErrors.getHubJiraTicketErrors().size() == 3);
    }

    @Test
    public void testWithOldTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final HashMap<String, String> ticketCreationErrors = new HashMap<>();

        ticketCreationErrors.put("Test Error Message", DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 2 \n line 2 of error \n should be removed",
                DateTime.now().minusDays(31).toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 3 \n line 2 of error \n should be removed",
                DateTime.now().minusDays(35).toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 4 \n line 2 of error \n line 3 of error",
                DateTime.now().minusDays(29).toString(JiraSettingsService.ERROR_TIME_FORMAT));

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, ticketCreationErrors);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraTicketErrors(requestMock);
        assertNotNull(response);
        final Object creationErrorsObject = response.getEntity();
        assertNotNull(creationErrorsObject);
        final TicketCreationErrorSerializable creationErrors = (TicketCreationErrorSerializable) creationErrorsObject;

        assertNotNull(creationErrors.getHubJiraTicketErrors());
        assertTrue(creationErrors.getHubJiraTicketErrors().size() == 2);
    }

    @Test
    public void testRemoveZeroTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final HashMap<String, String> ticketCreationErrors = new HashMap<>();

        ticketCreationErrors.put("Test Error Message", DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 2 \n line 2 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 3 \n line 2 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 4 \n line 2 of error \n line 3 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, ticketCreationErrors);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final TicketCreationErrorSerializable errorsToDelete = new TicketCreationErrorSerializable();

        final Response response = controller.removeErrors(errorsToDelete, requestMock);
        assertNotNull(response);

        final PluginSettings validateSettings = settingsFactory.createGlobalSettings();
        final Object errorMap = validateSettings.get(HubJiraConstants.HUB_JIRA_ERROR);
        assertNotNull(errorMap);
        final HashMap<String, String> validateTicketCreationErrors = (HashMap<String, String>) errorMap;
        assertTrue(validateTicketCreationErrors.size() == 4);
    }

    @Test
    public void testRemoveSomeTicketCreationErrors() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final HashMap<String, String> ticketCreationErrors = new HashMap<>();

        final TicketCreationError ticketCreationError1 = new TicketCreationError();
        ticketCreationError1.setStackTrace("Test Error Message 2 \n line 2 of error \n should get removed");
        ticketCreationError1.setTimeStamp(DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        final TicketCreationError ticketCreationError2 = new TicketCreationError();
        ticketCreationError2.setStackTrace("Should get removed");
        ticketCreationError2.setTimeStamp(DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));

        ticketCreationErrors.put("Test Error Message", DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put(ticketCreationError1.getStackTrace(), ticketCreationError1.getTimeStamp());
        ticketCreationErrors.put("Test Error Message 3 \n line 2 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 4 \n line 2 of error \n line 3 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put(ticketCreationError2.getStackTrace(), ticketCreationError2.getTimeStamp());

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, ticketCreationErrors);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final TicketCreationErrorSerializable errorsToDelete = new TicketCreationErrorSerializable();

        final HashSet<TicketCreationError> errorSetToDelete = new HashSet<>();
        errorSetToDelete.add(ticketCreationError1);
        errorSetToDelete.add(ticketCreationError2);

        errorsToDelete.setHubJiraTicketErrors(errorSetToDelete);

        final Response response = controller.removeErrors(errorsToDelete, requestMock);
        assertNotNull(response);

        final PluginSettings validateSettings = settingsFactory.createGlobalSettings();
        final Object errorMap = validateSettings.get(HubJiraConstants.HUB_JIRA_ERROR);
        assertNotNull(errorMap);
        final HashMap<String, String> validateTicketCreationErrors = (HashMap<String, String>) errorMap;
        assertTrue(validateTicketCreationErrors.size() == 3);
    }

    @Test
    public void testRemoveTicketCreationErrorsMissing() throws Exception {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final HashMap<String, String> ticketCreationErrors = new HashMap<>();

        ticketCreationErrors.put("Test Error Message", DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 2 \n line 2 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 3 \n line 2 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));
        ticketCreationErrors.put("Test Error Message 4 \n line 2 of error \n line 3 of error",
                DateTime.now().toString(JiraSettingsService.ERROR_TIME_FORMAT));

        final PluginSettings settings = settingsFactory.createGlobalSettings();
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, ticketCreationErrors);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        projectManagerMock.setProjectObjects(ProjectManagerMock.getTestProjectObjectsWithTaskIssueType());

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final TicketCreationErrorSerializable errorsToDelete = new TicketCreationErrorSerializable();

        final TicketCreationError ticketCreationError = new TicketCreationError();
        ticketCreationError.setStackTrace("Should be missing");
        ticketCreationError.setTimeStamp(DateTime.now().toString());

        final HashSet<TicketCreationError> errorSetToDelete = new HashSet<>();
        errorSetToDelete.add(ticketCreationError);

        errorsToDelete.setHubJiraTicketErrors(errorSetToDelete);

        final Response response = controller.removeErrors(errorsToDelete, requestMock);
        assertNotNull(response);
        final Object creationErrorsObject = response.getEntity();
        assertNotNull(creationErrorsObject);
        final TicketCreationErrorSerializable creationErrors = (TicketCreationErrorSerializable) creationErrorsObject;

        assertNotNull(creationErrors.getConfigError());

        final PluginSettings validateSettings = settingsFactory.createGlobalSettings();
        final Object errorMap = validateSettings.get(HubJiraConstants.HUB_JIRA_ERROR);
        assertNotNull(errorMap);
        final HashMap<String, String> validateTicketCreationErrors = (HashMap<String, String>) errorMap;
        assertTrue(validateTicketCreationErrors.size() == 4);
    }

    @Test
    public void testGetHubAdminConfigNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubAdminConfigNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubAdminConfigAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubAdminConfigSerializable adminConfig = (HubAdminConfigSerializable) configObject;
        assertNull(adminConfig.getHubJiraGroups());
        assertTrue(adminConfig.getJiraGroups().isEmpty());
    }

    @Test
    public void testGetHubAdminConfigAdminValid() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        final String hubJiraGroups = "Xmen, Mutants";
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, hubJiraGroups);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        groupPickerSearchServiceMock.addGroupByName("Group1");
        groupPickerSearchServiceMock.addGroupByName("Group2");

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubAdminConfigSerializable adminConfig = (HubAdminConfigSerializable) configObject;
        assertEquals(hubJiraGroups, adminConfig.getHubJiraGroups());
        assertTrue(!adminConfig.getJiraGroups().isEmpty());
    }

    @Test
    public void testGetHubAdminConfigInGroup() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(false);
        managerMock.addGroup("Xmen");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        final String hubJiraGroups = "Xmen, Mutants";
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, hubJiraGroups);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubAdminConfigSerializable adminConfig = (HubAdminConfigSerializable) configObject;
        assertEquals(hubJiraGroups, adminConfig.getHubJiraGroups());
        assertNull(adminConfig.getJiraGroups());
    }

    @Test
    public void testGetHubAdminConfigNotInGroup() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(false);
        managerMock.addGroup("Marvel");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        final String hubJiraGroups = "Xmen, Mutants";
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, hubJiraGroups);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubAdminConfigNotGroupsDefined() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(false);
        managerMock.addGroup("Xmen");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testGetHubAdminConfigInGroupValid() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.addGroup("Xmen");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final PluginSettings settings = settingsFactory.createGlobalSettings();
        final String hubJiraGroups = "Xmen, Mutants";
        settings.put(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS, hubJiraGroups);

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();
        groupPickerSearchServiceMock.addGroupByName("Group1");
        groupPickerSearchServiceMock.addGroupByName("Group2");

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final Response response = controller.getHubJiraAdminConfiguration(requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubAdminConfigSerializable adminConfig = (HubAdminConfigSerializable) configObject;
        assertEquals(hubJiraGroups, adminConfig.getHubJiraGroups());
        assertNull(adminConfig.getJiraGroups());
    }

    @Test
    public void testUpdateHubAdminConfigNullUser() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);
        final HubAdminConfigSerializable adminConfig = new HubAdminConfigSerializable();
        final Response response = controller.updateHubAdminConfiguration(adminConfig, requestMock);
        assertNotNull(response);
        assertEquals(Integer.valueOf(Status.UNAUTHORIZED.getStatusCode()), Integer.valueOf(response.getStatus()));
    }

    @Test
    public void testUpdateHubAdminConfigNotAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final HubAdminConfigSerializable adminConfig = new HubAdminConfigSerializable();
        final Response response = controller.updateHubAdminConfiguration(adminConfig, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNotNull(configObject);
        final HubAdminConfigSerializable responseAdminConfig = (HubAdminConfigSerializable) configObject;
        assertNull(responseAdminConfig.getHubJiraGroups());
        assertNull(responseAdminConfig.getJiraGroups());
        assertEquals(JiraConfigErrors.NON_SYSTEM_ADMINS_CANT_CHANGE_GROUPS,
                responseAdminConfig.getHubJiraGroupsError());
    }

    @Test
    public void testUpdateHubAdminConfigAdmin() {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();

        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        final HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);

        final HubAdminConfigSerializable adminConfig = new HubAdminConfigSerializable();
        final String hubJiraGroups = "Xmen, Mutants";
        adminConfig.setHubJiraGroups(hubJiraGroups);

        final Response response = controller.updateHubAdminConfiguration(adminConfig, requestMock);
        assertNotNull(response);
        final Object configObject = response.getEntity();
        assertNull(configObject);

        final String hubJiraGroupsAfter = (String) settingsFactory.createGlobalSettings()
                .get(HubJiraConfigKeys.HUB_CONFIG_JIRA_GROUPS);
        assertNotNull(hubJiraGroupsAfter);
        assertEquals(hubJiraGroups, hubJiraGroupsAfter);
    }

    @Test
    public void testResetSalKeys() throws IOException, BDRestException, URISyntaxException, ResourceDoesNotExistException {
        final UserManagerUIMock managerMock = new UserManagerUIMock();
        managerMock.setRemoteUsername("User");
        managerMock.setIsSystemAdmin(true);
        final PluginSettingsFactoryMock settingsFactory = new PluginSettingsFactoryMock();
        final TransactionTemplateMock transactionManager = new TransactionTemplateMock();
        final HttpServletRequestMock requestMock = new HttpServletRequestMock();
        final ProjectManagerMock projectManagerMock = new ProjectManagerMock();

        final GroupPickerSearchServiceMock groupPickerSearchServiceMock = new GroupPickerSearchServiceMock();

        HubJiraConfigController controller = new HubJiraConfigController(managerMock, settingsFactory,
                transactionManager, projectManagerMock, null, groupPickerSearchServiceMock);
        controller = spyControllerMockRestConnection(controller, "3.1.0", true);
        final PluginSettings settings = settingsFactory.createGlobalSettings();

        final Date runDate = new Date();

        final SimpleDateFormat dateFormatter = new SimpleDateFormat(RestConnection.JSON_DATE_FORMAT);
        dateFormatter.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));

        final String lastRun = dateFormatter.format(runDate);
        final String error = "BAD";

        settings.put(HubJiraConfigKeys.HUB_CONFIG_LAST_RUN_DATE, lastRun);
        settings.put(HubJiraConstants.HUB_JIRA_ERROR, error);

        final Response response = controller.resetHubJiraKeys(null, requestMock);
        assertNotNull(response);

        assertNotNull(settings.get(HubJiraConfigKeys.HUB_CONFIG_LAST_RUN_DATE));
        assertTrue(!lastRun.equals(settings.get(HubJiraConfigKeys.HUB_CONFIG_LAST_RUN_DATE)));
        assertNull(settings.get(HubJiraConstants.HUB_JIRA_ERROR));
    }

}
