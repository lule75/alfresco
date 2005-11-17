/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.model.filefolder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * @see org.alfresco.repo.model.filefolder.FileFolderServiceImpl
 * 
 * @author Derek Hulley
 */
public class FileFolderServiceImplTest extends TestCase
{
    private static final String IMPORT_VIEW = "filefolder/filefolder-test-import.xml";
    
    private static final ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private TransactionService transactionService;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private UserTransaction txn;
    private NodeRef rootNodeRef;
    
    @Override
    public void setUp() throws Exception
    {
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean("ServiceRegistry");
        transactionService = serviceRegistry.getTransactionService();
        nodeService = serviceRegistry.getNodeService();
        fileFolderService = serviceRegistry.getFileFolderService();
        AuthenticationComponent authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");

        // start the transaction
        txn = transactionService.getUserTransaction();
        txn.begin();
        
        // authenticate
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());
        
        // create a test store
        StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, getName() + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);
        
        // import the test data
        ImporterService importerService = serviceRegistry.getImporterService();
        Location importLocation = new Location(rootNodeRef);
        InputStream is = getClass().getClassLoader().getResourceAsStream(IMPORT_VIEW);
        if (is == null)
        {
            throw new NullPointerException("Test resource not found: " + IMPORT_VIEW);
        }
        Reader reader = new InputStreamReader(is);
        importerService.importView(reader, importLocation, null, null);
    }
    
    public void tearDown() throws Exception
    {
        if (txn.getStatus() == Status.STATUS_ACTIVE)
        {
            txn.rollback();
        }
    }
    
    public void testShallowFilesOnlyList() throws Exception
    {
        List<FileInfo> files = fileFolderService.listFiles(rootNodeRef);
        assertEquals("Expected exactly 2 files", 2, files.size());
    }
}
