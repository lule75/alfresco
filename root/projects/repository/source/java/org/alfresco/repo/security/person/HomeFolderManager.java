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
package org.alfresco.repo.security.person;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Manage home folder creation by binding to events from the cm:person type.
 *  
 * @author Andy Hind
 */
public class HomeFolderManager implements NodeServicePolicies.OnCreateNodePolicy
{
    
    private PolicyComponent policyComponent;

    private NodeService nodeService;
    
    private boolean enableHomeFolderCreationAsPeopleAreCreated = false;

    /**
     * A default provider
     */
    private HomeFolderProvider defaultProvider;

    /**
     * Providers that have registered and are looken up by name (== bean name)
     */
    private Map<String, HomeFolderProvider> providers = new HashMap<String, HomeFolderProvider>();

    /**
     * Bind the class behaviour to this implementation
     */
    public void init() throws Exception
    {
        if (enableHomeFolderCreationAsPeopleAreCreated)
        {
            policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode"));
        }
    }

    public void setEnableHomeFolderCreationAsPeopleAreCreated(boolean enableHomeFolderCreationAsPeopleAreCreated)
    {
        this.enableHomeFolderCreationAsPeopleAreCreated = enableHomeFolderCreationAsPeopleAreCreated;
    }

    /**
     * Set the policy component.
     * 
     * @param policyComponent
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    /**
     * Set the node service.
     * @param nodeService
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Register a home folder provider.
     * 
     * @param provider
     */
    public void addProvider(HomeFolderProvider provider)
    {
        providers.put(provider.getName(), provider);
    }

    /**
     * Set the default home folder provider (user which none is specified or when one is not found)
     * @param defaultProvider
     */
    public void setDefaultProvider(HomeFolderProvider defaultProvider)
    {
        this.defaultProvider = defaultProvider;
    }

    /**
     * Find the provider and call if eager home folder creation is enabled.
     */
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        if (enableHomeFolderCreationAsPeopleAreCreated)
        {
            makeHomeFolder(childAssocRef);
        }
    }

    /**
     * Find the provider and call.
     */
    public void makeHomeFolder(ChildAssociationRef childAssocRef)
    {
        HomeFolderProvider provider = defaultProvider;
        String providerName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(childAssocRef
                .getChildRef(), ContentModel.PROP_HOME_FOLDER_PROVIDER));
        if (providerName != null)
        {
            provider = providers.get(providerName);
            if (provider == null)
            {
                provider = defaultProvider;
            }
        }
        if (provider != null)
        {
            provider.onCreateNode(childAssocRef);
        }
    }
}
