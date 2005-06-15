package org.alfresco.web.bean.wizard;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.repository.Repository;
import org.apache.log4j.Logger;

/**
 * Handler class used by the Create In-line Content Wizard 
 * 
 * @author Kevin Roast
 */
public class CreateContentWizard extends BaseContentWizard
{
   private static Logger logger = Logger.getLogger(CreateContentWizard.class);

   // TODO: retrieve these from the config service
   private static final String WIZARD_TITLE = "Create Content Wizard";
   private static final String WIZARD_DESC = "Use this wizard to create a new document in a space.";
   private static final String STEP1_TITLE = "Step One - Enter Content";
   private static final String STEP1_DESCRIPTION = "Enter your document content into the repository.";
   private static final String STEP2_TITLE = "Step Two - Properties";
   private static final String STEP2_DESCRIPTION = "Enter information about this content.";
   
   // create content wizard specific properties
   private String content;
   
   
   /**
    * Deals with the finish button being pressed
    * 
    * @return outcome
    */
   public String finish()
   {
      String outcome = FINISH_OUTCOME;
      
      saveContent(null, this.content);
      
      return outcome;
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getWizardDescription()
    */
   public String getWizardDescription()
   {
      return WIZARD_DESC;
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getWizardTitle()
    */
   public String getWizardTitle()
   {
      return WIZARD_TITLE;
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepDescription()
    */
   public String getStepDescription()
   {
      String stepDesc = null;
      
      switch (this.currentStep)
      {
         case 1:
         {
            stepDesc = STEP1_DESCRIPTION;
            break;
         }
         case 2:
         {
            stepDesc = STEP2_DESCRIPTION;
            break;
         }
         case 3:
         {
            stepDesc = SUMMARY_DESCRIPTION;
            break;
         }
         default:
         {
            stepDesc = "";
         }
      }
      
      return stepDesc;
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepTitle()
    */
   public String getStepTitle()
   {
      String stepTitle = null;
      
      switch (this.currentStep)
      {
         case 1:
         {
            stepTitle = STEP1_TITLE;
            break;
         }
         case 2:
         {
            stepTitle = STEP2_TITLE;
            break;
         }
         case 3:
         {
            stepTitle = SUMMARY_TITLE;
            break;
         }
         default:
         {
            stepTitle = "";
         }
      }
      
      return stepTitle;
   }
   
   /**
    * @return Returns the content from the edited form.
    */
   public String getContent()
   {
      return this.content;
   }
   
   /**
    * @param content The content to edit (should be clear initially)
    */
   public void setContent(String content)
   {
      this.content = content;
   }
   
   /**
    * Initialises the wizard
    */
   public void init()
   {
      super.init();
      
      this.content = null;
      this.fileName = "newfile.html";
      this.contentType = Repository.getMimeTypeForFileName(
               FacesContext.getCurrentInstance(), this.fileName);
      this.title = this.fileName;
   }
   
   /**
    * @return Returns the summary data for the wizard.
    */
   public String getSummary()
   {
      // TODO: show first few lines of content here?
      return buildSummary(
            new String[] {"File Name", "Content Type", "Title", "Description", "Author"},
            new String[] {this.fileName, getSummaryContentType(), this.title, this.description, this.author});
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#determineOutcomeForStep(int)
    */
   protected String determineOutcomeForStep(int step)
   {
      String outcome = null;
      
      switch(step)
      {
         case 1:
         {
            outcome = "create";
            break;
         }
         case 2:
         {
            outcome = "properties";
            break;
         }
         case 3:
         {
            outcome = "summary";
            break;
         }
         default:
         {
            outcome = CANCEL_OUTCOME;
         }
      }
      
      return outcome;
   }
}
