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

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.om2m.ipe.flicio.model.FlicDeamon;
import org.eclipse.om2m.ipe.flicio.model.SampleModel;
import org.eclipse.om2m.ipe.flicio.util.ObixUtil;

import io.flic.fliclib.javaclient.Bdaddr;
import io.flic.fliclib.javaclient.ButtonConnectionChannel;
import io.flic.fliclib.javaclient.ButtonScanner;
import io.flic.fliclib.javaclient.FlicClient;
import io.flic.fliclib.javaclient.enums.ConnectionStatus;
import io.flic.fliclib.javaclient.enums.CreateConnectionChannelError;
import io.flic.fliclib.javaclient.enums.DisconnectReason;
import io.flic.fliclib.javaclient.enums.RemovedReason;

/*
 * This object handles the operations to perform when Flic.io button events occurs
 * The Controller manages both the "real world" (memory) and the oneM2M resources models
 */
public class SampleController {
	
	public static CseService CSE;
	protected static String AE_ID;
	
	private static Log LOGGER= LogFactory.getLog(SampleController.class);
	private static Map<String,Integer[]> COUNTEREVENTS = new HashMap<String, Integer[]>();
	private static int INDEX_counterEventsPosition = 0;
	private static int INDEX_counterEventsPeering = 1;
	private static int INDEX_counterEventsClick = 2;
	private static int INDEX_counterEventsDoubleClick = 3;
	private static int INDEX_counterEventsHold = 4;
	
	private static FlicClient flicClient;	
	private static ButtonScanner buttonScanner;
	
	/*
	 * Assign the FlicClient to use for the Button Scanning
	 * @param FlicClient the FlicClient for the BLE Flic.io buttons scanning

	 */
	public static void setFlicClient(FlicClient flicClient, FlicDeamon flicDeamonHost){
		SampleController.flicClient = flicClient;
		createFlicDeamonResources(flicDeamonHost);
	}
	
	/*
	 * Create the Flic.io Button scanner
	 */
	public static ButtonScanner createFlicButtonScanner() {  
		buttonScanner = new ButtonScanner() {
            @Override
            /**
             * This will be called for every received advertisement packet from a Flic button.
             *
             * @param bdaddr Bluetooth address
             * @param name Advertising name
             * @param rssi RSSI value in dBm
             * @param isPrivate The button is private and won't accept new connections from non-bonded clients
             * @param alreadyVerified The server has already verified this button, which means you can connect to it even if it's private
             */
            public void onAdvertisementPacket(final Bdaddr bdaddr, String name, int rssi, boolean isPrivate, boolean alreadyVerified) throws IOException {

                if (alreadyVerified) {
                	LOGGER.info("Flic.io button ["+bdaddr+"] is already associated; currrent RSSI signal strenght is ["+rssi+"]");
                    return;
                }
                if (isPrivate) {
            		LOGGER.info("Discovered Flic.io button ["+bdaddr+"], with currrent RSSI signal strenght of ["+rssi+"], in private mode. Hold it down for 7 seconds to make it public mode.");
            		
            		//TBD Send notification to the Web interface which interact with the user !!!
            		
                } else {            		
            		ButtonConnectionChannel.Callbacks buttonCallbacks = new ButtonConnectionChannel.Callbacks() {
            			
            		     /**
            	         * Called when the server has received the create connection channel command.
            	         *
            	         * If createConnectionChannelError is {@link CreateConnectionChannelError#NoError}, other events will arrive until {@link #onRemoved} is received.
            	         * There will be no {@link #onRemoved} if an error occurred.
            	         *
            	         * @param channel
            	         * @param createConnectionChannelError
            	         * @param connectionStatus
            	         * @throws IOException
            	         */
                        @Override
                        public void onCreateConnectionChannelResponse(final ButtonConnectionChannel channel, CreateConnectionChannelError createConnectionChannelError, ConnectionStatus connectionStatus) throws IOException {
                            if (connectionStatus == ConnectionStatus.Ready) {
                            	LOGGER.info("----onCreateConnectionChannelResponse: BLE Channel ["+channel.getBdaddr()+ "] from Flic.io Button Scanner added successfully a new Flic.io Button !");                           	
                            } else if (createConnectionChannelError != CreateConnectionChannelError.NoError) {
                            	LOGGER.error("---onCreateConnectionChannelResponse: BLE Channel [" + channel.getBdaddr() + "] can't be created by Flic.io Button Scanner: " + createConnectionChannelError + ", " + connectionStatus+". Waiting next attempt");
                            } else {
                            	LOGGER.info("----onCreateConnectionChannelResponse: BLE Channel ["+channel.getBdaddr()+ "] from Flic.io Button Scanner not yet READY !");
  /*                          	
                            	//TB UNDERSTAND WHY THIS TIMERTASK is required as show in the Flic API example??
                            	flicClient.setTimer(30 * 1000, new TimerTask() {
                                    @Override
                                    public void run() throws IOException {
                                    	//as soon as the Flic.io Button is associated with the Flic.io network deamon, 
                                    	//we do not need this connection channel to the Button
                                    	//the Button will be discovered by the GeneralCallbacks thru onNewVerifiedButton 
                                    	//and a new connection channel for monitoring Button event will be created
                                    	LOGGER.info("----onCreateConnectionChannelResponse: removing BLE Channel ["+channel.getBdaddr()+ "] from Flic.io Button Scanner after association of a new Flic.io Button !");
                                    	flicClient.removeConnectionChannel(channel);
                                    }
                                });
  */
                            }
                        }

                        /**
                         * Called when the connection channel has been removed.
                         *
                         * Check the removedReason to find out why. From this point, the connection channel can be re-added again if you wish.
                         *
                         * @param channel
                         * @param removedReason
                         * @throws IOException
                         */
                        @Override
                        public void onRemoved(ButtonConnectionChannel channel, RemovedReason removedReason) throws IOException {
                        	LOGGER.info("----onRemoved: BLE Channel [" + channel.getBdaddr() + "] removed from Flic.io Button Scanner: "+removedReason);
                        }

                        /**
                         * Called when the connection status changes.
                         *
                         * @param channel
                         * @param connectionStatus
                         * @param disconnectReason Only valid if connectionStatus is {@link ConnectionStatus#Disconnected}
                         * @throws IOException
                         */
                        @Override
                        public void onConnectionStatusChanged(ButtonConnectionChannel channel, ConnectionStatus connectionStatus, DisconnectReason disconnectReason) throws IOException {
                            if (connectionStatus == ConnectionStatus.Ready) {
                            	LOGGER.info("----onConnectionStatusChanged: BLE Channel [" + channel.getBdaddr() + "] from Flic.io Button Scanner added successfully a new Flic.io Button !");
                            	//as soon as the Flic.io Button is associated with the Flic.io network deamon, 
                            	//we do not need this connection channel to the Button
                            	//the Button will be discovered by the GeneralCallbacks thru onNewVerifiedButton 
                            	//and a new connection channel for monitoring Button event will be created
                            	LOGGER.info("----onConnectionStatusChanged: removing BLE Channel ["+channel.getBdaddr()+ "] from Flic.io Button Scanner after association of a new Flic.io Button !");
                            	flicClient.removeConnectionChannel(channel);                  
                            }
                        }
                    };
            		
            		ButtonConnectionChannel buttonConnectionChannel = new ButtonConnectionChannel(bdaddr, buttonCallbacks);
              		LOGGER.info("Discovered new public Flic.io button ["+bdaddr+"], with currrent RSSI signal strenght of ["+rssi+"], now connecting...");
                    flicClient.addConnectionChannel(buttonConnectionChannel);
                }
            }
        };
        return buttonScanner;
	}
	
	/*
	 * Start the Flic.io Button scanner
	 */
	public static void startFlicScanner() {  
		// Start Scanning for new Button(s) on the Flic.io network DEAMON
		LOGGER.info(SampleConstants.AE_NAME+": Starting Flic.io Button Scanner");

		buttonScanner = createFlicButtonScanner();
		
		if (buttonScanner!=null) {
			try {
				flicClient.addScanner(buttonScanner);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error("Flic.io oneM2M IPE IO error: "+e.toString());
			}
			LOGGER.info(SampleConstants.AE_NAME+": Flic.io Button Scanner added");
			LOGGER.info(SampleConstants.AE_NAME+": Flic.io Button Scanner started. Expecting user interaction [pressing the Flic.io button to pair with]");
		}
		 else {
				LOGGER.info(SampleConstants.AE_NAME+": Flic.io Button Scanner unavailable");
				LOGGER.error(SampleConstants.AE_NAME+": Flic.io Button Scanner was not started");		
		}
	}

	
	/*
	 * Stop the Flic.io Button scanner
	 */
	public static void stopFlicScanner() {  
		// Stop Scanning for new Button(s) on the Flic.io network DEAMON
		LOGGER.info(SampleConstants.AE_NAME+": Stopping Flic.io Button Scanner");
		
		if (buttonScanner!=null) {
			try {
				flicClient.removeScanner(buttonScanner);
				buttonScanner = null;
			} catch (IOException e) {
				LOGGER.error("Flic.io oneM2M IPE IO error: "+e.toString());
			}
			LOGGER.info(SampleConstants.AE_NAME+": Flic.io Button Scanner removed");
			LOGGER.info(SampleConstants.AE_NAME+": Flic.io Button stopped");
		} else {
			LOGGER.error(SampleConstants.AE_NAME+": Flic.io Button Scanner was not started");		
		}
	}
	
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
			
			Integer[] counters = new Integer[5];
			counters[INDEX_counterEventsPosition]=1;
			counters[INDEX_counterEventsPeering]=1;
			counters[INDEX_counterEventsClick]=1;
			counters[INDEX_counterEventsDoubleClick]=1;
			counters[INDEX_counterEventsHold]=1;
			COUNTEREVENTS.put(clickButton.getButtonID(), counters);
			
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
			
			COUNTEREVENTS.remove(clickButtonID);
			
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
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID +"/"+ BUTTON_FEATURE.DATA_BUTTON + "/" + BUTTON_FEATURE.DATA_POSITION;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, buttonPosition);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		Integer counterevents[] = COUNTEREVENTS.get(clickButtonID);
		int counterEventsPosition = counterevents[INDEX_counterEventsPosition]++;
		
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
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + SampleConstants.CONTAINER_NAME_CLICKBUTTON + "/" + BUTTON_FEATURE.DATA_PEERING;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, buttonPeering);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		Integer counterevents[] = COUNTEREVENTS.get(clickButtonID);
		int counterEventsPeering = counterevents[INDEX_counterEventsPeering]++;

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
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICKS + "/" + BUTTON_FEATURE.DATA_CLICK;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, click);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);		
		Integer counterevents[] = COUNTEREVENTS.get(clickButtonID);
		int counterEventsClick = counterevents[INDEX_counterEventsClick]++;

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
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICKS + "/" + BUTTON_FEATURE.DATA_DOUBLECLICK;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, doubleClick);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		Integer counterevents[] = COUNTEREVENTS.get(clickButtonID);
		int counterEventsDoubleClick = counterevents[INDEX_counterEventsDoubleClick]++;

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
		String targetID = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_BUTTON + "/" + BUTTON_FEATURE.DATA_HOLD;
		ContentInstance cin = new ContentInstance();
		String content = ObixUtil.getStateRep(clickButtonID, holdTime);
		cin.setContent(content);
		cin.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);

//		ResponsePrimitive response = RequestSender.createContentInstance(targetID, null, cin);
		Integer counterevents[] = COUNTEREVENTS.get(clickButtonID);
		int counterEventsHold = counterevents[INDEX_counterEventsHold]++;

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
		return ObixUtil.getStateRep(clickButtonID, getClickButtonClick(clickButtonID));
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
	 * @param FlicDeamon - the FlicDeamon reachability information
	 */
	private static void createFlicDeamonResources(FlicDeamon flicDeamonHost){
		String appID =  createAppID(flicDeamonHost); 
//		String poa = SampleConstants.POA+"-"+FlicDeamon.TYPE;
		String poa = SampleConstants.POA;
		LOGGER.info("***Creating oneM2M ressources for Flic.io network deamon under appID ["+appID+"] & poa ["+poa+"]");

		// Create the Application resource
		AE ae = new AE();
		ae.setRequestReachability(true);
		ae.getPointOfAccess().add(poa);
		ae.setAppID(appID);
		ae.setAppName(poa);
		
		ResponsePrimitive response = RequestSender.createAE(ae, appID);
		BigInteger statusCode;
		
		// Create Application sub-resources only if application not yet created
		if(response.getResponseStatusCode().equals(ResponseStatusCode.CREATED)) {
			LOGGER.info("oneM2M Application Entity for the Flic.io network deamon created under appID ["+appID+"] & poa ["+poa+"]");
			Container container = new Container();
			container.setName(SampleConstants.CONTAINER_NAME_FLICDEAMON);
			container.getLabels().add(SampleConstants.CONTAINER_NAME_FLICDEAMON);
			container.setMaxNrOfInstances(BigInteger.valueOf(1));
			
			ResponsePrimitive containerResponse = null;
			// Create DESCRIPTOR container sub-resource
			containerResponse =  RequestSender.createContainer(response.getLocation(), SampleConstants.CONTAINER_NAME_FLICDEAMON, container);
			
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DESCRIPTOR Container for the Flic.io network deamon created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DESCRIPTOR Container for the Flic.io network deamon can't be created under appID ["+appID+"] & poa ["+poa+"]");
 				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DESCRIPTOR Container for the Flic.io network deamon can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
						
			String content;
			ContentInstance contentInstance = new ContentInstance();
	
			ResponsePrimitive containerContentResponse = null;
			String target = null;
			// Create DESCRIPTION contentInstance on the DESCRIPTOR container resource
			content = ObixUtil.getDescriptorRep_FlicDeamon(SampleConstants.CSE_ID, appID, flicDeamonHost.getFlicDeamonID());
			
			contentInstance.setContent(content);
			contentInstance.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + SampleConstants.CONTAINER_NAME_FLICDEAMON;
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_DESCRIPTOR.toString(), contentInstance);
		
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DESCRIPTOR content instance for the Flic.io network deamon created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DESCRIPTOR content instance for the Flic.io network deamon can't be created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);		
				throw new BadRequestException("oneM2M DESCRIPTION content instance for the Flic.io network deamon can't be created under resource ["+target+"]");
			}
				
		} else {
			LOGGER.info("oneM2M Application Entity & ressource Container for the Flic.io network deamon were already created under appID ["+appID+"] & poa ["+poa+"]");
		}	
	}
	
	/**
	 * Creates all (ClickButton + features) required oneM2M resources.
	 * @param clickButtonID - the Click Button ID
	 * @param buttonPosition - current click button position status
	 * @param buttonPeering - current click button peering status
	 */
	private static void createClickButtonResources(String clickButtonID, ButtonPosition buttonPosition, ButtonPeering buttonPeering) throws BadRequestException {
		String appID =  createAppID(clickButtonID); 
//		String poa = SampleConstants.POA+"-"+ClickButton.TYPE;
		String poa = SampleConstants.POA;
		LOGGER.info("***Creating oneM2M ressources for clickButton under appID ["+appID+"] & poa ["+poa+"]");

		// Create the Application resource
		AE ae = new AE();
		ae.setRequestReachability(true);
		ae.getPointOfAccess().add(poa);
		ae.setAppID(appID);
		ae.setAppName(poa);
		
		ResponsePrimitive response = RequestSender.createAE(ae, appID);
		BigInteger statusCode;
		
		// Create Application sub-resources only if application not yet created
		if(response.getResponseStatusCode().equals(ResponseStatusCode.CREATED)) {
			LOGGER.info("oneM2M Application Entity for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
			Container container = new Container();
			container.getLabels().add(SampleConstants.CONTAINER_NAME_CLICKBUTTON);
			container.setMaxNrOfInstances(BigInteger.valueOf(10));
			
			ResponsePrimitive containerResponse = null;
			ResponsePrimitive containerResponseTMP = null;
			
			//========================================================
			//group the DESCRIPTOR & BUTTON_FEATURE.DATA_PEERING features into a specific subcontainer
			// Create DESCRIPTOR container sub-resource
			container.setName(SampleConstants.CONTAINER_NAME_CLICKBUTTON);
			containerResponse =  RequestSender.createContainer(response.getLocation(), SampleConstants.CONTAINER_NAME_CLICKBUTTON, container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DESCRIPTOR Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DESCRIPTOR Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
 				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DESCRIPTOR Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			containerResponseTMP = containerResponse;
			
			//===========================================
			// Create BLE Peering STATE container sub-resource
			container.setName(BUTTON_FEATURE.DATA_PEERING.toString());
			containerResponse =  RequestSender.createContainer(containerResponseTMP.getLocation(), BUTTON_FEATURE.DATA_PEERING.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA PEERING Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA PEERING Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA PEERING Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			//========================================================
			
			//========================================================
			//group the BUTTON_FEATURE.DATA_POSITION & BUTTON_FEATURE.DATA_HOLD features into a specific subcontainer			
			// Create Button STATE container sub-resource
			container.setName(BUTTON_FEATURE.DATA_BUTTON.toString());
			containerResponse =  RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_BUTTON.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M BUTTON DESCRIPTOR Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M BUTTON DESCRIPTOR Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M BUTTON Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			containerResponseTMP = containerResponse;
		
			// Create Position STATE container sub-resource
			container.setName(BUTTON_FEATURE.DATA_POSITION.toString());
			containerResponse =  RequestSender.createContainer(containerResponseTMP.getLocation(), BUTTON_FEATURE.DATA_POSITION.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA POSITION Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA POSITION Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA POSITION Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}

			// Create Hold STATE container sub-resource
			container.setName(BUTTON_FEATURE.DATA_HOLD.toString());
			containerResponse = RequestSender.createContainer(containerResponseTMP.getLocation(), BUTTON_FEATURE.DATA_HOLD.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA HOLD Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA HOLD Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA HOLD Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			//========================================================

			//========================================================
			//group the BUTTON_FEATURE.DATA_CLICK & BUTTON_FEATURE.DATA_DOUBLECLICK features into a specific subcontainer
			
			// Create ClickS STATE container sub-resource
			container.setName(BUTTON_FEATURE.DATA_CLICKS.toString());
			containerResponse =  RequestSender.createContainer(response.getLocation(), BUTTON_FEATURE.DATA_CLICKS.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M CLICKS DESCRIPTOR Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M CLICKS DESCRIPTOR Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M CLICKS Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			
			containerResponseTMP = containerResponse;
			
				// Create Click STATE container sub-resource
			container.setName(BUTTON_FEATURE.DATA_CLICK.toString());
			containerResponse =  RequestSender.createContainer(containerResponseTMP.getLocation(), BUTTON_FEATURE.DATA_CLICK.toString(), container);
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
			container.setName(BUTTON_FEATURE.DATA_DOUBLECLICK.toString());
			containerResponse = RequestSender.createContainer(containerResponseTMP.getLocation(), BUTTON_FEATURE.DATA_DOUBLECLICK.toString(), container);
			statusCode = containerResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DATA DOUBLECLICK Container for the clickButton created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.info(containerResponse);
			} else {
				LOGGER.error("oneM2M DATA DOUBLECLICK Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
				LOGGER.error(containerResponse);
				throw new BadRequestException("oneM2M DATA DOUBLECLICK Container for the clickButton can't be created under appID ["+appID+"] & poa ["+poa+"]");
			}
			//========================================================

			//////////////////////////////////////////////////////////////
			// Create initial contentInstance on the STATE containers resources
			
			String content;
			ContentInstance contentInstance = new ContentInstance();
	
			ResponsePrimitive containerContentResponse = null;
			String target = null;
			
			// Create DESCRIPTOR contentInstance on the DESCRIPTOR container resource
			content = ObixUtil.getDescriptorRep_ClickButton(SampleConstants.CSE_ID, appID, clickButtonID);
			
			contentInstance.setContent(content);
			contentInstance.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + SampleConstants.CONTAINER_NAME_CLICKBUTTON;
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_DESCRIPTOR.toString(), contentInstance);			
			
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M DESCRIPTOR content instance for the clickButton created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M DESCRIPTOR content instance for the clickButton can't be created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);		
				throw new BadRequestException("oneM2M DESCRIPTOR content instance for the clickButton can't be created under resource ["+target+"]");
			}
			
			//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
			
			
			// Create initial contentInstance on the button BLE Peering STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, buttonPeering);
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + SampleConstants.CONTAINER_NAME_CLICKBUTTON + "/" + BUTTON_FEATURE.DATA_PEERING;
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
			
			//===========================================
			//the BUTTON_FEATURE.DATA_POSITION & BUTTON_FEATURE.DATA_HOLD features are grouped into a specific subcontainer
			
			// Create BUTTON DESCRIPTION contentInstance on the BUTTON DESCRIPTOR container resource
			content = ObixUtil.getDescriptorRep_Button(SampleConstants.CSE_ID, appID, clickButtonID);
			
			contentInstance.setContent(content);
			contentInstance.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_BUTTON;
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_DESCRIPTOR.toString(), contentInstance);			
			
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M BUTTON DESCRIPTION content instance for the clickButton created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M BUTTON DESCRIPTION content instance for the clickButton can't be created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);		
				throw new BadRequestException("oneM2M BUTTON DESCRIPTION content instance for the clickButton can't be created under resource ["+target+"]");
			}
			
			// Create initial contentInstance on the button Position STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, buttonPosition);
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_BUTTON + "/" + BUTTON_FEATURE.DATA_POSITION;
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
	
			// Create initial contentInstance on the button Hold STATE container resources
			content = ObixUtil.getStateRep(clickButtonID,  Duration.ZERO);
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_BUTTON + "/" + BUTTON_FEATURE.DATA_HOLD;	
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
			
			//===========================================
			//the BUTTON_FEATURE.DATA_CLICK & BUTTON_FEATURE.DATA_CLICK features are grouped into a specific subcontainer
			// Create BUTTON DESCRIPTION contentInstance on the BUTTON DESCRIPTOR container resource
			content = ObixUtil.getDescriptorRep_Clicks(SampleConstants.CSE_ID, appID, clickButtonID);
			
			contentInstance.setContent(content);
			contentInstance.setContentInfo(MimeMediaType.OBIX + ":" + MimeMediaType.ENCOD_PLAIN);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICKS;
			containerContentResponse = RequestSender.createContentInstance(target,Operations.SET_STATE_DESCRIPTOR.toString(), contentInstance);			
			
			statusCode = containerContentResponse.getResponseStatusCode();
			if(statusCode.equals(ResponseStatusCode.CREATED)) {
				LOGGER.info("oneM2M CLICKS DESCRIPTION content instance for the clickButton created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.info(containerContentResponse);
			} else {
				LOGGER.error("oneM2M CLICKS DESCRIPTION content instance for the clickButton can't be created under resource ["+target+"]");
				LOGGER.debug("===\n"+content+"\n===");
				LOGGER.error(containerContentResponse);		
				throw new BadRequestException("oneM2M CLICKS DESCRIPTION content instance for the clickButton can't be created under resource ["+target+"]");
			}
			
			
			// Create initial contentInstance on the button Click STATE container resources
			content = ObixUtil.getStateRep(clickButtonID, new Click());
			contentInstance.setContent(content);
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICKS + "/" + BUTTON_FEATURE.DATA_CLICK;
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
			target = SampleConstants.CSE_PREFIX + "/" + appID + "/" + BUTTON_FEATURE.DATA_CLICKS + "/"+ BUTTON_FEATURE.DATA_DOUBLECLICK;
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
			

		} else {
			LOGGER.info("oneM2M Application Entity & ressources Containers for the clickButton were already created under appID ["+appID+"] & poa ["+poa+"]");
		}		
			
	}
	
	/**
	 * Remove all (ClickButton + features) oneM2M associated resources.
	 * @param appId - Application ID
	 * @param poa - click button Point of Access
	 */
	private static boolean removeClickButtonResources(String clickButtonID) throws BadRequestException  {
		String appID =  createAppID(clickButtonID); 
		String poa = SampleConstants.POA+"-"+ClickButton.TYPE;
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
		return SampleConstants.MN_AE_PREFIX+"_"+clickButtonID.replace(":", "-");
	}
	
	/**
	 * Returns a valid oneM2M appID from the Flic.io Network Deamon used to listen the Flic.io BLE Buttons 
	 * @param FlicDeamon - the FlicDeamon to which the FlicClient is connected
	 */
	private static String createAppID(FlicDeamon flicDeamonHost) {
		LOGGER.info("Generating appID oneM2M ressources for Flic.io Network Deamon ["+flicDeamonHost+"]");
		return SampleConstants.MN_AE_PREFIX+"_"+flicDeamonHost.getFlicDeamonID();
	}
	
}
