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
 * RecordsReferences tool component.
 *
 * @namespace Alfresco
 * @class Alfresco.RecordsReferences
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
       Event = YAHOO.util.Event,
       Element = YAHOO.util.Element;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
    * RecordsReferences constructor.
    *
    * @param {String} htmlId The HTML id �of the parent element
    * @return {Alfresco.RecordsReferences} The new RecordsReferences instance
    * @constructor
    */
   Alfresco.RecordsReferences = function(htmlId)
   {
      this.name = "Alfresco.RecordsReferences";
      Alfresco.RecordsReferences.superclass.constructor.call(this, htmlId);

      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "json", "history"], this.onComponentsLoaded, this);

      /* Decoupled event listeners */
      YAHOO.Bubbling.on("viewReferences", this.onViewReferences, this);
      YAHOO.Bubbling.on("createReference", this.onCreateReference, this);
      YAHOO.Bubbling.on("editReference", this.onEditReference, this);

      /* Define panel handlers */
      var parent = this;

      // NOTE: the panel registered first is considered the "default" view and is displayed first

      /* View Panel Handler */
      ViewPanelHandler = function ViewPanelHandler_constructor()
      {
         // Initialise prototype properties
         ViewPanelHandler.superclass.constructor.call(this, "view");
      };

      YAHOO.extend(ViewPanelHandler, Alfresco.ConsolePanelHandler,
      {

         /**
          /**
           * Remembers if the panel data has been loaded once
           *
           * @property initialDataLoaded
           * @type boolean
          */
         initialDataLoaded: false,

         /**
          * onLoad ConsolePanel event handler
          *
          * @method onLoad
          */
         onLoad: function ViewPanelHandler_onLoad()
         {
            // widgets
            this.widgets.newReferenceButton = Alfresco.util.createYUIButton(this, "newreference-button", this.onNewReferenceClick, {}, parent.id + "-newreference-button");

            // Setup data table
            this._setupDataSource();
            this._setupDataTable();
         },

         /**
          * Setup the datasource for the data table
          *
          * @method _setupDataSource
          * @private
          */
         _setupDataSource: function ViewPanelHandler__setupDataSource()
         {
            // DataSource definition
            var uriSearchResults = Alfresco.constants.PROXY_URI + "api/rma/admin/customreferencedefinitions";
            this.widgets.dataSource = new YAHOO.util.DataSource(uriSearchResults);
            this.widgets.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
            this.widgets.dataSource.connXhrMode = "queueRequests";
            this.widgets.dataSource.responseSchema =
            {
                resultsList: "data.customReferences",
                fields: ["referenceType", "source", "target", "label"] // "name",
            };
            this.widgets.dataSource.doBeforeParseData = function RecordsReferences_doBeforeParseData(oRequest , oFullResponse)
            {
               if (oFullResponse && oFullResponse.data && oFullResponse.data.customReferences)
               {
                  var items = oFullResponse.data.customReferences;

                  // Sort the referecens by their title
                  items.sort(function (reference1, reference2)
                  {
                     return (reference1.source > reference2.source) ? 1 : (reference1.source < reference2.source) ? -1 : 0;
                  });

                  // we need to wrap the array inside a JSON object so the DataTable is happy
                  return {
                     data:
                     {
                        customReferences: items
                     }
                  };
               }
               return oFullResponse;
            };
         },

         /**
          * Setup the datatable
          *
          * @method _setupDataTable
          * @private
          */
         _setupDataTable: function ViewPanelHandler__setupDataTable()
         {
            var me = this;

            /**
             * Icon custom datacell formatter
             *
             * @method renderCellIcon
             * @param elCell {object}
             * @param oRecord {object}
             * @param oColumn {object}
             * @param oData {object|string}
             */
            renderCellIcon = function ViewPanelHandler__setupDataTable_renderCellIcon(elCell, oRecord, oColumn, oData)
            {
               elCell.innerHTML = "&nbsp;";
            };

            /**
             * Description/detail custom datacell formatter
             *
             * @method renderCellReference
             * @param elCell {object}
             * @param oRecord {object}
             * @param oColumn {object}
             * @param oData {object|string}
             */
            renderCellReference = function ViewPanelHandler__setupDataTable_renderCellReference(elCell, oRecord, oColumn, oData)
            {
               var title = document.createElement("h4");
               var info = document.createElement("div");

               var typeLabel = document.createElement("span");
               Dom.addClass(typeLabel, "reference-type-label");
               typeLabel.appendChild(document.createTextNode(parent.msg("label.type") + ":"));
               info.appendChild(typeLabel);

               var typeValue = document.createElement("span");
               info.appendChild(typeValue);

               if (oRecord.getData("referenceType") == "parentchild")
               {
                  title.appendChild(document.createTextNode(parent.msg("label.title.parentchild", oRecord.getData("source"), oRecord.getData("target"))));
                  typeValue.appendChild(document.createTextNode(parent.msg("label.parentchild")));
               }
               else if (oRecord.getData("referenceType") == "bidirectional")
               {
                  title.appendChild(document.createTextNode(oRecord.getData("label")));
                  typeValue.appendChild(document.createTextNode(parent.msg("label.bidirectional")));
               }
               elCell.appendChild(title);
               elCell.appendChild(info);
            };

            /**
             * Actions custom datacell formatter
             *
             * @method renderCellActions
             * @param elCell {object}
             * @param oRecord {object}
             * @param oColumn {object}
             * @param oData {object|string}
             */
            renderCellActions = function ViewPanelHandler__setupDataTable_renderCellActions(elCell, oRecord, oColumn, oData)
            {
               /*
               var editBtn = new YAHOO.widget.Button(
               {
                  container: elCell,
                  label: parent.msg("button.edit")
               });
               editBtn.on("click", me.onEditReferenceClick, oRecord, me);

               var deleteBtn = new YAHOO.widget.Button(
               {
                  container: elCell,
                  label: parent.msg("button.delete")
               });
               deleteBtn.on("click", me.onDeleteReferenceClick, oRecord, me);
               */
            };

            // DataTable column defintions
            var columnDefinitions =
                  [
                     {
                        key: "icon", label: "Icon", sortable: false, formatter: renderCellIcon
                     },
                     {
                        key: "reference", label: "Reference", sortable: false, formatter: renderCellReference
                     },
                     {
                        key: "actions", label: "Actions", formatter: renderCellActions
                     }
                  ];

            // DataTable definition
            this.widgets.dataTable = new YAHOO.widget.DataTable(parent.id + "-references", columnDefinitions, this.widgets.dataSource,
            {
               renderLoopSize: 32,
               initialLoad: false,
               MSG_EMPTY: parent.msg("message.loading")
            });

            // Override abstract function within DataTable to set custom error message
            this.widgets.dataTable.doBeforeLoadData = function RecordsReferences_doBeforeLoadData(sRequest, oResponse, oPayload)
            {
               if (oResponse.error)
               {
                  try
                  {
                     var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                     this.widgets.dataTable.set("MSG_ERROR", response.message);
                  }
                  catch(e)
                  {
                     me._setDefaultDataTableErrors(me.widgets.dataTable);
                  }
               }
               else if (oResponse.results)
               {
                  if (oResponse.results.length === 0)
                  {
                     me.widgets.dataTable.set("MSG_EMPTY", '<span style="white-space: nowrap;">' + parent.msg("message.empty") + '</span>');
                  }
                  me.renderLoopSize = oResponse.results.length >> (YAHOO.env.ua.gecko === 1.8) ? 3 : 5;
               }

               // Must return true to have the "Searching..." message replaced by the error message
               return true;
            };
         },

         /**
          * Load the references
          *
          * @method _loadReferences
          * @private
          */
         _loadReferences: function ViewPanelHandler__loadReferences()
         {

            // Reset the custom error messages
            this._setDefaultDataTableErrors(this.widgets.dataTable);

            // empty results table
            this.widgets.dataTable.deleteRows(0, this.widgets.dataTable.getRecordSet().getLength());

            var successHandler = function ViewPanelHandler__loadReferences_successHandler(sRequest, oResponse, oPayload)
            {
               // Stop list from getting refreshed when not needed
               this.initialDataLoaded = true;

               // Display new data
               this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
            };

            var failureHandler = function ViewPanelHandler__loadReferences_failureHandler(sRequest, oResponse)
            {
               if (oResponse.status == 401)
               {
                  // Our session has likely timed-out, so refresh to offer the login page
                  window.location.reload();
               }
               else
               {
                  try
                  {
                     var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                     this.widgets.dataTable.set("MSG_ERROR", response.message);
                     this.widgets.dataTable.showTableMessage(response.message, YAHOO.widget.DataTable.CLASS_ERROR);
                  }
                  catch(e)
                  {
                     this._setDefaultDataTableErrors(this.widgets.dataTable);
                  }
               }
            };

            this.widgets.dataSource.sendRequest("",
            {
               success: successHandler,
               failure: failureHandler,
               scope: this
            });
         },

         /**
          * Resets the YUI DataTable errors to our custom messages
          *
          * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
          *
          * @method _setDefaultDataTableErrors
          * @param dataTable {object} Instance of the DataTable
          */
         _setDefaultDataTableErrors: function ViewPanelHandler__setDefaultDataTableErrors(dataTable)
         {
            dataTable.set("MSG_EMPTY", parent.msg("message.empty"));
            dataTable.set("MSG_ERROR", parent.msg("message.error"));
         },

         /**
          * Edit Reference button click handler
          *
          * @method onEditReferenceClick
          * @param e {object} DomEvent
          * @param oRecord {object} Object passed back from addListener method
          */
         onEditReferenceClick: function ViewPanelHandler_onEditReferenceClick(e, oRecord)
         {
            // update the current property context
            var id;
            if (oRecord.getData("referenceType") == "bidirectional")
            {
               id = oRecord.getData("label");
            }
            else if (oRecord.getData("referenceType") == "parentchild")
            {
               id = oRecord.getData("source") + "__" + oRecord.getData("target");
            }

            YAHOO.Bubbling.fire('editReference',
            {
               referenceName: id
            });
         },

         /**
          * Delete Reference button click handler.
          * Displays a confirmation dialog before the deletion is made.
          *
          * @method onDeleteReferenceClick
          * @param e {object} DomEvent
          * @param oRecord {object} Object passed back from addListener method
          */
         onDeleteReferenceClick: function ViewPanelHandler_onDeleteReferenceClick(e, oRecord)
         {
            var me = this;
            var text;
            if (oRecord.getData("referenceType") == "parentchild")
            {
               text = parent.msg("message.confirm.removereference.text.parentchild", oRecord.getData("source"), oRecord.getData("target"));
            }
            else
            {
               text = parent.msg("message.confirm.removereference.text", oRecord.getData("label"));
            }
            Alfresco.util.PopupManager.displayPrompt(
            {
               title: parent.msg("message.confirm.removereference.title"),
               text: text,
               buttons: [
                  {
                     text: parent.msg("button.yes"),
                     handler: function ViewPanelHandler_onDeleteReferenceClick_confirmYes()
                     {
                        this.destroy();
                        me._removeReference.call(me, oRecord);
                     }
                  },
                  {
                     text: parent.msg("button.no"),
                     handler: function ViewPanelHandler_onDeleteReferenceClick_confirmNo()
                     {
                        this.destroy();
                     },
                     isDefault: true
                  }]
            });
         },

         /**
          * Remove the reference
          *
          * @mthod _removeReference
          * @param oRecord {object} The reference
          */
         _removeReference: function ViewPanelHandler__removeReference(oRecord)
         {
            Alfresco.util.Ajax.jsonDelete(
            {
               url: Alfresco.constants.PROXY_URI + "api/rma/" + oRecord.getData("name"),
               successCallback:
               {
                  fn: function(o)
                  {
                     // Reload the references
                     this._loadReferences();

                     // Display success message
                     Alfresco.util.PopupManager.displayMessage(
                     {
                        text: parent.msg("message.removereference-success")
                     });
                  },
                  scope: this
               },
               failureMessage: parent.msg("message.removereference-failure")
            });
         },

         /**
          * onUpdate ConsolePanel event handler
          *
          * @method onUpdate
          */
         onUpdate: function onUpdate()
         {
            if (!this.initialDataLoaded || parent.refresh)
            {
               this._loadReferences();
            }
         },

         /**
          * New reference button click handler
          *
          * @method onNewReferenceClick
          * @param e {object} DomEvent
          * @param obj {object} Object passed back from addListener method
          */
         onNewReferenceClick: function ViewPanelHandler_onNewReferenceClick(e, obj)
         {
            // Send avenet so the create panel will be displayed
            YAHOO.Bubbling.fire('createReference', {});
         }
      });
      new ViewPanelHandler();

      /* Edit Metadata Panel Handler */
      EditPanelHandler = function EditPanelHandler_constructor()
      {
         // Initialise prototype properties

         EditPanelHandler.superclass.constructor.call(this, "edit");
      };

      YAHOO.extend(EditPanelHandler, Alfresco.ConsolePanelHandler,
      {
         /**
          * onLoad ConsolePanel event handler
          *
          * @method onLoad
          */
         onLoad: function EditPanelHandler_onLoad()
         {
            // Buttons
            this.widgets.saveButton = Alfresco.util.createYUIButton(parent, "save-button", null, {
               type: "submit"
            });
            this.widgets.cancelButton = Alfresco.util.createYUIButton(parent, "cancel-button", this.onCancelButtonClick);

            // Checkbox listeners
            var biDirectionalCheckBox = Dom.get(parent.id + "-type-bidirectional");
            Event.addListener(biDirectionalCheckBox, "click", this._disableTextInputs, true, this);
            var parentChildCheckBox = Dom.get(parent.id + "-type-parentchild");
            Event.addListener(parentChildCheckBox, "click", this._disableTextInputs, true, this);

            // Form definition
            var form = new Alfresco.forms.Form(parent.id + "-edit-form");
            form.setSubmitElements(this.widgets.saveButton);
            form.setShowSubmitStateDynamically(true);

            // Form field validation
            form.addValidation(parent.id + "-bidirectional-label", Alfresco.forms.validation.mandatory, null, "keyup");
            form.addValidation(parent.id + "-parentchild-source", Alfresco.forms.validation.mandatory, null, "keyup");
            form.addValidation(parent.id + "-parentchild-target", Alfresco.forms.validation.mandatory, null, "keyup");

            form.doBeforeFormSubmit =
            {
               fn: function(formEl)
               {
                  // Disable buttons during submit
                  this.widgets.saveButton.set("disabled", true);
                  this.widgets.cancelButton.set("disabled", true);

                  // Display a pengding message
                  this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage(
                  {
                     text: parent.msg("message.savingReference"),
                     spanClass: "wait",
                     displayTime: 0
                  });
               },
               scope: this
            };

            // Submit as an ajax submit (not leave the page), in json format
            form.setAJAXSubmit(true,
            {
               successCallback:
               {
                  fn: function(serverResponse, obj)
                  {
                     this.widgets.feedbackMessage.destroy();
                     this.widgets.saveButton.set("disabled", false);
                     this.widgets.cancelButton.set("disabled", false);

                     // Display the referencs list again, and refresh data since we change it
                     YAHOO.Bubbling.fire('viewReferences',
                     {
                        refresh: true
                     });

                  },
                  scope: this
               },
               failureCallback:
               {
                  fn: function(serverResponse, obj)
                  {
                     this.widgets.feedbackMessage.destroy();
                     this.widgets.saveButton.set("disabled", false);
                     this.widgets.cancelButton.set("disabled", false);
                     Alfresco.util.PopupManager.displayPrompt(
                     {
                        text: parent.msg("message.saveReferenceFailure")
                     });
                  },
                  scope: this
               }
            });
            form.setSubmitAsJSON(true);

            // Initialise the form
            form.init();

            this.widgets.editForm = form;
         },

         /**
          * onBeforeShow ConsolePanel event handler
          *
          * @method onBeforeShow
          */
         onBeforeShow: function EditPanelHandler_onBeforeShow()
         {
            var editFormEl = Dom.get(parent.id + "-edit-form");
            if (parent.referenceName && parent.referenceName != "")
            {
               // Existing reference, prepare form
               this.widgets.editForm.setAjaxSubmitMethod(Alfresco.util.Ajax.POST);
               editFormEl.attributes.action.nodeValue = Alfresco.constants.PROXY_URI_RELATIVE + "api/rma/admin/customreferencedefinitions";
               Dom.removeClass(parent.id + "-edit-title", "hidden");
               Dom.addClass(parent.id + "-create-title", "hidden");

               // Load reference data
               Alfresco.util.Ajax.jsonGet(
               {
                  url: Alfresco.constants.PROXY_URI + "api/rma/admin/customreferencedefinitions/" + parent.referenceName,
                  successCallback:
                  {
                     fn: function(serverResponse)
                     {
                        // apply current property values to form
                        if (serverResponse.json)
                        {
                           var ref = serverResponse.json.data.customReferences[0];
                           Dom.get(parent.id + "-type-" + ref.referenceType).checked = true;
                           //Dom.get(parent.id + "-name").value = (ref.name) ? ref.name : "";
                           Dom.get(parent.id + "-bidirectional-label").value = (ref.referenceType == "bidirectional" && ref.label) ? ref.label : "";
                           Dom.get(parent.id + "-parentchild-source").value = (ref.referenceType == "parentchild" && ref.source) ? ref.source: "";
                           Dom.get(parent.id + "-parentchild-target").value = (ref.referenceType == "parentchild" && ref.target) ? ref.target : "";
                           this._disableTextInputs();
                        }
                     },
                     scope: this
                  },
                  failureMessage: parent.msg("message.loadreference-failure")
               });
            }
            else
            {
               // New reference, clear the form
               this.widgets.editForm.setAjaxSubmitMethod(Alfresco.util.Ajax.POST);
               editFormEl.attributes.action.nodeValue = Alfresco.constants.PROXY_URI_RELATIVE + "api/rma/admin/customreferencedefinitions";
               Dom.removeClass(parent.id + "-create-title", "hidden");
               Dom.addClass(parent.id + "-edit-title", "hidden");
               Dom.get(parent.id + "-type-bidirectional").checked = true;
               //Dom.get(parent.id + "-name").value = "";
               Dom.get(parent.id + "-bidirectional-label").value = "";
               Dom.get(parent.id + "-parentchild-source").value = "";
               Dom.get(parent.id + "-parentchild-target").value = "";
               this._disableTextInputs();
            }
         },

         /**
          * onShow ConsolePanel event handler
          *
          * @method onShow
          */
         onShow: function EditPanelHandler_onShow()
         {
            Dom.get(parent.id + "-bidirectional-label").focus();
         },

         /**
          * Cancel Property button click handler
          *
          * @method onCancelButtonClick
          * @param e {object} DomEvent
          * @param obj {object} Object passed back from addListener method
          */
         onCancelButtonClick: function EditPanelHandler_onCancelButtonClick(e, obj)
         {
            // Display the referencs list again, but not need to refresh the data since we canceled
            YAHOO.Bubbling.fire('viewReferences',
            {
               refresh: false
            });
         },

         /**
          * Disables/enables type input fields depending on what type checkbox that was checked
          *
          * @method _disableTextInputs
          * @param e click event object
          * @param obj callback object containg action info & HTMLElements
          */
         _disableTextInputs: function EditPanelHandler__disableTextInputs(e, clearDisabledFields)
         {
            // Disable all type inputs
            var biDirectionalLabel = Dom.get(parent.id + "-bidirectional-label");
            var parentChildSource = Dom.get(parent.id + "-parentchild-source");
            var parentChildTarget = Dom.get(parent.id + "-parentchild-target");
            biDirectionalLabel.disabled = true;
            parentChildSource.disabled = true;
            parentChildTarget.disabled = true;
            if (clearDisabledFields)
            {
               biDirectionalLabel.value = "";
               parentChildSource.value = "";
               parentChildTarget.value = "";
            }

            // ...and then enable only the ones related the checked type checkbox
            if (Dom.get(parent.id + "-type-bidirectional").checked)
            {
               biDirectionalLabel.disabled = false;
               biDirectionalLabel.focus();
            }
            else if (Dom.get(parent.id + "-type-parentchild").checked)
            {
               parentChildSource.disabled = false;
               parentChildTarget.disabled = false;
               parentChildSource.focus();
            }
            this.widgets.editForm.updateSubmitElements();
         }

      });
      new EditPanelHandler();
   };

   YAHOO.extend(Alfresco.RecordsReferences, Alfresco.ConsoleTool,
   {
      /**
       * Currently selected reference
       *
       * @property referenceName
       * @type string
       */
      referenceName: null,

      /**
       * True if the data shall be refreshed on the page
       *
       * @property refresh
       * @type object
       */
      refresh: true,

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function RecordsReferences_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function RecordsReferences_onReady()
      {
         // Call super-class onReady() method
         Alfresco.RecordsReferences.superclass.onReady.call(this);
      },



      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */

      /**
       * View references event handler
       *
       * @method onViewReferences
       * @param e {object} DomEvent
       * @param args {array} Event parameters (depends on event type)
       */
      onViewReferences: function RecordsReferences_onViewReferences(e, args)
      {
         this.refreshUIState(
         {
            "panel": "view",
            "refresh": args[1].refresh + ""
         });
      },

      /**
       * Create reference event handler
       *
       * @method onCreateReference
       * @param e {object} DomEvent
       * @param args {array} Event parameters (depends on event type)
       */
      onCreateReference: function RecordsReferences_onCreateReference(e, args)
      {
         this.refreshUIState(
         {
            "panel": "edit",
            "referenceName": ""
         });
      },

      /**
       * Edit reference event handler
       *
       * @method onEditReference
       * @param e {object} DomEvent
       * @param args {array} Event parameters (depends on event type)
       */
      onEditReference: function RecordsReferences_onEditReference(e, args)
      {
         this.refreshUIState(
         {
            "panel": "edit",
            "referenceName": args[1].referenceName
         });
      },

      /**
       * History manager state change event handler (override base class)
       *
       * @method onStateChanged
       * @param e {object} DomEvent
       * @param args {array} Event parameters (depends on event type)
       */
      onStateChanged: function RecordsReferences_onStateChanged(e, args)
      {
         // Clear old states
         this.referenceName = undefined;
         this.refresh = true;

         var state = this.decodeHistoryState(args[1].state);
         if (state.referenceName)
         {
            this.referenceName = state.referenceName ? state.referenceName : "";
         }
         if (state.refresh)
         {
            this.refresh = state.refresh != "false";
         }

         // test if panel has actually changed?
         if (state.panel)
         {
            this.showPanel(state.panel);
         }
         this.updateCurrentPanel();
      },

      /**
       * Encode state object into a packed string for use as url history value.
       * Override base class.
       *
       * @method encodeHistoryState
       * @param obj {object} state object
       * @private
       */
      encodeHistoryState: function RecordsReferences_encodeHistoryState(obj)
      {
         // wrap up current state values
         var stateObj = {};
         if (this.currentPanelId !== "")
         {
            stateObj.panel = this.currentPanelId;
         }

         // convert to encoded url history state - overwriting with any supplied values
         var state = "";
         if (obj.panel || stateObj.panel)
         {
            state += "panel=" + encodeURIComponent(obj.panel ? obj.panel : stateObj.panel);
         }
         if (obj.referenceName)
         {
            state += state.length > 0 ? "&" : "";
            state += "referenceName=" + encodeURIComponent(obj.referenceName);
         }
         if (obj.refresh)
         {
            state += state.length > 0 ? "&" : "";
            state += "refresh=" + encodeURIComponent(obj.refresh);
         }
         return state;
      }

   });
})();