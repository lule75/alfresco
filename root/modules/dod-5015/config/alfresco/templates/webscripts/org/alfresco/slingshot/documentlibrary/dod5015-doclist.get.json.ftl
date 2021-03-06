<#import "item.lib.ftl" as itemLib />
<#escape x as jsonUtils.encodeJSONString(x)>
{
   "totalRecords": ${doclist.paging.totalRecords?c},
   "startIndex": ${doclist.paging.startIndex?c},
   "metadata":
   {
      <#if doclist.filePlan??>"filePlan": "${doclist.filePlan.nodeRef}",</#if>
      "parent":
      {
      <#if doclist.parent??>
         "nodeRef": "${doclist.parent.node.nodeRef}",
         "type": "${doclist.parent.type}",
         "permissions":
         {
            "userAccess":
            {
            <#list doclist.parent.userAccess?keys as perm>
               <#if doclist.parent.userAccess[perm]?is_boolean>
               "${perm?string}": ${doclist.parent.userAccess[perm]?string}<#if perm_has_next>,</#if>
               </#if>
            </#list>
            }
         }
      </#if>
      }
   },
   "items":
   [
      <#list doclist.items as item>
      {
         <@itemLib.itemJSON item=item />,
         "dod5015": <#noescape>${item.dod5015}</#noescape>
      }<#if item_has_next>,</#if>
      </#list>
   ]
}
</#escape>
