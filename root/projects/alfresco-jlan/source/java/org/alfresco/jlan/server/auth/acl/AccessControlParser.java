package org.alfresco.jlan.server.auth.acl;

/*
 * AccessControlParser.java
 *
 * Copyright (c) Starlasoft 2004. All rights reserved.
 */

import org.alfresco.config.ConfigElement;

/**
 * Access Control Parser Class
 * 
 * <p>Creates an AccessControl instance by parsing a set of name/value parameters.
 */
public abstract class AccessControlParser {

	//	Constants
	//
	//	Standard parameter names
	
	public final static String ParameterAccess	=	"access";
	
	//	Access control type names
	
	private final static String[] _accessTypes = { "None", "Read", "Write"};
	
	/**
	 * Return the access control type name that uniquely identifies this type of access control.
	 * 
	 * @return String
	 */
	public abstract String getType();
	
	/**
	 * Create an AccessControl instance by parsing the set of name/value parameters
	 *
	 * @param params ConfigElement
	 * @return AccessControl
	 * @exception ACLParseException
	 */
	public abstract AccessControl createAccessControl(ConfigElement params)
		throws ACLParseException;
		
	/**
	 * Find the access parameter and parse the value
	 * 
	 * @param params ConfigElement
	 * @return int
	 * @exception ACLParseException
	 */
	protected final int parseAccessType(ConfigElement params)
		throws ACLParseException {

		//	Check if the parameter list is valid
		
		if ( params == null)
			throw new ACLParseException("Empty parameter list");
			
		//	Find the access type parameter
		
		ConfigElement accessParam = params.getChild(ParameterAccess);
		if ( accessParam == null || accessParam.getValue() == null || accessParam.getValue().length() == 0)
			throw new ACLParseException("Required parameter 'access' missing");
			
		//	Parse the access type value
		
		String accessType = accessParam.getValue();
		return parseAccessTypeString(accessType);
	}
	
	/**
	 * Parse the access level type and validate
	 * 
	 * @param accessType String
	 * @return int
	 * @exception ACLParseException
	 */
	public static final int parseAccessTypeString(String accessType)
		throws ACLParseException {

		//	Check if the access type is valid
		
		if ( accessType == null || accessType.length() == 0)
			throw new ACLParseException("Empty access type string");
			
		//	Parse the access type value

		int access = -1;
		
		for ( int i = 0; i < _accessTypes.length; i++) {
			
			//	Check if the access type matches the current type
			
			if ( accessType.equalsIgnoreCase(_accessTypes[i]))
				access = i;
		}

		//	Check if we found a valid access type
		
		if ( access == -1)
			throw new ACLParseException("Invalid access type, " + accessType);
			
		//	Return the access type
		
		return access;		
	}
	
	/**
	 * Return the parser details as a string
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		
		str.append("[");
		str.append(getType());
		str.append("]");
		
		return str.toString();
	}
}
