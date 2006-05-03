/*
* Copyright (C) 2005 Alfresco, Inc.
*
* Licensed under the Alfresco Network License. You may obtain a
* copy of the License at
*
*   http://www.alfrescosoftware.com/legal/
*
* Please view the license relevant to your network subscription.
*
* BY CLICKING THE "I UNDERSTAND AND ACCEPT" BOX, OR INSTALLING,  
* READING OR USING ALFRESCO'S Network SOFTWARE (THE "SOFTWARE"),  
* YOU ARE AGREEING ON BEHALF OF THE ENTITY LICENSING THE SOFTWARE    
* ("COMPANY") THAT COMPANY WILL BE BOUND BY AND IS BECOMING A PARTY TO 
* THIS ALFRESCO NETWORK AGREEMENT ("AGREEMENT") AND THAT YOU HAVE THE   
* AUTHORITY TO BIND COMPANY. IF COMPANY DOES NOT AGREE TO ALL OF THE   
* TERMS OF THIS AGREEMENT, DO NOT SELECT THE "I UNDERSTAND AND AGREE"   
* BOX AND DO NOT INSTALL THE SOFTWARE OR VIEW THE SOURCE CODE. COMPANY   
* HAS NOT BECOME A LICENSEE OF, AND IS NOT AUTHORIZED TO USE THE    
* SOFTWARE UNLESS AND UNTIL IT HAS AGREED TO BE BOUND BY THESE LICENSE  
* TERMS. THE "EFFECTIVE DATE" FOR THIS AGREEMENT SHALL BE THE DAY YOU  
* CHECK THE "I UNDERSTAND AND ACCEPT" BOX.
*/

#pragma once
#include "afxcmn.h"

#include "alfresco\Alfresco.hpp"

// CFileStatusDialog dialog

class CFileStatusDialog : public CDialog
{
	DECLARE_DYNAMIC(CFileStatusDialog)

public:
	CFileStatusDialog( AlfrescoFileInfoList& fileList, CWnd* pParent = NULL);   // standard constructor
	virtual ~CFileStatusDialog();

// Dialog Data
	enum { IDD = IDD_FILESTATUS };

	// Initialize the dialog

	BOOL OnInitDialog();

protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support

	DECLARE_MESSAGE_MAP()
	CListCtrl m_listCtrl;

protected:
	// File information list

	AlfrescoFileInfoList& m_fileList;
};
