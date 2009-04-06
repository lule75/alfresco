<script type="text/javascript">//<![CDATA[
   new Alfresco.SiteGroups("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!""}",
      currentUser: "${user.id}",
      currentUserRole: "${currentUserRole}",
      <#if error??>error: "${error}",</#if>
      roles:
      [
         <#list siteRoles as siteRole>"${siteRole}"<#if siteRole_has_next>,</#if></#list>
      ],
      minSearchTermLength: ${args.minSearchTermLength!'3'},
      maxSearchResults: ${args.maxSearchResults!'100'}
   }).setMessages(
      ${messages}
   );
//]]></script>

<div id="${args.htmlid}-body" class="site-groups">
   
   <div class="heading"><label for="${args.htmlid}-term">${msg("site-groups.heading")}</label></div>
   
   <div class="add-groups">
      <span id="${args.htmlid}-addGroups" class="yui-button yui-link-button">
         <span class="first-child">
            <a href="add-groups">${msg("site-groups.add-groups")}</a>
         </span>
      </span>
   </div>

   <div class="finder-wrapper">
      <div class="search-controls theme-bg-color-3">
         <div class="search-text"><input id="${args.htmlid}-term" type="text" class="search-term" /></div>
         <div class="search-button"><button id="${args.htmlid}-button">${msg("site-groups.search-button")}</button></div>
      </div>

      <div id="${args.htmlid}-groups" class="results"></div>
   </div>
</div>