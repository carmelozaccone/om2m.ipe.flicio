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
package org.eclipse.om2m.ipe.flicio.constants;

import org.eclipse.om2m.commons.constants.Constants;

public class SampleConstants {
	
	private SampleConstants(){}
	
	public static final String POA = "Flic.io sample";
	public static final String FLIC_DEAMON_HOST = "localhost";
	public static enum BUTTON_FEATURE {
		DATA_POSITION("DATA_POSITION"), 
		DATA_PEERING("DATA_PEERING"), 
		DATA_CLICK("DATA_CLICK"), 
		DATA_DOUBLECLICK("DATA_DOUBLECLICK"), 
		DATA_HOLD("DATA_HOLD");
		private final String value;
		
		private BUTTON_FEATURE(final String value){
			this.value = value;
		}
		
		public String toString() {
			return value;
		}
	};

	public static enum ButtonPosition {
		buttondown("ButtonIsDown"), 
		buttonup("ButtonIsUp");
		private final String value;
		
		private ButtonPosition(final String value){
			this.value = value;
		}
		
		public String toString() {
			return value;
		}	
	};
	public static enum ButtonPeering {
		buttonpublic("ButtonIsPublic"), 
		buttonprivate("ButtonIsPrivate");
		private final String value;
		
		private ButtonPeering(final String value){
			this.value = value;
		}
		
		public String toString() {
			return value;
		}	
	};

	public static final String UNKNOW= "unknow";
	
	public static final String DESC = "DESCRIPTOR";
	public static final String AE_NAME = "AE_FLIC.IO_IPE";
	public static final String CONTAINER_NAME = "ClickButton";

	public static enum DATA_QUERY_STRING { 
		op("op"),
		clickbuttonid("clickbuttonid"),
		position("position"),
		peering("peering"),
		click("click"),
		doubleclick("doubleclick"),
		hold("hold");
		private final String value;
		
		private DATA_QUERY_STRING(final String value){
			this.value = value;
		}
		
		public String toString() {
			return value;
		}
	};	
		
	public static String CSE_ID = "/" + Constants.CSE_ID;
	public static String CSE_PREFIX = CSE_ID + "/" + Constants.CSE_NAME;
}
