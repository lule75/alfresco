/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.module.org_alfresco_module_dod5015.test.jscript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.test.TestUtilities;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseAlfrescoSpringTest;

/**
 * @author Roy Wetherall
 */
public class RMJScriptTest extends BaseAlfrescoSpringTest implements RecordsManagementModel
{
    private static String SCRIPT_PATH = "org/alfresco/module/org_alfresco_module_dod5015/test/jscript/";
    private static String CAPABILITIES_TEST = "CapabilitiesTest.js";
    
    protected static StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    private ScriptService scriptService;
    private PermissionService permissionService;
    private ImporterService importService;    
    private SearchService searchService;
    private RecordsManagementActionService rmActionService;
    
    private NodeRef filePlan;
    
    // example base test data for supplemental markings list (see also recordsModel.xml)
    protected final static String NOFORN     = "NOFORN";     // Not Releasable to Foreign Nationals/Governments/Non-US Citizens
    protected final static String NOCONTRACT = "NOCONTRACT"; // Not Releasable to Contractors or Contractor/Consultants
    protected final static String FOUO       = "FOUO";       // For Official Use Only 
    protected final static String FGI        = "FGI";        // Foreign Government Information
    
    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        this.scriptService = (ScriptService)this.applicationContext.getBean("ScriptService");
        this.permissionService = (PermissionService)this.applicationContext.getBean("PermissionService");
        this.importService = (ImporterService)this.applicationContext.getBean("importerComponent");
        this.searchService = (SearchService)this.applicationContext.getBean("SearchService");
        this.rmActionService = (RecordsManagementActionService)this.applicationContext.getBean("RecordsManagementActionService");
        
        // Set the current security context as admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        // Get the test data
        setUpTestData();
    }
    
    private void setUpTestData()
    {
        // Don't reload the fileplan data on each test method.
        if (filePlan == null)
        {
            filePlan = getRoot();
            if (filePlan == null)
            {
                filePlan = TestUtilities.loadFilePlanData(null, this.nodeService, this.importService, this.permissionService);
            }
        }
    }
    
    private NodeRef getRoot()
    {
        NodeRef result = null;
        String query = "ASPECT:\"" + ASPECT_RECORDS_MANAGEMENT_ROOT + "\"";
        ResultSet resultSet = this.searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, query);
        if (resultSet.getNodeRefs().size() != 0)
        {
            result = resultSet.getNodeRef(0);
        }
        return result;
    }
    
    public void testCapabilities() throws Exception
    {
        setComplete();
        endTransaction();
        
        UserTransaction txn = transactionService.getUserTransaction(false);
        txn.begin();
        
        NodeRef recordCategory = TestUtilities.getRecordCategory(this.searchService, "Reports", "AIS Audit Records");    
        assertNotNull(recordCategory);
        assertEquals("AIS Audit Records", this.nodeService.getProperty(recordCategory, ContentModel.PROP_NAME));
                
        NodeRef recordFolder = createRecordFolder(recordCategory, "March AIS Audit Records");
                
        txn.commit();
        txn = transactionService.getUserTransaction(false);
        txn.begin();
        
        NodeRef record = createRecord(recordFolder, "myRecord.txt");
        
        txn.commit();
        txn = transactionService.getUserTransaction(false);
        txn.begin();
        
        declareRecord(record);
        
        // Create a model to pass to the unit test scripts
        Map<String, Object> model = new HashMap<String, Object>(1);
        model.put("filePlan", filePlan);
        model.put("record", record);
        
        executeScript(CAPABILITIES_TEST, model);
        
        txn.commit();
    }
    
    private void executeScript(String script, Map<String, Object> model)
    {
        // Execute the unit test script
        ScriptLocation location = new ClasspathScriptLocation(SCRIPT_PATH + script);
        this.scriptService.executeScript(location, model);
    }
    
    private NodeRef createRecordFolder(NodeRef recordCategory, String folderName)
    {
        Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>(1);
        folderProps.put(ContentModel.PROP_NAME, folderName);
        NodeRef recordFolder = this.nodeService.createNode(recordCategory, 
                                                           ContentModel.ASSOC_CONTAINS, 
                                                           QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, folderName), 
                                                           TYPE_RECORD_FOLDER).getChildRef();
        return recordFolder;
    }
    
    private NodeRef createRecord(NodeRef recordFolder, String name)
    {
        // Create the document
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        props.put(ContentModel.PROP_NAME, name);
        NodeRef recordOne = this.nodeService.createNode(recordFolder, 
                                                        ContentModel.ASSOC_CONTAINS, 
                                                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name), 
                                                        ContentModel.TYPE_CONTENT).getChildRef();
        
        // Set the content
        ContentWriter writer = this.contentService.getWriter(recordOne, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent("There is some content in this record");
        
        return recordOne;
    } 
    
    private void declareRecord(NodeRef recordOne)
    {
        NodeService unprotectedNodeService = (NodeService)applicationContext.getBean("nodeService");
        // Declare record
        Map<QName, Serializable> propValues = this.nodeService.getProperties(recordOne);        
        propValues.put(RecordsManagementModel.PROP_PUBLICATION_DATE, new Date());       
        List<String> smList = new ArrayList<String>(2);
        smList.add(FOUO);
        smList.add(NOFORN);
        propValues.put(RecordsManagementModel.PROP_SUPPLEMENTAL_MARKING_LIST, (Serializable)smList);        
        propValues.put(RecordsManagementModel.PROP_MEDIA_TYPE, "mediaTypeValue"); 
        propValues.put(RecordsManagementModel.PROP_FORMAT, "formatValue"); 
        propValues.put(RecordsManagementModel.PROP_DATE_RECEIVED, new Date());       
        propValues.put(RecordsManagementModel.PROP_ORIGINATOR, "origValue");
        propValues.put(RecordsManagementModel.PROP_ORIGINATING_ORGANIZATION, "origOrgValue");
        propValues.put(ContentModel.PROP_TITLE, "titleValue");
        unprotectedNodeService.setProperties(recordOne, propValues);
        this.rmActionService.executeRecordsManagementAction(recordOne, "declareRecord");        
    }

}