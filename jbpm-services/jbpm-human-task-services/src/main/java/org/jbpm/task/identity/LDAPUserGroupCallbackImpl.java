/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.task.identity;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.enterprise.inject.Alternative;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.kie.internal.task.api.UserGroupCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDAP integration for Task Service to collect user and role/group information.
 * 
 * Following is a list of all supported properties:
 * <ul>
 *  <li>ldap.bind.user (optional if LDAP server accepts anonymous access)</li>
 *  <li>ldap.bind.pwd (optional if LDAP server accepts anonymous access</li>
 *  <li>ldap.user.ctx (mandatory)</li>
 *  <li>ldap.role.ctx (mandatory)</li>
 *  <li>ldap.user.roles.ctx (optional, if not given ldap.role.ctx will be used)</li>
 *  <li>ldap.user.filter (mandatory)</li>
 *  <li>ldap.role.filter (mandatory)</li>
 *  <li>ldap.user.roles.filter (mandatory)</li>
 *  <li>ldap.user.attr.id (optional, if not given 'uid' will be used)</li>
 *  <li>ldap.roles.attr.id (optional, if not given 'cn' will be used)</li>
 *  <li>ldap.user.id.dn (optional, is user id a DN, instructs the callback to query for user DN before searching for roles, default false)</li>
 *  <li>java.naming.factory.initial</li>
 *  <li>java.naming.security.authentication</li>
 *  <li>java.naming.security.protocol</li>
 *  <li>java.naming.provider.url</li>
 *  <li></li>
 * </ul>
 */

@Alternative
public class LDAPUserGroupCallbackImpl implements UserGroupCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(LDAPUserGroupCallbackImpl.class);
    
    protected static final String DEFAULT_PROPERTIES_NAME = "/jbpm.usergroup.callback.properties";
    
    public static final String BIND_USER = "ldap.bind.user";
    public static final String BIND_PWD = "ldap.bind.pwd";
    public static final String USER_CTX = "ldap.user.ctx";
    public static final String ROLE_CTX = "ldap.role.ctx";
    public static final String USER_ROLES_CTX = "ldap.user.roles.ctx";
    public static final String USER_FILTER = "ldap.user.filter";
    public static final String ROLE_FILTER = "ldap.role.filter";
    public static final String USER_ROLES_FILTER = "ldap.user.roles.filter";
    public static final String USER_ATTR_ID = "ldap.user.attr.id";
    public static final String ROLE_ATTR_ID = "ldap.roles.attr.id";
    public static final String IS_USER_ID_DN = "ldap.user.id.dn";
    
    protected static final String[] requiredProperties = {USER_CTX, ROLE_CTX, USER_FILTER, ROLE_FILTER, USER_ROLES_FILTER};

    
    private Properties config;
    
    
    public LDAPUserGroupCallbackImpl() {
        String propertiesLocation = System.getProperty("jbpm.usergroup.callback.properties");
        
        if (propertiesLocation == null) {
            propertiesLocation = DEFAULT_PROPERTIES_NAME;
        }
        logger.debug("Callback properties will be loaded from " + propertiesLocation);
        InputStream in = this.getClass().getResourceAsStream(propertiesLocation);
        if (in != null) {
            config = new Properties();
            try {
                config.load(in);
            } catch (IOException e) {
                e.printStackTrace();
                config = null;
            }
        }
       
        validate();
    }
    
    public LDAPUserGroupCallbackImpl(Properties config) {
        this.config = config;
        validate();
    }

    public boolean existsUser(String userId) {
        
        InitialLdapContext ctx = null;
        boolean exists = false;
        try {
            ctx = buildInitialLdapContext();
            
            String userContext = this.config.getProperty(USER_CTX);
            String userFilter = this.config.getProperty(USER_FILTER);
            String userAttrId = this.config.getProperty(USER_ATTR_ID, "uid");
            
            userFilter = userFilter.replaceAll("\\{0\\}", userId);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Seaching for user existence with filter " + userFilter + " on context " + userContext);
            }
            
            SearchControls constraints = new SearchControls();
            
            NamingEnumeration<SearchResult> result = ctx.search(userContext, userFilter, constraints);
            if (result.hasMore()) {
                
                SearchResult sr = result.next();
                Attribute ldapUserId = sr.getAttributes().get(userAttrId);
                
                if (ldapUserId.contains(userId)) {
                    exists = true;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Entry in LDAP found and result of matching with given user id is " + exists);
                }
            }
            result.close();
        
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
            
        }
        
        return exists;
    }

    public boolean existsGroup(String groupId) {
        
        InitialLdapContext ctx = null;
        boolean exists = false;
        try {
            ctx = buildInitialLdapContext();
            
            String roleContext = this.config.getProperty(ROLE_CTX);
            String roleFilter = this.config.getProperty(ROLE_FILTER);
            String roleAttrId = this.config.getProperty(ROLE_ATTR_ID, "cn");
            
            roleFilter = roleFilter.replaceAll("\\{0\\}", groupId);
            
            SearchControls constraints = new SearchControls();
            
            NamingEnumeration<SearchResult> result = ctx.search(roleContext, roleFilter, constraints);
            if (result.hasMore()) {
                SearchResult sr = result.next();
                Attribute ldapUserId = sr.getAttributes().get(roleAttrId);
                
                if (ldapUserId.contains(groupId)) {
                    exists = true;
                }
                
            }
            result.close();
        
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
            
        }
        
        return exists;
    }

    public List<String> getGroupsForUser(String userId, List<String> groupIds,
            List<String> allExistingGroupIds) {
       
        InitialLdapContext ctx = null;
        List<String> userGroups = new ArrayList<String>();
        try {
            ctx = buildInitialLdapContext();
            
            String userDN = null;
            // if user id is not DN look it up first in ldap
            if (!Boolean.parseBoolean(this.config.getProperty(IS_USER_ID_DN, "false"))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("User id is not DN, looking up user first...");
                }
                String userContext = this.config.getProperty(USER_CTX);
                String userFilter = this.config.getProperty(USER_FILTER);
                
                userFilter = userFilter.replaceAll("\\{0\\}", userId);
                SearchControls constraints = new SearchControls();
                if (logger.isDebugEnabled()) {
                    logger.debug("Searching for user DN with filter " + userFilter + " on context " + userContext);
                }
                
                NamingEnumeration<SearchResult> result = ctx.search(userContext, userFilter, constraints);
                if (result.hasMore()) {
                    SearchResult searchResult = result.nextElement();
                    userDN = searchResult.getNameInNamespace();
                    if (logger.isDebugEnabled()) {
                        logger.debug("User DN found, DN is " + userDN);
                    }
                }
                result.close();
            }
            
            String roleContext = this.config.getProperty(USER_ROLES_CTX, ROLE_CTX);
            String roleFilter = this.config.getProperty(USER_ROLES_FILTER);
            String roleAttrId = this.config.getProperty(ROLE_ATTR_ID, "cn");
            
            roleFilter = roleFilter.replaceAll("\\{0\\}", (userDN != null ? userDN : userId));
            SearchControls constraints = new SearchControls();
            if (logger.isDebugEnabled()) {
                logger.debug("Searching for groups for user with filter " + roleFilter + " on context " + roleContext);
            }
            NamingEnumeration<SearchResult> result = ctx.search(roleContext, roleFilter, constraints);
            if (result.hasMore()) {
                SearchResult searchResult = null;
                String name = null;
                while (result.hasMore()) {
                    searchResult = result.nextElement();
                    name = (String) searchResult.getAttributes().get(roleAttrId).get();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found group " + name);
                    }
                    userGroups.add(name);
                }
            }
            result.close();
        
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
            
        }
        return userGroups;
    }
    
    protected void validate() {
        if (this.config == null) {
            throw new IllegalArgumentException("No configuration found for LDAPUserGroupCallbackImpl, aborting...");
        }
        StringBuffer missingRequiredProps = new StringBuffer();
        for (String requiredProp : requiredProperties) {
            if (!this.config.containsKey(requiredProp)) {
                if (missingRequiredProps.length() > 0) {
                    missingRequiredProps.append(", ");
                }
                missingRequiredProps.append(requiredProp);
            }
        }
        
        if (missingRequiredProps.length() > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Validation failed due to missing required properties: " + missingRequiredProps.toString());
            }
            throw new IllegalArgumentException("Missing required properties to configure LDAPUserGroupCallbackImpl: " + missingRequiredProps.toString());
        }
    }
    
    protected InitialLdapContext buildInitialLdapContext() throws NamingException {

        // Set defaults for key values if they are missing
        String factoryName = this.config.getProperty(Context.INITIAL_CONTEXT_FACTORY);

        if (factoryName == null)  {

            factoryName = "com.sun.jndi.ldap.LdapCtxFactory";
            this.config.setProperty(Context.INITIAL_CONTEXT_FACTORY, factoryName);
        }

        String authType = this.config.getProperty(Context.SECURITY_AUTHENTICATION);

        if (authType == null) {

            this.config.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        String protocol = this.config.getProperty(Context.SECURITY_PROTOCOL);

        String providerURL = (String) this.config.getProperty(Context.PROVIDER_URL);
        if (providerURL == null) {

            providerURL = "ldap://localhost:"+ ((protocol != null && protocol.equals("ssl")) ? "636" : "389");
            this.config.setProperty(Context.PROVIDER_URL, providerURL);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Using following InitialLdapContext properties:");
            logger.debug("Factory " + this.config.getProperty(Context.INITIAL_CONTEXT_FACTORY));
            logger.debug("Authentication " + this.config.getProperty(Context.SECURITY_AUTHENTICATION));
            logger.debug("Protocol " +  this.config.getProperty(Context.SECURITY_PROTOCOL));
            logger.debug("Provider URL " +  this.config.getProperty(Context.PROVIDER_URL));
        }
        
        return new InitialLdapContext(this.config, null);
    }
    

}
