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
package org.alfresco.web.ui.repo.component.property;

import java.io.IOException;
import java.text.MessageFormat;

import javax.faces.FacesException;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.DataDictionary;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * Component to represent an individual child association within a property sheet
 * 
 * @author gavinc
 */
public class UIChildAssociation extends PropertySheetItem
{
   private static final String MSG_ERROR_CHILD_ASSOC = "error_child_association";
   private static final String MSG_ERROR_NOT_CHILD_ASSOC = "error_not_child_association";

   private static Log logger = LogFactory.getLog(UIChildAssociation.class);
   
   /**
    * Default constructor
    */
   public UIChildAssociation()
   {
      // set the default renderer
      setRendererType("org.alfresco.faces.ChildAssociationRenderer");
   }
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.ChildAssociation";
   }

   /**
    * @see org.alfresco.web.ui.repo.component.property.PropertySheetItem#getIncorrectParentMsg()
    */
   protected String getIncorrectParentMsg()
   {
      return "The childAssociation component must be nested within a property sheet component";
   }
   
   /**
    * @see org.alfresco.web.ui.repo.component.property.PropertySheetItem#generateItem(javax.faces.context.FacesContext, org.alfresco.web.bean.repository.Node, java.lang.String)
    */
   protected void generateItem(FacesContext context, Node node, String var) throws IOException
   {
      String associationName = (String)getName();

      // get details of the association
      DataDictionary dd = (DataDictionary)FacesContextUtils.getRequiredWebApplicationContext(
            context).getBean(Application.BEAN_DATA_DICTIONARY);
      AssociationDefinition assocDef = dd.getAssociationDefinition(node, associationName);
      
      if (assocDef == null)
      {
         logger.warn("Failed to find child association definition for association '" + associationName + "'");
         
         // add an error message as the property is not defined in the data dictionary
         String msg = MessageFormat.format(Application.getMessage(context, MSG_ERROR_CHILD_ASSOC), new Object[] {associationName});
         Utils.addErrorMessage(msg);
      }
      else
      {
         // we've found the association definition but we also need to check
         // that the association is a parent child one
         if (assocDef.isChild() == false)
         {
            String msg = MessageFormat.format(Application.getMessage(context, MSG_ERROR_NOT_CHILD_ASSOC), new Object[] {associationName});
            Utils.addErrorMessage(msg);
         }
         else
         {
            String displayLabel = (String)getDisplayLabel();
            if (displayLabel == null)
            {
               // try and get the repository assigned label
               displayLabel = assocDef.getTitle();
               
               // if the label is still null default to the local name of the property
               if (displayLabel == null)
               {
                  displayLabel = assocDef.getName().getLocalName();
               }
            }
            
            // generate the label and type specific control
            generateLabel(context, displayLabel);
            generateControl(context, assocDef, var);
         }
      }
   }
   
   /**
    * Generates an appropriate control for the given property
    * 
    * @param context JSF context
    * @param propDef The definition of the association to create the control for
    * @param varName Name of the variable the node is stored in the session as 
    *                (used for value binding expression)
    * @param parent The parent component for the control
    */
   private void generateControl(FacesContext context, AssociationDefinition assocDef, 
                                String varName)
   {
      UIPropertySheet propSheet = (UIPropertySheet)this.getParent();

      if (propSheet.getMode().equalsIgnoreCase(UIPropertySheet.VIEW_MODE) || isReadOnly() || assocDef.isProtected())
      {
         ValueBinding vb = context.getApplication().
                        createValueBinding("#{" + varName + ".childAssociations[\"" + 
                        assocDef.getName().toString() + "\"]}");
         
         // if we are in view mode simply output the text to the screen
         UIOutput control = (UIOutput)context.getApplication().createComponent("javax.faces.Output");
         control.setRendererType("javax.faces.Text");
         
         // if a converter has been specified we need to instantiate it
         // and apply it to the control otherwise add the standard one
         if (getConverter() == null)
         {
            // add the standard ChildAssociation converter that shows the current association state
            Converter conv = context.getApplication().createConverter("org.alfresco.faces.ChildAssociationConverter");
            control.setConverter(conv);
         }
         else
         {
            // catch null pointer exception to workaround bug in myfaces
            try
            {
               Converter conv = context.getApplication().createConverter(getConverter());
               ((UIOutput)control).setConverter(conv);
            }
            catch (FacesException fe)
            {
               logger.warn("Converter " + getConverter() + " could not be applied");
            }
         }
         
         // set up the common aspects of the control
         control.setId(context.getViewRoot().createUniqueId());
         control.setValueBinding("value", vb);
         
         // add the control itself
         this.getChildren().add(control);
      }
      else
      {
         ValueBinding vb = context.getApplication().createValueBinding("#{" + varName + "}");
         
         UIChildAssociationEditor control = (UIChildAssociationEditor)context.
            getApplication().createComponent("org.alfresco.faces.ChildAssociationEditor");
         control.setAssociationName(assocDef.getName().toString());
         
         // set up the common aspects of the control
         control.setId(context.getViewRoot().createUniqueId());
         control.setValueBinding("value", vb);
         
         // add the control itself
         this.getChildren().add(control);
      }
   }
}
