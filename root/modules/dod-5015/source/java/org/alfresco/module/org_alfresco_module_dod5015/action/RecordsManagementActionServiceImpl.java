/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.module.org_alfresco_module_dod5015.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPoliciesUtil;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.BeforeRMActionExecution;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.OnRMActionExecution;
import org.alfresco.repo.policy.ClassPolicyDelegate;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Records Management Action Service Implementation
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementActionServiceImpl implements RecordsManagementActionService
{
    /** Logger */
    private static Log logger = LogFactory.getLog(RecordsManagementActionServiceImpl.class);

    /** Registered records management actions */
    private Map<String, RecordsManagementAction> rmActions = new HashMap<String, RecordsManagementAction>(6);
    private Map<String, RecordsManagementAction> dispositionActions = new HashMap<String, RecordsManagementAction>(4);
    
    /** Policy component */
    PolicyComponent policyComponent;
    
    /** Node service */
    NodeService nodeService;
    
    /** Policy delegates */
    private ClassPolicyDelegate<BeforeRMActionExecution> beforeRMActionExecutionDelegate;
    private ClassPolicyDelegate<OnRMActionExecution> onRMActionExecutionDelegate;
    
    /**
     * Set the policy component
     * 
     * @param policyComponent policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * Set the node service
     * 
     * @param nodeService   node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Initialise RM action service
     */
    public void init()
    {
        // Register the various policies
        beforeRMActionExecutionDelegate = policyComponent.registerClassPolicy(BeforeRMActionExecution.class);
        onRMActionExecutionDelegate = policyComponent.registerClassPolicy(OnRMActionExecution.class);
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#register(org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAction)
     */
    public void register(RecordsManagementAction rmAction)
    {
        if (this.rmActions.containsKey(rmAction.getName()) == false)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Registering rmAction " + rmAction);
            }
            this.rmActions.put(rmAction.getName(), rmAction);
            
            if (rmAction.isDispositionAction() == true)
            {
                this.dispositionActions.put(rmAction.getName(), rmAction);
            }
        }
    }
    
    /**
     * Invoke beforeRMActionExecution policy
     * 
     * @param nodeRef       node reference
     * @param name          action name
     * @param parameters    action parameters
     */
    protected void invokeBeforeRMActionExecution(NodeRef nodeRef, String name, Map<String, Serializable> parameters)
    {
        // get qnames to invoke against
        Set<QName> qnames = RecordsManagementPoliciesUtil.getTypeAndAspectQNames(nodeService, nodeRef);
        // execute policy for node type and aspects
        BeforeRMActionExecution policy = beforeRMActionExecutionDelegate.get(qnames);
        policy.beforeRMActionExecution(nodeRef, name, parameters);
    }
    
    /**
     * Invoke onRMActionExecution policy
     * 
     * @param nodeRef       node reference
     * @param name          action name
     * @param parameters    action parameters
     */
    protected void invokeOnRMActionExecution(NodeRef nodeRef, String name, Map<String, Serializable> parameters)
    {
        // get qnames to invoke against
        Set<QName> qnames = RecordsManagementPoliciesUtil.getTypeAndAspectQNames(nodeService, nodeRef);
        // execute policy for node type and aspects
        OnRMActionExecution policy = onRMActionExecutionDelegate.get(qnames);
        policy.onRMActionExecution(nodeRef, name, parameters);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#getRecordsManagementActions()
     */
    public List<RecordsManagementAction> getRecordsManagementActions()
    {
        List<RecordsManagementAction> result = new ArrayList<RecordsManagementAction>(this.rmActions.size());
        result.addAll(this.rmActions.values());
        return Collections.unmodifiableList(result);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService#getDispositionActions(org.alfresco.service.cmr.repository.NodeRef)
     */
    public List<RecordsManagementAction> getDispositionActions(NodeRef nodeRef)
    {
        String userName = AuthenticationUtil.getFullyAuthenticatedUser();
        List<RecordsManagementAction> result = new ArrayList<RecordsManagementAction>(this.rmActions.size());
        
        for (RecordsManagementAction action : this.rmActions.values())
        {
            // TODO check the permissions on the action ...           
        }
        
        return Collections.unmodifiableList(result);
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#getDispositionActionDefinitions()
     */
    public List<RecordsManagementAction> getDispositionActions()
    {
        List<RecordsManagementAction> result = new ArrayList<RecordsManagementAction>(this.rmActions.size());
        result.addAll(this.dispositionActions.values());
        return Collections.unmodifiableList(result);
    }
    
    /*
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService#getDispositionAction(java.lang.String)
     */
    public RecordsManagementAction getDispositionAction(String name)
    {
        return this.dispositionActions.get(name);
    }

    /*
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService#getRecordsManagementAction(java.lang.String)
     */
    public RecordsManagementAction getRecordsManagementAction(String name)
    {
        return this.rmActions.get(name);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#executeRecordsManagementAction(org.alfresco.service.cmr.repository.NodeRef, java.lang.String)
     */
    public RecordsManagementActionResult executeRecordsManagementAction(NodeRef nodeRef, String name)
    {
        return executeRecordsManagementAction(nodeRef, name, null);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#executeRecordsManagementAction(java.util.List, java.lang.String)
     */
    public Map<NodeRef, RecordsManagementActionResult> executeRecordsManagementAction(List<NodeRef> nodeRefs, String name)
    {
        return executeRecordsManagementAction(nodeRefs, name, null);
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#executeRecordsManagementAction(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.util.Map)
     */
    public RecordsManagementActionResult executeRecordsManagementAction(NodeRef nodeRef, String name, Map<String, Serializable> parameters)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Executing record management action on " + nodeRef);
            logger.debug("    actionName = " + name);
            logger.debug("    parameters = " + parameters);
        }
        
        RecordsManagementAction rmAction = this.rmActions.get(name);
        if (rmAction == null)
        {
            StringBuilder msg = new StringBuilder();
            msg.append("The record management action '")
                .append(name)
                .append("' has not been defined");
            if (logger.isWarnEnabled())
            {
                logger.warn(msg.toString());
            }
            throw new AlfrescoRuntimeException(msg.toString());
        }
        
        // Execute action
        invokeBeforeRMActionExecution(nodeRef, name, parameters);
        RecordsManagementActionResult result = rmAction.execute(nodeRef, parameters);        
        if (nodeService.exists(nodeRef) == true)
        {
            invokeOnRMActionExecution(nodeRef, name, parameters);
        }
        
        return result;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#executeRecordsManagementAction(java.lang.String, java.util.Map)
     */
    public RecordsManagementActionResult executeRecordsManagementAction(String name, Map<String, Serializable> parameters)
    {
        RecordsManagementAction rmAction = rmActions.get(name);
        
        NodeRef implicitTargetNode = rmAction.getImplicitTargetNodeRef();
        if (implicitTargetNode == null)
        {
            StringBuilder msg = new StringBuilder();
            msg.append("Cannot execute rmAction ")
                .append(name)
                .append(" as the action implementation does not provide an implicit nodeRef.");
            if (logger.isWarnEnabled())
            {
                logger.warn(msg.toString());
            }
            throw new AlfrescoRuntimeException(msg.toString());
        }
        else
        {
            return this.executeRecordsManagementAction(implicitTargetNode, name, parameters);
        }
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementActionService#executeRecordsManagementAction(java.util.List, java.lang.String, java.util.Map)
     */
    public Map<NodeRef, RecordsManagementActionResult> executeRecordsManagementAction(List<NodeRef> nodeRefs, String name, Map<String, Serializable> parameters)
    {
        // Execute the action on each node in the list
        Map<NodeRef, RecordsManagementActionResult> results = new HashMap<NodeRef, RecordsManagementActionResult>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs)
        {
            RecordsManagementActionResult result = executeRecordsManagementAction(nodeRef, name, parameters);
            results.put(nodeRef, result);
        }
        
        return results;
    }
}
