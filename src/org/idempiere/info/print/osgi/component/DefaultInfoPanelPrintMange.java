/**********************************************************************
* This file is part of Idempiere ERP Bazaar                           *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Idempiere                                             *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
**********************************************************************/

package org.idempiere.info.print.osgi.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.adempiere.webui.apps.ProcessModalDialog;
import org.adempiere.webui.apps.ProcessParameterPanel;
import org.adempiere.webui.apps.WProcessCtl;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.ProcessInfoDialog;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.factory.IInfoButtonSetting;
import org.adempiere.webui.factory.IInfoPrintHandle;
import org.adempiere.webui.panel.InfoPanel;
import org.apache.commons.collections.CollectionUtils;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * 
 * @author hieplq
 *
 */
public class DefaultInfoPanelPrintMange implements IInfoPrintHandle, IInfoButtonSetting {

	public static final String COLUMNNAME_AllowDefaultPrint = "AllowDefaultPrint";
	public static final String COLUMNNAME_Process_ID = "AD_Process_ID";
	public static final String PARANAME_INFO_SELECTED_KEYS = "INFO_SELECTED_KEYS";
	public static final String PARANAME_PRINT_ALL = "INFO_PRINT_ALL";
	
	/**
	 * visible print button when define this info window can print
	 */
	@Override
	public void settingButton(InfoPanel infoPanel, int adInfoWindowID, ConfirmPanel confirmPanel) {
		MInfoWindow mInfoWindow = MInfoWindow.get(adInfoWindowID, null);
		if (mInfoWindow == null)
			return;// generic infoWindow
		
		if (mInfoWindow.get_ValueAsBoolean(COLUMNNAME_AllowDefaultPrint) || mInfoWindow.get_ValueAsInt(COLUMNNAME_Process_ID) > 0) {
			confirmPanel.setVisible(ConfirmPanel.A_PRINT, true);
			confirmPanel.setEnabled(ConfirmPanel.A_PRINT, infoPanel.getRowCount() > 0);// disable print button when not yet query
		}
	}

	/**
	 * call jasper process to print out
	 */
	@Override
	public void handlePrintClick(int adInfoWindowID, InfoPanel infoPanel) {
		MInfoWindow mInfoWindow = MInfoWindow.get(adInfoWindowID, null);
		if (mInfoWindow == null)
			return;// generic infoWindow
		
		if (mInfoWindow.get_ValueAsBoolean(COLUMNNAME_AllowDefaultPrint) && mInfoWindow.get_ValueAsInt(COLUMNNAME_Process_ID) == 0) {
			throw new UnsupportedOperationException("default print isn't yet implement");
		}else if (mInfoWindow.get_ValueAsInt(COLUMNNAME_Process_ID) > 0){
			runProcess (mInfoWindow.get_ValueAsInt(COLUMNNAME_Process_ID), mInfoWindow, infoPanel);
		}
	}
	
	protected void runProcess (final int processId, MInfoWindow mInfoWindow, InfoPanel infoPanel){
    	// init process info
		final MProcess m_process = MProcess.get(Env.getCtx(), processId);
    	final ProcessInfo m_pi = new ProcessInfo(m_process.getName(), processId);
		m_pi.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
		m_pi.setAD_Client_ID(Env.getAD_Client_ID(Env.getCtx()));

		MPInstance instance = new MPInstance(Env.getCtx(), processId, 0);
		instance.saveEx();
		final int pInstanceID = instance.getAD_PInstance_ID();
		// Execute Process
		m_pi.setAD_PInstance_ID(pInstanceID);		
		m_pi.setAD_InfoWindow_ID(mInfoWindow.getAD_InfoWindow_ID());
		m_pi.setAD_InfoWindow_ID(mInfoWindow.getAD_InfoWindow_ID());
		
		// call to show process dialog
		WProcessCtl.process(infoPanel.getWindowNo(), m_pi, (Trx)null, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				ProcessModalDialog processModalDialog = (ProcessModalDialog)event.getTarget();
				
				if (DialogEvents.ON_BEFORE_RUN_PROCESS.equals(event.getName())){
					// setting parameter
					setGUIParameter (processModalDialog.getParameterPanel(), m_pi);
					// must be call after setGUIParameter to check print all parameter
					setSelectedKey (m_pi, infoPanel);					
				}else if (ProcessModalDialog.ON_WINDOW_CLOSE.equals(event.getName())){ 
					if (processModalDialog.isCancel()){
						// enable or disable control button rely selected record status 
						// enableButtons();
					}else if (m_pi.isError()){
						ProcessInfoDialog.showProcessInfo(m_pi, infoPanel.getWindowNo(), infoPanel, true);
					}
				}
			}
		});   		
    }
	
	/**
	 * normal process like InvoicePrint will extent from SvrProcess, on prepare step it call to SvrProcess.getParameter to load parameter that save to database from dialog
	 * but ReportStarter don't do like that, it extend from ProcessCall. parameter fix by logic. example print from default window it direct pass RECORD_ID
	 * that function help to fill up this hole by get parameter direct from dialog and input to parameters
	 * @param parameterPanel
	 * @param processInfo
	 */
	protected void setGUIParameter(ProcessParameterPanel parameterPanel, ProcessInfo processInfo) {
		if (parameterPanel == null)
			return;
		
		MPInstancePara [] parasFromDialog = parameterPanel.getParameters();
		if (parasFromDialog == null)
			return;
		
		ProcessInfoParameter [] processInfoParas = processInfo.getParameter();
		List<ProcessInfoParameter> lsProcessInfoPara = null;
		
		if (processInfoParas != null) {
			lsProcessInfoPara = new ArrayList<>();
			CollectionUtils.addAll(lsProcessInfoPara, processInfoParas);
		}
		
		for (MPInstancePara paraFromGUI : parasFromDialog) {
			Object paraValue = null;
			Object paratoValue = null;
			paraValue = paraFromGUI.getP_String();
			paratoValue = paraFromGUI.getP_String_To();
			
			if (paraValue == null && paratoValue == null) {
				paraValue = paraFromGUI.getP_Date();
				paratoValue = paraFromGUI.getP_Date_To();
			}
			
			if (paraValue == null && paratoValue == null) {
				paraValue = paraFromGUI.getP_Number();
				paratoValue = paraFromGUI.getP_Number_To();
			}
			
			ProcessInfoParameter newProcessPara = new ProcessInfoParameter(paraFromGUI.getParameterName(), paraValue, paratoValue, paraFromGUI.getInfo(), paraFromGUI.getInfo_To());
			
			if (processInfoParas == null) {
				lsProcessInfoPara = new ArrayList<>();
			}
			
			for (ProcessInfoParameter verifyProcessInfoParas : lsProcessInfoPara) {
				if (verifyProcessInfoParas.getParameterName().equals(paraFromGUI.getParameterName())) {
					throw new IllegalArgumentException(String.format("On process % parameter %1 is duplicated with standard parameter", processInfo.getTitle(), paraFromGUI.getParameterName()));
				}
			}
			
			lsProcessInfoPara.add(newProcessPara);
		}
		
		if (lsProcessInfoPara != null) {
			processInfo.setParameter(lsProcessInfoPara.toArray(new ProcessInfoParameter[0]));
		}
		
	}

	/**
	 * set collection selected id to report parameter
	 * @param processInfo
	 * @param infoPanel
	 */
	protected void setSelectedKey(ProcessInfo processInfo, InfoPanel infoPanel) {
		ProcessInfoParameter [] processInfoParas = processInfo.getParameter();
		boolean isPrintAll = false;
		
		if (processInfoParas != null) {
			for (ProcessInfoParameter processPara : processInfoParas) {
				if (processPara.getParameterName().equals(PARANAME_PRINT_ALL)) {
					isPrintAll = processPara.getParameterAsBoolean();
				}
			}
		}
		
		Collection<KeyNamePair> selectedKeypairs = infoPanel.getSelectedKeyForPrint (isPrintAll);
		
		Collection<Integer> selectedKeys = new ArrayList<>();
		if (selectedKeypairs != null) {
			for (KeyNamePair keypair : selectedKeypairs) {
				selectedKeys.add(keypair.getKey());
			}
		}
		
		if (selectedKeys.size() == 0) {
			// https://community.jaspersoft.com/documentation/tibco-jaspersoft-studio-user-guide/v60/using-parameters-queries
			// avoid IN clause explore all record 
			// JasperReports handles special characters in each value. If the parameter is null or contains an empty list, meaning no value has been set for the parameter, the entire $X{} clause is evaluated as the always true statement “0 = 0”.
			selectedKeys.add(-1);
		}
		
		ProcessInfoParameter infoSelectedPara = new ProcessInfoParameter(PARANAME_INFO_SELECTED_KEYS, selectedKeys, null, null, null);
		
		List<ProcessInfoParameter> lsProcessInfoPara = null;
		if (processInfoParas == null) {
			lsProcessInfoPara = new ArrayList<>();
		}else {
			lsProcessInfoPara = new ArrayList<>();
			CollectionUtils.addAll(lsProcessInfoPara, processInfoParas);
		}
		lsProcessInfoPara.add(infoSelectedPara);
		
		processInfo.setParameter(lsProcessInfoPara.toArray(new ProcessInfoParameter[0]));
	}
}
