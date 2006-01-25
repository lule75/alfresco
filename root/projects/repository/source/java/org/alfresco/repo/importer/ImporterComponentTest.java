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
package org.alfresco.repo.importer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.view.ImporterProgress;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.debug.NodeStoreInspector;


public class ImporterComponentTest extends BaseSpringTest
{
    private ImporterService importerService;
    private ImporterBootstrap importerBootstrap;
    private NodeService nodeService;
    private StoreRef storeRef;
    private AuthenticationComponent authenticationComponent;

    
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        nodeService = (NodeService)applicationContext.getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
        importerService = (ImporterService)applicationContext.getBean(ServiceRegistry.IMPORTER_SERVICE.getLocalName());
        
        importerBootstrap = (ImporterBootstrap)applicationContext.getBean("importerBootstrap");
        
        this.authenticationComponent = (AuthenticationComponent)this.applicationContext.getBean("authenticationComponent");
        
        this.authenticationComponent.setSystemUserAsCurrentUser();
        
        // Create the store
        this.storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
    }
    
    @Override
    protected void onTearDownInTransaction() throws Exception
    {
        authenticationComponent.clearCurrentSecurityContext();
        super.onTearDownInTransaction();
    }
    
    public void testImport()
        throws Exception
    {
        InputStream test = getClass().getClassLoader().getResourceAsStream("org/alfresco/repo/importer/importercomponent_test.xml");
        InputStreamReader testReader = new InputStreamReader(test, "UTF-8");
        Location location = new Location(storeRef);
        importerService.importView(testReader, location, null, new ImportTimerProgress());
        System.out.println(NodeStoreInspector.dumpNodeStore(nodeService, storeRef));
    }
    
    /*
     * Temporary build fix
     */
    private static class ImportTimerProgress implements ImporterProgress
    {

        public void aspectAdded(NodeRef nodeRef, QName aspect)
        {
            throw new UnsupportedOperationException();
        }

        public void completed()
        {
            throw new UnsupportedOperationException();
        }

        public void contentCreated(NodeRef nodeRef, String sourceUrl)
        {
            throw new UnsupportedOperationException();
        }

        public void error(Throwable e)
        {
            throw new UnsupportedOperationException();
        }

        public void nodeCreated(NodeRef nodeRef, NodeRef parentRef, QName assocName, QName childName)
        {
            throw new UnsupportedOperationException();
        }

        public void nodeLinked(NodeRef nodeRef, NodeRef parentRef, QName assocName, QName childName)
        {
            throw new UnsupportedOperationException();
        }

        public void permissionSet(NodeRef nodeRef, AccessPermission permission)
        {
            throw new UnsupportedOperationException();
        }

        public void propertySet(NodeRef nodeRef, QName property, Serializable value)
        {
            throw new UnsupportedOperationException();
        }

        public void started()
        {
            throw new UnsupportedOperationException();
        }
    }
    
    
    public void testBootstrap()
    {
        StoreRef bootstrapStoreRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        importerBootstrap.setStoreUrl(bootstrapStoreRef.toString());
        importerBootstrap.bootstrap();
        authenticationComponent.setSystemUserAsCurrentUser();
        System.out.println(NodeStoreInspector.dumpNodeStore(nodeService, bootstrapStoreRef));
    }
}

