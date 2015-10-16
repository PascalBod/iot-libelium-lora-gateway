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

import javafx.collections.ObservableList;

/**
 * 
 * Handles a list of a maximum number of messages, sorted from most recent
 * to least recent one. When a new message is inserted, and maximum number
 * of messages is reached, oldest message is removed from the list.
 *
 */
public class ListViewMessages {
	
	private ObservableList<String> messageList;
	private int maxNumberOfMessages;

	/**
	 * 
	 * @param frameList
	 */
	public ListViewMessages(ObservableList<String> frameList, int maxNumberOfMessages) {
		
		this.messageList = frameList;
		this.maxNumberOfMessages = maxNumberOfMessages;
		
	}
	
	/**
	 * 
	 * @param frame
	 */
	public void addMessage(String message) {
		
		int s = messageList.size();
		if (s >= maxNumberOfMessages) {
			// Remove oldest element.
			messageList.remove(s - 1);
		}
		// Add new element.
		messageList.add(0, message);
		
	}

}
