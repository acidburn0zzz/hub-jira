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
package com.blackducksoftware.integration.jira.common;

public class HubJiraConstants {
	private final static String HUB_JIRA_GROUP = "hub-jira";
	public final static String HUB_JIRA_ERROR = HUB_JIRA_GROUP + "-ticket-error";
	public final static String HUB_JIRA_WORKFLOW_RESOURCE = "Hub Workflow.xml";

	// Issue type names (user visible)
	public final static String HUB_POLICY_VIOLATION_ISSUE = "Hub Policy Violation";
	public final static String HUB_VULNERABILITY_ISSUE = "Hub Security Vulnerability";

	// Field names (user visible)
	public final static String HUB_CUSTOM_FIELD_PROJECT = "BDS Hub Project";
	public final static String HUB_CUSTOM_FIELD_PROJECT_VERSION = "BDS Hub Project Version";
	public final static String HUB_CUSTOM_FIELD_COMPONENT = "BDS Hub Component";
	public final static String HUB_CUSTOM_FIELD_COMPONENT_VERSION = "BDS Hub Component Version";
	public final static String HUB_CUSTOM_FIELD_POLICY_RULE = "BDS Hub Policy Rule";

	// Configuration object names visible only to administrators
	public final static String HUB_JIRA_WORKFLOW = "BDS Hub PlugIn Workflow";
	public static final String HUB_FIELD_CONFIGURATION_SCHEME_NAME = "BDS Hub PlugIn Field Configuration Scheme";
	public static final String HUB_FIELD_CONFIGURATION = "BDS Hub PlugIn Field Configuration";
	public static final String HUB_POLICY_SCREEN_SCHEME_NAME = "BDS Hub PlugIn Policy Screen Scheme";
	public static final String HUB_SECURITY_SCREEN_SCHEME_NAME = "BDS Hub PlugIn Security Screen Scheme";
	public static final String HUB_POLICY_SCREEN_NAME = "BDS Hub PlugIn Policy Screen";
	public static final String HUB_SECURITY_SCREEN_NAME = "BDS Hub PlugIn Security Screen";
	public static final String HUB_SCREEN_TAB = "BDS Hub PlugIn Screen Tab";

	public final static String HUB_WORKFLOW_STATUS_OPEN = "Open";
	public final static String HUB_WORKFLOW_STATUS_RESOLVED = "Resolved";
	public final static String HUB_WORKFLOW_TRANSITION_REMOVE_OR_OVERRIDE = "Resolve";
	public final static String HUB_WORKFLOW_TRANSITION_READD_OR_OVERRIDE_REMOVED = "Re-Open";

	public final static String HUB_POLICY_VIOLATION_REOPEN = "Automatically re-opened in response to a new Black Duck Hub Policy Violation on this project / component / rule";
	public final static String HUB_POLICY_VIOLATION_RESOLVE = "Automatically resolved in response to a Black Duck Hub Policy Override on this project / component / rule";
	public final static String HUB_POLICY_VIOLATION_CLEARED_RESOLVE = "Automatically resolved in response to a Black Duck Hub Policy Violation Cleared event on this project / component / rule";

	public final static String HUB_VULNERABILITY_REOPEN = "Automatically re-opened in response to new Black Duck Hub vulnerabilities on this project from this component";
	public final static String HUB_VULNERABILITY_RESOLVE = "Automatically resolved; the Black Duck Hub reports no remaining vulnerabilities on this project from this component";

	public static final String BLACKDUCK_AVATAR_IMAGE_FILENAME = "Ducky-200.png";
	public static final String BLACKDUCK_AVATAR_IMAGE_PATH = "/images/" + BLACKDUCK_AVATAR_IMAGE_FILENAME;

	public static final String ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR = "=";
	public static final String ISSUE_PROPERTY_KEY_NAME_VALUE_PAIR_SEPARATOR = "|";
	public static final String ISSUE_PROPERTY_KEY_ISSUE_TYPE_NAME = "t";
	public static final String ISSUE_PROPERTY_KEY_ISSUE_TYPE_VALUE_POLICY = "p";
	public static final String ISSUE_PROPERTY_KEY_ISSUE_TYPE_VALUE_VULNERABILITY = "v";
	public static final String ISSUE_PROPERTY_KEY_JIRA_PROJECT_ID_NAME = "jp";
	public static final String ISSUE_PROPERTY_KEY_HUB_PROJECT_VERSION_REL_URL_HASHED_NAME = "hpv";

	public static final String ISSUE_PROPERTY_KEY_HUB_COMPONENT_REL_URL_HASHED_NAME = "hc";

	public static final String ISSUE_PROPERTY_KEY_HUB_COMPONENT_VERSION_REL_URL_HASHED_NAME = "hcv";
	public static final String ISSUE_PROPERTY_KEY_HUB_POLICY_RULE_REL_URL_HASHED_NAME = "hr";

	public static final String VULNERABLE_COMPONENTS_LINK_NAME = "vulnerable-components";
}
