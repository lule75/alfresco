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
package org.alfresco.repo.audit.model;

import org.alfresco.repo.audit.AuditMode;
import org.alfresco.repo.audit.AuditModel;
import org.alfresco.repo.audit.PublicServiceIdentifier;
import org.alfresco.repo.audit.RecordOptions;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

public abstract class AbstractAuditEntry
{
    private static Log s_logger = LogFactory.getLog(AbstractAuditEntry.class);

    private RecordOptionsImpl recordOptions = null;

    private AbstractFilter filter = null;

    private AuditMode auditMode = AuditMode.UNSET;

    private TrueFalseUnset enabled = TrueFalseUnset.UNSET;

    private TrueFalseUnset auditInternal = TrueFalseUnset.UNSET;

    private AbstractAuditEntry parent;

    private PublicServiceIdentifier publicServiceIdentifier;

    public AbstractAuditEntry()
    {
        super();
    }

    PublicServiceIdentifier getPublicServiceIdentifier()
    {
        return publicServiceIdentifier;
    }

    public void setPublicServiceIdentifier(PublicServiceIdentifier publicServiceIdentifier)
    {
        this.publicServiceIdentifier = publicServiceIdentifier;
    }

    void configure(AbstractAuditEntry parent, Element element, NamespacePrefixResolver namespacePrefixResolver)
    {
        this.parent = parent;

        Attribute auditModeAttribute = element.attribute(AuditModel.AT_MODE);
        if (auditModeAttribute != null)
        {
            auditMode = AuditMode.getAuditMode(auditModeAttribute.getValue());
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Audit Mode = "+auditMode);
        }
        

        Attribute enabledAttribute = element.attribute(AuditModel.AT_ENABLED);
        if (enabledAttribute != null)
        {
            enabled = TrueFalseUnset.getTrueFalseUnset(enabledAttribute.getValue());
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Enabled = "+enabled);
        }

        Attribute auditInternalAttribute = element.attribute(AuditModel.AT_AUDIT_INTERNAL);
        if (auditInternalAttribute != null)
        {
            auditInternal = TrueFalseUnset.getTrueFalseUnset(auditInternalAttribute.getValue());
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Audit Internal = "+auditInternal);
        }

        // Make record options
        Element recordOptionElement = element.element(AuditModel.EL_RECORD_OPTIONS);
        if (recordOptionElement != null)
        {
            recordOptions = new RecordOptionsImpl();
            recordOptions.configure(recordOptionElement, namespacePrefixResolver);
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Record Options = "+recordOptions);
        }

        // Make filters
        Element filterElement = element.element(AuditModel.EL_FILTER);
        if (filterElement != null)
        {
            filter = AbstractFilter.createFilter(filterElement, namespacePrefixResolver);
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Filter = "+filter);
        }

    }

    /* package */TrueFalseUnset getAuditInternal()
    {
        return auditInternal;
    }

    /* package */AuditMode getAuditMode()
    {
        return auditMode;
    }

    public TrueFalseUnset getEnabled()
    {
        return enabled;
    }

    /* package */AbstractFilter getFilter()
    {
        return filter;
    }

    /* package */AbstractAuditEntry getParent()
    {
        return parent;
    }

    /* package */RecordOptionsImpl getRecordOptions()
    {
        return recordOptions;
    }
    
    
    protected TrueFalseUnset getEffectiveAuditInternal()
    {
        TrueFalseUnset auditInternal;
        if (checkEnabled() == TrueFalseUnset.TRUE)
        {
            auditInternal = getAuditInternalOrParentAuditInternal();
        }
        else
        {
            auditInternal = TrueFalseUnset.FALSE;
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("... Effective audit internal is = "+auditInternal);
        }
        return auditInternal;
    }

    private TrueFalseUnset getAuditInternalOrParentAuditInternal()
    {
        TrueFalseUnset auditInternal = getAuditInternal();
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("... ...  audit internal is = "+auditInternal);
        }
        if (auditInternal == TrueFalseUnset.UNSET)
        {
            if (getParent() == null)
            {
                return TrueFalseUnset.UNSET;
            }
            else
            {
                return getParent().getAuditInternalOrParentAuditInternal();
            }
        }
        else
        {
            return auditInternal;
        }
    }

    protected AuditMode getEffectiveAuditMode()
    {
        AuditMode auditMode;
        if (checkEnabled() == TrueFalseUnset.TRUE)
        {
            auditMode = getAuditModeOrParentAuditMode();
        }
        else
        {
            auditMode = AuditMode.NONE;
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("... Effective audit mode is = "+auditMode);
        }
        return auditMode;
    }

    private AuditMode getAuditModeOrParentAuditMode()
    {
        AuditMode auditMode = getAuditMode();
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("... ...  audit mode is = "+auditMode);
        }
        if (auditMode == AuditMode.UNSET)
        {
            if (getParent() == null)
            {
                return AuditMode.UNSET;
            }
            else
            {
                return getParent().getAuditModeOrParentAuditMode();
            }
        }
        else
        {
            return auditMode;
        }

    }

    protected RecordOptions getEffectiveRecordOptions()
    {
        RecordOptions recordOptions;
        if (checkEnabled() == TrueFalseUnset.TRUE)
        {
            recordOptions = getRecordOptionsOrParentRecordOptions();
        }
        else
        {
            recordOptions = null;
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("...Record Options = "+recordOptions);
        }
        return recordOptions;
    }
    
    private RecordOptions getRecordOptionsOrParentRecordOptions()
    {
        RecordOptions recordOptions = getRecordOptions();
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("... ...  record options = "+recordOptions);
        }
        if (recordOptions == null)
        {
            if (getParent() == null)
            {
                return null;
            }
            else
            {
                return getParent().getRecordOptionsOrParentRecordOptions();
            }
        }
        else
        {
            return recordOptions;
        }

    }

    
    
    private TrueFalseUnset checkEnabled()
    {
        TrueFalseUnset effective = getEnabled();
        if (getParent() != null)
        {
            if ((getParent().checkEnabled() == TrueFalseUnset.TRUE) && (effective != TrueFalseUnset.FALSE))
            {
                return TrueFalseUnset.TRUE;
            }
        }
        else
        {
            if (effective == TrueFalseUnset.TRUE)
            {
                return TrueFalseUnset.TRUE;
            }
        }
        return TrueFalseUnset.FALSE;
    }
    
}
