<import resource="/org/alfresco/web/site/include/ads-support.js">

// things we receive
var componentId = wizard.request("componentId");
if(componentId != null)
{
	var component = sitedata.getObject("component", componentId);
	if(component != null)
	{
		sitedata.unbindComponent(componentId);
	}
}

wizard.setResponseMessage("The component was successfully unassociated");
wizard.setBrowserReload(true);	

wizard.setResponseCodeFinish();

