/*
 * Copyright (C) 2007 Alfresco, Inc.
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
package org.alfresco.repo.admin.registry;

import java.io.Serializable;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of registry service to provide generic storage
 * and retrieval of system-related metadata.
 * 
 * @author Derek Hulley
 */
public class RegistryServiceImpl implements RegistryService
{
    private static Log logger = LogFactory.getLog(RegistryServiceImpl.class);
    
    private AuthenticationComponent authenticationComponent;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private SearchService searchService;
    private StoreRef registryStoreRef;
    private String registryRootPath;
    
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * @param registryStoreRef the store in which the registry root is found
     */
    public void setRegistryStoreRef(StoreRef registryStoreRef)
    {
        this.registryStoreRef = registryStoreRef;
    }
    
    /**
     * @see #setRegistryStoreRef(StoreRef)
     */
    public void setRegistryStore(String registryStore)
    {
        this.setRegistryStoreRef(new StoreRef(registryStore));
    }

    /**
     * A root path e.g. <b>/sys:systemRegistry</b>
     * 
     * @param registryRootPath the path to the root of the registry
     */
    public void setRegistryRootPath(String registryRootPath)
    {
        this.registryRootPath = registryRootPath;
    }

    public void init()
    {
        // Check the properties
        PropertyCheck.mandatory(this, "authenticationComponent", authenticationComponent);
        PropertyCheck.mandatory(this, "namespaceService", namespaceService);
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "registryRootPath", searchService);
        PropertyCheck.mandatory(this, "registryStore", registryStoreRef);
        PropertyCheck.mandatory(this, "registryRootPath", registryRootPath);
    }
    
    private NodeRef getRegistryRootNodeRef()
    {
        NodeRef registryRootNodeRef = null;
        // Ensure that the registry root node is present
        ResultSet rs = searchService.query(registryStoreRef, SearchService.LANGUAGE_XPATH, registryRootPath);
        if (rs.length() == 0)
        {
            throw new AlfrescoRuntimeException(
                    "Registry root not present: \n" +
                    "   Store: " + registryStoreRef + "\n" +
                    "   Path:  " + registryRootPath);
        }
        else if (rs.length() > 1)
        {
            throw new AlfrescoRuntimeException(
                    "Registry root path has multiple targets: \n" +
                    "   Store: " + registryStoreRef + "\n" +
                    "   Path:  " + registryRootPath);
        }
        else
        {
            registryRootNodeRef = rs.getNodeRef(0);
        }
        // Check the root
        QName typeQName = nodeService.getType(registryRootNodeRef);
        if (!typeQName.equals(ContentModel.TYPE_CONTAINER))
        {
            throw new AlfrescoRuntimeException(
                    "Registry root is not of type " + ContentModel.TYPE_CONTAINER + ": \n" +
                    "   Node: " + registryRootNodeRef + "\n" +
                    "   Type: " + typeQName);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Found root for registry: \n" +
                    "   Store: " + registryStoreRef + "\n" +
                    "   Path : " + registryRootPath + "\n" +
                    "   Root:  " + registryRootNodeRef);
        }
        return registryRootNodeRef;
    }
    
    /**
     * @return Returns a pair representing the node path and the property name
     */
    private Pair<String, String> splitKey(String key)
    {
        int index = key.lastIndexOf('/');
        Pair<String, String> result = null;
        if (index < 0)                      // It is just a property
        {
            result = new Pair<String, String>("/", key);
        }
        else
        {
            String propertyName = key.substring(index + 1, key.length());
            if (propertyName.length() == 0)
            {
                throw new IllegalArgumentException("The registry key is invalid: " + key);
            }
            result = new Pair<String, String>(key.substring(0, index), propertyName);
        }
        // done
        return result;
    }
    
    /**
     * @return Returns the node and property name represented by the key or <tt>null</tt>
     *      if it doesn't exist and was not allowed to be created
     */
    private Pair<NodeRef, QName> getPath(String key, boolean create)
    {
        // Get the root
        NodeRef currentNodeRef = getRegistryRootNodeRef();
        // Split the key
        Pair<String, String> keyPair = splitKey(key);
        // Parse the key
        StringTokenizer tokenizer = new StringTokenizer(keyPair.getFirst(), "/");
        // Find the node and property to put the value
        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();
            String name = QName.createValidLocalName(token);
            QName qname = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, name);
            
            // Find the node
            List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(
                    currentNodeRef,
                    ContentModel.ASSOC_CHILDREN,
                    qname);
            int size = childAssocRefs.size();
            if (size == 0)                          // Found nothing with that path
            {
                if (create)                         // Must create the path
                {
                    // Create the node
                    currentNodeRef = nodeService.createNode(
                            currentNodeRef,
                            ContentModel.ASSOC_CHILDREN,
                            qname,
                            ContentModel.TYPE_CONTAINER).getChildRef();
                }
                else
                {
                    // There is no node and we are not allowed to create it
                    currentNodeRef = null;
                    break;
                }
            }
            else                                    // Found some results for that path
            {
                if (size > 1 && create)             // More than one association by that name
                {
                    // Too many, so trim it down
                    boolean first = true;
                    for (ChildAssociationRef assocRef : childAssocRefs)
                    {
                        if (first)
                        {
                            first = false;
                            continue;
                        }
                        // Remove excess assocs
                        nodeService.removeChildAssociation(assocRef);
                    }
                }
                // Use the first one
                currentNodeRef = childAssocRefs.get(0).getChildRef();
            }
        }
        // Create the result
        QName propertyQName = QName.createQName(
                NamespaceService.SYSTEM_MODEL_1_0_URI,
                QName.createValidLocalName(keyPair.getSecond()));
        Pair<NodeRef, QName> resultPair = new Pair<NodeRef, QName>(currentNodeRef, propertyQName);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Converted registry key: \n" +
                    "   key pair: " + keyPair + "\n" +
                    "   result:   " + resultPair);
        }
        if (resultPair.getFirst() == null)
        {
            return null;
        }
        else
        {
            return resultPair;
        }
    }

    /**
     * @inheritDoc
     */
    public void addValue(String key, Serializable value)
    {
        // Get the path, with creation support
        Pair<NodeRef, QName> keyPair = getPath(key, true);
        // We know that the node exists, so just set the value
        nodeService.setProperty(keyPair.getFirst(), keyPair.getSecond(), value);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Added value to registry: \n" +
                    "   Key:   " + key + "\n" +
                    "   Value: " + value);
        }
    }

    public Serializable getValue(String key)
    {
        // Get the path, without creating
        Pair<NodeRef, QName> keyPair = getPath(key, false);
        Serializable value = null;
        if (keyPair != null)
        {
            value = nodeService.getProperty(keyPair.getFirst(), keyPair.getSecond());
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Retrieved value from registry: \n" +
                    "   Key:   " + key + "\n" +
                    "   Value: " + value);
        }
        return value;
    }
}
