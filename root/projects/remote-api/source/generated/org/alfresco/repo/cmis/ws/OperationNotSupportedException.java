
package org.alfresco.repo.cmis.ws;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.0.6
 * Tue Jul 29 18:22:39 EEST 2008
 * Generated source version: 2.0.6
 * 
 */

@WebFault(name = "operationNotSupportedException", targetNamespace = "http://www.cmis.org/ns/1.0")

public class OperationNotSupportedException extends Exception {
    public static final long serialVersionUID = 20080729182239L;
    
    private org.alfresco.repo.cmis.ws.BasicFault operationNotSupportedException;

    public OperationNotSupportedException() {
        super();
    }
    
    public OperationNotSupportedException(String message) {
        super(message);
    }
    
    public OperationNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationNotSupportedException(String message, org.alfresco.repo.cmis.ws.BasicFault operationNotSupportedException) {
        super(message);
        this.operationNotSupportedException = operationNotSupportedException;
    }

    public OperationNotSupportedException(String message, org.alfresco.repo.cmis.ws.BasicFault operationNotSupportedException, Throwable cause) {
        super(message, cause);
        this.operationNotSupportedException = operationNotSupportedException;
    }

    public org.alfresco.repo.cmis.ws.BasicFault getFaultInfo() {
        return this.operationNotSupportedException;
    }
}
