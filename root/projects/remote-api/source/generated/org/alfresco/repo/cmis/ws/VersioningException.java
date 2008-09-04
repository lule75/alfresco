
package org.alfresco.repo.cmis.ws;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.0.6
 * Tue Jul 29 18:21:47 EEST 2008
 * Generated source version: 2.0.6
 * 
 */

@WebFault(name = "versioningException", targetNamespace = "http://www.cmis.org/ns/1.0")

public class VersioningException extends Exception {
    public static final long serialVersionUID = 20080729182147L;
    
    private org.alfresco.repo.cmis.ws.BasicFault versioningException;

    public VersioningException() {
        super();
    }
    
    public VersioningException(String message) {
        super(message);
    }
    
    public VersioningException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersioningException(String message, org.alfresco.repo.cmis.ws.BasicFault versioningException) {
        super(message);
        this.versioningException = versioningException;
    }

    public VersioningException(String message, org.alfresco.repo.cmis.ws.BasicFault versioningException, Throwable cause) {
        super(message, cause);
        this.versioningException = versioningException;
    }

    public org.alfresco.repo.cmis.ws.BasicFault getFaultInfo() {
        return this.versioningException;
    }
}
