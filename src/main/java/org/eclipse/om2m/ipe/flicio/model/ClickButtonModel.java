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
package org.eclipse.om2m.ipe.flicio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.om2m.commons.exceptions.BadRequestException;

public class ClickButtonModel {
	
	private static Map<String,ClickButton> CLICKBUTTONS = new HashMap<String, ClickButton>();
	
	private ClickButtonModel(){
	}
	
	/**
	 * Check if the provided id is correct
	 * @param buttonID
	 */
	public static void checkButtonIDValue(String buttonID){
		if(buttonID == null || !CLICKBUTTONS.containsKey(buttonID)){
			throw new BadRequestException("Unknow Click Button ID");
		}
	}
	
	public static void setModel(
			Map<String, ClickButton> clickButton2) {
		CLICKBUTTONS = clickButton2;
	}
	
}
