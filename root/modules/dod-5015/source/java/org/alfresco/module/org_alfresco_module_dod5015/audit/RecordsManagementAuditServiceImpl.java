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
package org.alfresco.module.org_alfresco_module_dod5015.audit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.repo.audit.AuditComponent;
import org.alfresco.repo.audit.model.AuditApplication;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.transaction.TransactionalResourceHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.alfresco.util.Pair;
import org.springframework.extensions.surf.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;

/**
 * Records Management Audit Service Implementation.
 * 
 * @author Gavin Cornwell
 * @since 3.2
 */
public class RecordsManagementAuditServiceImpl
        extends AbstractLifecycleBean
        implements RecordsManagementAuditService,
                   NodeServicePolicies.OnCreateNodePolicy,
                   NodeServicePolicies.BeforeDeleteNodePolicy,
                   NodeServicePolicies.OnUpdatePropertiesPolicy
{
    /** Logger */
    private static Log logger = LogFactory.getLog(RecordsManagementAuditServiceImpl.class);

    private static final String KEY_RM_AUDIT_NODE_RECORDS = "RMAUditNodeRecords";
    
    protected static final String AUDIT_TRAIL_FILE_PREFIX = "audit_";
    protected static final String AUDIT_TRAIL_JSON_FILE_SUFFIX = ".json";
    protected static final String AUDIT_TRAIL_HTML_FILE_SUFFIX = ".html";
    protected static final String FILE_ACTION = "file";
    
    private PolicyComponent policyComponent;
    private DictionaryService dictionaryService;
    private TransactionService transactionService;
    private NodeService nodeService;
    private ContentService contentService;
    private AuditComponent auditComponent;
    private AuditService auditService;
    private RecordsManagementService rmService;
    private RecordsManagementActionService rmActionService;
    
    private boolean shutdown = false;
    
    private RMAuditTxnListener txnListener;
    private Map<String, AuditEvent> auditEvents;
    
    public RecordsManagementAuditServiceImpl()
    {
    }
    
    /**
     * Set the component used to bind to behaviour callbacks
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    /**
     * Provides user-readable names for types
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * Set the component used to start new transactions
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * Sets the NodeService instance
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService; 
    }
    
    /**
     * Sets the ContentService instance
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService; 
    }

    /**
     * The component to create audit events
     */
    public void setAuditComponent(AuditComponent auditComponent)
    {
        this.auditComponent = auditComponent;
    }

    /**
     * Sets the AuditService instance
     */
    public void setAuditService(AuditService auditService)
    {
        this.auditService = auditService;
    }
    
    /**
     * Set the  RecordsManagementService
     */
    public void setRecordsManagementService(RecordsManagementService rmService)
    {
        this.rmService = rmService;
    }

    /**
     * Sets the RecordsManagementActionService instance
     */
    public void setRecordsManagementActionService(RecordsManagementActionService rmActionService)
    {
        this.rmActionService = rmActionService;
    }
    
    /**
     * Checks that all necessary properties have been set.
     */
    public void init()
    {
        PropertyCheck.mandatory(this, "policyComponent", policyComponent);
        PropertyCheck.mandatory(this, "transactionService", transactionService);
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "contentService", contentService);
        PropertyCheck.mandatory(this, "auditComponent", auditComponent);
        PropertyCheck.mandatory(this, "auditService", auditService);
        PropertyCheck.mandatory(this, "rmService", rmService);
        PropertyCheck.mandatory(this, "rmActionService", rmActionService);
        PropertyCheck.mandatory(this, "dictionaryService", dictionaryService);
        
        // setup the audit events map
        initAuditEvents();
    }
    
    protected void initAuditEvents()
    {
        // TODO: make this map configurable and localisable.
        this.auditEvents = new HashMap<String, AuditEvent>(32);
        
        this.auditEvents.put(RM_AUDIT_EVENT_UPDATE_RM_OBJECT, 
                    new AuditEvent(RM_AUDIT_EVENT_UPDATE_RM_OBJECT, "Updated Metadata"));
        this.auditEvents.put(RM_AUDIT_EVENT_CREATE_RM_OBJECT,
                    new AuditEvent(RM_AUDIT_EVENT_CREATE_RM_OBJECT, "Created Object"));
        this.auditEvents.put(RM_AUDIT_EVENT_DELETE_RM_OBJECT, 
                    new AuditEvent(RM_AUDIT_EVENT_DELETE_RM_OBJECT, "Delete Object"));
        this.auditEvents.put(RM_AUDIT_EVENT_LOGIN_SUCCESS,
                    new AuditEvent(RM_AUDIT_EVENT_LOGIN_SUCCESS, "Login Succeeded"));
        this.auditEvents.put(RM_AUDIT_EVENT_LOGIN_FAILURE,
                new AuditEvent(RM_AUDIT_EVENT_LOGIN_FAILURE, "Login Failed"));
        
        this.auditEvents.put("file", 
                    new AuditEvent("file", "Filed Record"));
        this.auditEvents.put("reviewed", 
                    new AuditEvent("reviewed", "Reviewed"));
        this.auditEvents.put("cutoff", 
                    new AuditEvent("cutoff", "Cut Off"));
        this.auditEvents.put("unCutoff", 
                    new AuditEvent("unCutoff", "Reversed Cut Off"));
        this.auditEvents.put("destroy", 
                    new AuditEvent("destroy", "Destroyed Item"));
        this.auditEvents.put("openRecordFolder", 
                    new AuditEvent("openRecordFolder", "Opened Record Folder"));
        this.auditEvents.put("closeRecordFolder", 
                    new AuditEvent("closeRecordFolder", "Closed Record Folder"));
        this.auditEvents.put("setupRecordFolder", 
                    new AuditEvent("setupRecordFolder", "Setup Recorder Folder"));
        this.auditEvents.put("declareRecord", 
                    new AuditEvent("declareRecord", "Declared Record"));
        this.auditEvents.put("undeclareRecord", 
                    new AuditEvent("undeclareRecord", "Undeclared Record"));
        this.auditEvents.put("freeze", 
                    new AuditEvent("freeze", "Froze Item"));
        this.auditEvents.put("relinquishHold", 
                    new AuditEvent("relinquishHold", "Relinquished Hold"));
        this.auditEvents.put("editHoldReason", 
                    new AuditEvent("editHoldReason", "Updated Hold Reason"));
        this.auditEvents.put("editReviewAsOfDate", 
                    new AuditEvent("editReviewAsOfDate", "Updated Review As Of Date"));
        this.auditEvents.put("editDispositionActionAsOfDate", 
                    new AuditEvent("editDispositionActionAsOfDate", "Updated Disposition As Of Date"));
        this.auditEvents.put("broadcastVitalRecordDefinition", 
                    new AuditEvent("broadcastVitalRecordDefinition", "Updated Vital Record Definition"));
        this.auditEvents.put("broadcastDispositionActionDefinitionUpdate", 
                    new AuditEvent("broadcastDispositionActionDefinitionUpdate", "Updated Disposition Action Definition"));
        this.auditEvents.put("completeEvent", 
                    new AuditEvent("completeEvent", "Completed Event"));
        this.auditEvents.put("undoEvent", 
                    new AuditEvent("undoEvent", "Reversed Completed Event"));
        this.auditEvents.put("transfer", 
                    new AuditEvent("transfer", "Transferred Item"));
        this.auditEvents.put("transferComplete", 
                    new AuditEvent("transferComplete", "Completed Transfer"));
        this.auditEvents.put("accession", 
                    new AuditEvent("accession", "Accession"));
        this.auditEvents.put("accessionComplete", 
                    new AuditEvent("accessionComplete", "Completed Accession"));
        this.auditEvents.put("applyScannedRecord", 
                    new AuditEvent("applyScannedRecord", "Set Record As A Scanned Record"));
        this.auditEvents.put("applyPdfRecord", 
                    new AuditEvent("applyPdfRecord", "Set Record As PDF A Record"));
        this.auditEvents.put("applyDigitalPhotographRecord", 
                    new AuditEvent("applyDigitalPhotographRecord", "Set Record As A Digital Photographic Record"));
        this.auditEvents.put("applyWebRecord", 
                    new AuditEvent("applyWebRecord", "Set Record As A Web Record"));
    }
    
    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        shutdown = false;
        txnListener = new RMAuditTxnListener();
        // Register to listen for property changes to rma:record types
        policyComponent.bindClassBehaviour(
                OnUpdatePropertiesPolicy.QNAME,
                RecordsManagementModel.ASPECT_RECORD_COMPONENT_ID,
                new JavaBehaviour(this, "onUpdateProperties"));   
        policyComponent.bindClassBehaviour(
                OnCreateNodePolicy.QNAME,
                RecordsManagementModel.ASPECT_RECORD_COMPONENT_ID,
                new JavaBehaviour(this, "onCreateNode"));   
        policyComponent.bindClassBehaviour(
                BeforeDeleteNodePolicy.QNAME,
                RecordsManagementModel.ASPECT_RECORD_COMPONENT_ID,
                new JavaBehaviour(this, "beforeDeleteNode"));     
    }

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        shutdown = true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return auditService.isAuditEnabled(
                RecordsManagementAuditService.RM_AUDIT_APPLICATION_NAME,
                RecordsManagementAuditService.RM_AUDIT_PATH_ROOT);
    }
    
    /**
     * {@inheritDoc}
     */
    public void start()
    {
        auditService.enableAudit(
                RecordsManagementAuditService.RM_AUDIT_APPLICATION_NAME,
                RecordsManagementAuditService.RM_AUDIT_PATH_ROOT);
        if (logger.isInfoEnabled())
            logger.info("Started Records Management auditing");
    }

    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        auditService.disableAudit(
                RecordsManagementAuditService.RM_AUDIT_APPLICATION_NAME,
                RecordsManagementAuditService.RM_AUDIT_PATH_ROOT);
        if (logger.isInfoEnabled())
            logger.info("Stopped Records Management auditing");
    }
    
    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        auditService.clearAudit(RecordsManagementAuditService.RM_AUDIT_APPLICATION_NAME);
        if (logger.isInfoEnabled())
            logger.debug("Records Management audit log has been cleared");
    }
    
    /**
     * {@inheritDoc}
     */
    public Date getDateLastStarted()
    {
        // TODO: return proper date, for now it's today's date
        return new Date();
    }
    
    /**
     * {@inheritDoc}
     */
    public Date getDateLastStopped()
    {
        // TODO: return proper date, for now it's today's date
        return new Date();
    }
    
    /**
     * A class to carry audit information through the transaction.
     * 
     * @author Derek Hulley
     * @since 3.2
     */
    private static class RMAuditNode
    {
        private String eventName;
        private Map<QName, Serializable> nodePropertiesBefore;
        private Map<QName, Serializable> nodePropertiesAfter;
        
        private RMAuditNode()
        {
        }

        public String getEventName()
        {
            return eventName;
        }

        public void setEventName(String eventName)
        {
            this.eventName = eventName;
        }

        public Map<QName, Serializable> getNodePropertiesBefore()
        {
            return nodePropertiesBefore;
        }

        public void setNodePropertiesBefore(Map<QName, Serializable> nodePropertiesBefore)
        {
            this.nodePropertiesBefore = nodePropertiesBefore;
        }

        public Map<QName, Serializable> getNodePropertiesAfter()
        {
            return nodePropertiesAfter;
        }

        public void setNodePropertiesAfter(Map<QName, Serializable> nodePropertiesAfter)
        {
            this.nodePropertiesAfter = nodePropertiesAfter;
        }
    }
    
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        auditRMEvent(nodeRef, RM_AUDIT_EVENT_UPDATE_RM_OBJECT, before, after);
    }

    public void beforeDeleteNode(NodeRef nodeRef)
    {
        auditRMEvent(nodeRef, RM_AUDIT_EVENT_DELETE_RM_OBJECT, null, null);
    }
    
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        auditRMEvent(childAssocRef.getChildRef(), RM_AUDIT_EVENT_CREATE_RM_OBJECT, null, null);
    }

    /**
     * {@inheritDoc}
     * @since 3.2
     */
    public void auditRMAction(
            RecordsManagementAction action,
            NodeRef nodeRef,
            Map<String, Serializable> parameters)
    {
        auditRMEvent(nodeRef, action.getName(), null, null);
    }
    
    /**
     * Audit an event for a node
     * 
     * @param nodeRef               the node to which the event applies
     * @param eventName             the name of the event
     * @param nodePropertiesBefore  properties before the event (optional)
     * @param nodePropertiesAfter   properties after the event (optional)
     */
    private void auditRMEvent(
            NodeRef nodeRef,
            String eventName,
            Map<QName, Serializable> nodePropertiesBefore,
            Map<QName, Serializable> nodePropertiesAfter)
    {
        // If we are deleting nodes, then we need to audit NOW
        if (eventName.equals(RecordsManagementAuditService.RM_AUDIT_EVENT_DELETE_RM_OBJECT))
        {
            // Deleted nodes will not be available at the end of the transaction.  The data needs to
            // be extracted now and the audit entry needs to be created now.
            Map<String, Serializable> auditMap = new HashMap<String, Serializable>(13);
            auditMap.put(
                    AuditApplication.buildPath(
                            RecordsManagementAuditService.RM_AUDIT_SNIPPET_EVENT,
                            RecordsManagementAuditService.RM_AUDIT_SNIPPET_NAME),
                    eventName);
            // Action node
            auditMap.put(
                    AuditApplication.buildPath(
                            RecordsManagementAuditService.RM_AUDIT_SNIPPET_EVENT,
                            RecordsManagementAuditService.RM_AUDIT_SNIPPET_NODE),
                    nodeRef);
            auditMap = auditComponent.recordAuditValues(RecordsManagementAuditService.RM_AUDIT_PATH_ROOT, auditMap);
            if (logger.isDebugEnabled())
            {
                logger.debug("RM Audit: Audited node deletion: \n" + auditMap);
            }
        }
        else
        {
            // Create an event for auditing post-commit
            Map<NodeRef, RMAuditNode> auditedNodes = TransactionalResourceHelper.getMap(KEY_RM_AUDIT_NODE_RECORDS);
            RMAuditNode auditedNode = auditedNodes.get(nodeRef);
            if (auditedNode == null)
            {
                auditedNode = new RMAuditNode();
                auditedNodes.put(nodeRef, auditedNode);
                // Bind the listener to the txn.  We could do it anywhere in the method, this position ensures
                // that we avoid some rebinding of the listener
                AlfrescoTransactionSupport.bindListener(txnListener);
            }
            // Only update the eventName if it has not already been done
            if (auditedNode.getEventName() == null)
            {
                auditedNode.setEventName(eventName);
            }
            // Set the properties before the start if they are not already available
            if (auditedNode.getNodePropertiesBefore() == null)
            {
                auditedNode.setNodePropertiesBefore(nodePropertiesBefore);
            }
            // Set the after values if they are provided.
            // Overwrite as we assume that these represent the latest state of the node.
            if (nodePropertiesAfter != null)
            {
                auditedNode.setNodePropertiesAfter(nodePropertiesAfter);
            }
            // That is it.  The values are queued for the end of the transaction.
        }
    }

    /**
     * A <b>stateless</b> transaction listener for RM auditing.  This component picks up the data of
     * modified nodes and generates the audit information.
     * <p/>
     * This class is not static so that the instances will have access to the action's implementation.
     * 
     * @author Derek Hulley
     * @since 3.2
     */
    private class RMAuditTxnListener extends TransactionListenerAdapter
    {
        private final Log logger = LogFactory.getLog(RecordsManagementAuditServiceImpl.class);
        
        /*
         * Equality and hashcode generation are left unimplemented; we expect to only have a single
         * instance of this class per action.
         */

        /**
         * Get the action parameters from the transaction and audit them.
         */
        @Override
        public void afterCommit()
        {
            final Map<NodeRef, RMAuditNode> auditedNodes = TransactionalResourceHelper.getMap(KEY_RM_AUDIT_NODE_RECORDS);
            
            // Start a *new* read-write transaction to audit in
            RetryingTransactionCallback<Void> auditCallback = new RetryingTransactionCallback<Void>()
            {
                public Void execute() throws Throwable
                {
                    auditInTxn(auditedNodes);
                    return null;
                }
            };
            transactionService.getRetryingTransactionHelper().doInTransaction(auditCallback, false, true);
        }

        /**
         * Do the actual auditing, assuming the presence of a viable transaction
         * 
         * @param auditedNodes              details of the nodes that were modified
         */
        private void auditInTxn(Map<NodeRef, RMAuditNode> auditedNodes) throws Throwable
        {
            // Go through all the audit information and audit it
            boolean auditedSomething = false;                       // We rollback if nothing is audited
            for (Map.Entry<NodeRef, RMAuditNode> entry : auditedNodes.entrySet())
            {
                NodeRef nodeRef = entry.getKey();
                
                // If the node is gone, then do nothing
                if (!nodeService.exists(nodeRef))
                {
                    continue;
                }
                
                RMAuditNode auditedNode = entry.getValue();
                
                Map<String, Serializable> auditMap = new HashMap<String, Serializable>(13);
                // Action description
                String eventName = auditedNode.getEventName();
                auditMap.put(
                        AuditApplication.buildPath(
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_EVENT,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_NAME),
                        eventName);
                // Action node
                auditMap.put(
                        AuditApplication.buildPath(
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_EVENT,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_NODE),
                        nodeRef);
                // Property changes
                Map<QName, Serializable> propertiesBefore = auditedNode.getNodePropertiesBefore();
                Map<QName, Serializable> propertiesAfter = auditedNode.getNodePropertiesAfter();
                Pair<Map<QName, Serializable>, Map<QName, Serializable>> deltaPair =
                        PropertyMap.getBeforeAndAfterMapsForChanges(propertiesBefore, propertiesAfter);
                auditMap.put(
                        AuditApplication.buildPath(
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_EVENT,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_NODE,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_CHANGES,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_BEFORE),
                        (Serializable) deltaPair.getFirst());
                auditMap.put(
                        AuditApplication.buildPath(
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_EVENT,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_NODE,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_CHANGES,
                                RecordsManagementAuditService.RM_AUDIT_SNIPPET_AFTER),
                        (Serializable) deltaPair.getSecond());
                // Audit it
                if (logger.isDebugEnabled())
                {
                    logger.debug("RM Audit: Auditing values: \n" + auditMap);
                }
                auditMap = auditComponent.recordAuditValues(RecordsManagementAuditService.RM_AUDIT_PATH_ROOT, auditMap);
                if (auditMap.isEmpty())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("RM Audit: Nothing was audited.");
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("RM Audit: Audited values: \n" + auditMap);
                    }
                    // We must commit the transaction to get the values in
                    auditedSomething = true;
                }
            }
            // Check if anything was audited
            if (!auditedSomething)
            {
                // Nothing was audited, so do nothing
                RetryingTransactionHelper.getActiveUserTransaction().setRollbackOnly();
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public File getAuditTrailFile(RecordsManagementAuditQueryParameters params, ReportFormat format)
    {
        ParameterCheck.mandatory("params", params);
        
        Writer fileWriter = null;
        try
        {
            File auditTrailFile = TempFileProvider.createTempFile(AUDIT_TRAIL_FILE_PREFIX, 
                format == ReportFormat.HTML ? AUDIT_TRAIL_HTML_FILE_SUFFIX : AUDIT_TRAIL_JSON_FILE_SUFFIX);
            fileWriter = new BufferedWriter(new FileWriter(auditTrailFile));
            // Get the results, dumping to file
            getAuditTrailImpl(params, null, fileWriter, format);
            // Done
            return auditTrailFile;
        }
        catch (Throwable e)
        {
            throw new AlfrescoRuntimeException("Failed to generate audit trail file", e);
        }
        finally
        {
            // close the writer
            if (fileWriter != null)
            {
                try { fileWriter.close(); } catch (IOException closeEx) {}
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public List<RecordsManagementAuditEntry> getAuditTrail(RecordsManagementAuditQueryParameters params)
    {
        ParameterCheck.mandatory("params", params);
        
        List<RecordsManagementAuditEntry> entries = new ArrayList<RecordsManagementAuditEntry>(50);
        try
        {
            getAuditTrailImpl(params, entries, null, null);
            // Done
            return entries;
        }
        catch (Throwable e)
        {
            // Should be
            throw new AlfrescoRuntimeException("Failed to generate audit trail", e);
        }
    }
    
    /**
     * Get the audit trail, optionally dumping the results the the given writer dumping to a list.
     * 
     * @param params                the search parameters
     * @param results               the list to which individual results will be dumped
     * @param writer                Writer to write the audit trail
     * @param reportFormat          Format to write the audit trail in, ignored if writer is <code>null</code>
     */
    private void getAuditTrailImpl(
            RecordsManagementAuditQueryParameters params,
            final List<RecordsManagementAuditEntry> results,
            final Writer writer,
            final ReportFormat reportFormat)
            throws IOException
    {
        if (logger.isDebugEnabled())
            logger.debug("Retrieving audit trail in '" + reportFormat + "' format using parameters: " + params);
        
        // define the callback
        AuditQueryCallback callback = new AuditQueryCallback()
        {
            private boolean firstEntry = true;

            
            public boolean valuesRequired()
            {
                return true;
            }

            /**
             * Just log the error, but continue
             */
            public boolean handleAuditEntryError(Long entryId, String errorMsg, Throwable error)
            {
                logger.warn(errorMsg, error);
                return true;
            }
            
            @SuppressWarnings("unchecked")
            public boolean handleAuditEntry(
                    Long entryId,
                    String applicationName,
                    String user,
                    long time,
                    Map<String, Serializable> values)
            {
                // Check for context shutdown
                if (shutdown)
                {
                    return false;
                }
                
                Date timestamp = new Date(time);
                String eventName = null;
                String fullName = null;
                String userRoles = null;
                NodeRef nodeRef = null;
                String nodeName = null;
                String nodeType = null;
                String nodeIdentifier = null;
                String namePath = null;
                Map<QName, Serializable> beforeProperties = null;
                Map<QName, Serializable> afterProperties = null;
                
                if (values.containsKey(RecordsManagementAuditService.RM_AUDIT_DATA_EVENT_NAME))
                {
                    // This data is /DOD5015/event/...
                    eventName = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_EVENT_NAME);
                    fullName = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_PERSON_FULLNAME);
                    userRoles = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_PERSON_ROLES);
                    nodeRef = (NodeRef) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_NODE_NODEREF);
                    nodeName = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_NODE_NAME);
                    QName nodeTypeQname = (QName) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_NODE_TYPE);
                    nodeIdentifier = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_NODE_IDENTIFIER);
                    namePath = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_NODE_NAMEPATH);
                    beforeProperties = (Map<QName, Serializable>) values.get(
                            RecordsManagementAuditService.RM_AUDIT_DATA_NODE_CHANGES_BEFORE);
                    afterProperties = (Map<QName, Serializable>) values.get(
                            RecordsManagementAuditService.RM_AUDIT_DATA_NODE_CHANGES_AFTER);
                    
                    // Convert some of the values to recognizable forms
                    nodeType = null;
                    if (nodeTypeQname != null)
                    {
                        TypeDefinition typeDef = dictionaryService.getType(nodeTypeQname);
                        nodeType = (typeDef != null) ? typeDef.getTitle() : null;
                    }
                }
                else if (values.containsKey(RecordsManagementAuditService.RM_AUDIT_DATA_LOGIN_USERNAME))
                {
                    user = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_LOGIN_USERNAME);
                    if (values.containsKey(RecordsManagementAuditService.RM_AUDIT_DATA_LOGIN_ERROR))
                    {
                        eventName = RecordsManagementAuditService.RM_AUDIT_EVENT_LOGIN_FAILURE;
                        fullName = user;            // The user didn't log in
                    }
                    else
                    {
                        eventName = RecordsManagementAuditService.RM_AUDIT_EVENT_LOGIN_SUCCESS;
                        fullName = (String) values.get(RecordsManagementAuditService.RM_AUDIT_DATA_LOGIN_FULLNAME);
                    }
                }
                else
                {
                    // This is not recognisable data
                    logger.warn(
                            "Unable to process audit entry for RM.  Unexpected data: \n" +
                            "   Entry: " + entryId + "\n" +
                            "   Data:  " + values);
                    // Skip it
                    return true;
                }
                
                // TODO: Refactor this to use the builder pattern
                RecordsManagementAuditEntry entry = new RecordsManagementAuditEntry(
                        timestamp,
                        user,
                        fullName,
                        userRoles,              // A concatenated string of roles
                        nodeRef,
                        nodeName,
                        nodeType,
                        eventName,
                        nodeIdentifier,
                        namePath,
                        beforeProperties,
                        afterProperties);
                
                // write out the entry to the file in requested format
                writeEntryToFile(entry);
                
                if (results != null)
                {
                    results.add(entry);
                }
                
                if (logger.isDebugEnabled())
                {
                    logger.debug("   " + entry);
                }
                
                // Keep going
                return true;
            }
            
            private void writeEntryToFile(RecordsManagementAuditEntry entry)
            {
                if (writer == null)
                {
                    return;
                }
                try
                {
                    if (!firstEntry)
                    {
                        if (reportFormat == ReportFormat.HTML)
                        {
                            writer.write("\n");
                        }
                        else
                        {
                            writer.write(",");
                        }
                    }
                    else
                    {
                        firstEntry = false;
                    }
                    
                    // write the entry to the file
                    if (reportFormat == ReportFormat.JSON)
                    {
                        writer.write("\n\t\t");
                    }
                    
                    writeAuditTrailEntry(writer, entry, reportFormat);
                }
                catch (IOException ioe)
                {
                    throw new AlfrescoRuntimeException("Failed to generate audit trail file", ioe);
                }
            }
        };
        
        String user = params.getUser();
        Long fromTime = (params.getDateFrom() == null ? null : new Long(params.getDateFrom().getTime()));
        Long toTime = (params.getDateTo() == null ? null : new Long(params.getDateTo().getTime()));
        NodeRef nodeRef = params.getNodeRef();
        String eventName = params.getEvent();
        QName propertyQName = params.getProperty();
        int maxEntries = params.getMaxEntries();
        boolean forward = maxEntries > 0 ? false : true;        // Reverse order if the results are limited
        
        // start the audit trail report
        writeAuditTrailHeader(writer, params, reportFormat);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("RM Audit: Issuing query: " + params);
        }

        // Build audit query parameters
        AuditQueryParameters auditQueryParams = new AuditQueryParameters();
        auditQueryParams.setForward(forward);
        auditQueryParams.setApplicationName(RecordsManagementAuditService.RM_AUDIT_APPLICATION_NAME);
        auditQueryParams.setUser(user);
        auditQueryParams.setFromTime(fromTime);
        auditQueryParams.setToTime(toTime);
        if (nodeRef != null)
        {
            auditQueryParams.addSearchKey(RecordsManagementAuditService.RM_AUDIT_DATA_NODE_NODEREF, nodeRef);
        }
        // Get audit entries
        auditService.auditQuery(callback, auditQueryParams, maxEntries);
        
        // finish off the audit trail report
        writeAuditTrailFooter(writer, reportFormat);
    }
    
    /**
     * {@inheritDoc}
     */
    public NodeRef fileAuditTrailAsRecord(RecordsManagementAuditQueryParameters params, 
                NodeRef destination, ReportFormat format)
    {
        ParameterCheck.mandatory("params", params);
        ParameterCheck.mandatory("destination", destination);
        
        // NOTE: the underlying RM services will check all the remaining pre-conditions
        
        NodeRef record = null;
        
        // get the audit trail for the provided parameters
        File auditTrail = this.getAuditTrailFile(params, format);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Filing audit trail in file " + auditTrail.getAbsolutePath() + 
                        " as a record in record folder: " + destination);
        }
        
        try
        {
            Map<QName, Serializable> properties = new HashMap<QName, Serializable>(1);
            properties.put(ContentModel.PROP_NAME, auditTrail.getName());
            
            // file the audit log as an undeclared record
            record = this.nodeService.createNode(destination, 
                        ContentModel.ASSOC_CONTAINS, 
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, 
                                    QName.createValidLocalName(auditTrail.getName())), 
                        ContentModel.TYPE_CONTENT, properties).getChildRef();

            // Set the content
            ContentWriter writer = this.contentService.getWriter(record, ContentModel.PROP_CONTENT, true);
            writer.setMimetype(format == ReportFormat.HTML ? MimetypeMap.MIMETYPE_HTML : MimetypeMap.MIMETYPE_JSON);
            writer.setEncoding("UTF-8");
            writer.putContent(auditTrail);
            
            // file the node as a record
            this.rmActionService.executeRecordsManagementAction(record, FILE_ACTION);
        }
        finally
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Audit trail report saved to temporary file: " + auditTrail.getAbsolutePath());
            }
            else
            {
                auditTrail.delete();
            }
        } 
        
        return record;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<AuditEvent> getAuditEvents()
    {
        List<AuditEvent> listAuditEvents = new ArrayList<AuditEvent>(this.auditEvents.size());
        listAuditEvents.addAll(this.auditEvents.values());
        return listAuditEvents;
    }
    
    /**
     * Writes the start of the audit trail stream to the given writer
     * 
     * @param writer The writer to write to
     * @params params The parameters being used
     * @param reportFormat The format to write the header in
     * @throws IOException
     */
    private void writeAuditTrailHeader(Writer writer, 
                RecordsManagementAuditQueryParameters params, 
                ReportFormat reportFormat) throws IOException
    {
        if (writer == null)
        {
            return;
        }
        
        if (reportFormat == ReportFormat.HTML)
        {
            // write header as HTML
            writer.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
            writer.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
            writer.write("<title>Audit Report</title></head>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: arial,verdana; font-size: 81%; color: #333; }\n");
            writer.write(".label { margin-right: 5px; font-weight: bold; }\n");
            writer.write(".value { margin-right: 40px; }\n");
            writer.write(".audit-info { background-color: #efefef; padding: 10px; margin-bottom: 4px; }\n");
            writer.write(".audit-entry { border: 1px solid #bbb; margin-top: 15px; }\n");
            writer.write(".audit-entry-header { background-color: #bbb; padding: 8px; }\n");
            writer.write(".audit-entry-node { padding: 10px; }\n");
            writer.write(".changed-values-table { margin-left: 6px; margin-bottom: 2px;width: 99%; }\n");
            writer.write(".changed-values-table th { text-align: left; background-color: #eee; padding: 4px; }\n");
            writer.write(".changed-values-table td { width: 33%; padding: 4px; border-top: 1px solid #eee; }\n");
            writer.write("</style>\n");
            writer.write("<body>\n<h2>Audit Report</h2>\n");
            writer.write("<div class=\"audit-info\">\n");
            
            writer.write("<span class=\"label\">From:</span>");
            writer.write("<span class=\"value\">");
            Date from = params.getDateFrom();
            writer.write(from == null ? "&lt;Not Set&gt;" : StringEscapeUtils.escapeHtml(from.toString()));
            writer.write("</span>");
            
            writer.write("<span class=\"label\">To:</span>");
            writer.write("<span class=\"value\">");
            Date to = params.getDateTo();
            writer.write(to == null ? "&lt;Not Set&gt;" : StringEscapeUtils.escapeHtml(to.toString()));
            writer.write("</span>");
            
            writer.write("<span class=\"label\">Property:</span>");
            writer.write("<span class=\"value\">");
            QName prop = params.getProperty();
            writer.write(prop == null ? "All" : StringEscapeUtils.escapeHtml(getPropertyLabel(prop)));
            writer.write("</span>");
            
            writer.write("<span class=\"label\">User:</span>");
            writer.write("<span class=\"value\">");
            writer.write(params.getUser() == null ? "All" : StringEscapeUtils.escapeHtml(params.getUser()));
            writer.write("</span>");
            
            writer.write("<span class=\"label\">Event:</span>");
            writer.write("<span class=\"value\">");
            writer.write(params.getEvent() == null ? "All" : StringEscapeUtils.escapeHtml(getAuditEventLabel(params.getEvent())));
            writer.write("</span>\n");
            
            writer.write("</div>\n");
        }
        else
        {
            // write header as JSON
            writer.write("{\n\t\"data\":\n\t{");
            writer.write("\n\t\t\"started\": \"");
            writer.write(ISO8601DateFormat.format(getDateLastStarted()));
            writer.write("\",\n\t\t\"stopped\": \"");
            writer.write(ISO8601DateFormat.format(getDateLastStopped()));
            writer.write("\",\n\t\t\"enabled\": ");
            writer.write(Boolean.toString(isEnabled()));
            writer.write(",\n\t\t\"entries\":[");
        }
    }
    
    /**
     * Writes an audit trail entry to the given writer
     * 
     * @param writer The writer to write to
     * @param entry The entry to write
     * @param reportFormat The format to write the header in
     * @throws IOException
     */
    private void writeAuditTrailEntry(Writer writer, RecordsManagementAuditEntry entry,
                ReportFormat reportFormat) throws IOException
    {
        if (writer == null)
        {
            return;
        }
        
        if (reportFormat == ReportFormat.HTML)
        {
            writer.write("<div class=\"audit-entry\">\n");
            writer.write("<div class=\"audit-entry-header\">");
            writer.write("<span class=\"label\">Timestamp:</span>");
            writer.write("<span class=\"value\">");
            writer.write(StringEscapeUtils.escapeHtml(entry.getTimestamp().toString()));
            writer.write("</span>");
            writer.write("<span class=\"label\">User:</span>");
            writer.write("<span class=\"value\">");
            writer.write(entry.getFullName() != null ? 
                            StringEscapeUtils.escapeHtml(entry.getFullName()) : 
                            StringEscapeUtils.escapeHtml(entry.getUserName()));
            writer.write("</span>");
            if (entry.getUserRole() != null && entry.getUserRole().length() > 0)
            {
                writer.write("<span class=\"label\">Role:</span>");
                writer.write("<span class=\"value\">");
                writer.write(StringEscapeUtils.escapeHtml(entry.getUserRole()));
                writer.write("</span>");
            }
            if (entry.getEvent() != null && entry.getEvent().length() > 0)
            {
                writer.write("<span class=\"label\">Event:</span>");
                writer.write("<span class=\"value\">");
                writer.write(StringEscapeUtils.escapeHtml(getAuditEventLabel(entry.getEvent())));
                writer.write("</span>\n");
            }
            writer.write("</div>\n");
            writer.write("<div class=\"audit-entry-node\">");
            if (entry.getIdentifier() != null && entry.getIdentifier().length() > 0)
            {
                writer.write("<span class=\"label\">Identifier:</span>");
                writer.write("<span class=\"value\">");
                writer.write(StringEscapeUtils.escapeHtml(entry.getIdentifier()));
                writer.write("</span>");
            }
            if (entry.getNodeType() != null && entry.getNodeType().length() > 0)
            {
                writer.write("<span class=\"label\">Type:</span>");
                writer.write("<span class=\"value\">");
                writer.write(StringEscapeUtils.escapeHtml(entry.getNodeType()));
                writer.write("</span>");
            }
            if (entry.getPath() != null && entry.getPath().length() > 0)
            {
                // we need to strip off the first part of the path
                String path = entry.getPath();
                String displayPath = path;
                int idx = path.indexOf("/", 1);
                if (idx != -1)
                {
                    displayPath = "/File Plan" + path.substring(idx);
                }
                
                writer.write("<span class=\"label\">Location:</span>");
                writer.write("<span class=\"value\">");
                writer.write(StringEscapeUtils.escapeHtml(displayPath));
                writer.write("</span>");
            }
            writer.write("</div>\n");
            
            if (entry.getChangedProperties() != null)
            {
                writer.write("<table class=\"changed-values-table\" cellspacing=\"0\">");
                writer.write("<tr><th>Property</th><th>Previous Value</th><th>New Value</th></tr>");
                
                // create an entry for each property that changed
                for (QName valueName : entry.getChangedProperties().keySet())
                {
                    Pair<Serializable, Serializable> values = entry.getChangedProperties().get(valueName);
                    writer.write("<tr><td>");
                    writer.write(getPropertyLabel(valueName));
                    writer.write("</td><td>");
                    Serializable oldValue = values.getFirst(); 
                    writer.write(oldValue == null ? "&lt;none&gt;" : StringEscapeUtils.escapeHtml(oldValue.toString()));
                    writer.write("</td><td>");
                    Serializable newValue = values.getSecond();
                    writer.write(newValue == null ? "&lt;none&gt;" : StringEscapeUtils.escapeHtml(newValue.toString()));
                    writer.write("</td></tr>");
                }
                
                writer.write("</table>\n");
            }
            
            writer.write("</div>");
        }
        else
        {
            try
            {
                JSONObject json = new JSONObject();
                
                json.put("timestamp", entry.getTimestampString());
                json.put("userName", entry.getUserName());
                json.put("userRole", entry.getUserRole() == null ? "": entry.getUserRole());
                json.put("fullName", entry.getFullName() == null ? "": entry.getFullName());
                json.put("nodeRef", entry.getNodeRef() == null ? "": entry.getNodeRef());
                json.put("nodeName", entry.getNodeName() == null ? "": entry.getNodeName());
                json.put("nodeType", entry.getNodeType() == null ? "": entry.getNodeType());
                json.put("event", entry.getEvent() == null ? "": getAuditEventLabel(entry.getEvent()));
                json.put("identifier", entry.getIdentifier() == null ? "": entry.getIdentifier());
                json.put("path", entry.getPath() == null ? "": entry.getPath());
            
                JSONArray changedValues = new JSONArray();
                
                if (entry.getChangedProperties() != null)
                {
                    // create an entry for each property that changed
                    for (QName valueName : entry.getChangedProperties().keySet())
                    {
                        Pair<Serializable, Serializable> values = entry.getChangedProperties().get(valueName);
                        
                        JSONObject changedValue = new JSONObject();
                        changedValue.put("name", getPropertyLabel(valueName));
                        changedValue.put("previous", values.getFirst() == null ? "" : values.getFirst().toString());
                        changedValue.put("new", values.getSecond() == null ? "" : values.getSecond().toString());
                        
                        changedValues.put(changedValue);
                    }
                }
                
                json.put("changedValues", changedValues);
                
                writer.write(json.toString());
            }
            catch (JSONException je)
            {
                writer.write("{}");
            }
        }
    }
    
    /**
     * Writes the end of the audit trail stream to the given writer
     * 
     * @param writer The writer to write to
     * @param reportFormat The format to write the footer in
     * @throws IOException
     */
    private void writeAuditTrailFooter(Writer writer, ReportFormat reportFormat) throws IOException
    {
        if (writer == null)
        {
            return;
        }
        
        if (reportFormat == ReportFormat.HTML)
        {
            // write footer as HTML
            writer.write("\n</body></html>");
        }
        else
        {
            // write footer as JSON
            writer.write("\n\t\t]\n\t}\n}");
        }
    }
    
    /**
     * Returns the display label for a property QName
     * 
     * @param property The property to get label for
     * @param ddService DictionaryService instance
     * @param namespaceService NamespaceService instance
     * @return The label
     */
    private String getPropertyLabel(QName property)
    {
        String label = null;
        
        PropertyDefinition propDef = this.dictionaryService.getProperty(property);
        if (propDef != null)
        {
            label = propDef.getTitle();
        }
        
        if (label == null)
        {
            label = property.getLocalName();
        }
        
        return label;
    }
    
    /**
     * Returns the display label for the given audit event key
     * 
     * @param eventKey The audit event key
     * @return The display label or null if the key does not exist
     */
    private String getAuditEventLabel(String eventKey)
    {
        String label = eventKey;
        
        AuditEvent event = this.auditEvents.get(eventKey);
        if (event != null)
        {
            label = event.getLabel();
        }
        
        return label;
    }
}