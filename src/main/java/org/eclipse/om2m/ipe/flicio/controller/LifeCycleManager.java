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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.constants.MimeMediaType;
import org.eclipse.om2m.commons.constants.ResponseStatusCode;
import org.eclipse.om2m.commons.resource.AE;
import org.eclipse.om2m.commons.resource.Container;
import org.eclipse.om2m.commons.resource.ContentInstance;
import org.eclipse.om2m.commons.resource.ResponsePrimitive;
import org.eclipse.om2m.ipe.flicio.RequestSender;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants;
import org.eclipse.om2m.ipe.flicio.gui.GUI;
import org.eclipse.om2m.ipe.flicio.model.Lamp;
import org.eclipse.om2m.ipe.flicio.model.SampleModel;
import org.eclipse.om2m.ipe.flicio.util.ObixUtil;

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
	
    private static ButtonConnectionChannel.Callbacks buttonCallbacks = new ButtonConnectionChannel.Callbacks() {
        @Override
        public void onCreateConnectionChannelResponse(ButtonConnectionChannel channel, CreateConnectionChannelError createConnectionChannelError, ConnectionStatus connectionStatus) {
            System.out.println("Create response " + channel.getBdaddr() + ": " + createConnectionChannelError + ", " + connectionStatus);
        }

        @Override
        public void onRemoved(ButtonConnectionChannel channel, RemovedReason removedReason) {
            System.out.println("Channel removed for " + channel.getBdaddr() + ": " + removedReason);
        }

        @Override
        public void onConnectionStatusChanged(ButtonConnectionChannel channel, ConnectionStatus connectionStatus, DisconnectReason disconnectReason) {
            System.out.println("New status for " + channel.getBdaddr() + ": " + connectionStatus + (connectionStatus == ConnectionStatus.Disconnected ? ", " + disconnectReason : ""));
        }

        @Override
        public void onButtonUpOrDown(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
            System.out.println(channel.getBdaddr() + " " + (clickType == ClickType.ButtonUp ? "Up" : "Down"));
          if(clickType == ClickType.ButtonDown) {
            	//SampleController.setAllOff();
            }
        }
        
        @Override
        public void onButtonSingleOrDoubleClickOrHold(ButtonConnectionChannel channel, ClickType clickType,
        		boolean wasQueued, int timeDiff) throws IOException {
        	  System.out.println(channel.getBdaddr() + " " + (clickType == ClickType.ButtonDoubleClick ? "double click" : clickType == ClickType.ButtonClick ? "click" : "hold"));
              if (clickType == ClickType.ButtonDoubleClick) {
              	SampleController.setAllOn();
              	
              	try {
					sendPost(99);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
              } else if(clickType == ClickType.ButtonHold) {
            	  SampleController.setAllOff();
            	  	try {
    					sendPost(0);
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
              }
              
              
        }
        
     // HTTP POST request
    	private void sendPost(int status) throws Exception {

    		String url = "http://10.238.98.136/remote/json-rpc";
    		URL obj = new URL(url);
    		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

    		//add reuqest header
    		con.setRequestMethod("POST");
    		con.setRequestProperty("Accept", "application/json");
    		con.setRequestProperty("Content-Type", "application/json");
    		con.setRequestProperty("Authorization", "Basic YWRtaW46YWRtaW4=");
    		

    		String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
    		
    		// Send post request
    		con.setDoOutput(true);
    		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    //		wr.writeBytes(urlParameters);
    		wr.writeBytes("{\"jsonrpc\": \"2.0\",\"method\":\"HDAccess/invokeDCOOperation\", \"params\": [\"hdm:ZWave:EFE6980E/7\", \"com.prosyst.mbs.services.zwave.deviceclasses.Basic\",\"basicSet\", {\"IN1\": "+status+"}],\"id\":\"1\"}");
    		wr.flush();
    		wr.close();

    		int responseCode = con.getResponseCode();
    		System.out.println("\nSending 'POST' request to URL : " + url);
    		System.out.println("Post parameters : " + urlParameters);
    		System.out.println("Response Code : " + responseCode);

    		BufferedReader in = new BufferedReader(
    		        new InputStreamReader(con.getInputStream()));
    		String inputLine;
    		StringBuffer response = new StringBuffer();

    		while ((inputLine = in.readLine()) != null) {
    			response.append(inputLine);
    		}
    		in.close();
    		
    		//print result
    		System.out.println(response.toString());

    	}
    };

	/**
	 * Handle the start of the plugin with the resource representation and the GUI
	 */
	public static void start(){
		Map<String, Lamp> lamps = new HashMap<String, Lamp>();
		for(int i=0; i<2; i++) {
			String lampId = Lamp.TYPE+"_"+i;
			lamps.put(lampId, new Lamp(lampId, false));
		}
		SampleModel.setModel(lamps);

		// Create initial resources for the 2 lamps
		for(int i=0; i<2; i++) {
			String lampId = Lamp.TYPE+"_"+i;
			createLampResources(lampId, false, SampleConstants.POA);
		}
		createLampAll(SampleConstants.POA);			

		// Start the GUI
		if(SampleConstants.GUI){
			GUI.init();
		}
		
		
		// Flic Client
		try {
		 final FlicClient client = new FlicClient("localhost");
	        client.getInfo(new GetInfoResponseCallback() {
	            @Override
	            public void onGetInfoResponse(BluetoothControllerState bluetoothControllerState, Bdaddr myBdAddr,
	                                          BdAddrType myBdAddrType, int maxPendingConnections, int maxConcurrentlyConnectedButtons,
	                                          int currentPendingConnections, boolean currentlyNoSpaceForNewConnection, Bdaddr[] verifiedButtons) throws IOException {

	                for (final Bdaddr bdaddr : verifiedButtons) {
	                    client.addConnectionChannel(new ButtonConnectionChannel(bdaddr, buttonCallbacks));
	                }
	            }
	        });
	        client.setGeneralCallbacks(new GeneralCallbacks() {
	            @Override
	            public void onNewVerifiedButton(Bdaddr bdaddr) throws IOException {
	                System.out.println("Another client added a new button: " + bdaddr + ". Now connecting to it...");
	                client.addConnectionChannel(new ButtonConnectionChannel(bdaddr, buttonCallbacks));
	            }
	        });
	        client.handleEvents();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Stop the GUI if it is present
	 */
	public static void stop(){
		if(SampleConstants.GUI){
			GUI.stop();
		}
	}

	/**
	 * Creates all required resources.
	 * @param appId - Application ID
	 * @param initValue - initial lamp value
	 * @param poa - lamp Point of Access
	 */
	private static void createLampResources(String appId, boolean initValue, String poa) {
		// Create the Application resource
		Container container = new Container();
		container.getLabels().add("lamp");
		container.setMaxNrOfInstances(BigInteger.valueOf(0));

		AE ae = new AE();
		ae.setRequestReachability(true);
		ae.getPointOfAccess().add(poa);
		ae.setAppID(appId);

		ResponsePrimitive response = RequestSender.createAE(ae, appId);
		// Create Application sub-resources only if application not yet created
		if(response.getResponseStatusCode().equals(ResponseStatusCode.CREATED)) {
			container = new Container();
			container.setMaxNrOfInstances(BigInteger.valueOf(10));
			// Create DESCRIPTOR container sub-resource
			LOGGER.info(RequestSender.createContainer(response.getLocation(), SampleConstants.DESC, container));
			// Create STATE container sub-resource
			LOGGER.info(RequestSender.createContainer(response.getLocation(), SampleConstants.DATA, container));

			String content;
			// Create DESCRIPTION contentInstance on the DESCRIPTOR container resource
			content = ObixUtil.getDescriptorRep(SampleConstants.CSE_ID, appId, SampleConstants.DATA);
			ContentInstance contentInstance = new ContentInstance();
			contentInstance.setContent(content);
			contentInstance.setContentInfo(MimeMediaType.OBIX);
			RequestSender.createContentInstance(
					SampleConstants.CSE_PREFIX + "/" + appId + "/" + SampleConstants.DESC, null, contentInstance);

			// Create initial contentInstance on the STATE container resource
			content = ObixUtil.getStateRep(appId, initValue);
			contentInstance.setContent(content);
			RequestSender.createContentInstance(
					SampleConstants.CSE_PREFIX + "/" + appId + "/" + SampleConstants.DATA, null, contentInstance);
		}
	}

	/**
	 * Create the LAMP_ALL container
	 * @param poa
	 */
	private static void createLampAll(String poa) {
		// Creation of the LAMP_ALL container
		AE ae = new AE();
		ae.setRequestReachability(true);
		ae.getPointOfAccess().add(poa);
		ae.setAppID("LAMP_ALL");
		ResponsePrimitive response = RequestSender.createAE(ae, "LAMP_ALL");

		// Create descriptor container if not yet created
		if(response.getResponseStatusCode().equals(ResponseStatusCode.CREATED)){
			// Creation of the DESCRIPTOR container
			Container cnt = new Container();
			cnt.setMaxNrOfInstances(BigInteger.valueOf(10));
			RequestSender.createContainer(SampleConstants.CSE_PREFIX + "/" + "LAMP_ALL", SampleConstants.DESC, cnt);

			// Create the description
			ContentInstance cin = new ContentInstance();
			cin.setContent(ObixUtil.createLampAllDescriptor());
			cin.setContentInfo(MimeMediaType.OBIX);
			RequestSender.createContentInstance(SampleConstants.CSE_PREFIX + "/" + "LAMP_ALL" + "/" + SampleConstants.DESC, null, cin);
		}
	}

}
