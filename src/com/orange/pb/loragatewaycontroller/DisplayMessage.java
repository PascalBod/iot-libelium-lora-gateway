/**
 * Copyright (C) 2015 Orange
 * 
 * This software is distributed under the terms and conditions of the GNU General 
 * Public License version 2 (GPLv2), which can be found in the file 'LICENSE.txt' 
 * in this package distribution or at 'https://www.gnu.org/licenses/gpl-2.0.txt'. 
 */

/**
 *  Controller for Libelium's LoRa gateway.
 *  
 *  Version:  see below
 *  Creation: 14-Aug-2015
 *  Author:   Pascal Bodin - pascal.bodin@orange.com  
 *            Orange Labs
 *            905 rue Albert Einstein
 *            06560 SOPHIA ANTIPOLIS
 *            FRANCE
 *    
 */

package com.orange.pb.loragatewaycontroller;

/**
 *
 * Interface used to specify display services provided by main class.
 *
 */
public interface DisplayMessage {

	/**
	 * Must be called from FX application context.
	 * @param message
	 */
	public void displayLog(String message);
	
	/**
	 * Can be called from any context.
	 * @param message
	 */
	public void displayLogLater(String message);
	
	/**
	 * Must be called from FX application context.
	 * @param message
	 */
	public void displayFrame(String message);
	
	/**
	 * Can be called from any context.
	 * @param message
	 */
	public void displayFrameLater(String message);
	
}
