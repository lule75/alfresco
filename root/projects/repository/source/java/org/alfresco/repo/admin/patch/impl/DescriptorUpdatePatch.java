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
package org.alfresco.repo.admin.patch.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Apply Version Edition to Repository Descriptor
 *
 * @author David Caruana
 */
public class DescriptorUpdatePatch extends AbstractPatch
{
    private static final String MSG_SUCCESS = "patch.descriptorUpdate.result";

    private ImporterBootstrap systemBootstrap;
    
    public void setSystemBootstrap(ImporterBootstrap systemBootstrap)
    {
        this.systemBootstrap = systemBootstrap;
    }

    @Override
    protected String applyInternal() throws Exception
    {
        checkPropertyNotNull(systemBootstrap, "systemBootstrap");
        
        // retrieve system descriptor location
        StoreRef storeRef = systemBootstrap.getStoreRef();
        Properties systemProperties = systemBootstrap.getConfiguration();

        // check for the store
        if (nodeService.exists(storeRef))
        {
            // get the current descriptor
            String path = systemProperties.getProperty("system.descriptor.current.childname");
            String searchPath = "/" + path;
            NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
            List<NodeRef> nodeRefs = searchService.selectNodes(rootNodeRef, searchPath, null, namespaceService, false);
            if (nodeRefs.size() > 0)
            {
                NodeRef descriptorNodeRef = nodeRefs.get(0);

                // set version edition
                Serializable value = nodeService.getProperty(descriptorNodeRef, ContentModel.PROP_SYS_VERSION_EDITION);
                if (value == null)
                {
                    String edition = systemProperties.getProperty("version.edition");
                    Collection<String> editions = new ArrayList<String>();
                    editions.add(edition);
                    nodeService.setProperty(descriptorNodeRef, ContentModel.PROP_SYS_VERSION_EDITION, (Serializable)editions);
                }
            }
        }
        
        // done
        String msg = I18NUtil.getMessage(MSG_SUCCESS);
        return msg;
    }
    
}
