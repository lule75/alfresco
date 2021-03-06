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
package org.alfresco.module.org_alfresco_module_dod5015;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.BeforeCreateReference;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.BeforeRemoveReference;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.OnCreateReference;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.OnRemoveReference;
import org.alfresco.module.org_alfresco_module_dod5015.caveat.RMListOfValuesConstraint;
import org.alfresco.module.org_alfresco_module_dod5015.caveat.RMListOfValuesConstraint.MatchLogic;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.dictionary.M2Aspect;
import org.alfresco.repo.dictionary.M2Association;
import org.alfresco.repo.dictionary.M2ChildAssociation;
import org.alfresco.repo.dictionary.M2ClassAssociation;
import org.alfresco.repo.dictionary.M2Constraint;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Property;
import org.alfresco.repo.policy.ClassPolicyDelegate;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.Constraint;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.GUID;
import org.springframework.extensions.surf.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Records Management AdminService Implementation.
 * 
 * @author Neil McErlean, janv
 */
public class RecordsManagementAdminServiceImpl implements RecordsManagementAdminService, RecordsManagementCustomModel
{
    /** Logger */
    private static Log logger = LogFactory.getLog(RecordsManagementAdminServiceImpl.class);
    
    public static final String RMC_CUSTOM_ASSOCS = RecordsManagementCustomModel.RM_CUSTOM_PREFIX + ":customAssocs";
    
    private static final String CUSTOM_CONSTRAINT_TYPE = org.alfresco.module.org_alfresco_module_dod5015.caveat.RMListOfValuesConstraint.class.getName();
    
    private static final NodeRef RM_CUSTOM_MODEL_NODE_REF = new NodeRef("workspace://SpacesStore/records_management_custom_model");
    
    private static final String PARAM_ALLOWED_VALUES = "allowedValues";
    private static final String PARAM_CASE_SENSITIVE = "caseSensitive";
    private static final String PARAM_MATCH_LOGIC = "matchLogic";
    
    public static final String RMA_RECORD = "rma:record";
    
    private static final String SOURCE_TARGET_ID_SEPARATOR = "__";
    
    /** Services */
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private ContentService contentService;

    private PolicyComponent policyComponent;
    
    /** Policy delegates */
    private ClassPolicyDelegate<BeforeCreateReference> beforeCreateReferenceDelegate;
    private ClassPolicyDelegate<OnCreateReference> onCreateReferenceDelegate;    
    private ClassPolicyDelegate<BeforeRemoveReference> beforeRemoveReferenceDelegate;
    private ClassPolicyDelegate<OnRemoveReference> onRemoveReferenceDelegate;
    
    /**
     * @param dictionaryService     the dictionary service
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
		this.dictionaryService = dictionaryService;
	}

    /**
     * @param namespaceService      the namespace service
     */
	public void setNamespaceService(NamespaceService namespaceService)
	{
		this.namespaceService = namespaceService;
	}

	/**
	 * @param nodeService      the node service
	 */
	public void setNodeService(NodeService nodeService)
	{
		this.nodeService = nodeService;
	}
	
	public void setContentService(ContentService contentService)
	{
	    this.contentService = contentService;
	}
	
	/**
	 * @param policyComponent  the policy component
	 */
	public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
	
	/**
	 * Initialisation method
	 */
	public void init()
    {
        // Register the various policies
        beforeCreateReferenceDelegate = policyComponent.registerClassPolicy(BeforeCreateReference.class);
        onCreateReferenceDelegate = policyComponent.registerClassPolicy(OnCreateReference.class);
        beforeRemoveReferenceDelegate = policyComponent.registerClassPolicy(BeforeRemoveReference.class);
        onRemoveReferenceDelegate = policyComponent.registerClassPolicy(OnRemoveReference.class);
    }
	
    protected void invokeBeforeCreateReference(NodeRef fromNodeRef, NodeRef toNodeRef, QName reference)
    {
        // get qnames to invoke against
        Set<QName> qnames = RecordsManagementPoliciesUtil.getTypeAndAspectQNames(nodeService, fromNodeRef);
        // execute policy for node type and aspects
        BeforeCreateReference policy = beforeCreateReferenceDelegate.get(qnames);
        policy.beforeCreateReference(fromNodeRef, toNodeRef, reference);
    }
    
    protected void invokeOnCreateReference(NodeRef fromNodeRef, NodeRef toNodeRef, QName reference)
    {
        // get qnames to invoke against
        Set<QName> qnames = RecordsManagementPoliciesUtil.getTypeAndAspectQNames(nodeService, fromNodeRef);
        // execute policy for node type and aspects
        OnCreateReference policy = onCreateReferenceDelegate.get(qnames);
        policy.onCreateReference(fromNodeRef, toNodeRef, reference);
    }
    
    protected void invokeBeforeRemoveReference(NodeRef fromNodeRef, NodeRef toNodeRef, QName reference)
    {
        // get qnames to invoke against
        Set<QName> qnames = RecordsManagementPoliciesUtil.getTypeAndAspectQNames(nodeService, fromNodeRef);
        // execute policy for node type and aspects
        BeforeRemoveReference policy = beforeRemoveReferenceDelegate.get(qnames);
        policy.beforeRemoveReference(fromNodeRef, toNodeRef, reference);
    }
    
    protected void invokeOnRemoveReference(NodeRef fromNodeRef, NodeRef toNodeRef, QName reference)
    {
        // get qnames to invoke against
        Set<QName> qnames = RecordsManagementPoliciesUtil.getTypeAndAspectQNames(nodeService, fromNodeRef);
        // execute policy for node type and aspects
        OnRemoveReference policy = onRemoveReferenceDelegate.get(qnames);
        policy.onRemoveReference(fromNodeRef, toNodeRef, reference);
    }

	/**
	 * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService#getCustomReferenceDefinitions()
	 */
    public Map<QName, AssociationDefinition> getCustomReferenceDefinitions()
    {
		QName relevantAspectQName = QName.createQName(RMC_CUSTOM_ASSOCS, namespaceService);
        AspectDefinition aspectDefn = dictionaryService.getAspect(relevantAspectQName);
        Map<QName, AssociationDefinition> assocDefns = aspectDefn.getAssociations();
        
        return assocDefns;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService#getCustomPropertyDefinitions()
     */
    public Map<QName, PropertyDefinition> getCustomPropertyDefinitions()
    {
    	Map<QName, PropertyDefinition> result = new HashMap<QName, PropertyDefinition>();
    	for (CustomisableRmElement elem : CustomisableRmElement.values())
    	{
    		result.putAll(getCustomPropertyDefinitions(elem));
    	}
        return result;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService#getCustomPropertyDefinitions(org.alfresco.module.org_alfresco_module_dod5015.CustomisableRmElement)
     */
    public Map<QName, PropertyDefinition> getCustomPropertyDefinitions(CustomisableRmElement rmElement)
    {
		QName relevantAspectQName = QName.createQName(rmElement.getCorrespondingAspect(), namespaceService);
        AspectDefinition aspectDefn = dictionaryService.getAspect(relevantAspectQName);
        Map<QName, PropertyDefinition> propDefns = aspectDefn.getProperties();

        return propDefns;
    }
    
    public QName addCustomPropertyDefinition(QName propId, String aspectName, String label, QName dataType, String title, String description)
    {
        return addCustomPropertyDefinition(propId, aspectName, label, dataType, title, description, null, false, false, false, null);
    }
    
    public QName addCustomPropertyDefinition(QName propId, String aspectName, String label, QName dataType, String title, String description, String defaultValue, boolean multiValued, boolean mandatory, boolean isProtected, QName lovConstraint)
    {
        // title parameter is currently ignored. Intentionally.
        if (propId == null)
        {
            // Generate a propId
            propId = this.generateQNameFor(label);
        }
        
        ParameterCheck.mandatoryString("aspectName", aspectName);
        ParameterCheck.mandatory("label", label);
        ParameterCheck.mandatory("dataType", dataType);
        
        M2Model deserializedModel = readCustomContentModel();
        M2Aspect customPropsAspect = deserializedModel.getAspect(aspectName);
        
        if (customPropsAspect == null)
        {
            throw new AlfrescoRuntimeException("Unknown aspect: " + aspectName);
        }
        
        String propIdAsString = propId.toPrefixString(namespaceService);

        M2Property customProp = customPropsAspect.getProperty(propIdAsString);
        if (customProp != null)
        {
            throw new AlfrescoRuntimeException("Property already exists: " + propIdAsString);
        }
        
        M2Property newProp = customPropsAspect.createProperty(propIdAsString);
        newProp.setName(propIdAsString);
        newProp.setType(dataType.toPrefixString(namespaceService));
        
        // Note that the title is used to store the RM 'label'.
        newProp.setTitle(label);
        newProp.setDescription(description);
        newProp.setDefaultValue(defaultValue);
        
        newProp.setMandatory(mandatory);
        newProp.setProtected(isProtected);
        newProp.setMultiValued(multiValued);
        
        newProp.setIndexed(true);
        newProp.setIndexedAtomically(true);
        newProp.setStoredInIndex(false);
        newProp.setIndexTokenisationMode(IndexTokenisationMode.FALSE);
        
        if (lovConstraint != null)
        {
            if (! dataType.equals(DataTypeDefinition.TEXT))
            {
                throw new AlfrescoRuntimeException("Cannot apply constraint '"+lovConstraint+"' to property '"+propIdAsString+"' with datatype '"+dataType+"' (expected: dataType = TEXT)");
            }
            
            String lovConstraintQNameAsString = lovConstraint.toPrefixString(namespaceService);
            newProp.addConstraintRef(lovConstraintQNameAsString);
        }
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("addCustomPropertyDefinition: "+label+
                    "=" + propIdAsString + " to aspect: "+aspectName);
        }
        
        return propId;
    }

    public QName setCustomPropertyDefinitionLabel(QName propQName, String newLabel)
    {
        ParameterCheck.mandatory("propQName", propQName);
        
        PropertyDefinition propDefn = dictionaryService.getProperty(propQName);
        if (propDefn == null)
        {
            throw new AlfrescoRuntimeException("DictionaryService does not contain property definition " + propQName);
        }
        
        if (newLabel == null) return propQName;
        
        M2Model deserializedModel = readCustomContentModel();
        M2Property targetProperty = findProperty(propQName, deserializedModel);

        targetProperty.setTitle(newLabel);
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("setCustomPropertyDefinitionLabel: "+propQName+
                    "=" + newLabel);
        }
        
        return propQName;
    }
    
    public QName setCustomPropertyDefinitionConstraint(QName propQName, QName newLovConstraint)
    {
        ParameterCheck.mandatory("propQName", propQName);
        
        PropertyDefinition propDefn = dictionaryService.getProperty(propQName);
        if (propDefn == null)
        {
            throw new AlfrescoRuntimeException("DictionaryService does not contain property definition " + propQName);
        }
        
        M2Model deserializedModel = readCustomContentModel();
        M2Property targetProp = findProperty(propQName, deserializedModel);
        String dataType = targetProp.getType();

        if (! dataType.equals(DataTypeDefinition.TEXT.toPrefixString(namespaceService)))
        {
            throw new AlfrescoRuntimeException("Cannot apply constraint '"+newLovConstraint+
                    "' to property '" + targetProp.getName() + "' with datatype '" + dataType + "' (expected: dataType = TEXT)");
        }
        String lovConstraintQNameAsString = newLovConstraint.toPrefixString(namespaceService);
        
        // Add the constraint - if it isn't already there.
        String refOfExistingConstraint = null;
        
        for (M2Constraint c : targetProp.getConstraints())
        {
            // There should only be one constraint.
            refOfExistingConstraint = c.getRef();
            break;
        }
        if (refOfExistingConstraint != null)
        {
            targetProp.removeConstraintRef(refOfExistingConstraint);
        }
        targetProp.addConstraintRef(lovConstraintQNameAsString);
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("addCustomPropertyDefinitionConstraint: "+lovConstraintQNameAsString);
        }
        
        return propQName;
    }

    public QName removeCustomPropertyDefinitionConstraints(QName propQName)
    {
        ParameterCheck.mandatory("propQName", propQName);
        
        PropertyDefinition propDefn = dictionaryService.getProperty(propQName);
        if (propDefn == null)
        {
            throw new AlfrescoRuntimeException("DictionaryService does not contain property definition " + propQName);
        }
        
        M2Model deserializedModel = readCustomContentModel();
        M2Property targetProperty = findProperty(propQName, deserializedModel);
        
        // Need to count backwards to remove constraints
        for (int i = targetProperty.getConstraints().size() - 1; i >= 0; i--) {
            String ref = targetProperty.getConstraints().get(i).getRef();
            targetProperty.removeConstraintRef(ref);
        }
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("removeCustomPropertyDefinitionConstraints: "+propQName);
        }
        
        return propQName;
    }

    private M2Property findProperty(QName propQName, M2Model deserializedModel)
    {
        List<M2Aspect> aspects = deserializedModel.getAspects();
        // Search through the aspects looking for the custom property
        for (M2Aspect aspect : aspects)
        {
            for (M2Property prop : aspect.getProperties())
            {
                if (propQName.toPrefixString(namespaceService).equals(prop.getName()))
                {
                    return prop;
                }
            }
        }
        throw new AlfrescoRuntimeException("Custom model does not contain property definition " + propQName);
    }

    public void removeCustomPropertyDefinition(QName propQName)
    {
        ParameterCheck.mandatory("propQName", propQName);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String propQNameAsString = propQName.toPrefixString(namespaceService);
        
        String aspectName = null;
        
        boolean found = false;
        
        // Need to select the correct aspect in the customModel from which we'll
        // attempt to delete the property definition.
        for (CustomisableRmElement elem : CustomisableRmElement.values())
        {
            aspectName = elem.getCorrespondingAspect();
            M2Aspect customPropsAspect = deserializedModel.getAspect(aspectName);
            
            if (customPropsAspect == null)
            {
                throw new AlfrescoRuntimeException("Unknown aspect: "+aspectName);
            }
            
            M2Property prop = customPropsAspect.getProperty(propQNameAsString);
            if (prop != null)
            {
                if (logger.isDebugEnabled())
                {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Attempting to delete custom property: ");
                    msg.append(propQNameAsString);
                    logger.debug(msg.toString());
                }
                
                found = true;
                customPropsAspect.removeProperty(propQNameAsString);
                break;
            }
        }
        
        if (! found)
        {
            throw new AlfrescoRuntimeException("Could not find property to delete: "+propQNameAsString);
        }
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("deleteCustomPropertyDefinition: "+propQNameAsString+" from aspect: "+aspectName);
        }
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService#addCustomReference(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
	public void addCustomReference(NodeRef fromNode, NodeRef toNode, QName refId)
	{
	    // Check that a definition for the reference type exists.
		Map<QName, AssociationDefinition> availableAssocs = this.getCustomReferenceDefinitions();

		AssociationDefinition assocDef = availableAssocs.get(refId);
		if (assocDef == null)
		{
			throw new IllegalArgumentException("No such custom reference: " + refId);
		}

		// Check if an instance of this reference type already exists in the same direction.
		boolean associationAlreadyExists = false;
        if (assocDef.isChild())
        {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(fromNode, assocDef.getName(), assocDef.getName());
            for (ChildAssociationRef chAssRef : childAssocs)
            {
                if (chAssRef.getChildRef().equals(toNode))
                {
                    associationAlreadyExists = true;
                }
            }
        }
        else
        {
            List<AssociationRef> assocs = nodeService.getTargetAssocs(fromNode, assocDef.getName());
            for (AssociationRef assRef : assocs)
            {
                if (assRef.getTargetRef().equals(toNode))
                {
                    associationAlreadyExists = true;
                }
            }
        }
        if (associationAlreadyExists)
        {
            StringBuilder msg = new StringBuilder();
            msg.append("Association '").append(refId).append("' already exists from ")
                .append(fromNode).append(" to ").append(toNode);
            throw new AlfrescoRuntimeException(msg.toString());
        }

		// Invoke before create reference policy
		invokeBeforeCreateReference(fromNode, toNode, refId);
		
		if (assocDef.isChild())
		{
			this.nodeService.addChild(fromNode, toNode, refId, refId);
		}
		else
		{
			this.nodeService.createAssociation(fromNode, toNode, refId);
		}
		
		// Invoke on create reference policy
        invokeOnCreateReference(fromNode, toNode, refId);
	}

	public void removeCustomReference(NodeRef fromNode, NodeRef toNode, QName assocId) 
	{
		Map<QName, AssociationDefinition> availableAssocs = this.getCustomReferenceDefinitions();

		AssociationDefinition assocDef = availableAssocs.get(assocId);
		if (assocDef == null)
		{
			throw new IllegalArgumentException("No such custom reference: " + assocId);
		}
		
		invokeBeforeRemoveReference(fromNode, toNode, assocId);

		if (assocDef.isChild())
		{
			List<ChildAssociationRef> children = nodeService.getChildAssocs(fromNode);
			for (ChildAssociationRef chRef : children)
			{
				if (assocId.equals(chRef.getTypeQName()) && chRef.getChildRef().equals(toNode))
				{
					nodeService.removeChildAssociation(chRef);
				}
			}
		}
		else
		{
			nodeService.removeAssociation(fromNode, toNode, assocId);
		}
		
		invokeOnRemoveReference(fromNode, toNode, assocId);
	}

	public List<AssociationRef> getCustomReferencesFrom(NodeRef node)
	{
    	List<AssociationRef> retrievedAssocs = nodeService.getTargetAssocs(node, RegexQNamePattern.MATCH_ALL);
    	return retrievedAssocs;
	}

	public List<ChildAssociationRef> getCustomChildReferences(NodeRef node)
	{
    	List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(node);
    	return childAssocs;
	}
	
    public List<AssociationRef> getCustomReferencesTo(NodeRef node)
    {
        List<AssociationRef> retrievedAssocs = nodeService.getSourceAssocs(node, RegexQNamePattern.MATCH_ALL);
        return retrievedAssocs;
    }

    public List<ChildAssociationRef> getCustomParentReferences(NodeRef node)
    {
        List<ChildAssociationRef> result = nodeService.getParentAssocs(node);
        return result;
    }

    public QName addCustomAssocDefinition(String label)
    {
        ParameterCheck.mandatoryString("label", label);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String aspectName = RecordsManagementAdminServiceImpl.RMC_CUSTOM_ASSOCS;
        M2Aspect customAssocsAspect = deserializedModel.getAspect(aspectName);
        
        if (customAssocsAspect == null)
        {
            throw new AlfrescoRuntimeException("Unknown aspect: "+aspectName);
        }

        // If this label is already taken...
        if (getQNameForClientId(label) != null)
        {
            throw new IllegalArgumentException("Reference label already in use: " + label);
        }
        
        QName generatedQName = this.generateQNameFor(label);
        String generatedShortQName = generatedQName.toPrefixString(namespaceService);
        
        M2ClassAssociation customAssoc = customAssocsAspect.getAssociation(generatedShortQName);
        if (customAssoc != null)
        {
            throw new AlfrescoRuntimeException("Assoc already exists: "+generatedShortQName);
        }
        
        M2Association newAssoc = customAssocsAspect.createAssociation(generatedShortQName);
        newAssoc.setSourceMandatory(false);
        newAssoc.setTargetMandatory(false);

        // MOB-1573
        newAssoc.setSourceMany(true);
        newAssoc.setTargetMany(true);

        // The label is stored in the title.
        newAssoc.setTitle(label);
        
        // TODO Could be the customAssocs aspect
        newAssoc.setTargetClassName(RecordsManagementAdminServiceImpl.RMA_RECORD);
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("addCustomAssocDefinition: ("+label+")");
        }
        
        return generatedQName;
    }

    public QName addCustomChildAssocDefinition(String source, String target)
    {
        ParameterCheck.mandatoryString("source", source);
        ParameterCheck.mandatoryString("target", target);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String aspectName = RecordsManagementAdminServiceImpl.RMC_CUSTOM_ASSOCS;
        M2Aspect customAssocsAspect = deserializedModel.getAspect(aspectName);
        
        if (customAssocsAspect == null)
        {
            throw new AlfrescoRuntimeException("Unknown aspect: "+aspectName);
        }

        String compoundID = this.getCompoundIdFor(source, target);
        if (getQNameForClientId(compoundID) != null)
        {
            throw new IllegalArgumentException("Reference label already in use: " + compoundID);
        }
        
        M2ClassAssociation customAssoc = customAssocsAspect.getAssociation(compoundID);
        if (customAssoc != null)
        {
            throw new AlfrescoRuntimeException("ChildAssoc already exists: "+compoundID);
        }
        QName generatedQName = this.generateQNameFor(compoundID);
        
        M2ChildAssociation newAssoc = customAssocsAspect.createChildAssociation(generatedQName.toPrefixString(namespaceService));
        newAssoc.setSourceMandatory(false);
        newAssoc.setTargetMandatory(false);
        
        // MOB-1573
        newAssoc.setSourceMany(true);
        newAssoc.setTargetMany(true);

        // source and target are stored in title.
        newAssoc.setTitle(compoundID);
        
        // TODO Could be the custom assocs aspect
        newAssoc.setTargetClassName(RecordsManagementAdminServiceImpl.RMA_RECORD);
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("addCustomChildAssocDefinition: ("+source+","+target+")");
        }
        
        return generatedQName;
    }
    
    public QName updateCustomChildAssocDefinition(QName refQName, String newSource, String newTarget)
    {
        String compoundId = getCompoundIdFor(newSource, newTarget);
        return persistUpdatedAssocTitle(refQName, compoundId);
    }
    
    public QName updateCustomAssocDefinition(QName refQName, String newLabel)
    {
        return persistUpdatedAssocTitle(refQName, newLabel);
    }

    /**
     * This method writes the specified String into the association's title property.
     * For RM custom properties and references, Title is used to store the identifier.
     */
    private QName persistUpdatedAssocTitle(QName refQName, String newTitle)
    {
        ParameterCheck.mandatory("refQName", refQName);
        
        AssociationDefinition assocDefn = dictionaryService.getAssociation(refQName);
        if (assocDefn == null)
        {
            throw new AlfrescoRuntimeException("DictionaryService does not contain association definition " + refQName);
        }
        
        M2Model deserializedModel = readCustomContentModel();
        M2Aspect customAssocsAspect = deserializedModel.getAspect(RMC_CUSTOM_ASSOCS);
        for (M2ClassAssociation assoc : customAssocsAspect.getAssociations())
        {
            if (refQName.toPrefixString(namespaceService).equals(assoc.getName()))
            {
                if (newTitle != null)
                {
                    assoc.setTitle(newTitle);
                }
            }
        }
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("persistUpdatedAssocTitle: "+refQName+
                    "=" + newTitle + " to aspect: " + RMC_CUSTOM_ASSOCS);
        }
        
        return refQName;
    }
    
    public void addCustomConstraintDefinition(QName constraintName, String title, boolean caseSensitive, List<String> allowedValues, MatchLogic matchLogic) 
    {
        ParameterCheck.mandatory("constraintName", constraintName);
        ParameterCheck.mandatoryString("title", title);
        ParameterCheck.mandatory("allowedValues", allowedValues);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String constraintNameAsPrefixString = constraintName.toPrefixString(namespaceService);
        
        M2Constraint customConstraint = deserializedModel.getConstraint(constraintNameAsPrefixString);
        if (customConstraint != null)
        {
            throw new AlfrescoRuntimeException("Constraint already exists: "+constraintNameAsPrefixString);
        }
        
        M2Constraint newCon = deserializedModel.createConstraint(constraintNameAsPrefixString, CUSTOM_CONSTRAINT_TYPE);
        
        newCon.setTitle(title);
        newCon.createParameter(PARAM_ALLOWED_VALUES, allowedValues);
        newCon.createParameter(PARAM_CASE_SENSITIVE, caseSensitive ? "true" : "false");
        newCon.createParameter(PARAM_MATCH_LOGIC, matchLogic.toString());
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("addCustomConstraintDefinition: "+constraintNameAsPrefixString+" (valueCnt: "+allowedValues.size()+")");
        }
    }
    
    /*
    public void addCustomConstraintDefinition(QName constraintName, String description, Map<String, Object> parameters) 
    {
        // TODO Auto-generated method stub
    }
    */
    
    public void changeCustomConstraintValues(QName constraintName, List<String> newAllowedValues)
    {
        ParameterCheck.mandatory("constraintName", constraintName);
        ParameterCheck.mandatory("newAllowedValues", newAllowedValues);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String constraintNameAsPrefixString = constraintName.toPrefixString(namespaceService);
        
        M2Constraint customConstraint = deserializedModel.getConstraint(constraintNameAsPrefixString);
        if (customConstraint == null)
        {
            throw new AlfrescoRuntimeException("Unknown constraint: "+constraintNameAsPrefixString);
        }
        
        String type = customConstraint.getType();
        if ((type == null) || (! type.equals(CUSTOM_CONSTRAINT_TYPE)))
        {
            throw new AlfrescoRuntimeException("Unexpected type '"+type+"' for constraint: "+constraintNameAsPrefixString+" (expected '"+CUSTOM_CONSTRAINT_TYPE+"')");
        }
        
        customConstraint.removeParameter(PARAM_ALLOWED_VALUES);
        customConstraint.createParameter(PARAM_ALLOWED_VALUES, newAllowedValues);
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("changeCustomConstraintValues: "+constraintNameAsPrefixString+" (valueCnt: "+newAllowedValues.size()+")");
        }
    }
    
    public void changeCustomConstraintTitle(QName constraintName, String title)
    {
        ParameterCheck.mandatory("constraintName", constraintName);
        ParameterCheck.mandatoryString("title", title);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String constraintNameAsPrefixString = constraintName.toPrefixString(namespaceService);
        
        M2Constraint customConstraint = deserializedModel.getConstraint(constraintNameAsPrefixString);
        if (customConstraint == null)
        {
            throw new AlfrescoRuntimeException("Unknown constraint: "+constraintNameAsPrefixString);
        }
        
        String type = customConstraint.getType();
        if ((type == null) || (! type.equals(CUSTOM_CONSTRAINT_TYPE)))
        {
            throw new AlfrescoRuntimeException("Unexpected type '"+type+"' for constraint: "+constraintNameAsPrefixString+" (expected '"+CUSTOM_CONSTRAINT_TYPE+"')");
        }
        
        customConstraint.setTitle(title);
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("changeCustomConstraintTitle: "+constraintNameAsPrefixString+" (title: "+title+")");
        }
    }
    
    public List<ConstraintDefinition> getCustomConstraintDefinitions(QName modelQName) 
    {
        Collection<ConstraintDefinition> conDefs = dictionaryService.getConstraints(modelQName, true);
        
        for (ConstraintDefinition conDef : conDefs)
        {
            Constraint con = conDef.getConstraint();
            if (! (con instanceof RMListOfValuesConstraint))
            {
                conDefs.remove(conDef);
            }
        }
        
        return new ArrayList<ConstraintDefinition>(conDefs);
    }
    
    public void removeCustomConstraintDefinition(QName constraintQName) 
    {
        ParameterCheck.mandatory("constraintQName", constraintQName);
        
        M2Model deserializedModel = readCustomContentModel();
        
        String constraintNameAsPrefixString = constraintQName.toPrefixString(namespaceService);
        
        M2Constraint customConstraint = deserializedModel.getConstraint(constraintNameAsPrefixString);
        if (customConstraint == null)
        {
            throw new AlfrescoRuntimeException("Constraint does not exist: "+constraintNameAsPrefixString);
        }
        
        deserializedModel.removeConstraint(constraintNameAsPrefixString);
        
        writeCustomContentModel(deserializedModel);
        
        if (logger.isInfoEnabled())
        {
            logger.info("deleteCustomConstraintDefinition: "+constraintNameAsPrefixString);
        }
    }
    
    private M2Model readCustomContentModel()
    {
        ContentReader reader = this.contentService.getReader(RM_CUSTOM_MODEL_NODE_REF,
                                                             ContentModel.TYPE_CONTENT);
        
        if (reader.exists() == false) {throw new AlfrescoRuntimeException("RM CustomModel has no content.");}
        
        InputStream contentIn = null;
        M2Model deserializedModel = null;
        try
        {
            contentIn = reader.getContentInputStream();
            deserializedModel = M2Model.createModel(contentIn);
        }
        finally
        {
            try
            {
                if (contentIn != null) contentIn.close();
            }
            catch (IOException ignored)
            {
                // Intentionally empty.`
            }
        }
        return deserializedModel;
    }
    
    private void writeCustomContentModel(M2Model deserializedModel)
    {
        ContentWriter writer = this.contentService.getWriter(RM_CUSTOM_MODEL_NODE_REF,
                                                             ContentModel.TYPE_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_XML);
        writer.setEncoding("UTF-8");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        deserializedModel.toXML(baos);
        
        String updatedModelXml;
        try
        {
            updatedModelXml = baos.toString("UTF-8");
            writer.putContent(updatedModelXml);
            // putContent closes all resources.
            // so we don't have to.
        } catch (UnsupportedEncodingException uex)
        {
            throw new AlfrescoRuntimeException("Exception when writing custom model xml.", uex);
        }
    }

    
    public QName getQNameForClientId(String localName)
    {
        //TODO 1. After certification. This implementation currently does not support reference,
        // property, constraints definitions with the same names, which is technically allowed by Alfresco.

        //TODO 2. Note the implicit assumption here that all custom references will have
        // unique titles. This is, in fact, not guaranteed.
        
        QName propertyResult = null;
        for (QName qn : getCustomPropertyDefinitions().keySet())
        {
            if (localName != null && localName.equals(qn.getLocalName()))
            {
                propertyResult = qn;
            }
        }
        
        if (propertyResult != null)
        {
            return propertyResult;
        }
        
        QName referenceResult = null;
        for (QName refQn : getCustomReferenceDefinitions().keySet())
        {
            if (localName != null && localName.equals(refQn.getLocalName()))
            {
                referenceResult = refQn;
            }
        }
        
        // TODO Handle the case where both are not null
        return referenceResult;
    }

    private QName generateQNameFor(String clientId)
    {
        if (getQNameForClientId(clientId) != null)
        {
            // TODO log it's already taken. What to do?
            throw new IllegalArgumentException("clientId already in use: " + clientId);
        }
        
        String newGUID = GUID.generate();
        QName newQName = QName.createQName(RM_CUSTOM_PREFIX, newGUID, namespaceService);
        
        return newQName;
    }
   
    public String[] splitSourceTargetId(String sourceTargetId)
    {
        if (!sourceTargetId.contains(SOURCE_TARGET_ID_SEPARATOR))
        {
            throw new IllegalArgumentException("Illegal sourceTargetId: " + sourceTargetId);
        }
        return sourceTargetId.split(SOURCE_TARGET_ID_SEPARATOR);
    }
    
    public String getCompoundIdFor(String sourceId, String targetId)
    {
        ParameterCheck.mandatoryString("sourceId", sourceId);
        ParameterCheck.mandatoryString("targetId", targetId);
        
        if (sourceId.contains(SOURCE_TARGET_ID_SEPARATOR))
        {
            throw new IllegalArgumentException("sourceId cannot contain '" + SOURCE_TARGET_ID_SEPARATOR
                    + "': " + sourceId);
        }
        StringBuilder result = new StringBuilder();
        result.append(sourceId)
            .append(SOURCE_TARGET_ID_SEPARATOR)
            .append(targetId);
        return result.toString();
    }
}
