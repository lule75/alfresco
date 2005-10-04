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
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.view.Exporter;
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
    private final static String VIEW_LOCALNAME = "view";
    private final static String VALUE_LOCALNAME = "value";
    private final static String CHILDNAME_LOCALNAME = "childName";
    private final static String ASPECTS_LOCALNAME = "aspects";
    private final static String PROPERTIES_LOCALNAME = "properties";
    private final static String ASSOCIATIONS_LOCALNAME = "associations";
    private final static String DATATYPE_LOCALNAME = "datatype";
    private static QName VIEW_QNAME; 
    private static QName VALUE_QNAME;
    private static QName PROPERTIES_QNAME;
    private static QName ASPECTS_QNAME;
    private static QName ASSOCIATIONS_QNAME; 
    private static QName CHILDNAME_QNAME;
    private static QName DATATYPE_QNAME;
    
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private ContentHandler contentHandler;
    private final static AttributesImpl EMPTY_ATTRIBUTES = new AttributesImpl();
    

    /**
     * Construct
     * 
     * @param namespaceService  namespace service
     * @param nodeService  node service
     * @param contentHandler  content handler
     */
    ViewXMLExporter(NamespaceService namespaceService, NodeService nodeService, DictionaryService dictionaryService, ContentHandler contentHandler)
    {
        this.namespaceService = namespaceService;
        this.nodeService = nodeService;
        this.dictionaryService = dictionaryService;
        this.contentHandler = contentHandler;
        
        VIEW_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, VIEW_LOCALNAME, namespaceService);
        VALUE_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, namespaceService);
        CHILDNAME_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, CHILDNAME_LOCALNAME, namespaceService);
        ASPECTS_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ASPECTS_LOCALNAME, namespaceService);
        PROPERTIES_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, PROPERTIES_LOCALNAME, namespaceService);
        ASSOCIATIONS_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, ASSOCIATIONS_LOCALNAME, namespaceService);
        DATATYPE_QNAME = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, DATATYPE_LOCALNAME, namespaceService);
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#start()
     */
    public void start()
    {
        try
        {
            contentHandler.startDocument();
            contentHandler.startPrefixMapping(NamespaceService.REPOSITORY_VIEW_PREFIX, NamespaceService.REPOSITORY_VIEW_1_0_URI);
            contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VIEW_LOCALNAME, VIEW_QNAME.toPrefixString(), EMPTY_ATTRIBUTES);
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
            Path path = nodeService.getPath(nodeRef);
            String childName = path.last().getElementString();
            QName childQName = QName.createQName(childName);
            AttributesImpl attrs = new AttributesImpl(); 
            attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_1_0_URI, CHILDNAME_LOCALNAME, CHILDNAME_QNAME.toPrefixString(), null, toPrefixString(childQName));
            
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
        if (value != null)
        {
            try
            {
                // determine data type of value
                QName valueDataType = null;
                DataTypeDefinition dataTypeDef = dictionaryService.getProperty(property).getDataType();
                if (dataTypeDef.getName().equals(DataTypeDefinition.ANY))
                {
                    dataTypeDef = dictionaryService.getDataType(value.getClass());
                    if (dataTypeDef != null)
                    {
                        valueDataType = dataTypeDef.getName();
                    }
                }

                // output value wrapper if property data type is any
                if (valueDataType != null)
                {
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_PREFIX, DATATYPE_LOCALNAME, DATATYPE_QNAME.toPrefixString(), null, toPrefixString(valueDataType));
                    contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME), attrs);
                }
                
                // output value
                String strValue = (String)DefaultTypeConverter.INSTANCE.convert(String.class, value);
                contentHandler.characters(strValue.toCharArray(), 0, strValue.length());

                // output value wrapper if property data type is any
                if (valueDataType != null)
                {
                    contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME));
                }
            }
            catch (SAXException e)
            {
                throw new ExporterException("Failed to process value event - nodeRef " + nodeRef + "; property " + toPrefixString(property) + "; value " + value, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#value(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.util.Collection)
     */
    public void value(NodeRef nodeRef, QName property, Collection values)
    {
        try
        {
            DataTypeDefinition dataTypeDef = dictionaryService.getProperty(property).getDataType();
            
            for (Object value : values)
            {
                if (dataTypeDef.getName().equals(DataTypeDefinition.ANY))
                {
                    // determine value type
                    DataTypeDefinition valueDataTypeDef = (value == null) ? null : dictionaryService.getDataType(value.getClass());
                    QName valueDataType = null;
                    if (valueDataTypeDef != null)
                    {
                        valueDataType = dataTypeDef.getName(); 
                    }

                    // output value wrapper with datatype
                    AttributesImpl attrs = new AttributesImpl();
                    if (valueDataType != null)
                    {
                        attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_PREFIX, DATATYPE_LOCALNAME, DATATYPE_QNAME.toPrefixString(), null, toPrefixString(valueDataType));
                    }
                    contentHandler.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME), attrs);

                    // output value
                    String strValue = (String)DefaultTypeConverter.INSTANCE.convert(String.class, value);
                    contentHandler.characters(strValue.toCharArray(), 0, strValue.length());

                    // output value wrapper if property data type is any
                    contentHandler.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, VALUE_LOCALNAME, toPrefixString(VALUE_QNAME));
                }
            }
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

}
