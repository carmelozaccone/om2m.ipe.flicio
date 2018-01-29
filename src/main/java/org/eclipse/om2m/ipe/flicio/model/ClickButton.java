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
 * 		Carmelo Zaccone	: Developer
 *******************************************************************************/
package org.eclipse.om2m.ipe.flicio.model;

import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPeering;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPosition;

import io.flic.fliclib.javaclient.Bdaddr;
import io.flic.fliclib.javaclient.ButtonConnectionChannel;

public class ClickButton {
    /** Default Button location */
    public final static String LOCATION = "Home";
    /** Default Button type */
    public final static String TYPE = "BUTTON-Flic.io";
    /** Button ID */
    private String buttonID;
    
    /** ButtonPosition */
    ButtonPosition buttonPosition;
 
    /** ButtonPosition */
    ButtonPeering buttonPeering;
 
    /** Click*/
    private Click click;
    
    /** Click*/
    private DoubleClick doubleClick;

    @SuppressWarnings("unused")
	private ButtonConnectionChannel buttonConnectionChannel;

    public final static String createButtonID(ButtonConnectionChannel buttonConnectionChannel) {
		Bdaddr bdaddr = buttonConnectionChannel.getBdaddr();
		return ClickButton.TYPE+"_"+bdaddr.toString();
    }
    
	public ClickButton(ButtonConnectionChannel buttonConnectionChannel){
	  	this.buttonConnectionChannel = buttonConnectionChannel;
	    this.buttonID =createButtonID(buttonConnectionChannel);
	    this.buttonPosition = null;
	    this.buttonPeering = ButtonPeering.buttonprivate;
    	this.click = null;
    	this.doubleClick = null;
    }

	/**
	 * @return the buttonPosition
	 */
	public ButtonPosition getButtonPosition() {
		return buttonPosition;		
	}
	
	/**
	 * @param buttonPosition the buttonPosition to set
	 */
	public void setButtonPosition(ButtonPosition buttonPosition) {
		this.buttonPosition = buttonPosition;
	}
	
	/**
	 * @return the buttonPeering
	 */
	public ButtonPeering getButtonPeering() {
		return buttonPeering;	
	}
	
	/**
	 * @param buttonPeering the buttonPeering to set
	 */
	public void setButtonPeering(ButtonPeering buttonPeering) {
		this.buttonPeering = buttonPeering;
	}
	
	/**
	 * @return the buttonID
	 */
	public String getButtonID() {
		return buttonID;
	}

	/**
	 * @return the click
	 */
	public Click getClick() {
		return click;
	}

	/**
	 * @param click the click to set
	 */
	public void setClick(Click click) {
		this.click = click;
	}

	/**
	 * @return the doubleClick
	 */
	public DoubleClick getDoubleClick() {
		return doubleClick;
	}

	/**
	 * @param doubleClick the doubleClick to set
	 */
	public void setDoubleClick(DoubleClick doubleClick) {
		this.doubleClick = doubleClick;
	}
}
