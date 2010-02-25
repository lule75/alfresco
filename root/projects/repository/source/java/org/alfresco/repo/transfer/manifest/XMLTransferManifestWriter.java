/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.transfer.manifest;

import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.alfresco.repo.transfer.TransferModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * XMLTransferManifestWriter
 * 
 * Writes the transfer manifest out to XML format.
 *
 * @author Mark Rogers
 */
public class XMLTransferManifestWriter implements TransferManifestWriter
{   
    public XMLTransferManifestWriter()
    {
    }
    
    private XMLWriter writer;
    
    final AttributesImpl EMPTY_ATTRIBUTES = new AttributesImpl();
    
    final String PREFIX = ManifestModel.MANIFEST_PREFIX;
    
    /**
     * Start the transfer manifest
     */
    public void startTransferManifest(Writer writer) throws SAXException
    {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(false);
        format.setIndentSize(3);
        format.setEncoding("UTF-8");
        
        this.writer = new XMLWriter(writer, format);
        this.writer.startDocument();
        
        this.writer.startPrefixMapping(PREFIX, TransferModel.TRANSFER_MODEL_1_0_URI);
        this.writer.startPrefixMapping("cm", NamespaceService.CONTENT_MODEL_1_0_URI);
    
        // Start Transfer Manifest  // uri, name, prefix
        this.writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_TRANSFER_MAINIFEST, PREFIX + ":" + ManifestModel.LOCALNAME_TRANSFER_MAINIFEST, EMPTY_ATTRIBUTES);
    }
    
    /**
     * End the transfer manifest 
     */
    public void endTransferManifest() throws SAXException
    {
        // End Transfer Manifest
       writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_TRANSFER_MAINIFEST,  PREFIX + ":" + ManifestModel.LOCALNAME_TRANSFER_MAINIFEST);
       writer.endPrefixMapping(PREFIX);
       
       writer.endDocument();
    }

    /**
     * Write the transfer manifest header
     */
    public void writeTransferManifestHeader(TransferManifestHeader header) throws SAXException
    {
        if(header.getCreatedDate() != null)
        {
        // Start Header
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_TRANSFER_HEADER, PREFIX + ":" + ManifestModel.LOCALNAME_TRANSFER_HEADER,  EMPTY_ATTRIBUTES);
        
        // Created Date
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_HEADER_CREATED_DATE, PREFIX + ":" + ManifestModel.LOCALNAME_HEADER_CREATED_DATE,  EMPTY_ATTRIBUTES);
        writeDate(header.getCreatedDate());
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_HEADER_CREATED_DATE, PREFIX + ":" + ManifestModel.LOCALNAME_HEADER_CREATED_DATE);
       
        // End Header
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_TRANSFER_HEADER, PREFIX + ":" + ManifestModel.LOCALNAME_TRANSFER_HEADER);
        }
    }
    
    /**
     * Write a deleted node to the manifest file
     * 
     * @param node
     * @throws SAXException
     */
    public void writeTransferManifestNode(TransferManifestDeletedNode node) throws SAXException
    {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("uri", "nodeRef", "nodeRef", "String", node.getNodeRef().toString());
        
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_NODE, PREFIX + ":" +ManifestModel.LOCALNAME_ELEMENT_DELETED_NODE,  attributes);
        
        if(node.getPrimaryParentAssoc() != null)
        {
            writePrimaryParent(node.getPrimaryParentAssoc(), node.getParentPath());
        }  
        
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_NODE, PREFIX + ":" +ManifestModel.LOCALNAME_ELEMENT_DELETED_NODE);
    
    }
        
    public void writeTransferManifestNode(TransferManifestNode node) throws SAXException
    {
        if(node instanceof TransferManifestDeletedNode)
        {
            TransferManifestDeletedNode node2 = (TransferManifestDeletedNode)node;
            writeTransferManifestNode(node2);
        }
        else if(node instanceof TransferManifestNormalNode)
        {
            TransferManifestNormalNode node2 = (TransferManifestNormalNode)node;
            writeTransferManifestNode(node2);
        }
        else
        {
        throw new IllegalArgumentException("Unexpected type" + node.getClass().getName());
    
        }
    }
    
    /**
     * Write a normal transfer manifest node
     * @param nodeRef
     * @throws SAXException
     */
    public void writeTransferManifestNode(TransferManifestNormalNode node) throws SAXException
    {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("uri", "nodeRef", "nodeRef", "String", node.getNodeRef().toString());
        attributes.addAttribute("uri", "nodeType", "nodeType", "String", formatQName(node.getType()));
        
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_NODE, PREFIX + ":" +ManifestModel.LOCALNAME_ELEMENT_NODE,  attributes);
        
        if(node.getPrimaryParentAssoc() != null)
        {
            writePrimaryParent(node.getPrimaryParentAssoc(), node.getParentPath());
        }       
        
        writeAspects(node.getAspects());
        
        writeProperties(node.getProperties());

        writeParentAssocs(node.getParentAssocs());
        
        writeChildAssocs(node.getChildAssocs());
        
        writeTargetAssocs(node.getTargetAssocs());
        
        writeSourceAssocs(node.getSourceAssocs());
        
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_NODE, PREFIX + ":" +ManifestModel.LOCALNAME_ELEMENT_NODE);
    }
    
    private void writeProperties(Map<QName, Serializable> properties) throws SAXException
    {
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PROPERTIES, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PROPERTIES,  EMPTY_ATTRIBUTES);
        if(properties != null )
        {
            for(Entry<QName, Serializable> entry : properties.entrySet())
            {
                writeProperty(entry.getKey(), entry.getValue());
            }
        }
        
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PROPERTIES, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PROPERTIES);
    }

    private void writeProperty(QName name, Serializable value) throws SAXException
    {              
        {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "name", "name", "String", formatQName(name));
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PROPERTY, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PROPERTY,  attributes);
        }
        
        //  TODO - Char [] does not seem to be processed correctly
        if(value.getClass().isArray())
        {
            writeValue(value.toString());
        }
        // Collection
        else if(value instanceof ContentData)
        {
            ContentData data = (ContentData)value;
            AttributesImpl dataAttributes = new AttributesImpl();
            dataAttributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "contentURL","contentURL", "String", data.getContentUrl());
            dataAttributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "mimetype", "mimetype", "String", data.getMimetype());
            dataAttributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "size", "size", "String", Long.toString(data.getSize()));
            dataAttributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "encoding", "encoding", "String", data.getEncoding());
            dataAttributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "locale", "locale", "String", data.getLocale().toString());
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_CONTENT_HEADER, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_CONTENT_HEADER,  dataAttributes);
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_CONTENT_HEADER, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_CONTENT_HEADER);
        }
            // Collection
        else if(value instanceof Collection)
        {
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_VALUES, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_VALUES,  EMPTY_ATTRIBUTES);
            int index = 0;
            for (Object valueInCollection : (Collection)value)
            {
                writeValue((Serializable)valueInCollection);
                index++;
            }
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_VALUES, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_VALUES);
        }
        else if(value instanceof MLText)
        {
            MLText mltext = (MLText)value;
            for(Entry<Locale, String> entry : mltext.entrySet())
            {
                 writeMLValue(entry.getKey(), entry.getValue());
            }
        }
        else
        {
            writeValue(value);
        }
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PROPERTY, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PROPERTY);    
    }
    
    private void writeValue(Serializable value) throws SAXException
    {
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_VALUE, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_VALUE,  EMPTY_ATTRIBUTES);
        String strValue = (String)DefaultTypeConverter.INSTANCE.convert(String.class, value);
        
        writer.characters(strValue.toCharArray(), 0, strValue.length());
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_VALUE, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_VALUE);
    }
    
    private void writeMLValue(Locale locale, Serializable value) throws SAXException
    {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "locale", "locale", "String", locale.toString());
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_MLVALUE, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_MLVALUE,  attributes);
        String strValue = (String)DefaultTypeConverter.INSTANCE.convert(String.class, value);
        writer.characters(strValue.toCharArray(), 0, strValue.length());
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_MLVALUE, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_MLVALUE);
    }
    
    private void writeAspects(Set<QName> aspects) throws SAXException
    {
       writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_ASPECTS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_ASPECTS,  EMPTY_ATTRIBUTES);
       
       if(aspects != null)
       {
           for(QName aspect : aspects)
           {
               writeAspect(aspect);
           }   
       }
        
       writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_ASPECTS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_ASPECTS); 
    }
    
    private void writeAspect(QName aspect) throws SAXException
    {
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_ASPECT, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_ASPECT,  EMPTY_ATTRIBUTES);
        String name=formatQName(aspect); 
        writer.characters(name.toCharArray(), 0, name.length());
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_ASPECT, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_ASPECT); 
    }
    
    private void writeDate(Date date) throws SAXException
    {
        String dates = ISO8601DateFormat.format(date);
        writer.characters(dates.toCharArray(), 0, dates.length());
        
    }
    
    private String formatQName(QName qname)
    {
        return qname.toString();
    }
    
    private void writePrimaryParent(ChildAssociationRef parentAssoc, Path parentPath) throws SAXException
    {   
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PARENT, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PARENT,  EMPTY_ATTRIBUTES);

        writeParentAssoc(parentAssoc);
        
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PATH, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PATH,  EMPTY_ATTRIBUTES);
        if(parentPath != null)
        {  
            String path = parentPath.toString();
            writer.characters(path.toCharArray(), 0, path.length()); 
        }
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PATH, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PATH); 

        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PARENT, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PRIMARY_PARENT); 
    }
    
    private void writeParentAssocs(List<ChildAssociationRef> refs) throws SAXException
    {
        if(refs != null)
        {
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOCS,  EMPTY_ATTRIBUTES);

            for(ChildAssociationRef assoc : refs)
            {
                writeParentAssoc(assoc);
            }
            
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOCS); 
        }
    }
    private void writeChildAssocs(List<ChildAssociationRef> refs) throws SAXException
    {
        if(refs != null)
        {
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOCS,  EMPTY_ATTRIBUTES);

            for(ChildAssociationRef assoc : refs)
            {
                writeChildAssoc(assoc);
            }
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOCS); 
        }
    }
    
    private void writeParentAssoc(ChildAssociationRef assoc) throws SAXException
    {
        if(assoc != null)
        {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "from", "from", "String", assoc.getParentRef().toString());     
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "type", "type", "String", formatQName(assoc.getTypeQName()));
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "type", "isPrimary", "Boolean", assoc.isPrimary()?"true":"false");
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOC, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOC,  attributes);
            String name= formatQName(assoc.getQName());
            writer.characters(name.toCharArray(), 0, name.length());            
            assoc.isPrimary();

            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOC, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_PARENT_ASSOC); 
        }
    }
    
    private void writeChildAssoc(ChildAssociationRef assoc) throws SAXException
    {
        if(assoc != null)
        {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "to", "to", "String", assoc.getChildRef().toString());
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "type", "type", "String", formatQName(assoc.getTypeQName()));
            attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "type", "isPrimary", "Boolean", assoc.isPrimary()?"true":"false");
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOC, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOC,  attributes);
            String name= formatQName(assoc.getQName());
            writer.characters(name.toCharArray(), 0, name.length());
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOC, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_CHILD_ASSOC); 
        }
    }
    
    private void writeTargetAssocs(List<AssociationRef> refs) throws SAXException
    {
        if(refs != null)
        {
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_TARGET_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_TARGET_ASSOCS,  EMPTY_ATTRIBUTES);

            for(AssociationRef assoc : refs)
            {
                writeAssoc(assoc);
            }
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_TARGET_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_TARGET_ASSOCS); 
        }
    }
    private void writeSourceAssocs(List<AssociationRef> refs) throws SAXException
    {
        if(refs != null)
        {
            writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_SOURCE_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_SOURCE_ASSOCS,  EMPTY_ATTRIBUTES);

            for(AssociationRef assoc : refs)
            {
                writeAssoc(assoc);
            }
            writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_SOURCE_ASSOCS, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_SOURCE_ASSOCS); 
        }
    }
    private void writeAssoc(AssociationRef ref) throws SAXException
    {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "source", "source", "String", ref.getSourceRef().toString());
        attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "target", "target", "String", ref.getTargetRef().toString());
        attributes.addAttribute(TransferModel.TRANSFER_MODEL_1_0_URI, "type", "type", "String", formatQName(ref.getTypeQName()));
 
        writer.startElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_ASSOC, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_ASSOC,  attributes);       
        writer.endElement(TransferModel.TRANSFER_MODEL_1_0_URI, ManifestModel.LOCALNAME_ELEMENT_ASSOC, PREFIX + ":" + ManifestModel.LOCALNAME_ELEMENT_ASSOC); 
    }
}