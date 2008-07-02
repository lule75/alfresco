<import resource="/org/alfresco/web/site/include/ads-support.js">


// inputs
var componentId = wizard.request("componentId");
var component = sitedata.getObject("component", componentId);

// process
if(component != null)
{
	var markupData = wizard.getSafeProperty(component, "markupData");
	markupData = sitedata.decode(markupData);

	// the controls
	markupData = sitedata.encode(markupData);
	wizard.addElement("markupData", markupData);
	wizard.addElementFormat("markupData", null, "textarea", 460, 210);
}

