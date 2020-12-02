/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.mgt.core.model;

/**
 * Organization-User-Role mapping.
 */
public class OrganizationUserRoleMapping {

    private String organizationId;
    private String userId;
    private Integer hybridRoleId;
    private String roleId;
    private boolean cascadedRole;
    private String assignedOrganizationLevel;

    public OrganizationUserRoleMapping() {

    }

    public OrganizationUserRoleMapping(String organizationId, String userId, String roleId, Integer hybridRoleId) {

        this.organizationId = organizationId;
        this.userId = userId;
        this.hybridRoleId = hybridRoleId;
        this.roleId = roleId;
    }

    public OrganizationUserRoleMapping(String organizationId, String userId, String roleId, int hybridRoleId,
                                       boolean cascadedRole, String assignedOrganizationLevel) {

        this.organizationId = organizationId;
        this.userId = userId;
        this.hybridRoleId = hybridRoleId;
        this.roleId = roleId;
        this.cascadedRole = cascadedRole;
        this.assignedOrganizationLevel = assignedOrganizationLevel;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public void setHybridRoleId(Integer hybridRoleId) {

        this.hybridRoleId = hybridRoleId;
    }

    public void setRoleId(String roleId) {

        this.roleId = roleId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public void setCascadedRole(boolean cascadedRole) {

        this.cascadedRole = cascadedRole;
    }

    public void setAssignedOrganizationLevel(String assignedOrganizationLevel) {

        this.assignedOrganizationLevel = assignedOrganizationLevel;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public String getUserId() {

        return userId;
    }

    public int getHybridRoleId() {

        return hybridRoleId;
    }

    public String getRoleId() {

        return roleId;
    }

    public boolean isCascadedRole() {

        return cascadedRole;
    }

    public String getAssignedOrganizationLevel() {

        return assignedOrganizationLevel;
    }
}
