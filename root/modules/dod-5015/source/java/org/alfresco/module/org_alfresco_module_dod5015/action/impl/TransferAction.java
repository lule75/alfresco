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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.action.RMDispositionActionExecuterAbstractBase;
import org.alfresco.module.org_alfresco_module_dod5015.DOD5015Model;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition;
import org.alfresco.repo.action.executer.ActionExecuter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Transfer action
 * 
 * @author Roy Wetherall
 */
public class TransferAction extends RMDispositionActionExecuterAbstractBase
{    
    /** Transfer node reference key */
    public static final String KEY_TRANSFER_NODEREF = "transferNodeRef";
    
    /** Indicates whether the transfer is an accession or not */
    private boolean isAccession = false;
    
    /**
     * Indicates whether this transfer is an accession or not
     * 
     * @param isAccession
     */
    public void setIsAccession(boolean isAccession)
    {
        this.isAccession = isAccession;
    }
    
    /**
     * Do not set the transfer action to auto-complete
     * 
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RMDispositionActionExecuterAbstractBase#getSetDispositionActionComplete()
     */
    @Override
    public boolean getSetDispositionActionComplete()
    {
        return false;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RMDispositionActionExecuterAbstractBase#executeRecordFolderLevelDisposition(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeRecordFolderLevelDisposition(Action action, NodeRef recordFolder)
    {
        doTransfer(action, recordFolder);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RMDispositionActionExecuterAbstractBase#executeRecordLevelDisposition(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeRecordLevelDisposition(Action action, NodeRef record)
    {
        doTransfer(action, record);
    }
    
    /**
     * Create the transfer node and link the disposition lifecycle node beneath it
     * 
     * @param dispositionLifeCycleNodeRef        disposition lifecycle node
     */
    private void doTransfer(Action action, NodeRef dispositionLifeCycleNodeRef)
    {
        // Get the root rm node
        NodeRef root = this.recordsManagementService.getRecordsManagementRoot(dispositionLifeCycleNodeRef);
        
        // Get the hold object
        NodeRef transferNodeRef = (NodeRef)AlfrescoTransactionSupport.getResource(KEY_TRANSFER_NODEREF);            
        if (transferNodeRef == null)
        {
            // Calculate a transfer name
            QName nodeDbid = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "node-dbid");
            Long dbId = (Long)this.nodeService.getProperty(dispositionLifeCycleNodeRef, nodeDbid);
            String transferName = padString(dbId.toString(), 10);
            
            // Create the transfer object
            Map<QName, Serializable> transferProps = new HashMap<QName, Serializable>(2);
            transferProps.put(ContentModel.PROP_NAME, transferName);
            transferProps.put(PROP_TRANSFER_ACCESSION_INDICATOR, this.isAccession);
            
            // setup location property from disposition schedule
            DispositionAction da = recordsManagementService.getNextDispositionAction(dispositionLifeCycleNodeRef);
            if (da != null)
            {
                DispositionActionDefinition actionDef = da.getDispositionActionDefinition();
                if (actionDef != null)
                {
                    transferProps.put(PROP_TRANSFER_LOCATION, actionDef.getLocation());
                }
            }
            
            transferNodeRef = this.nodeService.createNode(root, 
                                                      ASSOC_TRANSFERS, 
                                                      QName.createQName(RM_URI, transferName), 
                                                      TYPE_TRANSFER,
                                                      transferProps).getChildRef();
            
            // Bind the hold node reference to the transaction
            AlfrescoTransactionSupport.bindResource(KEY_TRANSFER_NODEREF, transferNodeRef);
        }
        
        // Link the record to the hold
        this.nodeService.addChild(transferNodeRef, 
                                  dispositionLifeCycleNodeRef, 
                                  ASSOC_TRANSFERRED, 
                                  ASSOC_TRANSFERRED);
        
        // Set PDF indicator flag
        setPDFIndicationFlag(transferNodeRef, dispositionLifeCycleNodeRef);
        
        // Set the return value of the action
        action.setParameterValue(ActionExecuter.PARAM_RESULT, transferNodeRef);
    }
    
    /**
     * 
     * @param transferNodeRef
     * @param dispositionLifeCycleNodeRef
     */
    private void setPDFIndicationFlag(NodeRef transferNodeRef, NodeRef dispositionLifeCycleNodeRef)
    {
       if (recordsManagementService.isRecordFolder(dispositionLifeCycleNodeRef) == true)
       {
           List<NodeRef> records = recordsManagementService.getRecords(dispositionLifeCycleNodeRef);
           for (NodeRef record : records)
           {
               setPDFIndicationFlag(transferNodeRef, record);
           }
       }
       else
       {
           ContentData contentData = (ContentData)nodeService.getProperty(dispositionLifeCycleNodeRef, ContentModel.PROP_CONTENT);
           if (contentData != null &&
               (MimetypeMap.MIMETYPE_PDF.equals(contentData.getMimetype()) == true || nodeService.hasAspect(dispositionLifeCycleNodeRef, DOD5015Model.ASPECT_PDF_RECORD)))
           {
               // Set the property indicator
               nodeService.setProperty(transferNodeRef, PROP_TRANSFER_PDF_INDICATOR, true);
           }           
       }
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RMDispositionActionExecuterAbstractBase#isExecutableImpl(org.alfresco.service.cmr.repository.NodeRef, java.util.Map, boolean)
     */
    @Override    
    protected boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException)
    {
      
        if(!super.isExecutableImpl(filePlanComponent, parameters, throwException))
        {
            // super will throw ...
            return false;
        }
        NodeRef transferNodeRef = (NodeRef)AlfrescoTransactionSupport.getResource(KEY_TRANSFER_NODEREF);            
        if (transferNodeRef != null)
        {
            List<ChildAssociationRef> transferredAlready = nodeService.getChildAssocs(transferNodeRef, ASSOC_TRANSFERRED, ASSOC_TRANSFERRED);
            for(ChildAssociationRef car : transferredAlready)
            {
                if(car.getChildRef().equals(filePlanComponent))
                {
                    if (throwException)
                    {
                        throw new AlfrescoRuntimeException("Already in transfer (" + filePlanComponent.toString() + ")");
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
