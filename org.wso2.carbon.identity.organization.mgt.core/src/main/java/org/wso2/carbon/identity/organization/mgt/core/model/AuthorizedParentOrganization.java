package org.wso2.carbon.identity.organization.mgt.core.model;

/**
 * This class represents the authorized parent organization of a given user
 */
public class AuthorizedParentOrganization {

    private int inherit;
    private String displayName;
    private String path;

    public AuthorizedParentOrganization(int inherit, String displayName, String path) {
        this.inherit = inherit;
        this.displayName = displayName;
        this.path = path;
    }

    public int getInherit() {
        return inherit;
    }

    public void setInherit(int inherit) {
        this.inherit = inherit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
