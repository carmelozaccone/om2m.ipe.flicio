package org.eclipse.om2m.ipe.flicio.model;

import java.time.Duration;
import java.time.Instant;

public class DoubleClick extends Click {

	/** Starting Timestamp of 1st Click*/
    private Instant clickInstant1;
    /** Holding time which complete the 1st Click*/
    private Duration clickHold1;

    /** Holding time which between the 2 Clicks*/
    private Duration interClicksHold;
    
	/** Starting Timestamp of 2nd Click*/
    private Instant clickInstant2;
    /** Holding time which complete the 2nd Click*/
    private Duration clickHold2;
    
	public DoubleClick() {
    	this.clickInstant1 = Instant.EPOCH;
    	this.clickHold1  =  Duration.ZERO;

    	this.interClicksHold = Duration.ZERO;

    	this.clickInstant2 = Instant.EPOCH;
    	this.clickHold2  =  Duration.ZERO;
	}

	/**
	 * @return the 1st clickInstant
	 */
	public Instant getClickInstant1() {
		return clickInstant1;
	}

	/**
	 * @param clickInstant the Instant to set
	 */
	public void setClickInstant12(Instant clickInstant1) {
		this.clickInstant1 = clickInstant1;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public Duration getClickHold1() {
		return clickHold1;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public void setInterClicksHold(Duration interclickHold) {
		this.interClicksHold = interclickHold;
	}
	
	
	/**
	 * @param clickHold the Duration to set
	 */
	public Duration getInterClicksHold() {
		return interClicksHold;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public void setClickHold1(Duration clickHold) {
		this.clickHold1 = clickHold;
	}
	
	/**
	 * @return the 1st clickInstant
	 */
	public Instant getClickInstant2() {
		return clickInstant2;
	}

	/**
	 * @param clickInstant the Instant to set
	 */
	public void setClickInstant2(Instant clickInstant1) {
		this.clickInstant2 = clickInstant1;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public Duration getClickHold2() {
		return clickHold2;
	}
	
	/**
	 * @param clickHold the Duration to set
	 */
	public void setClickHold2(Duration clickHold) {
		this.clickHold2 = clickHold;
	}
}
