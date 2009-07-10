/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.capability;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.vote.AccessDecisionVoter;

import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.caveat.RMCaveatConfigImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.SimplePermissionReference;
import org.alfresco.repo.security.permissions.impl.acegi.ACLEntryVoterException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

public class RMEntryVoter implements AccessDecisionVoter, InitializingBean
{
    private static Log logger = LogFactory.getLog(RMEntryVoter.class);

    private static final String RM = "RM";

    private static final String RM_ALLOW = "RM_ALLOW";

    private static final String RM_DENY = "RM_DENY";

    private static final String RM_CAP = "RM_CAP";

    private NamespacePrefixResolver nspr;

    private NodeService nodeService;

    private PermissionService permissionService;

    protected RMCaveatConfigImpl caveatConfigImpl;

    private static HashMap<String, Policy> policies = new HashMap<String, Policy>();

    static SimplePermissionReference VIEW_RECORDS = SimplePermissionReference.getPermissionReference(RecordsManagementModel.ASPECT_FILE_PLAN_COMPONENT,
            RMPermissionModel.VIEW_RECORDS);

    static
    {
        policies.put("Read", new ReadPolicy());
        policies.put("ReadRmOrDm", new ReadRmOrDmPolicy());
        policies.put("Create", new CreatePolicy());
        policies.put("Move", new MovePolicy());
        policies.put("Update", new UpdatePolicy());
        policies.put("Delete", new DeletePolicy());
        policies.put("UpdateProperties", new UpdatePropertiesPolicy());
        policies.put("Assoc", new AssocPolicy());
        policies.put("WriteContent", new WriteContentPolicy());
        policies.put("Capability", new CapabilityPolicy());

    }

    /**
     * Set the permission service
     * 
     * @param permissionService
     */
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    /**
     * Set the node service
     * 
     * @param nodeService
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Set the name space prefix resolver
     * 
     * @param nspr
     */
    public void setNamespacePrefixResolver(NamespacePrefixResolver nspr)
    {
        this.nspr = nspr;
    }

    public void setCaveatConfigImpl(RMCaveatConfigImpl caveatConfigImpl)
    {
        this.caveatConfigImpl = caveatConfigImpl;
    }

    public boolean supports(ConfigAttribute attribute)
    {
        if ((attribute.getAttribute() != null)
                && (attribute.getAttribute().equals(RM_ALLOW) || attribute.getAttribute().equals(RM_DENY) || attribute.getAttribute().startsWith(RM_CAP) || attribute
                        .getAttribute().startsWith(RM)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean supports(Class clazz)
    {
        return (MethodInvocation.class.isAssignableFrom(clazz));
    }

    public int checkRead(NodeRef nodeRef)
    {
        if (nodeRef != null)
        {
            // now we know the node - we can abstain for certain types and aspects (eg, rm)
            return checkRead(this, nodeRef, false);
        }

        return AccessDecisionVoter.ACCESS_ABSTAIN;
    }

    public int vote(Authentication authentication, Object object, ConfigAttributeDefinition config)
    {
        if (logger.isDebugEnabled())
        {
            MethodInvocation mi = (MethodInvocation) object;
            logger.debug("Method: " + mi.getMethod().toString());
        }
        if (AuthenticationUtil.isRunAsUserTheSystemUser())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Access granted for the system user");
            }
            return AccessDecisionVoter.ACCESS_GRANTED;
        }

        List<ConfigAttributeDefintion> supportedDefinitions = extractSupportedDefinitions(config);

        if (supportedDefinitions.size() == 0)
        {
            return AccessDecisionVoter.ACCESS_GRANTED;
        }

        MethodInvocation invocation = (MethodInvocation) object;

        Method method = invocation.getMethod();
        Class[] params = method.getParameterTypes();

        for (ConfigAttributeDefintion cad : supportedDefinitions)
        {
            if (cad.typeString.equals(RM_DENY))
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else if (cad.typeString.equals(RM_ALLOW))
            {
                return AccessDecisionVoter.ACCESS_GRANTED;
            }
            else if (
                        ((cad.parameters.get(0) != null) && (cad.parameters.get(0) >= invocation.getArguments().length)) || 
                        ((cad.parameters.get(1) != null) && (cad.parameters.get(1) >= invocation.getArguments().length))
                    )
            {
                continue;
            }
            else if (cad.typeString.equals(RM_CAP))
            {
                if (checkCapability(invocation, params, cad) == AccessDecisionVoter.ACCESS_DENIED)
                {
                    return AccessDecisionVoter.ACCESS_DENIED;
                }
            }
            else if (cad.typeString.equals(RM))
            {
                if (checkPolicy(invocation, params, cad) == AccessDecisionVoter.ACCESS_DENIED)
                {
                    return AccessDecisionVoter.ACCESS_DENIED;
                }
            }
        }

        return AccessDecisionVoter.ACCESS_GRANTED;

    }

    private int checkCapability(MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
    {
        NodeRef testNodeRef = getTestNode(nodeService, invocation, params, cad.parameters.get(0), cad.parent);

        if (testNodeRef != null)
        {
            if (nodeService.hasAspect(testNodeRef, RecordsManagementModel.ASPECT_FILE_PLAN_COMPONENT))
            {
                // now we know the node - we can abstain for certain types and aspects (eg, rm)

                if (logger.isDebugEnabled())
                {
                    logger.debug("\t\tNode ref is not null");
                }
                if (permissionService.hasPermission(testNodeRef, cad.required.toString()) == AccessStatus.DENIED)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("\t\tPermission is denied");
                        Thread.dumpStack();
                    }
                    return AccessDecisionVoter.ACCESS_DENIED;
                }
                else
                {
                    return AccessDecisionVoter.ACCESS_GRANTED;
                }
            }
            else
            {
                return AccessDecisionVoter.ACCESS_ABSTAIN;
            }
        }

        return AccessDecisionVoter.ACCESS_ABSTAIN;

    }

    private static NodeRef getTestNode(NodeService nodeService, MethodInvocation invocation, Class[] params, int position, boolean parent)
    {
        NodeRef testNodeRef = null;
        if (StoreRef.class.isAssignableFrom(params[position]))
        {
            if (invocation.getArguments()[position] != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("\tPermission test against the store - using permissions on the root node");
                }
                StoreRef storeRef = (StoreRef) invocation.getArguments()[position];
                if (nodeService.exists(storeRef))
                {
                    testNodeRef = nodeService.getRootNode(storeRef);
                }
            }
        }
        else if (NodeRef.class.isAssignableFrom(params[position]))
        {
            testNodeRef = (NodeRef) invocation.getArguments()[position];
            if (parent)
            {
                testNodeRef = nodeService.getPrimaryParent(testNodeRef).getParentRef();
                if (logger.isDebugEnabled())
                {
                    if (nodeService.exists(testNodeRef))
                    {
                        logger.debug("\tPermission test for parent on node " + nodeService.getPath(testNodeRef));
                    }
                    else
                    {
                        logger.debug("\tPermission test for parent on non-existing node " + testNodeRef);
                    }
                    logger.debug("\tPermission test for parent on node " + nodeService.getPath(testNodeRef));
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    if (nodeService.exists(testNodeRef))
                    {
                        logger.debug("\tPermission test on node " + nodeService.getPath(testNodeRef));
                    }
                    else
                    {
                        logger.debug("\tPermission test on non-existing node " + testNodeRef);
                    }
                }
            }
        }
        else if (ChildAssociationRef.class.isAssignableFrom(params[position]))
        {
            if (invocation.getArguments()[position] != null)
            {
                if (parent)
                {
                    testNodeRef = ((ChildAssociationRef) invocation.getArguments()[position]).getParentRef();
                }
                else
                {
                    testNodeRef = ((ChildAssociationRef) invocation.getArguments()[position]).getChildRef();
                }
                if (logger.isDebugEnabled())
                {
                    if (nodeService.exists(testNodeRef))
                    {
                        logger.debug("\tPermission test on node " + nodeService.getPath(testNodeRef));
                    }
                    else
                    {
                        logger.debug("\tPermission test on non-existing node " + testNodeRef);
                    }
                }
            }
        }
        else if (AssociationRef.class.isAssignableFrom(params[position]))
        {
            if (invocation.getArguments()[position] != null)
            {
                if (parent)
                {
                    testNodeRef = ((AssociationRef) invocation.getArguments()[position]).getSourceRef();
                }
                else
                {
                    testNodeRef = ((AssociationRef) invocation.getArguments()[position]).getTargetRef();
                }
                if (logger.isDebugEnabled())
                {
                    if (nodeService.exists(testNodeRef))
                    {
                        logger.debug("\tPermission test on node " + nodeService.getPath(testNodeRef));
                    }
                    else
                    {
                        logger.debug("\tPermission test on non-existing node " + testNodeRef);
                    }
                }
            }
        }
        else
        {
            throw new ACLEntryVoterException("The specified parameter is not a NodeRef or ChildAssociationRef");
        }
        return testNodeRef;
    }

    private int checkPolicy(MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
    {
        Policy policy = policies.get(cad.policyName);
        if (policy == null)
        {
            return AccessDecisionVoter.ACCESS_GRANTED;
        }
        else
        {
            return policy.evaluate(this, invocation, params, cad);
        }
    }

    public void afterPropertiesSet() throws Exception
    {
        // TODO Auto-generated method stub

    }

    private List<ConfigAttributeDefintion> extractSupportedDefinitions(ConfigAttributeDefinition config)
    {
        List<ConfigAttributeDefintion> definitions = new ArrayList<ConfigAttributeDefintion>(2);
        Iterator iter = config.getConfigAttributes();

        while (iter.hasNext())
        {
            ConfigAttribute attr = (ConfigAttribute) iter.next();

            if (this.supports(attr))
            {
                definitions.add(new ConfigAttributeDefintion(attr));
            }

        }
        return definitions;
    }

    private class ConfigAttributeDefintion
    {
        String typeString;

        String policyName;

        SimplePermissionReference required;

        HashMap<Integer, Integer> parameters = new HashMap<Integer, Integer>(2, 1.0f);
        
        boolean parent = false;

        ConfigAttributeDefintion(ConfigAttribute attr)
        {
            StringTokenizer st = new StringTokenizer(attr.getAttribute(), ".", false);
            if (st.countTokens() < 1)
            {
                throw new ACLEntryVoterException("There must be at least one token in a config attribute");
            }
            typeString = st.nextToken();

            if (!(typeString.equals(RM) || typeString.equals(RM_ALLOW) || typeString.equals(RM_CAP) || typeString.equals(RM_DENY)))
            {
                throw new ACLEntryVoterException("Invalid type: must be ACL_NODE, ACL_PARENT or ACL_ALLOW");
            }

            if (typeString.equals(RM))
            {
                policyName = st.nextToken();
                int position = 0;
                while (st.hasMoreElements())
                {
                    String numberString = st.nextToken();
                    Integer value = Integer.parseInt(numberString);
                    parameters.put(position, value);
                    position++;
                }
            }
            else if (typeString.equals(RM_CAP))
            {
                String numberString = st.nextToken();
                String qNameString = st.nextToken();
                String permissionString = st.nextToken();

                Integer value = Integer.parseInt(numberString);
                parameters.put(0, value);

                QName qName = QName.createQName(qNameString, nspr);

                required = SimplePermissionReference.getPermissionReference(qName, permissionString);

                if (st.hasMoreElements())
                {
                    parent = true;
                }
            }
        }
    }

    private static int checkRead(RMEntryVoter voter, NodeRef nodeRef, boolean allowDMRead)
    {
        if (voter.nodeService.hasAspect(nodeRef, RecordsManagementModel.ASPECT_FILE_PLAN_COMPONENT))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("\t\tNode ref is not null");
            }
            if (voter.permissionService.hasPermission(nodeRef, VIEW_RECORDS.toString()) == AccessStatus.DENIED)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("\t\tPermission is denied");
                    Thread.dumpStack();
                }
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                if (voter.caveatConfigImpl.hasAccess(nodeRef))
                {
                    return AccessDecisionVoter.ACCESS_GRANTED;
                }
                else
                {
                    return AccessDecisionVoter.ACCESS_DENIED;
                }
            }
        }
        else
        {
            if (allowDMRead)
            {
                // Check DM read for copy etc
                // DM does not grant - it can only deny
                if (voter.permissionService.hasPermission(nodeRef, PermissionService.READ) == AccessStatus.DENIED)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("\t\tPermission is denied");
                        Thread.dumpStack();
                    }
                    return AccessDecisionVoter.ACCESS_DENIED;
                }
                else
                {
                    return AccessDecisionVoter.ACCESS_GRANTED;
                }
            }
            else
            {
                return AccessDecisionVoter.ACCESS_ABSTAIN;
            }
        }
    }

    interface Policy
    {
        int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad);
    }

    private static class ReadPolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            NodeRef testNodeRef = getTestNode(voter.nodeService, invocation, params, cad.parameters.get(0), cad.parent);

            if (testNodeRef != null)
            {
                // now we know the node - we can abstain for certain types and aspects (eg, rm)
                return checkRead(voter, testNodeRef, false);
            }

            return AccessDecisionVoter.ACCESS_ABSTAIN;

        }

    }

    private static class ReadRmOrDmPolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            int first = AccessDecisionVoter.ACCESS_ABSTAIN;
            int second = AccessDecisionVoter.ACCESS_ABSTAIN;

            if (cad.parameters.get(0) > -1)
            {
                NodeRef testNodeRef = getTestNode(voter.nodeService, invocation, params, cad.parameters.get(0), cad.parent);

                if (testNodeRef != null)
                {
                    // now we know the node - we can abstain for certain types and aspects (eg, rm)
                    first = checkRead(voter, testNodeRef, true);
                }
            }
            
            if (cad.parameters.get(1) > -1)
            {
                NodeRef testNodeRef = getTestNode(voter.nodeService, invocation, params, cad.parameters.get(1), cad.parent);

                if (testNodeRef != null)
                {
                    // now we know the node - we can abstain for certain types and aspects (eg, rm)
                    second = checkRead(voter, testNodeRef, true);
                }
            }

            if((first == AccessDecisionVoter.ACCESS_DENIED ) || (second  == AccessDecisionVoter.ACCESS_DENIED))
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            
            if((first == AccessDecisionVoter.ACCESS_GRANTED ) && (second  == AccessDecisionVoter.ACCESS_GRANTED))
            {
                return AccessDecisionVoter.ACCESS_GRANTED;
            }      
            
            return AccessDecisionVoter.ACCESS_ABSTAIN;

        }

    }

    private static class CreatePolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class MovePolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("ReadRmOrDm");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class UpdatePolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class DeletePolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class UpdatePropertiesPolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class AssocPolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class WriteContentPolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }

    private static class CapabilityPolicy implements Policy
    {

        public int evaluate(RMEntryVoter voter, MethodInvocation invocation, Class[] params, ConfigAttributeDefintion cad)
        {
            Policy policy = policies.get("Read");
            if (policy == null)
            {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
            else
            {
                return policy.evaluate(voter, invocation, params, cad);
            }
        }

    }
}