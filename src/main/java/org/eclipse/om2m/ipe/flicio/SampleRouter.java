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
package org.eclipse.om2m.ipe.flicio;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.constants.MimeMediaType;
import org.eclipse.om2m.commons.constants.ResponseStatusCode;
import org.eclipse.om2m.commons.exceptions.BadRequestException;
import org.eclipse.om2m.commons.resource.RequestPrimitive;
import org.eclipse.om2m.commons.resource.ResponsePrimitive;
import org.eclipse.om2m.interworking.service.InterworkingService;
import org.eclipse.om2m.ipe.flicio.constants.Operations;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants;
import org.eclipse.om2m.ipe.flicio.constants.SampleConstants.DATA_QUERY_STRING;
import org.eclipse.om2m.ipe.flicio.controller.SampleController;

public class SampleRouter implements InterworkingService{

	private static Log LOGGER = LogFactory.getLog(SampleRouter.class);

	@Override
	public ResponsePrimitive doExecute(RequestPrimitive request) {
		ResponsePrimitive response = new ResponsePrimitive(request);
		
		LOGGER.info(SampleConstants.AE_NAME+": Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] in Flic.io Sample IPE");
		
		if(request.getQueryStrings().containsKey(DATA_QUERY_STRING.op.toString())){
			
			String operation = request.getQueryStrings().get(DATA_QUERY_STRING.op.toString()).get(0);	
			
			if (operation != null) {
				
				Operations op = Operations.getOperationFromString(operation);
				String content = null;

				switch(op){	
					//The information response to such requests are obtained from the application memory
				
					//Operations on Flic.io Buttons

					case GET_STATE_POSITION_DIRECT:
					case GET_STATE_PEERING_DIRECT:
					case GET_STATE_CLICK_DIRECT:	
					case GET_STATE_DOUBLECLICK_DIRECT:
					case GET_STATE_HOLD_DIRECT:
					case GET_STATE_POSITION:
					case GET_STATE_PEERING:
					case GET_STATE_CLICK:
					case GET_STATE_DOUBLECLICK:
					case GET_STATE_HOLD:
						String clickButtonID = null;
						
						if(request.getQueryStrings().containsKey(DATA_QUERY_STRING.clickbuttonid.toString())){
							clickButtonID = request.getQueryStrings().get(DATA_QUERY_STRING.clickbuttonid.toString()).get(0);
			
							if (clickButtonID != null) {
								LOGGER.info(SampleConstants.AE_NAME+"| Received request in Flic.io Sample IPE: "+DATA_QUERY_STRING.op+"=" + operation + " ; "+DATA_QUERY_STRING.clickbuttonid+" [" + clickButtonID+"]");
						
								switch(op){			
								//The information response to such requests are obtained from the application memory
								case GET_STATE_POSITION_DIRECT:
									content = SampleController.getFormattedClickButtonPosition(clickButtonID);
									response.setContent(content);
									request.setReturnContentType(MimeMediaType.OBIX);
									response.setResponseStatusCode(ResponseStatusCode.OK);
									break;
													
								case GET_STATE_PEERING_DIRECT:
									content = SampleController.getFormattedClickButtonPeering(clickButtonID);
									response.setContent(content);
									request.setReturnContentType(MimeMediaType.OBIX);
									response.setResponseStatusCode(ResponseStatusCode.OK);
									break;							
					
								case GET_STATE_CLICK_DIRECT:
									content = SampleController.getFormattedClickButtonClick(clickButtonID);
									response.setContent(content);
									request.setReturnContentType(MimeMediaType.OBIX);
									response.setResponseStatusCode(ResponseStatusCode.OK);
									break;
												
								case GET_STATE_DOUBLECLICK_DIRECT:
									content = SampleController.getFormattedClickButtonDoubleClick(clickButtonID);
									response.setContent(content);
									request.setReturnContentType(MimeMediaType.OBIX);
									response.setResponseStatusCode(ResponseStatusCode.OK);
									break;
			
								case GET_STATE_HOLD_DIRECT:
									content = SampleController.getFormattedClickButtonHold(clickButtonID);
									response.setContent(content);
									request.setReturnContentType(MimeMediaType.OBIX);
									response.setResponseStatusCode(ResponseStatusCode.OK);
									break;
										
								case GET_STATE_POSITION:
								case GET_STATE_PEERING:
								case GET_STATE_CLICK:
								case GET_STATE_DOUBLECLICK:
								case GET_STATE_HOLD:
									// Such operation requests addressing for Flic.io Button do not contain any query string fields
									// Indeed the information response to such requests are found in the oneM2M resources repository
									// Shall not get there...
								default:
									LOGGER.error(SampleConstants.AE_NAME+"| Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] for a non valid operation ["+operation+"]");									
									throw new BadRequestException();
								}
							} 
							else {				
								LOGGER.error(SampleConstants.AE_NAME+"| Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] for operation ["+operation+"] does not contain any value for field ["+DATA_QUERY_STRING.clickbuttonid+"]");									
								response.setResponseStatusCode(ResponseStatusCode.BAD_REQUEST);
							}
						} else {				
							LOGGER.error(SampleConstants.AE_NAME+"| Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] does not contain any field ["+DATA_QUERY_STRING.clickbuttonid+"]");					
							response.setResponseStatusCode(ResponseStatusCode.BAD_REQUEST);
						}	
						break;
				
				
					//--------------------------------------------------
					//Operations on Flic.io network deamon			
					case SET_SCANNER_ON:
						SampleController.startFlicScanner();
		//				content = SampleController.getFormatted...
		//				response.setContent(content);
		//				request.setReturnContentType(MimeMediaType.OBIX);
						response.setResponseStatusCode(ResponseStatusCode.OK);
						break;
		
					case SET_SCANNER_OFF:
						SampleController.stopFlicScanner();
		
		//				content = SampleController.getFormatted...
		//				response.setContent(content);
		//				request.setReturnContentType(MimeMediaType.OBIX);
						response.setResponseStatusCode(ResponseStatusCode.OK);
						break;
						
					//--------------------------------------------------		
					default:
						LOGGER.error(SampleConstants.AE_NAME+"| Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] for a non valid operation ["+operation+"]");									
						throw new BadRequestException();
					}	

				
				
			} else {				
				LOGGER.error(SampleConstants.AE_NAME+"| Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] does not contain any value for field ["+DATA_QUERY_STRING.op+"]");					
				response.setResponseStatusCode(ResponseStatusCode.BAD_REQUEST);
			}	
			
		} else {				
			LOGGER.error(SampleConstants.AE_NAME+"| Received request (named ["+request.getName()+"]) addressed to ["+request.getTo()+"] does not contain any field ["+DATA_QUERY_STRING.op+"]");					
			response.setResponseStatusCode(ResponseStatusCode.BAD_REQUEST);
		}
		return response;
	}

	@Override
	public String getAPOCPath() {
		return SampleConstants.POA;
	}
	
}
