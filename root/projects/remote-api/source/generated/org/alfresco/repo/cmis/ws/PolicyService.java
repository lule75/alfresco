
/*
 * 
 */

package org.alfresco.repo.cmis.ws;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 2.1.2
 * Mon Oct 12 11:20:37 EEST 2009
 * Generated source version: 2.1.2
 * 
 */


@WebServiceClient(name = "PolicyService", 
                  wsdlLocation = "file:/D:/java/eclipse/workspace/WS-Binding-07b3/source/wsdl/CMISWS-Service.wsdl",
                  targetNamespace = "http://docs.oasis-open.org/ns/cmis/ws/200908/") 
public class PolicyService extends Service {

    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://docs.oasis-open.org/ns/cmis/ws/200908/", "PolicyService");
    public final static QName PolicyServicePort = new QName("http://docs.oasis-open.org/ns/cmis/ws/200908/", "PolicyServicePort");
    static {
        URL url = null;
        try {
            url = new URL("file:/D:/java/eclipse/workspace/WS-Binding-07b3/source/wsdl/CMISWS-Service.wsdl");
        } catch (MalformedURLException e) {
            System.err.println("Can not initialize the default wsdl from file:/D:/java/eclipse/workspace/WS-Binding-07b3/source/wsdl/CMISWS-Service.wsdl");
            // e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public PolicyService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public PolicyService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public PolicyService() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * 
     * @return
     *     returns PolicyServicePort
     */
    @WebEndpoint(name = "PolicyServicePort")
    public PolicyServicePort getPolicyServicePort() {
        return super.getPort(PolicyServicePort, PolicyServicePort.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns PolicyServicePort
     */
    @WebEndpoint(name = "PolicyServicePort")
    public PolicyServicePort getPolicyServicePort(WebServiceFeature... features) {
        return super.getPort(PolicyServicePort, PolicyServicePort.class, features);
    }

}
