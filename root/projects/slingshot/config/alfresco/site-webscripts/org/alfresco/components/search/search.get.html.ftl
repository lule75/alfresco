<script type="text/javascript">//<![CDATA[
   new Alfresco.Search("${args.htmlid}").setOptions(
   {
      siteId: "${siteId}",
      containerId: "",
      initialSearchTerm: "${searchTerm}",
      initialSearchAll: "${searchAll?string}"
   }).setMessages(
      ${messages}
   );
//]]></script>

<#macro resultbar uniqueid>
   <div class="resultbar">
      <span class="search-result-info">
      </span>
      
      <#-- Only add switch if we are in a site context -->
      <#if siteId?has_content>
      <span>
      (
      <a href="#" id="${args.htmlid}-toggleSearchScope-${uniqueid}" class="search-scope-toggle">
         <#if searchAll>
         Search site only
         <#else>
         Search All Sites
         </#if>
      </a>
      )
      </span>
      </#if>
   </div>
</#macro>

<div id="${args.htmlid}-body" class="search">

   <@resultbar uniqueid="first" />

   <#-- this div contains the search results -->
   <div id="${args.htmlid}-results" class="results"></div>

   <@resultbar uniqueid="second" />
</div>