/*******************************************************************************
 * Copyright (c) 2013-2016 LAAS-CNRS (www.laas.fr)
 * 7 Colonel Roche 31077 Toulouse - France
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 *     Thierry Monteil : Project manager, technical co-manager
 *     Mahdi Ben Alaya : Technical co-manager
 *     Samir Medjiah : Technical co-manager
 *     Khalil Drira : Strategy expert
 *     Guillaume Garzone : Developer
 *     François Aïssaoui : Developer
 *
 * New contributors :
 *******************************************************************************/
package org.eclipse.om2m.ipe.flicio.util;

import java.time.Duration;

import org.eclipse.om2m.commons.constants.Constants;
import org.eclipse.om2m.commons.constants.ShortName;
import org.eclipse.om2m.commons.obix.Abstime;
import org.eclipse.om2m.commons.obix.Contract;
import org.eclipse.om2m.commons.obix.List;
import org.eclipse.om2m.commons.obix.Obj;
import org.eclipse.om2m.commons.obix.Op;
import org.eclipse.om2m.commons.obix.Reltime;
import org.eclipse.om2m.commons.obix.Str;
import org.eclipse.om2m.commons.obix.Uri;
import org.eclipse.om2m.commons.obix.io.ObixEncoder;
import org.eclipse.om2m.ipe.flicio.constants.Operations;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.BUTTON_FEATURE;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPeering;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPosition;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.DATA_QUERY_STRING;
import org.eclipse.om2m.ipe.flicio.model.Click;
import org.eclipse.om2m.ipe.flicio.model.ClickButton;
import org.eclipse.om2m.ipe.flicio.model.DoubleClick;
import org.eclipse.om2m.ipe.flicio.model.FlicDeamon;

public class ObixUtil {
	
	/**
	 * Returns an obix XML representation describing the Flic.io network deamon.
	 * @param cseId - SclBase id
	 * @param appId - Application Id
	 * @param String - the FlicDeamon ID
	 * @return Obix XML representation
	 */
	public static String getDescriptorRep_FlicDeamon(String cseId, String appId, String flicDeamonID) {
		String prefix = cseId+"/"+ Constants.CSE_NAME + "/" + appId;
		// oBIX
		Obj descriptor = new Obj();
		descriptor.add(new Str("type",SampleConstants.POA+"-"+FlicDeamon.TYPE));
		descriptor.add(new Str("location",FlicDeamon.LOCATION));
		descriptor.add(new Str("appId",appId));

		// OP GetStatePosition
		Op opScannerOn = new Op();
		opScannerOn.setName(Operations.SET_SCANNER_ON.toString());
		opScannerOn.setHref(prefix + "?op="+ Operations.SET_SCANNER_ON);
		opScannerOn.setIs(new Contract("execute"));
		descriptor.add(opScannerOn);

		// OP GetStatePosition		
		Op opScannerOff = new Op();
		opScannerOff.setName(Operations.SET_SCANNER_OFF.toString());
		opScannerOff.setHref(prefix + "?op=" + Operations.SET_SCANNER_OFF);
		opScannerOff.setIs(new Contract("execute"));
		descriptor.add(opScannerOff);
		
		return ObixEncoder.toString(descriptor);
	}
	
	/**
	 * Returns an obix XML representation describing the click button.
	 * @param cseId - SclBase id
	 * @param appId - Application Id
	 * @param String - the ClickButton ID
	 * @return Obix XML representation
	 */
	public static String getDescriptorRep_ClickButton(String cseId, String appId, String clickButtonID) {
		String prefix = cseId+"/"+ Constants.CSE_NAME + "/" + appId;
		// oBIX
		Obj descriptor = new Obj();
		descriptor.add(new Str("type",ClickButton.TYPE));
		descriptor.add(new Str("location",ClickButton.LOCATION));
		descriptor.add(new Str("appId",appId));
		
		// OP GetStatePeering from SCL DataBase
		Op opStatePeering = new Op();
		opStatePeering.setName("getStatePeering");
		opStatePeering.setHref(new Uri(prefix  +"/"+BUTTON_FEATURE.DATA_PEERING+"/"+ ShortName.LATEST));
		opStatePeering.setIs(new Contract("retrieve"));
		opStatePeering.setIn(new Contract("obix:Nil"));
		opStatePeering.setOut(new Contract("obix:Nil"));
		descriptor.add(opStatePeering);

		// OP GetStatePeering from SCL IPU
		Op opStateDownPeering = new Op();
		opStateDownPeering.setName("getStatePeering(Direct)");
		opStateDownPeering.setHref(new Uri(prefix + "?"+DATA_QUERY_STRING.op+"="+ Operations.GET_STATE_PEERING_DIRECT +"&"+DATA_QUERY_STRING.clickbuttonid+"=" + clickButtonID));
		opStateDownPeering.setIs(new Contract("execute"));
		opStateDownPeering.setIn(new Contract("obix:Nil"));
		opStateDownPeering.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateDownPeering);

		return ObixEncoder.toString(descriptor);
	}

	/**
	 * Returns an obix XML representation describing the click button.
	 * @param cseId - SclBase id
	 * @param appId - Application Id
	 * @param String - the ClickButton ID
	 * @return Obix XML representation
	 */
	public static String getDescriptorRep_Button(String cseId, String appId, String clickButtonID) {
		String prefix = cseId+"/"+ Constants.CSE_NAME + "/" + appId;
		// oBIX
		Obj descriptor = new Obj();
		descriptor.add(new Str("type",ClickButton.TYPE));
		descriptor.add(new Str("location",ClickButton.LOCATION));
		descriptor.add(new Str("appId",appId));
		
		//========================================================
		//group the BUTTON_FEATURE.DATA_POSITION & BUTTON_FEATURE.DATA_HOLD features into a specific subcontainer
		// OP GetStatePosition from SCL DataBase
		Op opStatePosition = new Op();
		opStatePosition.setName("getStatePosition");
		opStatePosition.setHref(new Uri(prefix  +"/"+BUTTON_FEATURE.DATA_BUTTON+"/"+BUTTON_FEATURE.DATA_POSITION+"/"+ ShortName.LATEST));
		opStatePosition.setIs(new Contract("retrieve"));
		opStatePosition.setIn(new Contract("obix:Nil"));
		opStatePosition.setOut(new Contract("obix:Nil"));
		descriptor.add(opStatePosition);
		
		// OP GetStatePosition from SCL IPU
		Op opStatePositionDirect = new Op();
		opStatePositionDirect.setName("getStatePosition(Direct)");
		opStatePositionDirect.setHref(new Uri(prefix + "?"+DATA_QUERY_STRING.op+"="+ Operations.GET_STATE_POSITION_DIRECT +"&"+DATA_QUERY_STRING.clickbuttonid+"=" + clickButtonID));
		opStatePositionDirect.setIs(new Contract("execute"));
		opStatePositionDirect.setIn(new Contract("obix:Nil"));
		opStatePositionDirect.setOut(new Contract("obix:Nil"));
		descriptor.add(opStatePositionDirect);
				
		// OP GetStateHold from SCL DataBase
		Op opStateHold = new Op();
		opStateHold.setName("getStateHold");
		opStateHold.setHref(new Uri(prefix  +"/"+BUTTON_FEATURE.DATA_BUTTON+"/"+BUTTON_FEATURE.DATA_HOLD+"/"+ ShortName.LATEST));
		opStateHold.setIs(new Contract("retrieve"));
		opStateHold.setIn(new Contract("obix:Nil"));
		opStateHold.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateHold);
		
		// OP GetStateHold from SCL IPU
		Op opStateHoldDirect = new Op();
		opStateHoldDirect.setName("getStateHold(Direct)");
		opStateHoldDirect.setHref(new Uri(prefix + "?"+DATA_QUERY_STRING.op+"="+ Operations.GET_STATE_HOLD_DIRECT +"&"+DATA_QUERY_STRING.clickbuttonid+"=" + clickButtonID));
		opStateHoldDirect.setIs(new Contract("execute"));
		opStateHoldDirect.setIn(new Contract("obix:Nil"));
		opStateHoldDirect.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateHoldDirect);
		
		//========================================================
		return ObixEncoder.toString(descriptor);
	}
	
	/**
	 * Returns an obix XML representation describing the click button.
	 * @param cseId - SclBase id
	 * @param appId - Application Id
	 * @param String - the ClickButton ID
	 * @return Obix XML representation
	 */
	public static String getDescriptorRep_Clicks(String cseId, String appId, String clickButtonID) {
		String prefix = cseId+"/"+ Constants.CSE_NAME + "/" + appId;
		// oBIX
		Obj descriptor = new Obj();
		descriptor.add(new Str("type",ClickButton.TYPE));
		descriptor.add(new Str("location",ClickButton.LOCATION));
		descriptor.add(new Str("appId",appId));

		//========================================================
		//group the BUTTON_FEATURE.DATA_CLICK & BUTTON_FEATURE.DATA_DOUBLECLICK features into a specific subcontainer
		// OP GetStateClick from SCL DataBase
		Op opStateClick = new Op();
		opStateClick.setName("getStateClick");
		opStateClick.setHref(new Uri(prefix  +"/"+BUTTON_FEATURE.DATA_CLICKS+"/"+BUTTON_FEATURE.DATA_CLICK+"/"+ ShortName.LATEST));
		opStateClick.setIs(new Contract("retrieve"));
		opStateClick.setIn(new Contract("obix:Nil"));
		opStateClick.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateClick);
		
		
		// OP GetStateClick from SCL IPU
		Op opStateClickDirect = new Op();
		opStateClickDirect.setName("getStateClick(Direct)");
		opStateClickDirect.setHref(new Uri(prefix + "?"+DATA_QUERY_STRING.op+"="+ Operations.GET_STATE_CLICK_DIRECT+"&"+DATA_QUERY_STRING.clickbuttonid+"=" + clickButtonID));
		opStateClickDirect.setIs(new Contract("execute"));
		opStateClickDirect.setIn(new Contract("obix:Nil"));
		opStateClickDirect.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateClickDirect);

		// OP GetStateDoubleClick from SCL DataBase
		Op opStateDoubleClick = new Op();
		opStateDoubleClick.setName("getStateDoubleClick");
		opStateDoubleClick.setHref(new Uri(prefix  +"/"+BUTTON_FEATURE.DATA_CLICKS+"/"+BUTTON_FEATURE.DATA_DOUBLECLICK+"/"+ ShortName.LATEST));
		opStateDoubleClick.setIs(new Contract("retrieve"));
		opStateDoubleClick.setIn(new Contract("obix:Nil"));
		opStateDoubleClick.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateDoubleClick);
		
		// OP GetStateDoubleClick from SCL IPU
		Op opStateDoubleClickDirect = new Op();
		opStateDoubleClickDirect.setName("getStateDoubleClick(Direct)");
		opStateDoubleClickDirect.setHref(new Uri(prefix + "?"+DATA_QUERY_STRING.op+"="+ Operations.GET_STATE_DOUBLECLICK_DIRECT +"&"+DATA_QUERY_STRING.clickbuttonid+"=" + clickButtonID));
		opStateDoubleClickDirect.setIs(new Contract("execute"));
		opStateDoubleClickDirect.setIn(new Contract("obix:Nil"));
		opStateDoubleClickDirect.setOut(new Contract("obix:Nil"));
		descriptor.add(opStateDoubleClickDirect);
		//========================================================

		return ObixEncoder.toString(descriptor);
	}
	
	/**
	 * Returns an obix XML representation describing the Position state of the ClickButton oneM2M resource
	 * @param clickButtonID - oneM2M Application ID
	 * @param buttonPosition - the click button Position
	 * @return Obix XML representation
	 */
	public static String getStateRep(String clickButtonID, ButtonPosition buttonPosition) {
		// oBIX
		Obj obj = new Obj();
		obj.add(new Str("type",ClickButton.TYPE));
		obj.add(new Str("location",ClickButton.LOCATION));
		obj.add(new Str("clickbuttonid",clickButtonID));
		if (buttonPosition!=null) {
			obj.add(new Str(BUTTON_FEATURE.DATA_POSITION.toString(), buttonPosition.toString()));
		} else {
			obj.add(new Str(BUTTON_FEATURE.DATA_POSITION.toString(), SampleConstants.UNKNOW));		
		}
		return ObixEncoder.toString(obj);
	}
	
	/**
	 * Returns an obix XML representation describing the BLE Peering state of the ClickButton oneM2M resource
	 * @param clickButtonID - oneM2M Application ID
	 * @param buttonPeering - the click button BLE Peering
	 * @return Obix XML representation
	 */
	public static String getStateRep(String clickButtonID, ButtonPeering buttonPeering) {
		// oBIX
		Obj obj = new Obj();
		obj.add(new Str("type",ClickButton.TYPE));
		obj.add(new Str("location",ClickButton.LOCATION));
		obj.add(new Str("clickbuttonid",clickButtonID));
		if (buttonPeering!=null) {
			obj.add(new Str(BUTTON_FEATURE.DATA_PEERING.toString(), buttonPeering.toString()));		
		} else {
			obj.add(new Str(BUTTON_FEATURE.DATA_PEERING.toString(), SampleConstants.UNKNOW));					
		}
		return ObixEncoder.toString(obj);
	}
	
	/**
	 * Returns an obix XML representation describing the Click state of the ClickButton oneM2M resource
	 * @param clickButtonID - oneM2M Application ID
	 * @param buttonClick - current click 
	 * @return Obix XML representation
	 */
	public static String getStateRep(String clickButtonID, Click buttonClick) {
		// oBIX
		Obj obj = new Obj();
		obj.setName("clickbutton");
		obj.add(new Str("type",ClickButton.TYPE));
		obj.add(new Str("location",ClickButton.LOCATION));
		obj.add(new Str("clickbuttonid",clickButtonID));
		
		//list to hold click details
		List list = new List();
		list.setName("click");
				
		if (buttonClick!=null) {
			Abstime abst = new Abstime();
			abst.setName("clicktimestamp");
			//set the click timestamp
			abst.setVal(buttonClick.getClickInstant().toString());
			
			Reltime relt = new Reltime();
			relt.setName("clickduration");
			//set the click duration
			//TBD HOW TO create the obix Realtime
	//		relt.setIs(buttonClick.getClickHold().toString());
			
			list.add(abst);
			list.add(relt);
				
		}		
		obj.add(list);

		return ObixEncoder.toString(obj);
	}
	
	/**
	 * Returns an obix XML representation describing the DoubleClick state of the ClickButton oneM2M resource
	 * @param clickButtonID - oneM2M Application ID	 
	 * @param buttonDoubleClick - current doubleclick
	 * @return Obix XML representation
	 */
	public static String getStateRep(String clickButtonID, DoubleClick buttonDoubleClick) {
		// oBIX
		Obj obj = new Obj();
		obj.setName("clickbutton");
		obj.add(new Str("type",ClickButton.TYPE));
		obj.add(new Str("location",ClickButton.LOCATION));
		obj.add(new Str("clickbuttonid",clickButtonID));
		
		//list to hold doubleclick details
		List list = new List();
		list.setName("doubleclick");
		
		if (buttonDoubleClick!=null) {
			Abstime abst1 = new Abstime();
			abst1.setName("click1timestamp");
			//set the 1st click timestamp
			buttonDoubleClick.getClickInstant1();
			abst1.setVal(buttonDoubleClick.getClickInstant1().toString());
			
			Reltime relt1 = new Reltime();
			relt1.setName("click1duration");
			//set the 1st click duration
			//TBD HOW TO create the obix Realtime
	//		relt1.setIs(buttonDoubleClick.getClickHold1().toString());
			
			list.add(abst1);
			list.add(relt1);
	
			Reltime relt3 = new Reltime();
			relt3.setName("interclicksduration");
			//set the inter clicks duration
			//TBD HOW TO create the obix Realtime
	//		relt1.setIs(buttonDoubleClick.getInterClicksHold());
			
			list.add(relt3);
			
			Abstime abst2 = new Abstime();
			abst2.setName("click2timestamp");
			//set the 2nd click timestamp
			abst2.setVal(buttonDoubleClick.getClickInstant2().toString());
			
			Reltime relt2 = new Reltime();
			relt2.setName("click2duration");
			//set the 1st click duration
			//TBD HOW TO create the obix Realtime
	//		relt2.setIs(buttonDoubleClick.getClickHold2().toString());
			
			list.add(abst2);
			list.add(relt2);
		}
		obj.add(list);	
		return ObixEncoder.toString(obj);
	}
	
	/**
	 * Returns an obix XML representation describing the hold time state of the ClickButton oneM2M resource
	 * @param clickButtonID - oneM2M Application ID
	 * @param buttonClick - current click 
	 * @return Obix XML representation
	 */
	public static String getStateRep(String clickButtonID, Duration buttonHoldDuration) {
		// oBIX
		Obj obj = new Obj();
		obj.setName("clickbutton");
		obj.add(new Str("type",ClickButton.TYPE));
		obj.add(new Str("location",ClickButton.LOCATION));
		obj.add(new Str("clickbuttonid",clickButtonID));
		
		//list to hold doubleclick details
		List list = new List();
		list.setName("holdtimeduration");
		
		if (buttonHoldDuration!=null) {
			Reltime relt = new Reltime();
			relt.setName("singleclickduration");
			//set the button hold duration
			//TBD HOW TO create the obix Realtime
	//		relt.setIs(buttonHoldDuration.toString());
			list.add(relt);
		}		
		obj.add(list);
		return ObixEncoder.toString(obj);
	}
}
