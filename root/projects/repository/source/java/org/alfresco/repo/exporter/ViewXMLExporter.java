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
package org.alfresco.repo.exporter;

import java.io.InputStream;
import java.util.Collection;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.view.Exporter;
import org.alfresco.service.cmr.view.ExporterContext;
import org.alfresco.service.cmr.view.ExporterException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * Exporter that exports Repository information to XML (Alfresco Repository View Schema)
 * 
 * @author David Caruana
 */
/*package*/ class ViewXMLExporter
    implements Exporter
{
    // Repository View Schema Definitions
    private static final String VIEW_LOCALNAME = "view";
    private static final String VALUES_LOCALNAME = "values";
    private static final String VALUE_LOCALNAME = "value";
    private static final String CHILDNAME_LOCALNAME = "childName";
    private static final String ASPECTS_LOCALNAME = "aspects";
    private static final String PROPERTIES_LOCALNAME = "properties";
    private static final String ASSOCIATIONS_LOCALNAME = "associations";
    private static final String DATATYPE_LOCALNAME = "datatype";
    private static final String ISNULL_LOCALNAME = "isNull";
    private static final String METADATA_LOCALNAME  = "metadata";
    private static final String EXPORTEDBY_LOCALNAME  = "exportBy";
    private static final String EXPORTEDDATE_LOCALNAME  = "exportDate";
    private static final String EXPORTERVERSION_LOCALNAME  = "exporterVersion";
    private static final String EXPORTOF_LOCALNAME  = "exportOf";
    private static final String ACL_LOCALNAME  = "acl";
    private static final String ACE_LOCALNAME  = "ace";
    private static final String ACCESS_LOCALNAME  = "access";
    private static final String AUTHORITY_LOCALNAME  = "authority";
    private static final String PERMISSION_LOCALNAME  = "permission";
    private static final String INHERITPERMISSIONS_LOCALNAME  = "inherit";
    private static QName VIEW_QNAME;
    private static QName VALUES_QNAME;
    private static QName VALUE_QNAME;
    private static QName PROPERTIES_QNAME;
    private static QName ASPECTS_QNAME;
    private static QName ASSOCIATIONS_QNAME; 
    private static QName CHILDNAME_QNAME;
    private static QName DATATYPE_QNAME;
    private static QName ISNULL_QNAME;
    private static QName METADATA_QNAME;
    private static QName EXPORTEDBY_QNAME;
    private static QName EXPORTEDDATE_QNAME;
    private static QName EXPORTERVERSION_QNAME;
    private static QName EXPORTOF_QNAME;
    private static QName ACL_QNAME;
    private static QName ACE_QNAME;
    private static QName ACCESS_QNAME;
    private static QName AUTHORITY_QNAME;
    private static QName PERMISSION_QNAME;
    private static QName INHERITPERMISSIONS_QNAME;
    private static final AttributesImpl EMPTY_ATTRIBUTES = new AttributesImpl();
    
    // Service dependencies
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private PermissionService permissionService;
    
    // View context
    private ContentHandler contentHandler;
    private Path exportNodePath;
    

    /**
     * Construct
     * 
     * @param namespaceService  namespace service
     * @param nodeService  node service
     * @param contentHandler  content handler
     */
    ViewXMLExporter(NamespaceService namespaceService, NodeService nodeService,
            DictionaryService dictionaryService, PermissionService permissionService, ContentHandler contentHandler)
    {
        this.namespaceService = namespaceService;
        this.nodeService = nodeService;
        this.dictionaryService = dictionaryService;
        this.permissionService = permissionService;
        this.contentHandler = contentHandler;
        
        VIEW_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, VIEW_LOCALNAME, namespaceService);
        VALUE_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, namespaceService);
        VALUES_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUES_LOCALNAME, namespaceService);
        CHILDNAME_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, CHILDNAME_LOCALNAME, namespaceService);
        ASPECTS_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ASPECTS_LOCALNAME, namespaceService);
        PROPERTIES_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, PROPERTIES_LOCALNAME, namespaceService);
        ASSOCIATIONS_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ASSOCIATIONS_LOCALNAME, namespaceService);
        DATATYPE_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, DATATYPE_LOCALNAME, namespaceService);
        ISNULL_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ISNULL_LOCALNAME, namespaceService);
        METADATA_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, METADATA_LOCALNAME, namespaceService);
        EXPORTEDBY_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTEDBY_LOCALNAME, namespaceService);
        EXPORTEDDATE_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTEDDATE_LOCALNAME, namespaceService);
        EXPORTERVERSION_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTERVERSION_LOCALNAME, namespaceService);
        EXPORTOF_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTOF_LOCALNAME, namespaceService);
        ACL_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ACL_LOCALNAME, namespaceService);
        ACE_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ACE_LOCALNAME, namespaceService);
        ACCESS_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ACCESS_LOCALNAME, namespaceService);
        AUTHORITY_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, AUTHORITY_LOCALNAME, namespaceService);
        PERMISSION_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, PERMISSION_LOCALNAME, namespaceService);
        INHERITPERMISSIONS_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, INHERITPERMISSIONS_LOCALNAME, namespaceService);
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#start()
     */
    public void start(ExporterContext context)
    {
        try
        {
            exportNodePath = nodeService.getPath(context.getExportOf());
            contentHandler.startDocument();
            contentHandler.startPrefixMapping(NamespaceService.REPOSITORY_VIEW_PREFIX, NamespaceService.REPOSITORY_VIEW_1_0_URI);
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VIEW_LOCALNAME, VIEW_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);

            //
            // output metadata
            //
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, METADATA_LOCALNAME, METADATA_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);

            // exported by
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTEDBY_LOCALNAME, EXPORTEDBY_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);
            contentHandler.characters(context.getExportedBy().toCharArray(), 0, context.getExportedBy().length());
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTEDBY_LOCALNAME, EXPORTEDBY_QNAME.toPrefixString());

            // exported date
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTEDDATE_LOCALNAME, EXPORTEDDATE_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);
            String date = DefaultTypeConverter.INSTANCE.convert(String.class, context.getExportedDate());
            contentHandler.characters(date.toCharArray(), 0, date.length());
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTEDDATE_LOCALNAME, EXPORTEDDATE_QNAME.toPrefixString());
            
            // exporter version
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTERVERSION_LOCALNAME, EXPORTERVERSION_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);
            contentHandler.characters(context.getExporterVersion().toCharArray(), 0, context.getExporterVersion().length());
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTERVERSION_LOCALNAME, EXPORTERVERSION_QNAME.toPrefixString());

            // export of
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTOF_LOCALNAME, EXPORTOF_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);
            String path = exportNodePath.toPrefixString(namespaceService);
            contentHandler.characters(path.toCharArray(), 0, path.length());
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, EXPORTOF_LOCALNAME, EXPORTOF_QNAME.toPrefixString());
            
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, METADATA_LOCALNAME, METADATA_QNAME.toPrefixString());
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process export start event", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startNamespace(java.lang.String, java.lang.String)
     */
    public void startNamespace(String prefix, String uri)
    {
        try
        {
            contentHandler.startPrefixMapping(prefix, uri);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start namespace event - prefix " + prefix + " uri " + uri, e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endNamespace(java.lang.String)
     */
    public void endNamespace(String prefix)
    {
        try
        {
            contentHandler.endPrefixMapping(prefix);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end namespace event - prefix " + prefix, e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startNode(NodeRef nodeRef)
    {
        try
        {
            AttributesImpl attrs = new AttributesImpl(); 

            Path path = nodeService.getPath(nodeRef);
            if (path.size() > 1)
            {
                // a child name does not exist for root
                Path.ChildAssocElement pathElement = (Path.ChildAssocElement)path.last();
                QName childQName = pathElement.getRef().getQName();
                attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_1_0_URI, CHILDNAME_LOCALNAME, CHILDNAME_QNAME.toPrefixString(), null, toPrefixString(childQName));
            }
            
            QName type = nodeService.getType(nodeRef);
            contentHandler.startElement(type.getNamespaceURI(), type.getLocalName(), toPrefixString(type), attrs);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start node event - node ref " + nodeRef.toString(), e);
        }
    }

    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endNode(NodeRef nodeRef)
    {
        try
        {
            QName type = nodeService.getType(nodeRef);
            contentHandler.endElement(type.getNamespaceURI(), type.getLocalName(), toPrefixString(type));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end node event - node ref " + nodeRef.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAspects(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startAspects(NodeRef nodeRef)
    {
        try
        {
            contentHandler.startElement(ASPECTS_QNAME.getNamespaceURI(), ASPECTS_LOCALNAME, toPrefixString(ASPECTS_QNAME), EMPTY_ATTRIBUTES);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start aspects", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAspects(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endAspects(NodeRef nodeRef)
    {
        try
        {
            contentHandler.endElement(ASPECTS_QNAME.getNamespaceURI(), ASPECTS_LOCALNAME, toPrefixString(ASPECTS_QNAME));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end aspects", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAspect(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void startAspect(NodeRef nodeRef, QName aspect)
    {
        try
        {
            contentHandler.startElement(aspect.getNamespaceURI(), aspect.getLocalName(), toPrefixString(aspect), EMPTY_ATTRIBUTES);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start aspect event - node ref " + nodeRef.toString() + "; aspect " + toPrefixString(aspect), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAspect(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void endAspect(NodeRef nodeRef, QName aspect)
    {
        try
        {
            contentHandler.endElement(aspect.getNamespaceURI(), aspect.getLocalName(), toPrefixString(aspect));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end aspect event - node ref " + nodeRef.toString() + "; aspect " + toPrefixString(aspect), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startACL(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startACL(NodeRef nodeRef)
    {
        try
        {
            AttributesImpl attrs = new AttributesImpl(); 
            boolean inherit = permissionService.getInheritParentPermissions(nodeRef);
            if (!inherit)
            {
                attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_1_0_URI, INHERITPERMISSIONS_LOCALNAME, INHERITPERMISSIONS_QNAME.toPrefixString(), null, "false");
            }
            contentHandler.startElement(ACL_QNAME.getNamespaceURI(), ACL_QNAME.getLocalName(), toPrefixString(ACL_QNAME), attrs);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start ACL event - node ref " + nodeRef.toString());
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#permission(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.security.AccessPermission)
     */
    public void permission(NodeRef nodeRef, AccessPermission permission)
    {
        try
        {
            // output access control entry
            AttributesImpl attrs = new AttributesImpl(); 
            attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_1_0_URI, ACCESS_LOCALNAME, ACCESS_QNAME.toPrefixString(), null, permission.getAccessStatus().toString());
            contentHandler.startElement(ACE_QNAME.getNamespaceURI(), ACE_QNAME.getLocalName(), toPrefixString(ACE_QNAME), attrs);

            // output authority
            contentHandler.startElement(AUTHORITY_QNAME.getNamespaceURI(), AUTHORITY_QNAME.getLocalName(), toPrefixString(AUTHORITY_QNAME), EMPTY_ATTRIBUTES);
            String authority = permission.getAuthority();
            contentHandler.characters(authority.toCharArray(), 0, authority.length());
            contentHandler.endElement(AUTHORITY_QNAME.getNamespaceURI(), AUTHORITY_QNAME.getLocalName(), toPrefixString(AUTHORITY_QNAME));

            // output permission
            contentHandler.startElement(PERMISSION_QNAME.getNamespaceURI(), PERMISSION_QNAME.getLocalName(), toPrefixString(PERMISSION_QNAME), EMPTY_ATTRIBUTES);
            String strPermission = permission.getPermission();
            contentHandler.characters(strPermission.toCharArray(), 0, strPermission.length());
            contentHandler.endElement(PERMISSION_QNAME.getNamespaceURI(), PERMISSION_QNAME.getLocalName(), toPrefixString(PERMISSION_QNAME));
            
            // end access control entry
            contentHandler.endElement(ACE_QNAME.getNamespaceURI(), ACE_QNAME.getLocalName(), toPrefixString(ACE_QNAME));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process permission event - node ref " + nodeRef.toString() + "; permission " + permission);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endACL(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endACL(NodeRef nodeRef)
    {
        try
        {
            contentHandler.endElement(ACL_QNAME.getNamespaceURI(), ACL_QNAME.getLocalName(), toPrefixString(ACL_QNAME));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end ACL event - node ref " + nodeRef.toString());
        }
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startProperties(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startProperties(NodeRef nodeRef)
    {
        try
        {
            contentHandler.startElement(PROPERTIES_QNAME.getNamespaceURI(), PROPERTIES_LOCALNAME, toPrefixString(PROPERTIES_QNAME), EMPTY_ATTRIBUTES);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start properties", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endProperties(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endProperties(NodeRef nodeRef)
    {
        try
        {
            contentHandler.endElement(PROPERTIES_QNAME.getNamespaceURI(), PROPERTIES_LOCALNAME, toPrefixString(PROPERTIES_QNAME));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start properties", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startProperty(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void startProperty(NodeRef nodeRef, QName property)
    {
        try
        {
            contentHandler.startElement(property.getNamespaceURI(), property.getLocalName(), toPrefixString(property), EMPTY_ATTRIBUTES);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start property event - nodeRef " + nodeRef + "; property " + toPrefixString(property), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endProperty(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void endProperty(NodeRef nodeRef, QName property)
    {
        try
        {
            contentHandler.endElement(property.getNamespaceURI(), property.getLocalName(), toPrefixString(property));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end property event - nodeRef " + nodeRef + "; property " + toPrefixString(property), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#value(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.io.Serializable)
     */
    public void value(NodeRef nodeRef, QName property, Object value)
    {
        try
        {
            // determine data type of value
            QName valueDataType = null;
            PropertyDefinition propDef = dictionaryService.getProperty(property);
            DataTypeDefinition dataTypeDef = (propDef == null) ? null : propDef.getDataType();
            if (dataTypeDef == null || dataTypeDef.getName().equals(DataTypeDefinition.ANY))
            {
                dataTypeDef = (value == null) ? null : dictionaryService.getDataType(value.getClass());
                if (dataTypeDef != null)
                {
                    valueDataType = dataTypeDef.getName();
                }
            }

            // convert node references to paths
            if (value instanceof NodeRef)
            {
                Path nodeRefPath = createRelativePath(nodeRef, (NodeRef)value);
                value = (nodeRefPath == null) ? null : nodeRefPath.toPrefixString(namespaceService);
            }
            
            // output value wrapper if value is null or property data type is ANY
            if (value == null || valueDataType != null)
            {
                AttributesImpl attrs = new AttributesImpl();
                if (value == null)
                {
                    attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_PREFIX, ISNULL_LOCALNAME, ISNULL_QNAME.toPrefixString(), null, "true");
                }
                if (valueDataType != null)
                {
                    attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_PREFIX, DATATYPE_LOCALNAME, DATATYPE_QNAME.toPrefixString(), null, toPrefixString(valueDataType));
                }
                contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME), attrs);
            }
            
            // output value
            String strValue = (String)DefaultTypeConverter.INSTANCE.convert(String.class, value);
            if (strValue != null)
            {
                contentHandler.characters(strValue.toCharArray(), 0, strValue.length());
            }

            // output value wrapper if property data type is any
            if (value == null || valueDataType != null)
            {
                contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME));
            }
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process value event - nodeRef " + nodeRef + "; property " + toPrefixString(property) + "; value " + value, e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#value(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.util.Collection)
     */
    public void value(NodeRef nodeRef, QName property, Collection values)
    {
        try
        {
            PropertyDefinition propDef = dictionaryService.getProperty(property);
            DataTypeDefinition dataTypeDef = (propDef == null) ? null : propDef.getDataType();
            
            // start collection
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUES_LOCALNAME, toPrefixString(VALUES_QNAME), EMPTY_ATTRIBUTES);
            
            for (Object value : values)
            {
                // determine data type of value
                QName valueDataType = null;
                if (dataTypeDef == null || dataTypeDef.getName().equals(DataTypeDefinition.ANY))
                {
                    dataTypeDef = (value == null) ? null : dictionaryService.getDataType(value.getClass());
                    if (dataTypeDef != null)
                    {
                        valueDataType = dataTypeDef.getName();
                    }
                }

                // output value wrapper with datatype
                AttributesImpl attrs = new AttributesImpl();
                if (value == null)
                {
                    attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_PREFIX, ISNULL_LOCALNAME, ISNULL_QNAME.toPrefixString(), null, "true");
                }
                if (valueDataType != null)
                {
                    attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_PREFIX, DATATYPE_LOCALNAME, DATATYPE_QNAME.toPrefixString(), null, toPrefixString(valueDataType));
                }
                contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME), attrs);

                // convert node references to paths
                if (value instanceof NodeRef)
                {
                    value = createRelativePath(nodeRef, (NodeRef)value).toPrefixString(namespaceService);
                }
                
                // output value
                String strValue = (String)DefaultTypeConverter.INSTANCE.convert(String.class, value);
                if (strValue != null)
                {
                    contentHandler.characters(strValue.toCharArray(), 0, strValue.length());
                }

                // output value wrapper if property data type is any
                contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME));
            }

            // end collection
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUES_LOCALNAME, toPrefixString(VALUES_QNAME));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process multi-value event - nodeRef " + nodeRef + "; property " + toPrefixString(property), e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#content(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.io.InputStream)
     */
    public void content(NodeRef nodeRef, QName property, InputStream content, ContentData contentData)
    {
        // TODO: Base64 encode content and send out via Content Handler
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAssoc(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void startAssoc(NodeRef nodeRef, QName assoc)
    {
        try
        {
            contentHandler.startElement(assoc.getNamespaceURI(), assoc.getLocalName(), toPrefixString(assoc), EMPTY_ATTRIBUTES);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start assoc event - nodeRef " + nodeRef + "; association " + toPrefixString(assoc), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAssoc(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void endAssoc(NodeRef nodeRef, QName assoc)
    {
        try
        {
            contentHandler.endElement(assoc.getNamespaceURI(), assoc.getLocalName(), toPrefixString(assoc));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end assoc event - nodeRef " + nodeRef + "; association " + toPrefixString(assoc), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAssocs(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startAssocs(NodeRef nodeRef)
    {
        try
        {
            contentHandler.startElement(ASSOCIATIONS_QNAME.getNamespaceURI(), ASSOCIATIONS_LOCALNAME, toPrefixString(ASSOCIATIONS_QNAME), EMPTY_ATTRIBUTES);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start associations", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAssocs(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endAssocs(NodeRef nodeRef)
    {
        try
        {
            contentHandler.endElement(ASSOCIATIONS_QNAME.getNamespaceURI(), ASSOCIATIONS_LOCALNAME, toPrefixString(ASSOCIATIONS_QNAME));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end associations", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#warning(java.lang.String)
     */
    public void warning(String warning)
    {
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#end()
     */
    public void end()
    {
        try
        {
            contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VIEW_LOCALNAME, VIEW_QNAME.toPrefixString());
            contentHandler.endPrefixMapping(NamespaceService.REPOSITORY_VIEW_PREFIX);
            contentHandler.endDocument();
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end export event", e);
        }
    }

    /**
     * Get the prefix for the specified URI
     * @param uri  the URI
     * @return  the prefix (or null, if one is not registered)
     */
    private String toPrefixString(QName qname)
    {
        return qname.toPrefixString(namespaceService);
    }

    /**
     * Return relative path between from and to references within export root
     * 
     * @param fromRef  from reference
     * @param toRef  to reference
     * @return  path
     */    
    private Path createRelativePath(NodeRef fromRef, NodeRef toRef)
    {
        // Check that item exists first
        if (!nodeService.exists(toRef))
        {
            // return null path
            return null;
        }
        
        Path fromPath = nodeService.getPath(fromRef);
        Path toPath = nodeService.getPath(toRef);
        Path relativePath = null;

        try
        {
            // Determine if 'to path' is a category
            // TODO: This needs to be resolved in a more appropriate manner - special support is
            //       required for categories.
            for (int i = 0; i < toPath.size(); i++)
            {
                Path.Element pathElement = toPath.get(i);
                if (pathElement.getPrefixedString(namespaceService).equals("cm:categoryRoot"))
                {
                    Path.ChildAssocElement childPath = (Path.ChildAssocElement)pathElement;
                    relativePath = new Path();
                    relativePath.append(new Path.ChildAssocElement(new ChildAssociationRef(null, null, null, childPath.getRef().getParentRef())));
                    relativePath.append(toPath.subPath(i + 1, toPath.size() -1));
                    break;
                }
            }

            if (relativePath == null)
            {
                // Determine if from node is relative to export tree
                int i = 0;
                while (i < exportNodePath.size() && i < fromPath.size() && exportNodePath.get(i).equals(fromPath.get(i)))
                {
                    i++;
                }
                if (i == exportNodePath.size())
                {
                    // Determine if to node is relative to export tree
                    i = 0;
                    while (i < exportNodePath.size() && i < toPath.size() && exportNodePath.get(i).equals(toPath.get(i)))
                    {
                        i++;
                    }
                    if (i == exportNodePath.size())
                    {
                        // build relative path between from and to
                        relativePath = new Path();
                        for (int p = 0; p < fromPath.size() - i; p++)
                        {
                            relativePath.append(new Path.ParentElement());
                        }
                        if (i < toPath.size())
                        {
                            relativePath.append(toPath.subPath(i, toPath.size() -1));
                        }
                    }
                }
            }
            
            if (relativePath == null)
            {
                // default to absolute path
                relativePath = toPath;
            }
        }
        catch(Throwable e)
        {
            String msg = "Failed to determine relative path: export path=" + exportNodePath + "; from path=" + fromPath + "; to path=" + toPath;
            throw new ExporterException(msg, e);
        }
        
        return relativePath;
    }
    
}
