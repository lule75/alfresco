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
package org.alfresco.module.org_alfresco_module_dod5015.action.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.action.RMActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;

/**
 * Transfer complete action
 * 
 * @author Roy Wetherall
 */
public class TransferCompleteAction extends RMActionExecuterAbstractBase
{
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RMActionExecuterAbstractBase#isExecutableImpl(org.alfresco.service.cmr.repository.NodeRef,
     *      java.util.Map, boolean)
     */
    @Override
    protected boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException)
    {
        QName className = this.nodeService.getType(filePlanComponent);
        if (this.dictionaryService.isSubClass(className, TYPE_TRANSFER) == true)
        {
            return true;
        }
        else
        {
            List<ChildAssociationRef> assocs = this.nodeService.getParentAssocs(filePlanComponent, ASSOC_TRANSFERRED, RegexQNamePattern.MATCH_ALL);
            return assocs.size() > 0;
        }
    }

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action,
     *      org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        QName className = this.nodeService.getType(actionedUponNodeRef);
        if (this.dictionaryService.isSubClass(className, TYPE_TRANSFER) == true)
        {
            boolean accessionIndicator = ((Boolean)nodeService.getProperty(actionedUponNodeRef, PROP_TRANSFER_ACCESSION_INDICATOR)).booleanValue();
            
            List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(actionedUponNodeRef, ASSOC_TRANSFERRED, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef assoc : assocs)
            {
                markComplete(assoc.getChildRef(), accessionIndicator);
            }

            // Delete the transfer object
            this.nodeService.deleteNode(actionedUponNodeRef);

            NodeRef transferNodeRef = (NodeRef) AlfrescoTransactionSupport.getResource(TransferAction.KEY_TRANSFER_NODEREF);
            if (transferNodeRef != null)
            {
                if (transferNodeRef.equals(actionedUponNodeRef))
                {
                    AlfrescoTransactionSupport.bindResource(TransferAction.KEY_TRANSFER_NODEREF, null);
                }
            }
        }
        else
        {
            throw new AlfrescoRuntimeException("Actioned upon node is not a valid transfer object.");
        }
    }

    /**
     * Marks the node complete
     * 
     * @param nodeRef
     *            disposition lifecycle node reference
     */
    private void markComplete(NodeRef nodeRef, boolean accessionIndicator)
    {
        // Set the completed date
        DispositionAction da = recordsManagementService.getNextDispositionAction(nodeRef);
        if (da != null)
        {
            nodeService.setProperty(da.getNodeRef(), PROP_DISPOSITION_ACTION_COMPLETED_AT, new Date());
            nodeService.setProperty(da.getNodeRef(), PROP_DISPOSITION_ACTION_COMPLETED_BY, AuthenticationUtil.getRunAsUser());
        }
        
        // Determine which marker aspect to use
        QName markerAspectQName = null;
        if (accessionIndicator == true)
        {
            markerAspectQName = ASPECT_ASCENDED;
        }
        else
        {
            markerAspectQName = ASPECT_TRANSFERRED;
        }
        
        // Mark the object and children accordingly
        nodeService.addAspect(nodeRef, markerAspectQName, null);
        if (recordsManagementService.isRecordFolder(nodeRef) == true)
        {
            List<NodeRef> records = recordsManagementService.getRecords(nodeRef);
            for (NodeRef record : records)
            {
                nodeService.addAspect(record, markerAspectQName, null);
            }
        }

        // Update to the next disposition action
        updateNextDispositionAction(nodeRef);
    }
}
