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

import org.eclipse.om2m.commons.exceptions.BadRequestException;
/**
 * Represent a operation 
 *
 */
public enum Operations {
	
	GET_STATE_POSITION("getStatePosition"),
	GET_STATE_POSITION_DIRECT("getStatePositionDirect"),	
	GET_STATE_PEERING("getStatePeering"),
	GET_STATE_PEERING_DIRECT("getStatePeeringDirect"),
	GET_STATE_CLICK("getStateClick"),
	GET_STATE_CLICK_DIRECT("getStateClickDirect"),
	GET_STATE_DOUBLECLICK("getStateDoubleClick"),
	GET_STATE_DOUBLECLICK_DIRECT("getStateDoubleClickDirect"),
	GET_STATE_HOLD("getStateHold"),
	GET_STATE_HOLD_DIRECT("getStateHoldDirect"),
	
	SET_STATE_DESCRIPTOR("Descriptor"),
	SET_STATE_POSITION("Position"),
	SET_STATE_PEERING("Peering"),
	SET_STATE_CLICK("Click-Entity"),
	SET_STATE_DOUBLECLICK("DoubleClick-Entity"),
	SET_STATE_HOLD("Hold-Time_BetweenPositions");
	
	private final String value;
	
	private Operations(final String value){
		this.value = value;
	}
	
	public String toString() {
		return value;
	}
	
	public String getValue(){
		return value;
	}
	
	/**
	 * Return the operation from the string
	 * @param operation
	 * @return
	 */
	public static Operations getOperationFromString(String operation) throws BadRequestException {
		for(Operations op : values()){
			if(op.getValue().equals(operation)){
				return op;
			}
		}
		throw new BadRequestException("Unknow Operation");
	}
}
