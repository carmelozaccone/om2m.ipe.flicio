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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.om2m.commons.exceptions.BadRequestException;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPeering;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPosition;

public class SampleModel {
	
	private static Map<String,FlicDeamon> FLICDEAMONS = new HashMap<String, FlicDeamon>();	
	private static Map<String,ClickButton> CLICKBUTTONS = new HashMap<String, ClickButton>();	

	private SampleModel(){
	}

	public static void addFlicDeamon(FlicDeamon flicDeamon) throws BadRequestException {
		String flicDeamonID = flicDeamon.getFlicDeamonID();
		if (FLICDEAMONS.containsKey(flicDeamonID)) {
			throw new BadRequestException("Existing Flic Deamon ID\n");
		}
		FLICDEAMONS.put(flicDeamonID, flicDeamon);
	}

	public static void addClickButton(ClickButton clickButton) throws BadRequestException {
		String clickButtonID = clickButton.getButtonID();
		if (CLICKBUTTONS.containsKey(clickButtonID)) {
			throw new BadRequestException("Existing click button ID\n");
		}
		CLICKBUTTONS.put(clickButtonID, clickButton);
	}


	public static ClickButton getClickButton(String clickButtonID) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		return CLICKBUTTONS.get(clickButtonID);
	}
	
	public static void removeClickButton(String clickButtonID) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		CLICKBUTTONS.remove(clickButtonID);
	}
	
	public static int getClickButtonPoolSize() {
		return CLICKBUTTONS.size();
	}
	
	public static ButtonPosition getClickButtonPosition(String clickButtonID) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		return CLICKBUTTONS.get(clickButtonID).getButtonPosition();
	}

	public static void setClickButtonPosition(String clickButtonID, ButtonPosition buttonPosition) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		ClickButton clickButtton =	CLICKBUTTONS.get(clickButtonID);
		clickButtton.setButtonPosition(buttonPosition);
		//set the different timers to define the hold time
		Click click = null;
		switch (buttonPosition) {
			case buttondown:
				click = new Click();
				//when button is pressed, this defines the starting moment of the upcoming click  
				click.setClickInstant(Instant.now());
				clickButtton.setClick(click);
				break;
				
			case buttonup:
				click = clickButtton.getClick();
				
				if (click == null) {
					//buttondown event was not capture before present buttonup event
					//we don't consider this click event
					break;
				}
				
				Instant buttonDown = click.getClickInstant();
				//when button is released, this defines the ending moment of the click  
				Instant buttonUp = Instant.now();
				//click duration is defines by the enlapsed time between the starting & ending moments of the click  
				Duration duration = Duration.between(buttonDown, buttonUp);
				click.setClickHold(duration);
				break;
		}
	}

	public static ButtonPeering getClickButtonPeering(String clickButtonID) {
		checkClickButtonIDValue(clickButtonID);
		return CLICKBUTTONS.get(clickButtonID).getButtonPeering();
	}
	
	public static void setClickButtonPeering(String clickButtonID, ButtonPeering buttonPeering) throws BadRequestException{
		checkClickButtonIDValue(clickButtonID);
		CLICKBUTTONS.get(clickButtonID).setButtonPeering(buttonPeering);
	}

	public static Click getClickButtonClick(String clickButtonID) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		return CLICKBUTTONS.get(clickButtonID).getClick();
	}
	
	public static void setClickButtonClick(String clickButtonID, Click click) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		CLICKBUTTONS.get(clickButtonID).setClick(click);
	}
	
	public static DoubleClick getClickButtonDoubleClick(String clickButtonID) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		return CLICKBUTTONS.get(clickButtonID).getDoubleClick();
	}
	
	public static void setClickButtonDoubleClick(String clickButtonID, DoubleClick doubleClick) throws BadRequestException {
		checkClickButtonIDValue(clickButtonID);
		CLICKBUTTONS.get(clickButtonID).setDoubleClick(doubleClick);
	}
	
	public static void checkClickButtonIDValue(String clickButtonID) throws BadRequestException {
		if (!CLICKBUTTONS.isEmpty() & (clickButtonID == null || !CLICKBUTTONS.containsKey(clickButtonID))) {
			throw new BadRequestException("Unknow clickbutton ID");
		}
	}
	
	public static void setModel(Map<String, ClickButton> clickButtons) {
		CLICKBUTTONS = clickButtons;
	}
}
