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

package org.wso2.carbon.identity.organization.user.role.mgt.core.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants;
import org.wso2.carbon.identity.organization.user.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.user.role.mgt.core.exception.OrganizationUserRoleMgtServerException;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.Role;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.RoleAssignedLevel;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.RoleAssignment;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.RoleMember;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.UserRoleMappingUser;
import org.wso2.carbon.identity.scim2.common.impl.IdentitySCIMManager;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.UserResourceManager;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_HYBRID_ROLE_ID_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.AND;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.ASSIGNED_AT_ADDING;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.COUNT_COLUMN_NAME;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.DELETE_ORGANIZATION_USER_ROLE_MAPPINGS_ASSIGNED_AT_ORG_LEVEL;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_INHERITANCE;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.GET_ORGANIZATION_USER_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.GET_ROLES_BY_ORG_AND_USER;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.GET_ROLE_ID_BY_SCIM_GROUP_NAME;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.GET_USERS_BY_ORG_AND_ROLE;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.INHERIT_ADDING;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.INSERT_ALL;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.ORG_ID_ADDING;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.SELECT_DUMMY_RECORD;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.UPDATE_ORGANIZATION_USER_ROLE_MAPPING_INHERIT_PROPERTY;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_ASSIGNED_AT_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_ASSIGNED_AT_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_ID_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_INHERIT_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_ROLE_ID_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_ROLE_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.constant.SQLConstants.VIEW_USER_ID_COLUMN;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.organization.user.role.mgt.core.util.Utils.handleServerException;

/**
 * Organization User Role Mgt DAO Implementation.
 */
public class OrganizationUserRoleMgtDAOImpl implements OrganizationUserRoleMgtDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationUserRoleMgtDAOImpl.class);

    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    @Override
    public void addOrganizationUserRoleMappings(List<OrganizationUserRoleMapping> organizationUserRoleMappings,
                                                int tenantID)
            throws OrganizationUserRoleMgtServerException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            // Will be added only if the particular mapping is not existing.
            jdbcTemplate.executeInsert(buildQueryForMultipleInserts(organizationUserRoleMappings.size()),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        for (OrganizationUserRoleMapping organizationUserRoleMapping : organizationUserRoleMappings) {
                            preparedStatement.setString(++parameterIndex, generateUniqueID());
                            preparedStatement.setString(++parameterIndex, organizationUserRoleMapping.getUserId());
                            preparedStatement.setString(++parameterIndex, organizationUserRoleMapping.getRoleId());
                            preparedStatement.setInt(++parameterIndex, organizationUserRoleMapping.getHybridRoleId());
                            preparedStatement.setInt(++parameterIndex, tenantID);
                            preparedStatement
                                    .setString(++parameterIndex, organizationUserRoleMapping.getOrganizationId());
                            preparedStatement.setString(++parameterIndex,
                                    organizationUserRoleMapping.getAssignedLevelOrganizationId());
                            preparedStatement
                                    .setInt(++parameterIndex, organizationUserRoleMapping.isCascadedRole() ? 1 : 0);
                        }
                    }, organizationUserRoleMappings, false);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR, "", e);
        }
    }

    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    @Override
    public void addOrganizationUserRoleMappingsWithSp (List<UserRoleMappingUser> userList, String roleId,
                                                       int hybridRoleId, int tenantID, String assignedAt)
            throws OrganizationUserRoleMgtException {

        try (Connection connection = IdentityDatabaseUtil.getUserDBConnection()) {
            connection.setAutoCommit(false);
            try (CallableStatement callableStatement = connection
                    .prepareCall(SQLConstants.INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_USING_SP)) {
                for (UserRoleMappingUser user: userList) {
                    callableStatement.setString(1, user.getUserId());
                    callableStatement.setString(2, roleId);
                    callableStatement.setInt(3, hybridRoleId);
                    callableStatement.setInt(4, tenantID);
                    callableStatement.setString(5, assignedAt);
                    callableStatement.setInt(6, user.isCascadedRole() ? 1 : 0);

                    callableStatement.addBatch();
                }
                //execute batch insert
                callableStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error occurred while executing the batch insert: ", e);
                }
                connection.rollback();
                throw handleServerException(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR, "", e);
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error occurred while executing the batch insert: ", e);
            }
            throw handleServerException(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR, "", e);
        }
    }

    @SuppressFBWarnings({"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", "SIC_INNER_SHOULD_BE_STATIC_ANON"})
    @Override
    public List<RoleMember> getUserIdsByOrganizationAndRole(String organizationId, String roleId, int offset, int limit,
                                                            List<String> requestedAttributes, int tenantID,
                                                            String filter)
            throws OrganizationUserRoleMgtServerException {

        boolean paginationReq = offset > -1 || limit > 0;
        JdbcTemplate jdbcTemplate = getNewTemplate();
        List<OrganizationUserRoleMapping> organizationUserRoleMappings;
        Map<String, List<RoleAssignment>> userRoleAssignments = new HashMap<>();
        List<RoleMember> roleMembers = new ArrayList<>();

        try {
            organizationUserRoleMappings = jdbcTemplate.executeQuery(GET_USERS_BY_ORG_AND_ROLE,
                    (resultSet, rowNumber) ->
                            new OrganizationUserRoleMapping(organizationId,
                                    resultSet.getString(VIEW_USER_ID_COLUMN), roleId,
                                    resultSet.getString(VIEW_ASSIGNED_AT_COLUMN),
                                    resultSet.getString(VIEW_ASSIGNED_AT_NAME_COLUMN),
                                    resultSet.getInt(VIEW_INHERIT_COLUMN) == 1),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, organizationId);
                        preparedStatement.setString(++parameterIndex, roleId);
                        preparedStatement.setInt(++parameterIndex, tenantID);
                    });

            organizationUserRoleMappings.stream().map(organizationUserRoleMapping -> userRoleAssignments
                    .computeIfAbsent(organizationUserRoleMapping.getUserId(), k -> new ArrayList<>())
                    .add(new RoleAssignment(organizationUserRoleMapping.isCascadedRole(),
                            new RoleAssignedLevel(organizationUserRoleMapping.getAssignedLevelOrganizationId(),
                                    organizationUserRoleMapping.getAssignedLevelOrganizationName()))))
                    .collect(Collectors.toList());

            for (Map.Entry<String, List<RoleAssignment>> entry : userRoleAssignments.entrySet()) {

                String userId = entry.getKey();
                // Obtain the user store manager.
                UserManager userManager = IdentitySCIMManager.getInstance().getUserManager();
                // Create charon-SCIM user endpoint and hand-over the request.
                UserResourceManager userResourceManager = new UserResourceManager();

                // Modify the given filter by adding the user ID.
                String modifiedFilter;
                if (StringUtils.isNotEmpty(filter)) {
                    modifiedFilter = filter + " and id eq " + userId;
                } else {
                    modifiedFilter = "id eq " + userId;
                }

                SCIMResponse scimResponse = userResourceManager.listWithGET(userManager, modifiedFilter,
                        1, 1, null, null, null,
                        requestedAttributes.stream().collect(Collectors.joining(",")), null);

                // Decode the received response.
                Map<String, Object> attributes;
                ObjectMapper mapper = new ObjectMapper();
                attributes =
                        mapper.readValue(scimResponse.getResponseMessage(), new TypeReference<Map<String, Object>>() {
                        });
                if (attributes.containsKey("totalResults") && ((Integer) attributes.get("totalResults")) > 0 &&
                        attributes.containsKey("Resources") && ((ArrayList) attributes.get("Resources")).size() > 0) {
                    Map<String, Object> userAttributes =
                            (Map<String, Object>) ((ArrayList) attributes.get("Resources")).get(0);
                    userAttributes.put("assignedMeta", entry.getValue());
                    RoleMember roleMember = new RoleMember(userAttributes);
                    roleMembers.add(roleMember);
                }
            }
            // Sort role member list.
            Collections.sort(roleMembers, (m1, m2) -> ((String) m1.getUserAttributes().get("userName")).compareTo(
                    String.valueOf(m2.getUserAttributes().get("userName"))));

            if (paginationReq && CollectionUtils.isNotEmpty(roleMembers)) {
                return getPaginatedResult(roleMembers, offset, limit);
            }
        } catch (CharonException | IOException | DataAccessException e) {
            String message = String.format(String.valueOf(ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR), roleId,
                    organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR.getCode(), e);
        }
        return roleMembers;
    }

    private List<RoleMember> getPaginatedResult(List<RoleMember> roleMembers, int offset, int limit) {

        if (offset < 0) {
            offset = 0;
        }
        return roleMembers.subList(offset, Math.min(offset + limit, roleMembers.size()));
    }

    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    @Override
    public void deleteOrganizationsUserRoleMapping(String deleteInvokedOrgId, List<String> organizationIds,
                                                   String userId, String roleId,
                                                   int tenantId) throws OrganizationUserRoleMgtException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(buildQueryForMultipleRoleMappingDeletion(organizationIds.size()),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, userId);
                        preparedStatement.setString(++parameterIndex, roleId);
                        preparedStatement.setInt(++parameterIndex, tenantId);
                        preparedStatement.setString(++parameterIndex, deleteInvokedOrgId);
                        for (String organizationId : organizationIds) {
                            preparedStatement.setString(++parameterIndex, organizationId);
                        }
                    });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR), roleId,
                            userId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR.getCode(), e);
        }
    }

    @Override
    public void deleteOrganizationsUserRoleMappings(String userId, int tenantId)
            throws OrganizationUserRoleMgtException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID,
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, userId);
                        preparedStatement.setInt(++parameterIndex, tenantId);
                    });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR),
                            userId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR.getCode(), e);
        }
    }

    @Override
    public List<Role> getRolesByOrganizationAndUser(String organizationId, String userId, int tenantID)
            throws OrganizationUserRoleMgtServerException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        List<Role> roles;
        try {
            roles = jdbcTemplate.executeQuery(GET_ROLES_BY_ORG_AND_USER,
                    (resultSet, rowNumber) -> new Role(resultSet.getString(VIEW_ROLE_ID_COLUMN),
                            "Internal/" + resultSet.getString(VIEW_ROLE_NAME_COLUMN)),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, organizationId);
                        preparedStatement.setString(++parameterIndex, userId);
                        preparedStatement.setInt(++parameterIndex, tenantID);
                    });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR), userId,
                            organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR.getCode(), e);
        }

        return roles;
    }

    @Override
    public void updateIncludeSubOrgProperty(String organizationID, String roleId, String userId, boolean includeSubOrg,
                                            List<OrganizationUserRoleMapping> organizationUserRoleMappingsToAdd,
                                            List<String> childOrganizationIdsToDeleteRecords, int tenantId)
            throws OrganizationUserRoleMgtServerException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            jdbcTemplate.withTransaction(template -> {
                // Update the directly updated record
                template.executeUpdate(UPDATE_ORGANIZATION_USER_ROLE_MAPPING_INHERIT_PROPERTY, preparedStatement -> {
                            int parameterIndex = 0;
                            preparedStatement.setInt(++parameterIndex, includeSubOrg ? 1 : 0);
                            preparedStatement.setString(++parameterIndex, userId);
                            preparedStatement.setString(++parameterIndex, roleId);
                            preparedStatement.setString(++parameterIndex, organizationID);
                            preparedStatement.setString(++parameterIndex, organizationID);
                            preparedStatement.setInt(++parameterIndex, tenantId);
                        });
                // If 'includeSubOrg' is true, more entries should be added if child orgs exists.
                if (includeSubOrg && CollectionUtils.isNotEmpty(organizationUserRoleMappingsToAdd)) {
                    template.executeInsert(buildQueryForMultipleInserts(organizationUserRoleMappingsToAdd.size()),
                            preparedStatement -> {
                                int parameterIndex = 0;
                                for (OrganizationUserRoleMapping organizationUserRoleMapping
                                        : organizationUserRoleMappingsToAdd) {
                                    preparedStatement.setString(++parameterIndex, generateUniqueID());
                                    preparedStatement
                                            .setString(++parameterIndex, organizationUserRoleMapping.getUserId());
                                    preparedStatement
                                            .setString(++parameterIndex, organizationUserRoleMapping.getRoleId());
                                    preparedStatement
                                            .setInt(++parameterIndex, organizationUserRoleMapping.getHybridRoleId());
                                    preparedStatement.setInt(++parameterIndex, tenantId);
                                    preparedStatement
                                            .setString(++parameterIndex,
                                                    organizationUserRoleMapping.getOrganizationId());
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                    preparedStatement
                                            .setInt(++parameterIndex,
                                                    organizationUserRoleMapping.isCascadedRole() ? 1 : 0);
                                }
                            }, organizationUserRoleMappingsToAdd, false);
                } else if (!includeSubOrg && CollectionUtils.isNotEmpty(childOrganizationIdsToDeleteRecords)) {
                    // If 'includeSubOrg' is false, some entries should be deleted if child orgs exists.
                    template.executeUpdate(
                            buildQueryForMultipleRoleMappingDeletion(childOrganizationIdsToDeleteRecords.size()),
                            preparedStatement -> {
                                int parameterIndex = 0;
                                preparedStatement.setString(++parameterIndex, userId);
                                preparedStatement.setString(++parameterIndex, roleId);
                                preparedStatement.setInt(++parameterIndex, tenantId);
                                preparedStatement.setString(++parameterIndex, organizationID);
                                for (String organizationId : childOrganizationIdsToDeleteRecords) {
                                    preparedStatement.setString(++parameterIndex, organizationId);
                                }
                            });
                }
                return null;
            });
        } catch (TransactionException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR),
                            organizationID, userId, roleId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR.getCode(), e);
        }
    }

    @Override
    public void updateIncludeSubOrgPropertyWithSp(String organizationID, String roleId, String userId, boolean includeSubOrg,
                                                  List<UserRoleMappingUser> userIdList, int hybridRoleId, int tenantId)
            throws OrganizationUserRoleMgtServerException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            jdbcTemplate.withTransaction(template -> {
                // Update the directly updated record
                template.executeUpdate(UPDATE_ORGANIZATION_USER_ROLE_MAPPING_INHERIT_PROPERTY, preparedStatement -> {
                    int parameterIndex = 0;
                    preparedStatement.setInt(++parameterIndex, includeSubOrg ? 1 : 0);
                    preparedStatement.setString(++parameterIndex, userId);
                    preparedStatement.setString(++parameterIndex, roleId);
                    preparedStatement.setString(++parameterIndex, organizationID);
                    preparedStatement.setString(++parameterIndex, organizationID);
                    preparedStatement.setInt(++parameterIndex, tenantId);
                });
                // If 'includeSubOrg' is true, more entries should be added if child orgs exists.
                if (includeSubOrg && CollectionUtils.isNotEmpty(userIdList)) {
                    addOrganizationUserRoleMappingsWithSp(userIdList, roleId, hybridRoleId, tenantId, organizationID);
                }
                return null;
            });
        } catch (TransactionException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR),
                            organizationID, userId, roleId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR.getCode(), e);
        }
    }

    @Override
    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    public boolean isOrganizationUserRoleMappingExists(String organizationId, String userId, String roleId,
                                                       String assignedLevel, boolean includeSubOrg,
                                                       boolean checkInheritance, int tenantId)
            throws OrganizationUserRoleMgtException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        int mappingsCount = 0;
        try {
            mappingsCount = jdbcTemplate
                    .fetchSingleRecord(buildIsRoleMappingExistsQuery(assignedLevel, checkInheritance),
                            (resultSet, rowNumber) ->
                                    resultSet.getInt(COUNT_COLUMN_NAME),
                            preparedStatement -> {
                                int parameterIndex = 0;
                                preparedStatement.setString(++parameterIndex, userId);
                                preparedStatement.setString(++parameterIndex, roleId);
                                preparedStatement.setInt(++parameterIndex, tenantId);
                                preparedStatement.setString(++parameterIndex, organizationId);
                                if (StringUtils.isNotEmpty(assignedLevel)) {
                                    preparedStatement.setString(++parameterIndex, assignedLevel);
                                }
                                if (checkInheritance) {
                                    preparedStatement.setInt(++parameterIndex, includeSubOrg ? 1 : 0);
                                }
                            });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR), roleId,
                            userId, organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getCode(), e);
        }
        return mappingsCount > 0;
    }

    @Override
    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    public int getDirectlyAssignedOrganizationUserRoleMappingInheritance(String organizationId, String userId,
                                                                         String roleId, int tenantId)
            throws OrganizationUserRoleMgtException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        int directlyAssignedRoleMappingInheritance = -1;
        try {
            boolean mappingExists = jdbcTemplate
                    .fetchSingleRecord(buildIsRoleMappingExistsQuery(organizationId, false),
                            (resultSet, rowNumber) ->
                                    resultSet.getInt(COUNT_COLUMN_NAME) == 1,
                            preparedStatement -> {
                                int parameterIndex = 0;
                                preparedStatement.setString(++parameterIndex, userId);
                                preparedStatement.setString(++parameterIndex, roleId);
                                preparedStatement.setInt(++parameterIndex, tenantId);
                                preparedStatement.setString(++parameterIndex, organizationId);
                                if (StringUtils.isNotEmpty(organizationId)) {
                                    preparedStatement.setString(++parameterIndex, organizationId);
                                }
                            });
            if (!mappingExists) {
                return directlyAssignedRoleMappingInheritance;
            }
            directlyAssignedRoleMappingInheritance =
                    jdbcTemplate.fetchSingleRecord(GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_INHERITANCE,
                            (resultSet, rowNumber) -> resultSet.getInt(VIEW_INHERIT_COLUMN),
                            preparedStatement -> {
                                int parameterIndex = 0;
                                preparedStatement.setString(++parameterIndex, userId);
                                preparedStatement.setString(++parameterIndex, roleId);
                                preparedStatement.setInt(++parameterIndex, tenantId);
                                preparedStatement.setString(++parameterIndex, organizationId);
                                preparedStatement.setString(++parameterIndex, organizationId);
                            });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR), roleId,
                            userId, organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getCode(), e);
        }
        return directlyAssignedRoleMappingInheritance;
    }

    @Override
    public Integer getRoleIdBySCIMGroupName(String roleName, int tenantId)
            throws OrganizationUserRoleMgtServerException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            return jdbcTemplate.fetchSingleRecord(GET_ROLE_ID_BY_SCIM_GROUP_NAME,
                    (resultSet, rowNumber) ->
                            resultSet.getInt(VIEW_ID_COLUMN),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, roleName);
                        preparedStatement.setInt(++parameterIndex, tenantId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_HYBRID_ROLE_ID_RETRIEVING_ERROR, roleName);
        }
    }

    private String buildQueryForMultipleInserts(Integer numberOfMapings) {

        StringBuilder sb = new StringBuilder();
        sb.append(INSERT_ALL);

        for (int i = 0; i < numberOfMapings; i++) {
            sb.append(INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING);
        }
        sb.append(SELECT_DUMMY_RECORD);
        return sb.toString();
    }

    private String buildQueryForMultipleRoleMappingDeletion(int numberOfOrganizations) {

        StringBuilder sb = new StringBuilder();
        sb.append(DELETE_ORGANIZATION_USER_ROLE_MAPPINGS_ASSIGNED_AT_ORG_LEVEL);
        sb.append(AND).append("(");
        for (int i = 0; i < numberOfOrganizations; i++) {
            sb.append(ORG_ID_ADDING);
            if (i != numberOfOrganizations - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildIsRoleMappingExistsQuery(String assignedLevel, boolean checkInheritance) {

        StringBuilder sb = new StringBuilder();
        sb.append(GET_ORGANIZATION_USER_ROLE_MAPPING);
        if (StringUtils.isNotEmpty(assignedLevel)) {
            sb.append(AND).append(ASSIGNED_AT_ADDING);
        }
        if (checkInheritance) {
            sb.append(AND).append(INHERIT_ADDING);
        }
        return sb.toString();
    }

}
