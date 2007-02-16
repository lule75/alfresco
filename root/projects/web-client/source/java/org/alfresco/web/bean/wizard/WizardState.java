/*
 * Copyright (C) 2005 Alfresco, Inc.
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
package org.alfresco.web.bean.wizard;

import java.util.List;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.config.WizardsConfigElement.PageConfig;
import org.alfresco.web.config.WizardsConfigElement.StepConfig;
import org.alfresco.web.config.WizardsConfigElement.WizardConfig;

/**
 * Object responsible for holding the current state of an active wizard.
 * 
 * @author gavinc
 */
public final class WizardState
{
   private int currentStep = 1;
   private PageConfig currentPageCfg;
   private WizardConfig config;
   private IWizardBean wizard;
   private List<StepConfig> steps;
   
   /**
    * Default constructor
    * 
    * @param config The configuration for the wizard
    * @param wizard The wizard bean instance
    */
   public WizardState(WizardConfig config, IWizardBean wizard)
   {
      this.config = config;
      this.wizard = wizard;
      
      this.steps = this.config.getStepsAsList();
   }

   /**
    * Sets the configuration for the current page of the wizard
    * 
    * @param currentPageCfg The configuration
    */
   public void setCurrentPageCfg(PageConfig currentPageCfg)
   {
      this.currentPageCfg = currentPageCfg;
   }
   
   /**
    * Sets the current step the wizard is on
    * 
    * @param currentStep The current step number
    */
   public void setCurrentStep(int currentStep)
   {
      this.currentStep = currentStep;
   }
   
   /**
    * Returns the wizard bean instance
    * 
    * @return The wizard bean instance
    */
   public IWizardBean getWizard()
   {
      return this.wizard;
   }

   /**
    * Returns the configuration for the current wizard
    * 
    * @return The wizard configuration
    */
   public WizardConfig getConfig()
   {
      return this.config;
   }
   
   /**
    * Returns the configuration for the current page of the wizard
    * 
    * @return The current page configuration
    */
   public PageConfig getCurrentPageCfg()
   {
      return currentPageCfg;
   }

   /**
    * The current step the wizard is on
    * 
    * @return The current wizard step
    */
   public int getCurrentStep()
   {
      return currentStep;
   }

   /**
    * Returns the list of steps the wizard has
    * 
    * @return List of wizard steps
    */
   public List<StepConfig> getSteps()
   {
      return steps;
   }
   
   @Override
   public String toString()
   {
      return AlfrescoNavigationHandler.WIZARD_PREFIX + this.config.getName() + 
            "[" + this.currentStep + "]";
   }
}
