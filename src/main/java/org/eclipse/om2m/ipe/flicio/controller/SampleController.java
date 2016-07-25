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
package org.eclipse.om2m.ipe.flicio.controller;

import java.math.BigInteger;
import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.constants.MimeMediaType;
import org.eclipse.om2m.commons.constants.ResponseStatusCode;
import org.eclipse.om2m.commons.exceptions.BadRequestException;
import org.eclipse.om2m.commons.resource.AE;
import org.eclipse.om2m.commons.resource.Container;
import org.eclipse.om2m.commons.resource.ContentInstance;
import org.eclipse.om2m.commons.resource.ResponsePrimitive;
import org.eclipse.om2m.core.service.CseService;
import org.eclipse.om2m.ipe.flicio.RequestSender;
import org.eclipse.om2m.ipe.flicio.constants.Operations;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.BUTTON_FEATURE;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPeering;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPosition;
import org.eclipse.om2m.ipe.flicio.model.Click;
import org.eclipse.om2m.ipe.flicio.model.ClickButton;
import org.eclipse.om2m.ipe.flicio.model.DoubleClick;
import org.eclipse.om2m.ipe.flicio.model.SampleModel;
import org.eclipse.om2m.ipe.flicio.util.ObixUtil;

import io.flic.fliclib.javaclient.ButtonConnectionChannel;

/*
 * This object handles the operations to perform when Flic.io button events occurs
 * The Controller manages both the "real world" (memory) and the oneM2M resources models
 */
public class SampleController {
	
	public static CseService CSE;
	protected static String AE_ID;
	private static Log LOGGER= LogFactory.getLog(SampleController.class);
	private static int counterEventsPosition;
	private static int counterEventsPeering;
	private static int counterEventsClick;
	private static int counterEventsDoubleClick;
	private static int counterEventsHold;
	
	/*
	 * Create a ClickButton and the oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel which joined the Flic.io client upon new BLE association
	 * @return ClickButton
	 */
	public static ClickButton addClickButton(ButtonConnectionChannel channel) {    
	
		ClickButton clickButton = new ClickButton(channel);
		
		// Create initial resources for the button status
		try {
			// Send the information to the CSE
			createClickButtonResources(clickButton.getButtonID(), clickButton.getButtonPosition(), clickButton.getButtonPeering());
			// Set the value in the "real world" model
			SampleModel.addClickButton(clickButton);	
			LOGGER.info("ClickButton and oneM2M ressourses created: ["+clickButton.getButtonID()+"]");
			return clickButton;
        } catch (BadRequestException e) {
			LOGGER.error("ClickButton and oneM2M ressourses can't be created: ["+clickButton.getButtonID()+"]\n"+e.toString());
			return null;
        }
	}
	
	/*
	 * Returns the ClickButton representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel which generated the event to the Flic.io client
	 */	
	public static ClickButton getClickButton(ButtonConnectionChannel channel) {    
		String clickButtonID = ClickButton.createButtonID(channel);
		try {
			return SampleModel.getClickButton(clickButtonID);
		} catch (BadRequestException e) {
			LOGGER.error("ClickButton can't be accessed: ["+clickButtonID+"]\n"+e.toString());
			return null;
		}
	}

	/*
	 * Remove the ClickButton and the oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel which left the Flic.io client
	 * return String the clickButtonID if a ClickButton existed for the ButtonConnectionChannel, null otherwise
	 */		
	public static String removeClickButton(ButtonConnectionChannel channel) {    
		String clickButtonID = ClickButton.createButtonID(channel);
		
		// Remove the resources for the button status
		try {
			// Send the information to the CSE
			removeClickButtonResources(clickButtonID);
			// Remove the resource in the "real world" model
			SampleModel.removeClickButton(clickButtonID);
			LOGGER.info("ClickButton and oneM2M ressourses removed: ["+clickButtonID+"]");
			
		} catch (BadRequestException e) {
			LOGGER.error("ClickButton and oneM2M ressourses can't be removed: ["+clickButtonID+"]\n"+e.toString());
			return null;
		}
		return clickButtonID;
	}
	
	/*
	 * Set the status of the position for the ClickButton and the oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel for which an update was received by the Flic.io client
	 * @param buttonPosition the actual position of the Flic.io button to sync with the corresponding ClickButton and oneM2M resources
	 * return String the clickButtonID if a ClickButton existed for the ButtonConnectionChannel, null otherwise
	 */		
	public static String setClickButtonPosition(ButtonConnectionChannel channel, ButtonPosition buttonPosition) throws BadRequestException{
		String clickButtonID = ClickButton.createButtonID(channel);
		String appID  = createAppID(clickButtonID);
		
		// Send the information to the CSE
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_POSITION;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, buttonPosition);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		counterEventsPosition++;	
		ResponsePrimitive response = RequestSender.createContentInstance(targetID, Operations.SET_STATE_POSITION.toString()+"_"+counterEventsPosition, cin);
		BigInteger statusCode = response.getResponseStatusCode();
		if(statusCode.equals(ResponseStatusCode.OK) | statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED)) {
			// Set the value in the "real world" model
			SampleModel.setClickButtonPosition(clickButtonID, buttonPosition);
			LOGGER.info("ClickButton and oneM2M ressourse (position["+buttonPosition+"]) updated ("+counterEventsPosition+"): ["+clickButtonID+"]");
			LOGGER.debug("===\n"+content+"\n===");			
			LOGGER.debug(response);
			return clickButtonID;
		} else {
			LOGGER.error("ClickButton and oneM2M ressourse (position ["+buttonPosition+"]) can't be updated("+counterEventsPosition+"): ["+clickButtonID+"]");	
			LOGGER.debug("Response StatusCode "+statusCode+"\n");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return null;
		}
	}
	
	/*
	 * Set the status of the BLE peering for the ClickButton and the oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel for which an update was received by the Flic.io client
	 * @param buttonPering the actual peering of the Flic.io button to sync with the corresponding ClickButton and oneM2M resources
	 * return String the clickButtonID if a ClickButton existed for the ButtonConnectionChannel, null otherwise
	 */		
	public static String setClickButtonPeering(ButtonConnectionChannel channel, ButtonPeering buttonPeering) throws BadRequestException{
		String clickButtonID = ClickButton.createButtonID(channel);
		String appID  = createAppID(clickButtonID);
		
		// Send the information to the CSE
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_PEERING;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, buttonPeering);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		counterEventsPeering++;
		ResponsePrimitive response = RequestSender.createContentInstance(targetID, Operations.SET_STATE_PEERING.toString()+"_"+counterEventsPeering, cin);
		BigInteger statusCode = response.getResponseStatusCode();
		if(statusCode.equals(ResponseStatusCode.OK) | statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED)) {
			// Set the value in the "real world" model
			SampleModel.setClickButtonPeering(clickButtonID, buttonPeering);
			LOGGER.info("ClickButton and oneM2M ressourse (peering ["+buttonPeering+"]) updated ("+counterEventsPeering+"): ["+clickButtonID+"]");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return clickButtonID;
		} else {
			LOGGER.error("ClickButton and oneM2M ressourse (peering ["+buttonPeering+"]) can't be updated ("+counterEventsPeering+"): ["+clickButtonID+"]");		
			LOGGER.debug("Response StatusCode "+statusCode+"\n");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return null;
		}
	}
	
	/*
	 * Associate a new Click for the ClickButton and the oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel for which an update was received by the Flic.io client
	 * @param click the new Click to bind to the ClickButton associated with the ButtonConnectionChannel
	 * return String the clickButtonID if a ClickButton existed for the ButtonConnectionChannel, null otherwise
	 */	
	public static String setClickButtonClick(ButtonConnectionChannel channel, Click click){
		String clickButtonID = ClickButton.createButtonID(channel);
		String appID  = createAppID(clickButtonID);
		
		// Send the information to the CSE
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICK;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, click);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		counterEventsClick++;
		ResponsePrimitive response = RequestSender.createContentInstance(targetID, Operations.SET_STATE_CLICK.toString()+"_"+counterEventsClick, cin);
		BigInteger statusCode = response.getResponseStatusCode();
		if(statusCode.equals(ResponseStatusCode.OK) | statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED)) {
			// Set the value in the "real world" model
			SampleModel.setClickButtonClick(clickButtonID, click);
			LOGGER.info("ClickButton and oneM2M ressourse (click) updated ("+counterEventsClick+"): ["+clickButtonID+"]");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return clickButtonID;
		} else {
			LOGGER.error("ClickButton and oneM2M ressourse (click) can't be updated ("+counterEventsClick+"): ["+clickButtonID+"]");		
			LOGGER.debug("Response StatusCode "+statusCode+"\n");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return null;
		}
	}
	
	/*
	 * Associate a new DoubleClick for the ClickButton and the oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel for which an update was received by the Flic.io client
	 * @param click the new DoubleClick to bind to the ClickButton associated with the ButtonConnectionChannel
	 * return String the clickButtonID if a ClickButton existed for the ButtonConnectionChannel, null otherwise
	 */	
	public static String setClickButtonDoubleClick(ButtonConnectionChannel channel, DoubleClick doubleClick){
		String clickButtonID = ClickButton.createButtonID(channel);
		String appID  = createAppID(clickButtonID);
		
		// Send the information to the CSE
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_DOUBLECLICK;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, doubleClick);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		counterEventsDoubleClick++;	
		ResponsePrimitive response = RequestSender.createContentInstance(targetID, Operations.SET_STATE_DOUBLECLICK.toString()+"_"+counterEventsDoubleClick, cin);
		BigInteger statusCode = response.getResponseStatusCode();
		if(statusCode.equals(ResponseStatusCode.OK) | statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED)) {
			// Set the value in the "real world" model
			SampleModel.setClickButtonDoubleClick(clickButtonID, doubleClick);
			LOGGER.info("ClickButton and oneM2M ressourse (double click) updated ("+counterEventsDoubleClick+"): ["+clickButtonID+"]");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return clickButtonID;
		} else {
			LOGGER.error("ClickButton and oneM2M ressourse (double click) can't be updated ("+counterEventsDoubleClick+"): ["+clickButtonID+"]");			
			LOGGER.debug("Response StatusCode "+statusCode+"\n");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return null;
		}
	}
	
	/*
	 * Associate a new Hold Time for the button hold oneM2M resources representing the Flic.io button associated with the ButtonConnectionChannel
	 * @param channel the ButtonConnectionChannel for which an update was received by the Flic.io client
	 * @param holdTime the duration of the button hold associated with the ButtonConnectionChannel
	 * return String the clickButtonID if a ClickButton existed for the ButtonConnectionChannel, null otherwise
	 */	
	public static String setClickButtonHold(ButtonConnectionChannel channel, Duration holdTime){
		String clickButtonID = ClickButton.createButtonID(channel);
		String appID  = createAppID(clickButtonID);
		
		// Send the information to the CSE
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_HOLD;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, holdTime);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		counterEventsHold++;
		ResponsePrimitive response = RequestSender.createContentInstance(targetID, Operations.SET_STATE_HOLD.toString()+"_"+counterEventsHold, cin);
		BigInteger statusCode = response.getResponseStatusCode();
		if(statusCode.equals(ResponseStatusCode.OK) | statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED)) {
			LOGGER.info("oneM2M ressourse (button hold time) updated: ["+clickButtonID+"]");
			LOGGER.debug("Response StatusCode "+statusCode+"\n");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return clickButtonID;
		} else {
			LOGGER.error("oneM2M ressourse (button hold time) can't be updated: ["+clickButtonID+"]");		
			LOGGER.debug("Response StatusCode "+statusCode+"\n");
			LOGGER.debug("===\n"+content+"\n===");	
			LOGGER.debug(response);
			return null;
		}
	}
	
	/*
	 * Returns an obix XML representation describing the current state for the button position of the ClickButton oneM2M resource 
	 * @param clickButtonID the ID of the ClickButton
	 * return String obix XML representation for the oneM2M button position of the ClickButton oneM2M resource 
	 */	
	public static String getFormattedClickButtonPosition(String clickButtonID){
		return ObixUtil.getStateRep(clickButtonID, getClickButtonPosition(clickButtonID));
	}
	
	/*
	 * Returns the position DOWN or UP of the ClickButton
	 * @param clickButtonID the ID of the ClickButton
	 * return ButtonPosition the actual position of the ClickButton
	 */	
	public static ButtonPosition getClickButtonPosition(String clickButtonID){
		return SampleModel.getClickButtonPosition(clickButtonID);
	}		
		
	/*
	 * Returns an obix XML representation describing the current state for the BLE peering of the ClickButton oneM2M resource 
	 * @param channel the ButtonConnectionChannel which generated the event to the Flic.io client
	 * @param clickButtonID the ID of the ClickButton
	 * return String obix XML representation for the oneM2M button BLE peering of the ClickButton oneM2M resource 
	 */	
	public static String getFormattedClickButtonPeering(String clickButtonID){
		return ObixUtil.getStateRep(clickButtonID, getClickButtonPeering(clickButtonID));
	}
	
	/*
	 * Returns the BLE peering PUBLIC or PRIVATE of the ClickButton
	 * @param clickButtonID the ID of the ClickButton
	 * return ButtonPeering the actual BLE peering of the ClickButton
	 */	
	public static ButtonPeering getClickButtonPeering(String clickButtonID){
		return SampleModel.getClickButtonPeering(clickButtonID);
	}	

	/*
	 * Returns an obix XML representation describing the current state for the Click oneM2M resource 
	 * @param clickButtonID the ID of the ClickButton
	 * return String obix XML representation for the oneM2M click of the ClickButton oneM2M resource 
	 */	
	public static String getFormattedClickButtonClick(String clickButtonID){
		return ObixUtil.getStateRep(clickButtonID, getClickButtonPeering(clickButtonID));
	}
	
	/*
	 * Returns the current Click binded to the ClickButton
	 * @param clickButtonID the ID of the ClickButton
	 * return Click the current click binded to the ClickButton
	 */	
	public static Click getClickButtonClick(String clickButtonID){
		return SampleModel.getClickButtonClick(clickButtonID);
	}	

	/*
	 * Returns an obix XML representation describing the current state for the DoubleClick oneM2M resource 
	 * @param clickButtonID the ID of the ClickButton
	 * return String obix XML representation for the oneM2M doubleclick of the ClickButton oneM2M resource 
	 */	
	public static String getFormattedClickButtonDoubleClick(String clickButtonID){
		return ObixUtil.getStateRep(clickButtonID, getClickButtonDoubleClick(clickButtonID));
	}
	
	/*
	 * Returns the current DoubleClick binded to the ClickButton
	 * @param clickButtonID the ID of the ClickButton
	 * return DoubleClick the current doubleclick binded to the ClickButton
	 */	
	public static DoubleClick getClickButtonDoubleClick(String clickButtonID){
		return SampleModel.getClickButtonDoubleClick(clickButtonID);
	}	
	
	/*
	 * Returns an obix XML representation describing the current state for the DoubleClick oneM2M resource 
	 * @param clickButtonID the ID of the ClickButton
	 * return String obix XML representation for the oneM2M doubleclick of the ClickButton oneM2M resource 
	 */	
	public static String getFormattedClickButtonHold(String clickButtonID){
		return ObixUtil.getStateRep(clickButtonID, getClickButtonHold(clickButtonID));
	}
	
	/*
	 * Returns the current duration binded to the ClickButton
	 * @param clickButtonID the ID of the ClickButton
	 * return Duration for the current hold binded of the ClickButton
	 */	
	public static Duration getClickButtonHold(String clickButtonID){
		//TBD understand Flic.io hold event
		return Duration.ZERO;
//		return SampleModel.getClickButtonHold(clickButtonID);
	}
	
	public static void setCse(CseService cse){
		CSE = cse;
	}
	
	/**
	 * Creates all (ClickButton + features) required oneM2M resources.
	 * @param clickButtonID - the Click Button ID
	 * @param buttonPosition - current click button position status
	 * @param buttonPeering - current click button peering status
	 */
	private static void createClickButtonResources(String clickButtonID, ButtonPosition buttonPosition, ButtonPeering buttonPeering) throws BadRequestException {
		String appID =  createAppID(clickButtonID); 
		String poa = SampleConstants.POA;
		LOGGER.info("***Creating oneM2M ressources for clickButton under appID ["+appID+"] & poa ["+poa+"]");

		// Create the Application resource
		Container container = new Container();
		container.getLabels().add(SampleConstants.CONTAINER_NAME);
		container.setMaxNrOfInstances(BigInteger.valueOf(0));

		AE ae = new AE();
		ae.setRequestReachability(true);
		ae.getPointOfAccess().add(poa);
		ae.setAppID(appID);
		
		ResponsePrimitive response = RequestSender.createAE(ae, appID);
		BigInteger statusCode;
		
		// Create Application sub-resources only if application not yet created
		if(response.getResponseStatusCode().equals(ResponseStatusCode.CREATED)) {
			LOGGER.info("oneM2M Application Entity for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
			container = new Container();
			container.setMaxNrOfInstances(BigInteger.valueOf(10));
			
			ResponsePrimitive containerResponse = null;
			// Create DESCRIPTOR container sub-resource
			containerResponse =  RequestSender.createContainer(response.getLocation(), SampleConstants.DESC, container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DESCRIPTOR Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DESCRIPTOR Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
 				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA POSITION Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			// Create Position STATE container sub-resource
			containerResponse =  RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_POSITION.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA POSITION Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA POSITION Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA POSITION Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			// Create BLE Peering STATE container sub-resource
			containerResponse =  RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_PEERING.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA PEERING Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA PEERING Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA PEERING Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			// Create Click STATE container sub-resource
			containerResponse =  RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_CLICK.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA CLICK Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA CLICK Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA CLICK Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			// Create Double Click STATE container sub-resource
			containerResponse = RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_DOUBLECLICK.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA DOUBLECLICK Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA DOUBLECLICK Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA DOUBLECLICK Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			// Create Hold STATE container sub-resource
			containerResponse = RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_HOLD.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA HOLD Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA HOLD Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA HOLD Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
	
			LOGGER.info("***Creating oneM2M ressources contents for clickButton under appID ["+appID+"] & poa ["+poa+"]");	
			
			String content;
			ContentInstance contentInstance = new ContentInstance();
	
			ResponsePrimitive containerContentResponse = null;
			String target = null;
			// Create DESCRIPTION contentInstance on the DESCRIPTOR container resource
			content = ObixUtil.getDescriptorRep(SampleConstants.CSE_ID, appID, clickButtonID);
			
			contentInstance.setContent(content);
			contentInstance.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + SampleConstants.DESC;
//			containerContentResponse = RequestSender.createContentInstance(target,null, contentInstance);
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_DESCRIPTOR.toString(), contentInstance);
		
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DESCRIPTION content instance for the clickButton created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DESCRIPTION content instance for the clickButton can't be created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);		
				throw new BadRequestException("oneM2M DESCRIPTION content instance for the clickButton can't be created under resource ["+target+"]");
			}
				
			// Create initial contentInstance on the STATE containers resources
			// Create initial contentInstance on the button Position STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, buttonPosition);
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_POSITION;
//			containerContentResponse = RequestSender.createContentInstance(target,null, contentInstance);
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_POSITION.toString(), contentInstance);
			
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED) | statusCode.equals(ResponseStatusCode.OK)) {
				LOGGER.info("oneM2M DATA POSITION content instance for the clickButton defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DATA POSITION content instance for the clickButton can't be defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);
				throw new BadRequestException("oneM2M DATA POSITION content instance for the clickButton can't be defined under resource ["+target+"]");
			}
	
			// Create initial contentInstance on the button BLE Peering STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, buttonPeering);
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_PEERING;
//			containerContentResponse = RequestSender.createContentInstance(target,null, contentInstance);
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_PEERING.toString(), contentInstance);
		
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED) | statusCode.equals(ResponseStatusCode.OK)) {
				LOGGER.info("oneM2M DATA PEERING content instance for the clickButton defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DATA PEERING content instance for the clickButton can't be defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);
				throw new BadRequestException("oneM2M DATA PEERING content instance for the clickButton can't be defined under resource ["+target+"]");
			}
	
			// Create initial contentInstance on the button Click STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, new Click());
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICK;
//			containerContentResponse = RequestSender.createContentInstance(target,null, contentInstance);
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_CLICK.toString(), contentInstance);
		
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED) | statusCode.equals(ResponseStatusCode.OK)) {
				LOGGER.info("oneM2M DATA CLICK content instance for the clickButton defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DATA CLICK content instance for the clickButton can't be defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);
				throw new BadRequestException("oneM2M DATA CLICK content instance for the clickButton can't be defined under resource ["+target+"]");
			}
	
			// Create initial contentInstance on the button DoubleClick STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, new DoubleClick());
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_DOUBLECLICK;
//			containerContentResponse = RequestSender.createContentInstance(target,null, contentInstance);
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_DOUBLECLICK.toString(), contentInstance);
		
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED) | statusCode.equals(ResponseStatusCode.OK)) {
				LOGGER.info("oneM2M DATA DOUBLECLICK content instance for the clickButton defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DATA DOUBLECLICK content instance for the clickButton can't be defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);
				throw new BadRequestException("oneM2M DATA DOUBLECLICK content instance for the clickButton can't be defined under resource ["+target+"]");
			}
			
			// Create initial contentInstance on the button Hold STATE container resources
			content = ObixUtil.getStateRep(clickButtonID,  Duration.ZERO);
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_HOLD;	
//			containerContentResponse = RequestSender.createContentInstance(target,null, contentInstance);
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_HOLD.toString(), contentInstance);
		
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED) | statusCode.equals(ResponseStatusCode.UPDATED) | statusCode.equals(ResponseStatusCode.OK)) {
				LOGGER.info("oneM2M DATA HOLD content instance for the clickButton defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DATA HOLD content instance for the clickButton can't be defined under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);
				throw new BadRequestException("oneM2M DATA HOLD content instance for the clickButton can't be defined under resource ["+target+"]");
			}
		} else {
			LOGGER.error("oneM2M Application Entity & ressources Containers for the clickButton were already created under appID ["+clickButtonID+"] & poa ["+poa+"]");
		}		
			
	}
	
	/**
	 * Remove all (ClickButton + features) oneM2M associated resources.
	 * @param appId - Application ID
	 * @param poa - click button Point of Access
	 */
	private static boolean removeClickButtonResources(String clickButtonID) throws BadRequestException  {
		String appID =  createAppID(clickButtonID); 
		String poa = SampleConstants.POA;
		LOGGER.info("Removing oneM2M ressources for clickButton ["+clickButtonID+"] under appID ["+appID+"] & poa ["+poa+"]");
        boolean result = true;
   
        return result;
	}
	
	/**
	 * Returns a valid oneM2M appID from the ClickButton
	 * @param clickButton - the clickButton for which to create oneM2M associated resources.
	 */
	private static String createAppID(String clickButtonID) {
		//":" char in the BLE adressing is not valid for oneM2M appID
		LOGGER.info("Generating appID oneM2M ressources for clickButton ["+clickButtonID+"]");
		return clickButtonID.replace(":", "-");
	}
}
