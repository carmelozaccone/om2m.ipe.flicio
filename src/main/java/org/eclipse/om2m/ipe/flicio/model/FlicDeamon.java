package org.eclipse.om2m.ipe.flicio.model;

import org.eclipse.om2m.ipe.flicio.constants.SampleConstants;

public class FlicDeamon {

	/** Default Flic.io network deamon location */
    public final static String LOCATION = "Home";
    /** Default  Flic.io network deamon type */
    public final static String TYPE = "DEAMON-Flic.io";
    
	private String host;
	private int port;
	private final String flicDeamonID;

	public FlicDeamon(){
		this.host = SampleConstants.FLIC_DEAMON_HOST;
		this.port = SampleConstants.FLIC_DEAMON_PORT;
		this.flicDeamonID = FlicDeamon.TYPE+"_"+SampleConstants.FLIC_DEAMON_HOST+"_"+SampleConstants.FLIC_DEAMON_PORT;
	}
	
	public FlicDeamon(String valueHost,final int valuePort){
		this.host = valueHost;
		this.port = valuePort;
		this.flicDeamonID = FlicDeamon.TYPE+"_"+valueHost+"*"+valuePort;
	}

	
	public String getHostname() {
		return host;
	}
	
	public int getNetworkPort() {
		return port;
	}

	public String getFlicDeamonID() {
		return flicDeamonID;
	}
	
	public String toString() {
		return host+":"+port;
	}
}
