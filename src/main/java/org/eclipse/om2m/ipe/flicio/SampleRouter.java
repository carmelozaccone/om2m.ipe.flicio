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
		if(request.getQueryStrings().containsKey(DATA_QUERY_STRING.op)){
			String operation = request.getQueryStrings().get(DATA_QUERY_STRING.op).get(0);
			Operations op = Operations.getOperationFromString(operation);
			String clickButtonID = null;
			if(request.getQueryStrings().containsKey(DATA_QUERY_STRING.clickbuttonid)){
				clickButtonID = request.getQueryStrings().get(DATA_QUERY_STRING.clickbuttonid).get(0);
			}
			LOGGER.info("Received request in Flic.io Sample IPE: "+DATA_QUERY_STRING.op+"=" + operation + " ; "+DATA_QUERY_STRING.clickbuttonid+" [" + clickButtonID+"]");

			String content = null;
			switch(op){
			case GET_STATE_POSITION:
				// Shall not get there...
				throw new BadRequestException();
		
			case GET_STATE_POSITION_DIRECT:
				content = SampleController.getFormattedClickButtonPosition(clickButtonID);
				response.setContent(content);
				request.setReturnContentType(MimeMediaType.OBIX);
				response.setResponseStatusCode(ResponseStatusCode.OK);
				break;
				
			case GET_STATE_PEERING:
				// Shall not get there...
				throw new BadRequestException();
	
			case GET_STATE_PEERING_DIRECT:
				content = SampleController.getFormattedClickButtonPeering(clickButtonID);
				response.setContent(content);
				request.setReturnContentType(MimeMediaType.OBIX);
				response.setResponseStatusCode(ResponseStatusCode.OK);
				break;
				
			case GET_STATE_CLICK:
				// Shall not get there...
				throw new BadRequestException();

			case GET_STATE_CLICK_DIRECT:
				content = SampleController.getFormattedClickButtonClick(clickButtonID);
				response.setContent(content);
				request.setReturnContentType(MimeMediaType.OBIX);
				response.setResponseStatusCode(ResponseStatusCode.OK);
				break;
				
			case GET_STATE_DOUBLECLICK:
				// Shall not get there...
				throw new BadRequestException();

			case GET_STATE_DOUBLECLICK_DIRECT:
				content = SampleController.getFormattedClickButtonDoubleClick(clickButtonID);
				response.setContent(content);
				request.setReturnContentType(MimeMediaType.OBIX);
				response.setResponseStatusCode(ResponseStatusCode.OK);
				break;
				
			case GET_STATE_HOLD:
				// Shall not get there...
				throw new BadRequestException();

			case GET_STATE_HOLD_DIRECT:
				content = SampleController.getFormattedClickButtonHold(clickButtonID);
				response.setContent(content);
				request.setReturnContentType(MimeMediaType.OBIX);
				response.setResponseStatusCode(ResponseStatusCode.OK);
				break;
			default:
				throw new BadRequestException();
			}
		}
		if(response.getResponseStatusCode() == null){
			response.setResponseStatusCode(ResponseStatusCode.BAD_REQUEST);
		}
		return response;
	}

	@Override
	public String getAPOCPath() {
		return SampleConstants.POA;
	}
	
}
