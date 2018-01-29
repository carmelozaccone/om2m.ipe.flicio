package org.eclipse.om2m.ipe.flicio.model;

import java.time.Duration;
import java.time.Instant;

public class Click {
	   
    /** Starting Timestamp of a Click*/
    private Instant clickInstant;
    /** Holding time which complete the Click*/
    private Duration clickHold;
    
	public Click(){
    	this.clickInstant = Instant.EPOCH;
    	this.clickHold  =  Duration.ZERO;
    }
	
	/**
	 * @return the clickInstant
	 */
	public Instant getClickInstant() {
		return clickInstant;
	}

	/**
	 * @param clickInstant the Instant to set
	 */
	public void setClickInstant(Instant clickInstant) {
		this.clickInstant = clickInstant;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public Duration getClickHold() {
		return clickHold;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public void setClickHold(Duration clickHold) {
		this.clickHold = clickHold;
	}
}
