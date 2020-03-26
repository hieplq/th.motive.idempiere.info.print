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

import org.adempiere.webui.factory.IInfoButtonSetting;
import org.adempiere.webui.factory.IInfoButtonSettingFactory;
import org.adempiere.webui.factory.IInfoPrintHandle;
import org.adempiere.webui.factory.IInfoPrintHandleFactory;
import org.adempiere.webui.panel.InfoPanel;
import org.osgi.service.component.annotations.Component;

/**
 * 
 * @author hieplq
 *
 */
@Component(
		 property= {"service.ranking:Integer=2"}
		 )
public class DefaultInfoPanelPrintMangeFactory implements IInfoButtonSettingFactory, IInfoPrintHandleFactory{
	@Override
	public IInfoPrintHandle getInfoPrintHandle(InfoPanel infoPanel) {
		return new DefaultInfoPanelPrintMange();
	}

	@Override
	public IInfoButtonSetting getInfoButtonSetting(InfoPanel infoPanel) {
		return new DefaultInfoPanelPrintMange();
	}

}
