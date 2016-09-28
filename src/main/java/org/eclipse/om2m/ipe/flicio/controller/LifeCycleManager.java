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
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPeering;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.ButtonPosition;
import org.eclipse.om2m.ipe.flicio.model.Click;
import org.eclipse.om2m.ipe.flicio.model.ClickButton;
import org.eclipse.om2m.ipe.flicio.model.ClickButtonModel;
import org.eclipse.om2m.ipe.flicio.model.DoubleClick;
import org.eclipse.om2m.ipe.flicio.model.FlicDeamon;

import io.flic.fliclib.javaclient.Bdaddr;
import io.flic.fliclib.javaclient.ButtonConnectionChannel;
import io.flic.fliclib.javaclient.FlicClient;
import io.flic.fliclib.javaclient.GeneralCallbacks;
import io.flic.fliclib.javaclient.GetInfoResponseCallback;
import io.flic.fliclib.javaclient.enums.BdAddrType;
import io.flic.fliclib.javaclient.enums.BluetoothControllerState;
import io.flic.fliclib.javaclient.enums.ClickType;
import io.flic.fliclib.javaclient.enums.ConnectionStatus;
import io.flic.fliclib.javaclient.enums.CreateConnectionChannelError;
import io.flic.fliclib.javaclient.enums.DisconnectReason;
import io.flic.fliclib.javaclient.enums.RemovedReason;
 
public class LifeCycleManager {

	private static Log LOGGER = LogFactory.getLog(LifeCycleManager.class); 
	private static Map<String, ClickButton> clickButtons;
	
	private static FlicDeamon flicDeamonHost;
	private static FlicClient flicClient = null;
	
	private static boolean HANDLE_HOLD_EVENT_onButtonClickOrHold= false;
	private static boolean HANDLE_HOLD_EVENT_onButtonSingleOrDoubleClickOrHold = !HANDLE_HOLD_EVENT_onButtonClickOrHold;
	private static boolean HANDLE_CLICK_EVENT= false;
	private static boolean HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClick = !HANDLE_CLICK_EVENT;
	private static boolean HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClickOrHold = !HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClick;
	private static boolean HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClick= false;
	private static boolean HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClickOrHold = !HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClick;

	/*
	 * Initialise a FlicDeamon Network Host for the FlicCient to connect to
	 */
	public static void defineFlicDeamon() {  
		//default parameter are used
		flicDeamonHost = new FlicDeamon();
		//load config from MANIFEST
		String configFile ="TOBEDONE"; 
		LOGGER.info("Reading Flic Deamon Network Socket configuration from: ["+configFile+"]");
		flicDeamonHost.loadConfig();
	}
	
	/*
	 * Create the FlicClient to the FlicDeamon
	 * @return FlicCient
	 */
	public static FlicClient getFlicClient() {    
		defineFlicDeamon();
		LOGGER.info(SampleConstants.AE_NAME+": connecting to Flic.io network Daemon ["+flicDeamonHost+"]");
		boolean connecting=true;
		while(connecting)
		{
			try {
				LOGGER.info(SampleConstants.AE_NAME+": trying to connect to Flic.io network Daemon ["+flicDeamonHost+"]");
				flicClient = new FlicClient(flicDeamonHost.getHostname(),flicDeamonHost.getNetworkPort());
				LOGGER.info(SampleConstants.AE_NAME+": Connection to Flic.io network Daemon ["+flicDeamonHost+"] succedded");
				connecting=false;
			} catch (UnknownHostException e) {
				LOGGER.error("Flic.io oneM2M IPE can't connect to an unknown host: "+e.toString()+"Please modify the Flic.io network Deamon to connect to");
				connecting=false;
				//host is not known so could never connect to it
				return null;
			} catch (IOException e) {
				LOGGER.error("Flic.io oneM2M IPE can't connect to Flic.io network Daemon ["+flicDeamonHost+"]: "+e.toString()+".\nWaiting ["+SampleConstants.FLIC_DEAMON_CONNECTION_RETRY+"] seconds and trying again");
				//Reconnect to Flic.io Daemon if connection is lost
		        try
		        {
		            Thread.sleep(SampleConstants.FLIC_DEAMON_CONNECTION_RETRY*1000);//in seconds
		        }
		        catch(InterruptedException ie){
		            ie.printStackTrace();
		        }
			}
		}
		return flicClient;
	}
	


	/*
	 * The Flic.io button events handler
	 * Everytime a button event occur, we operate the required actions:
	 * -creating a new button and the oneM2M resources
	 * -updating the button status and corresponding oneM2M resources
	 * -removing a button and the oneM2M resources
	 * The operations are delegated to the SampleController
	 */
	private static ButtonConnectionChannel.Callbacks buttonCallbacks = new ButtonConnectionChannel.Callbacks() {
		/** Flic.io Semantic for io.flic.fliclib.javaclient.enums.ClickType
		 * ButtonDown 			- The button was pressed.
		 * ButtonUp		 		- The button was released.
		 * ButtonClick 			- The button was clicked, and was held for at most 1 seconds between press and release.
		 * ButtonSingleClick	- The button was clicked once.
		 * ButtonDoubleClick 	- The button was clicked twice. The time between the first and second press must be at most 0.5 seconds.
		 * ButtonHold 			- The button was held for at least 1 second.
		 */

		@Override
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
        public void onCreateConnectionChannelResponse(ButtonConnectionChannel channel, CreateConnectionChannelError createConnectionChannelError, ConnectionStatus connectionStatus) {
        	LOGGER.info("---onCreateConnectionChannelResponse: Channel [" + channel.getBdaddr() + "] created: " + createConnectionChannelError + ", " + connectionStatus);
        	
        	ClickButton clickButton = SampleController.addClickButton(channel);
            if (clickButton != null) {
    			LOGGER.info("----onCreateConnectionChannelResponse: Channel associated to a new clickButton: ["+clickButton.getButtonID()+"]");
            } else {
       			LOGGER.error("----onCreateConnectionChannelResponse: Channel [" + channel.getBdaddr() + "] can't be associated to a new clickButton");
            }
        }
        
        @Override
        /**
         * Called when the connection channel has been removed.
         *
         * Check the removedReason to find out why. From this point, the connection channel can be re-added again if you wish.
         *
         * @param channel
         * @param removedReason
         * @throws IOException
         */
        public void onRemoved(ButtonConnectionChannel channel, RemovedReason removedReason) {
        	LOGGER.info("---onRemoved: Channel [" + channel.getBdaddr() + "] removed: " + removedReason);
            
	         String clickButtonID = SampleController.removeClickButton(channel);
	         if (clickButtonID != null) {
	 			LOGGER.info("----onRemoved: BLE Channel unassociated for the clickButton: ["+clickButtonID+"]");
	         } else {
		 		LOGGER.error("----onRemoved: BLE Channel [" + channel.getBdaddr() + "] can't be unassotiated from any clickButton");
	         }    	 
        }

        @Override
        /**
         * Called when the connection status changes.
         *
         * @param channel
         * @param connectionStatus
         * @param disconnectReason Only valid if connectionStatus is {@link ConnectionStatus#Disconnected}
         * @throws IOException
         */
        public void onConnectionStatusChanged(ButtonConnectionChannel channel, ConnectionStatus connectionStatus, DisconnectReason disconnectReason) {
			/*	DisconnectReason:
			 *     Unspecified,
			 *     ConnectionEstablishmentFailed,
			 *     TimedOut,
			 *     BondingKeysMismatch
			 */     	
	     	LOGGER.info("----onConnectionStatusChanged: New status for BLE channel [" + channel.getBdaddr() + "]: " + connectionStatus + (connectionStatus == ConnectionStatus.Disconnected ? ", " + disconnectReason : ""));
	 
	     	String clickButtonID = null;
        	switch (connectionStatus) {
        		case Connected:
	                 clickButtonID = ClickButton.createButtonID(channel);
	       	         if (clickButtonID != null) {
	       	 			LOGGER.info("----onConnectionStatusChanged: BLE channel [" + channel.getBdaddr() + "] is ["+connectionStatus+"] for the clickButton: ["+clickButtonID+"]");
	       	         } else {
	       		 		LOGGER.error("----onConnectionStatusChanged: BLE channel [" + channel.getBdaddr() + "] ["+connectionStatus+"] state can't be updated from any clickButton");
	       	         }
	       	         break;
	       	         
	       	    case Ready:       
	            	 // Set BLE peering to private
	                 clickButtonID = SampleController.setClickButtonPeering(channel, ButtonPeering.buttonprivate);
	       	         if (clickButtonID != null) {
	       	 			LOGGER.info("----onConnectionStatusChanged: BLE channel [" + channel.getBdaddr() + "] is ["+connectionStatus+"] for the clickButton: ["+clickButtonID+"]");
	       	         } else {
	       		 		LOGGER.error("----onConnectionStatusChanged: BLE channel [" + channel.getBdaddr() + "] ["+connectionStatus+"] state can't be updated from any clickButton");
	       	         }
	       	         break;
       	         
         		case Disconnected:   
	               	 // Set BLE peering to public
	                 clickButtonID = SampleController.setClickButtonPeering(channel, ButtonPeering.buttonpublic);
	      	         if (clickButtonID != null) {
	      	 			LOGGER.info("----onConnectionStatusChanged: BLE channel [" + channel.getBdaddr() + "] is disconnected for the clickButton: ["+clickButtonID+"]");
	      	         } else {
	      		 		LOGGER.error("----onConnectionStatusChanged: BLE channel [" + channel.getBdaddr() + "] disconnected state can't be updated from any clickButton");
	      	         }
	      	         break;  			
         			
        		default:
        	       	   clickButtonID = ClickButton.createButtonID(channel);
                	   LOGGER.error("----onConnectionStatusChanged: Flic.io event (["+connectionStatus+"]) from BLE channel [" + channel.getBdaddr() + "] for the clickButton: ["+clickButtonID+"]");
                	   break;
        		}
        	}   
        
        @Override
        public void onButtonUpOrDown(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	LOGGER.info("----onButtonUpOrDown: Position event on BLE channel [" +channel.getBdaddr() + "] " + (clickType == ClickType.ButtonUp ? "Up" : "Down")+ "; was queued "+wasQueued+", time diff: "+timeDiff);

            String clickButtonID;
            switch (clickType) {
            case ButtonDown: 
            	clickButtonID = SampleController.setClickButtonPosition(channel, ButtonPosition.buttondown);
                if (clickButtonID != null) {
                	LOGGER.info("----onButtonUpOrDown: Position event updated (DOWN) for the clickButton: ["+clickButtonID+"]");
                } else {
                	LOGGER.error("----onButtonUpOrDown: Position event from BLE channel [" + channel.getBdaddr() + "] can't be updated (DOWN) from any clickButton");                	
                }
            	break;
            
            case ButtonUp: 
            	clickButtonID = SampleController.setClickButtonPosition(channel, ButtonPosition.buttonup);
            	
                if (clickButtonID != null) {
                	Duration duration = SampleController.getClickButtonClick(clickButtonID).getClickHold();
                	LOGGER.info("----onButtonUpOrDown: Position event updated (UP) for the clickButton: ["+clickButtonID+"], duration of the click was ["+duration+"]");           		
                } else {
               	 	LOGGER.error("----onButtonUpOrDown: Position event from BLE channel [" + channel.getBdaddr() + "] can't be updated (UP) from any clickButton");
                }
                break;
                
           default:
        	   clickButtonID = ClickButton.createButtonID(channel);
        	   LOGGER.error("----onButtonUpOrDown: Flic.io event (["+clickType.toString()+"]) from BLE channel [" + channel.getBdaddr() + "] for the clickButton: ["+clickButtonID+"]");
	           /**   ButtonClick,
	            *    ButtonSingleClick,
	            *    ButtonDoubleClick,
	            *    ButtonHold
	           */ 
               break;
            }
         }
        
        @Override
        public void onButtonClickOrHold(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	LOGGER.info("----onButtonClickOrHold: Click or Hold event on channel [" +channel.getBdaddr() + "] " + (clickType == ClickType.ButtonClick ? "Click" : "Hold")+ "; was queued "+wasQueued+", time diff: "+timeDiff);

            String clickButtonID;
            switch (clickType) {
            case ButtonClick: 
              	if (HANDLE_CLICK_EVENT) {     	
	            	Click click = new Click(); 
	            	clickButtonID = SampleController.setClickButtonClick(channel, click);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonClickOrHold: Click event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                   	LOGGER.error("----onButtonClickOrHold: Click event can't be updated for the clickButton: ["+clickButtonID+"]");
	                }
	          	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
	            	LOGGER.info("----onButtonClickOrHold: Click event handling defined to **Single click** for : ["+clickButtonID+"]");            		
	        	}
                break;
            
            case ButtonHold: 
            	if (HANDLE_HOLD_EVENT_onButtonClickOrHold) {
	            	Duration buttonHoldTime;
	            	//TBD determine what is the Flic.io duration
	            	buttonHoldTime = Duration.ZERO;
	            	clickButtonID = SampleController.setClickButtonHold(channel,buttonHoldTime);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonClickOrHold: Hold event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                   	LOGGER.error("----onButtonClickOrHold: Hold event can't be updated for the clickButton: ["+clickButtonID+"]");               	
	                }
	          	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
	            	LOGGER.info("----onButtonClickOrHold: Hold event handling defined to **onButtonSingleOrDoubleClickOrHold** for : ["+clickButtonID+"]");            		
	        	}

                break;
                
           default:
        	   clickButtonID = ClickButton.createButtonID(channel);
        	   LOGGER.info("----onButtonClickOrHold: Flic.io event (["+clickType.toString()+"]) from BLE channel [" + channel.getBdaddr() + "] for the clickButton: ["+clickButtonID+"]");
	           /**   ButtonUp,
	            * 	 ButtonDown
	            * 	 ButtonClick,
	            *    ButtonHold,
	           */ 
               break;
            }   
        }

        @Override
        public void onButtonSingleOrDoubleClick(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	LOGGER.info("---onButtonSingleOrDoubleClick: Single/Double click event on channel [" +channel.getBdaddr() + "] " + (clickType == ClickType.ButtonSingleClick ? "SingleClick" : "DoubleClick")+ "; was queued "+wasQueued+", time diff: "+timeDiff);
   
            String clickButtonID;
            switch (clickType) {
            case ButtonSingleClick: 
              	if (HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClick) {
	            	Click click = new Click();
	            	clickButtonID = SampleController.setClickButtonClick(channel, click);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonSingleOrDoubleClick: Single click event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                	LOGGER.error("----onButtonSingleOrDoubleClick: Single click event can't be updated for the clickButton: ["+clickButtonID+"]");                	
	                }
              	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
                	LOGGER.info("----onButtonSingleOrDoubleClick: Single click event handling replaced by **Click** event handling for : ["+clickButtonID+"]");            		
            	}
                break;
            
            case ButtonDoubleClick: 
            	if (HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClick) {
	            	DoubleClick doubleClick = new DoubleClick();
	            	clickButtonID = SampleController.setClickButtonDoubleClick(channel, doubleClick);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonSingleOrDoubleClick: Double click event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                	LOGGER.error("----onButtonSingleOrDoubleClick: Double click event can't be updated for the clickButton: ["+clickButtonID+"]");               
	                }
              	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
                	LOGGER.info("----onButtonSingleOrDoubleClick: Double click event handling defined to **onButtonSingleOrDoubleClickOrHold** for : ["+clickButtonID+"]");            		
            	}   
                break;
                
           default:
        	   clickButtonID = ClickButton.createButtonID(channel);
        	   LOGGER.info("----onButtonSingleOrDoubleClick: FLic.io event (["+clickType.toString()+"]) from BLE channel [" + channel.getBdaddr() + "] for the clickButton: ["+clickButtonID+"]");
	           /**   ButtonUp,
	            * 	 ButtonDown
	            * 	 ButtonClick
	            * 	 ButtonHold
	           */ 
               break;
            }
        }

        @Override
        public void onButtonSingleOrDoubleClickOrHold(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Single/Double click or hold event on channel [" +channel.getBdaddr() + "] " + (clickType == ClickType.ButtonSingleClick ? "SingleClick" : (clickType == ClickType.ButtonDoubleClick ? "DoubleClick" : "Hold"))+ "; was queued "+wasQueued+", time diff: "+timeDiff);
   
            String clickButtonID = null;
            switch (clickType) {
            case ButtonSingleClick: 
              	if (HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClickOrHold) {
	            	Click click = new Click();
	            	clickButtonID = SampleController.setClickButtonClick(channel, click);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Single click event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                	LOGGER.error("----onButtonSingleOrDoubleClickOrHold: Single click event can't be updated for the clickButton: ["+clickButtonID+"]");                	
	                }
               	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
                	LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Single click event handling defined to **Click** event handling for : ["+clickButtonID+"]");            		
            	}
            	break;
            
            case ButtonDoubleClick: 
            	if (HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClickOrHold) {
	            	DoubleClick doubleClick = new DoubleClick();
	            	clickButtonID = SampleController.setClickButtonDoubleClick(channel, doubleClick);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Double click event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                	LOGGER.error("----onButtonSingleOrDoubleClickOrHold: Double click event can't be updated for the clickButton: ["+clickButtonID+"]");                	
	                }           	
              	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
                	LOGGER.info("----onButtonSingleOrDoubleClick: Double click event handling defined to **onButtonSingleOrDoubleClick** for : ["+clickButtonID+"]");            		
            	}   
                break;
    
            case ButtonHold: 
            	if (HANDLE_HOLD_EVENT_onButtonSingleOrDoubleClickOrHold) {
	            	Duration buttonHoldTime;
	            	//TBD determine what is the Flic.io duration
	            	buttonHoldTime = Duration.ZERO;
	            	clickButtonID = SampleController.setClickButtonHold(channel,buttonHoldTime);
	                if (clickButtonID != null) {
	                	LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Hold event updated for the clickButton: ["+clickButtonID+"]");
	                } else {
	                	LOGGER.error("----onButtonSingleOrDoubleClickOrHold: Hold event can't be updated for the clickButton: ["+clickButtonID+"]");                
	                }
            	} else {
	          		clickButtonID = ClickButton.createButtonID(channel);
                	LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Hold event handling defined to **onButtonClickOrHold** for : ["+clickButtonID+"]");            		
            	}
                break;

           default:
        	   clickButtonID = ClickButton.createButtonID(channel);
        	   LOGGER.info("----onButtonSingleOrDoubleClickOrHold: Single/Double click or hold event (["+clickType.toString()+"]) from BLE channel [" + channel.getBdaddr() + "] for the clickButton: ["+clickButtonID+"]");
	           /**   ButtonUp,
	            * 	 ButtonDown
	            *    ButtonClick,
	           */ 
               break;
            }
        }
	 };


	/**
	 * Handle the start of the plugin with the resource representation
	 */
	public static void start() {
		LOGGER.info(SampleConstants.AE_NAME+": Starting Flic.io oneM2M IPE");
		try {
			// Create Flic.io client to the Flic.io network DEAMON
			flicClient = getFlicClient();
			if (flicClient == null) {
				LOGGER.info(SampleConstants.AE_NAME+": Flic.io network Daemon unavailable");
				stop();
			}
			LOGGER.info(SampleConstants.AE_NAME+": connected to Flic.io network Daemon");
			
			//assign the Flic.io clien to the Controller to enable further interactions between IPE administration GUI and Flic.io network Deamon   
			SampleController.setFlicClient(flicClient,flicDeamonHost);
			
			clickButtons = new HashMap<String, ClickButton>();
			ClickButtonModel.setModel(clickButtons);
			
			
			//Define in which method handler the Hold Flic.io event should be processed
			//Selection should be only one to true !!  
			HANDLE_HOLD_EVENT_onButtonClickOrHold= false;
			HANDLE_HOLD_EVENT_onButtonSingleOrDoubleClickOrHold = !HANDLE_HOLD_EVENT_onButtonClickOrHold;	
			//Define which method handler between the Click or the SingleClick Flic.io events should be processed
			//Selection should be only one to true !!  		
			HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClick = false;
			HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClickOrHold = !HANDLE_DOUBLECLICK_EVENT_onButtonSingleOrDoubleClick;
			//Define which method handler between the Click or the SingleClick Flic.io events should be processed
			//Selection should be only one to true !!  
			HANDLE_CLICK_EVENT= false;
			HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClick = !HANDLE_CLICK_EVENT;
			//Define which method handler the Single Click Flic.io events should be processed
			//Selection should be only one to true !!  
			HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClickOrHold = !HANDLE_SINGLECLICK_EVENT_onButtonSingleOrDoubleClick;

						
	        flicClient.getInfo(new GetInfoResponseCallback() {
	            @Override
	            public void onGetInfoResponse(BluetoothControllerState bluetoothControllerState, Bdaddr myBdAddr,
	                                          BdAddrType myBdAddrType, int maxPendingConnections, int maxConcurrentlyConnectedButtons,
	                                          int currentPendingConnections, boolean currentlyNoSpaceForNewConnection, Bdaddr[] verifiedButtons) throws IOException {

	            	//  Register the already peered click buttons
	                for (final Bdaddr bdaddr : verifiedButtons) {
	                	LOGGER.info("Flic.io button [" + bdaddr + "] already peered. Now connecting to it...");
	        			
		                ButtonConnectionChannel buttonConnectionChannel = new ButtonConnectionChannel(bdaddr, buttonCallbacks);
		                flicClient.addConnectionChannel(buttonConnectionChannel);
	                }
	            }
	        });
	        
	        flicClient.setGeneralCallbacks(new GeneralCallbacks() {
	            @Override
	            public void onNewVerifiedButton(Bdaddr bdaddr) throws IOException {
	            	//  Register the newly peered click button
	            	LOGGER.info("A Flic.io BUTTON SCANNER added a new button: " + bdaddr + ". Now connecting to it...");
      			
	                ButtonConnectionChannel buttonConnectionChannel = new ButtonConnectionChannel(bdaddr, buttonCallbacks);
	                flicClient.addConnectionChannel(buttonConnectionChannel);
	            }
	        });
		}	catch (IOException e) {
			LOGGER.error("Flic.io oneM2M IPE error while setting up the client: "+e.toString());
			e.printStackTrace();
		}
		try {
	        flicClient.handleEvents();
		}	catch (IOException e) {
			LOGGER.error("Flic.io oneM2M IPE error while using the client: "+e.toString());
			e.printStackTrace();
		}
	}
	
	public static void stop() {
		LOGGER.info(SampleConstants.AE_NAME+": Stopping Flic.io oneM2M IPE");
		if (flicClient != null) { 
			LOGGER.info(SampleConstants.AE_NAME+": Disconnecting to Flic.io network Daemon");
			try {
				// Ending Flic.io client to the Flic.io network DEAMON
				flicClient.close();
			} catch (IOException e) {
				LOGGER.error("Flic.io oneM2M IPE IO error while closing the client: "+e.toString());
			}
		}
		LOGGER.info(SampleConstants.AE_NAME+": Flic.io oneM2M IPE stopped");
	}
}
