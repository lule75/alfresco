/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing
 */

/**
 * RulesList component.
 *
 * @namespace Alfresco
 * @class Alfresco.RulesList
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event,
      Selector = YAHOO.util.Selector;

   /**
    * RulesList constructor.
    *
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.RulesList} The new RulesList instance
    * @constructor
    */
   Alfresco.RulesList = function RulesList_constructor(htmlId)
   {
      Alfresco.RulesList.superclass.constructor.call(this, "Alfresco.RulesList", htmlId, []);

      // Decoupled event listeners
      YAHOO.Bubbling.on("ruleSelected", this.onRuleSelected, this);
      YAHOO.Bubbling.on("folderDetailsAvailable", this.onFolderDetailsAvailable, this);     
      YAHOO.Bubbling.on("folderRulesDetailsAvailable", this.onFolderRulesDetailsAvailable, this);

      return this;
   };

   YAHOO.extend(Alfresco.RulesList, Alfresco.component.Base,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * nodeRef of folder who's rules are being viewed
          *
          * @property nodeRef
          * @type string
          */
         nodeRef: null,

         /**
          * Current siteId.
          *
          * @property siteId
          * @type string
          */
         siteId: "",
         
         /**
          * The filter of the rule list.
          * Allowed values are: "all", "folder" or "inherited"
          *
          * @property filter
          * @type string
          */
         filter: "all",


         /**
          * The filter of the rule list.
          * Allowed values are: "all", "folder" or "inherited"
          *
          * @property selectDefault
          * @type boolean
          */
         selectDefault: false
      },

      /**
       * Flag set after component is instantiated.
       *
       * @property isReady
       * @type boolean
       */
      isReady: false,

      /**
       * Folder from page load.
       *
       * @property rules
       * @type {object}
       */
      folder: null,

      /**
       * Rules on page load.
       *
       * @property rules
       * @type {array}
       */
      rules: null,

      /**
       * Fired by YUI when parent element is available for scripting.
       * Template initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function RulesList_onReady()
      {
         // Get the html elements from the Dom
         this.widgets.rulesListText = Dom.get(this.id + "-rulesListText");
         this.widgets.rulesListBarText = Dom.get(this.id + "-rulesListBarText");
         this.widgets.rulesListContainerEl = Dom.get(this.id + "-rulesListContainer");
         this.widgets.ruleTemplateEl = Dom.get(this.id + "-ruleTemplate");

         Dom.addClass(this.widgets.rulesListContainerEl, this.options.filter);
         
         // Render rules if the info have been given (from an external event)
         this.isReady = true;
         if (this.rules !== null)
         {
            this._renderRules(this.rules);
         }
         if (this.folder !== null)
         {
            this._renderText();
         }
      },

      /**
       * Event handler called when the "ruleSelected" event is received
       *
       * @method onRuleSelected
       * @param layer
       * @param args
       */
      onRuleSelected: function RulesHeader_onRuleSelected(layer, args)
      {
         var nodeRef= args[1].ruleDetails.nodeRef;
         if (!Selector.query('input[name=nodeRef][value=' + nodeRef + ']', this.widgets.rulesListContainerEl, true))
         {
            Alfresco.util.setSelectedClass(this.widgets.rulesListContainerEl);
         }
      },

      /**
       * Event handler called when the "folderDetailsAvailable" event is received
       *
       * @method onFolderDetailsAvailable
       * @param layer
       * @param args
       */
      onFolderDetailsAvailable: function RulesHeader_onFolderDetailsAvailable(layer, args)
      {
         // Defer if event received before we're ready
         this.folder = args[1].folderDetails;
         if (this.isReady)
         {
            this._renderText();
         }
      },


      /**
       * Event handler called when the "folderFulesDetailsAvailable" event is received
       *
       * @method onFolderRulesDetailsAvailable
       * @param layer
       * @param args
       */
      onFolderRulesDetailsAvailable: function RulesList_onFolderRulesDetailsAvailable(layer, args)
      {
         var folderRulesData = args[1].folderRulesDetails;

         // Defer if event received before we're ready
         this.rules = folderRulesData.rules ? folderRulesData.rules : [];
         if (this.isReady)
         {
            this._renderRules(this.rules);
         }
      },


      /**
       * Renders the text above the rules
       *
       * @method _renderText
       * @private
       */
      _renderText: function RulesList__renderText()
      {
         // Set info/message bar
         if (this.options.filter == "inherited")
         {
            this.widgets.rulesListText.innerHTML = this.msg("label.inheritedRules");
            this.widgets.rulesListBarText.innerHTML = this.msg("info.inheritedRulesRunOrder");
         }
         else if (this.options.filter == "folder")
         {
            this.widgets.rulesListText.innerHTML = this.msg("label.folderRules", this.folder.fileName);
            this.widgets.rulesListBarText.innerHTML = this.msg("info.folderRulesRunOrder");
         }
         else if (this.options.filter == "all")
         {
            this.widgets.rulesListText.innerHTML = this.msg("label.allRules", this.folder.fileName);
            // todo check if any folders are linking to this folders rule set
            if (false)
            {
               this.widgets.rulesListBarText.innerHTML = this.msg("info.folderRulesRunOrder");
            }
         }
      },

      /**
       * Renders the rules
       *
       * @method _renderRules
       * @param rules {Array} of rule objects
       * @private
       */
      _renderRules: function RulesList__renderRules(rules)
      {
         var rule,
            ruleEl,
            counter = 0;

         // Remove all rules
         while(this.widgets.rulesListContainerEl.hasChildNodes())
         {
            this.widgets.rulesListContainerEl.removeChild(this.widgets.rulesListContainerEl.firstChild);
         }

         // Render rules
         for (var i = 0, ii = rules.length; i < ii; i++)
         {
            rule = rules[i];
            rule.index = i;
            if ((this.options.filter == "inherited" && rule.inheritedFolder) ||
                (this.options.filter == "folder" && !rule.inheritedFolder) ||
                this.options.filter == "all")
            {
               ruleEl = this._createRule(rule);
               ruleEl = this.widgets.rulesListContainerEl.appendChild(ruleEl);
               counter++;
            }

            // Select the first rule as default
            if (counter == 1 && this.options.selectDefault)
            {
               this.onRuleClick(null,
               {
                  rule: rule,
                  ruleEl: ruleEl
               });
            }
         }
         
         // Display message that no rules exist
         if (counter == 0)
         {
            var noRulesDiv = document.createElement("li");
            Dom.addClass(noRulesDiv, "message");
            noRulesDiv.innerHTML = this.msg("message.noRules");
            this.widgets.rulesListContainerEl.appendChild(noRulesDiv);
         }
      },
      

      /**
       * Create a rule in the list
       *
       * @method _createRule
       * @param rule The rule info object
       * @private
       */
      _createRule: function RulesList__createRule(rule)
      {
         // Clone template
         var ruleEl = this.widgets.ruleTemplateEl.cloneNode(true);

         // Rule Id for later submit of reordering
         Dom.getElementsByClassName("nodeRef", "input", ruleEl)[0].value = rule.nodeRef;

         // Display rest of values
         Dom.getElementsByClassName("no", "div", ruleEl)[0].innerHTML = rule.index + 1;
         Dom.getElementsByClassName("title", "span", ruleEl)[0].innerHTML = rule.title;
         Dom.getElementsByClassName("description", "span", ruleEl)[0].innerHTML = rule.description;

         if (rule.active != true)
         {
            Dom.addClass(Dom.getElementsByClassName("active-icon", "div", ruleEl)[0], "inactive-icon");
         }
         if (rule.inheritedFolder)
         {
            Dom.getElementsByClassName("inherited", "span", ruleEl)[0].innerHTML = this.msg("label.inheritedShort");
            Dom.getElementsByClassName("inherited-from", "span", ruleEl)[0].innerHTML = this.msg("label.inheritedFrom");
            
            var a = Dom.getElementsByClassName("inherited-folder", "a", ruleEl)[0];
            a.href = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "page/site/{siteId}/folder-rules?nodeRef={nodeRef}",
            {
               siteId: this.options.siteId,
               nodeRef: rule.inheritedFolder.nodeRef.replace(":/", "")
            });
            a.innerHTML = this.msg("label.inheritedFolder", rule.inheritedFolder.name);            
         }

         // Add listener to clicks on the rule
         Event.addListener(ruleEl, "click", this.onRuleClick,
         {
            rule: rule,
            ruleEl: ruleEl
         }, this);

         return ruleEl;
      },


      /**
       * Called when user clicks on a rule
       *
       * @method onRuleClick
       * @param e click event object
       * @param obj callback object containg rule info & HTMLElements
       */
      onRuleClick: function DispositionEdit_onRuleClick(e, obj)
      {
         Alfresco.util.setSelectedClass(obj.ruleEl.parentNode, obj.ruleEl);

         // Fire event to inform any listening components that the data is ready
         YAHOO.Bubbling.fire("ruleSelected",
         {
            ruleDetails: obj.rule
         });
      }

   });
})();