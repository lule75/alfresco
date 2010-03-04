const THUMBNAIL_NAME = "doclib",
   TYPE_SITES = "st:sites",
   PREF_DOCUMENT_FAVOURITES = "org.alfresco.share.documents.favourites",
   PREF_FOLDER_FAVOURITES = "org.alfresco.share.folders.favourites";

var Common =
{
   /**
    * Cache for person objects
    */
   PeopleCache: {},

   /**
    * Gets / caches a person object
    *
    * @method getPerson
    * @param username {string} User name
    */
   getPerson: function Common_getPerson(username)
   {
      if (username == null || username == "")
      {
         return null;
      }

      if (typeof Common.PeopleCache[username] == "undefined")
      {
         var person = people.getPerson(username);
         if (person == null && username == "System")
         {
            person =
            {
               properties:
               {
                  userName: "System",
                  firstName: "System",
                  lastName: "User"
               },
               assocs: {}
            };
         }
         Common.PeopleCache[username] =
         {
            userName: person.properties.userName,
            firstName: person.properties.firstName,
            lastName: person.properties.lastName,
            displayName: (person.properties.firstName + " " + person.properties.lastName).replace(/^\s+|\s+$/g, "")
         };
         if (person.assocs["cm:avatar"] != null)
         {
            Common.PeopleCache[username].avatar = person.assocs["cm:avatar"][0];
         }
      }
      return Common.PeopleCache[username];
   },

   /**
    * Cache for group objects
    */
   GroupCache: {},

   /**
    * Gets / caches a group object
    *
    * @method getGroup
    * @param groupname {string} Group name
    */
   getGroup: function Common_getGroup(groupname)
   {
      if (groupname == null || groupname == "")
      {
         return null;
      }

      if (typeof Common.GroupCache[groupname] == "undefined")
      {
         var group = groups.getGroupForFullAuthorityName(groupname);
         if (group == null && groupname == "GROUP_EVERYONE")
         {
            group =
            {
               fullName: groupname,
               shortName: "EVERYONE",
               displayName: "EVERYONE"
            };
         }
         Common.GroupCache[groupname] = group;
      }
      return Common.GroupCache[groupname];
   },

   /**
    * Cache for site objects
    */
   SiteCache: {},

   /**
    * Gets / caches a site object
    *
    * @method getSite
    * @param siteId {string} Site ID
    */
   getSite: function Common_getSite(siteId)
   {
      if (typeof Common.SiteCache[siteId] == "undefined")
      {
         Common.SiteCache[siteId] = siteService.getSite(siteId);
      }
      return Common.SiteCache[siteId];
   },

   /**
    * Get the user's favourite docs and folders from our slightly eccentric Preferences Service
    *
    * @method getFavourites
    */
   getFavourites: function Common_getFavourites()
   {
      var prefs = preferenceService.getPreferences(person.properties.userName, PREF_DOCUMENT_FAVOURITES),
         favourites = {},
         arrFavs = [],
         strFavs, f, ff;
      try
      {
         /**
          * Fasten seatbelts...
          * An "eval" could be used here, but the Rhino debugger will complain if throws an exception, which gets old very quickly.
          * e.g. var strFavs = eval('try{(prefs.' + PREF_DOCUMENT_FAVOURITES + ')}catch(e){}');
          */
         if (prefs && prefs.org && prefs.org.alfresco && prefs.org.alfresco.share && prefs.org.alfresco.share.documents)
         {
            strFavs = prefs.org.alfresco.share.documents.favourites;
            if (typeof strFavs == "string")
            {
               arrFavs = strFavs.split(",");
               for (f = 0, ff = arrFavs.length; f < ff; f++)
               {
                  favourites[arrFavs[f]] = true;
               }
            }
         }
         // Same thing but for folders
         prefs = preferenceService.getPreferences(person.properties.userName, PREF_FOLDER_FAVOURITES);
         if (prefs && prefs.org && prefs.org.alfresco && prefs.org.alfresco.share && prefs.org.alfresco.share.folders)
         {
            strFavs = prefs.org.alfresco.share.folders.favourites;
            if (typeof strFavs == "string")
            {
               arrFavs = strFavs.split(",");
               for (f = 0, ff = arrFavs.length; f < ff; f++)
               {
                  favourites[arrFavs[f]] = true;
               }
            }
         }
      }
      catch (e)
      {
      }
   
      return favourites;
   },
   
   /**
    * Generates a location object literal for a given node.
    * Location is Site-relative unless a libraryRoot node is passed in.
    *
    * @method getLocation
    * @param node {ScriptNode} Node to generate location for
    * @param libraryRoot {ScriptNode} Optional node to work out relative location from.
    * @return {object} Location object literal.
    */
   getLocation: function Common_getLocation(node, libraryRoot)
   {
      var location = null,
         qnamePaths = node.qnamePath.split("/"),
         displayPaths = node.displayPath.split("/");

      if (libraryRoot)
      {
         if (node.isContainer && String(node.nodeRef) != String(libraryRoot.nodeRef))
         {
            // We want the path to include the parent folder name
            displayPaths = displayPaths.concat([node.name]);
         }

         // Generate the path from the supplied library root
         location =
         {
            site: null,
            siteTitle: null,
            container: null,
            path: "/" + displayPaths.slice(libraryRoot.displayPath.split("/").length + 1, displayPaths.length).join("/")
         };
      }
      else if ((qnamePaths.length > 4) && (qnamePaths[2] == TYPE_SITES))
      {
         if (node.isContainer)
         {
            // We want the path to include the parent folder name
            displayPaths = displayPaths.concat([node.name]);
         }

         var siteId = displayPaths[3],
            siteNode = Common.getSite(siteId),
            containerId = qnamePaths[4].substr(3);

         if (siteNode != null)
         {
            location = 
            {
               site: siteId,
               siteNode: siteNode,
               siteTitle: siteNode.title,
               container: containerId,
               containerNode: siteNode.getContainer(containerId),
               path: "/" + displayPaths.slice(5, displayPaths.length).join("/")
            };
         }
      }
      
      if (location == null)
      {
         location =
         {
            site: null,
            siteTitle: null,
            container: null,
            path: "/" + displayPaths.slice(2, displayPaths.length).join("/")
         };
      }
      
      return location;
   }
};

var ParseArgs =
{
   /**
    * Get and parse arguments
    *
    * @method getParsedArgs
    * @return {array|null} Array containing the validated input parameters
    */
   getParsedArgs: function ParseArgs_getParsedArgs(containerType)
   {
      var type = url.templateArgs.type,
         libraryRoot = args.libraryRoot,
         rootNode = null,
         pathNode = null,
         nodeRef = null,
         path = "";

      if (url.templateArgs.store_type !== null)
      {
         /**
          * nodeRef input: store_type, store_id and id
          */
         var storeType = url.templateArgs.store_type,
            storeId = url.templateArgs.store_id,
            id = url.templateArgs.id;

         nodeRef = storeType + "://" + storeId + "/" + id;
         rootNode = ParseArgs.resolveVirtualNodeRef(nodeRef);
         if (rootNode == null)
         {
            rootNode = search.findNode(nodeRef);
            if (rootNode === null)
            {
               status.setCode(status.STATUS_NOT_FOUND, "Not a valid nodeRef: '" + nodeRef + "'");
               return null;
            }
         }
         
         // Special case: make sure filter picks up correct mode
         if (type == null && args.filter == null)
         {
            args.filter = "node";
         }
      }
      else
      {
         /**
          * Site and container input
          */
         var siteId = url.templateArgs.site,
            containerId = url.templateArgs.container,
            siteNode = siteService.getSite(siteId);

         if (siteNode === null)
         {
            status.setCode(status.STATUS_NOT_FOUND, "Site not found: '" + siteId + "'");
            return null;
         }

         rootNode = siteNode.getContainer(containerId);
         if (rootNode === null)
         {
            rootNode = siteNode.createContainer(containerId, containerType || "cm:folder");
            if (rootNode === null)
            {
               status.setCode(status.STATUS_NOT_FOUND, "Document Library container '" + containerId + "' not found in '" + siteId + "'. (No permission?)");
               return null;
            }
            
            rootNode.properties["cm:description"] = "Document Library";

            /**
             * MOB-593: Add email alias on documentLibrary container creation
             *
            rootNode.addAspect("emailserver:aliasable");
            var emailAlias = siteId;
            if (containerId != "documentLibrary")
            {
               emailAlias += "-" + containerId;
            }
            rootNode.properties["emailserver:alias"] = emailAlias;
            */
            rootNode.save();
         }
      }

      // Path input?
      path = url.templateArgs.path || "";
      pathNode = path.length > 0 ? rootNode.childByNamePath(path) : rootNode;
      if (pathNode === null)
      {
         status.setCode(status.STATUS_NOT_FOUND, "Path not found: '" + path + "'");
         return null;
      }

      // Is this library rooted from a non-site nodeRef?
      if (libraryRoot !== null)
      {
         libraryRoot = ParseArgs.resolveVirtualNodeRef(libraryRoot) || search.findNode(libraryRoot);
      }

      var objRet =
      {
         rootNode: rootNode,
         pathNode: pathNode,
         libraryRoot: libraryRoot,
         location: Common.getLocation(pathNode, libraryRoot),
         path: path,
         nodeRef: nodeRef,
         type: type
      };

      // Multiple input files in the JSON body?
      var files = ParseArgs.getMultipleInputValues("nodeRefs");
      if (typeof files != "string")
      {
         objRet.files = files;
      }

      return objRet;
   },

   /**
    * Resolve "virtual" nodeRefs into nodes
    *
    * @method resolveVirtualNodeRef
    * @param virtualNodeRef {string} nodeRef
    * @return {ScriptNode|null} Node corresponding to supplied virtual nodeRef. Returns null if supplied nodeRef isn't a "virtual" type
    */
   resolveVirtualNodeRef: function ParseArgs_resolveVirtualNodeRef(nodeRef)
   {
      var node = null;
      if (nodeRef == "alfresco://company/home")
      {
         node = companyhome;
      }
      else if (nodeRef == "alfresco://user/home")
      {
         node = userhome;
      }
      else if (nodeRef == "alfresco://sites/home")
      {
         node = companyhome.childrenByXPath("st:sites")[0];
      }
      return node;
   },

   /**
    * Get multiple input files
    *
    * @method getMultipleInputValues
    * @param param {string} Property name containing the files array
    * @return {array|string} Array containing the files, or string error
    */
   getMultipleInputValues: function ParseArgs_getMultipleInputValues(param)
   {
      var values = [],
         error = null;

      try
      {
         // Was a JSON parameter list supplied?
         if (typeof json == "object")
         {
            if (!json.isNull(param))
            {
               var jsonValues = json.get(param);
               // Convert from JSONArray to JavaScript array
               for (var i = 0, j = jsonValues.length(); i < j; i++)
               {
                  values.push(jsonValues.get(i));
               }
            }
         }
      }
      catch(e)
      {
         error = e.toString();
      }

      // Return the values array, or the error string if it was set
      return (error !== null ? error : values);
   }
};
