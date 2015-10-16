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
	
import java.util.ArrayList;

import com.orange.pb.loragatewaycontroller.UserInterfaceController.ProcessAction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * 
 * Main class.
 * Implements DisplayMessage interface.
 *
 */
public class Main extends Application implements DisplayMessage, ProcessAction {
	
	// Current version.
	private final static String VERSION = "0.7";
	
	// Size of main window.
	private final static int WIDTH = 600;
	private final static int HEIGHT = 600;
	
	// Constants defining possible values for gateway configuration.
	public static enum FrequencyChannel {
		CH_10_868, CH_11_868, CH_12_868, CH_13_868, CH_14_868, CH_15_868, CH_16_868, CH_17_868
	}
	public static enum Bandwidth {
		BW_125, BW_250, BW_500
	}
	public static enum CodingRate {
		CR_5, CR_6, CR_7, CR_8
	}
	public static enum SpreadingFactor {
		SF_6, SF_7, SF_8, SF_9, SF_10, SF_11, SF_12
	}
	
	private UserInterfaceController controller;
	private PortHandler portHandler;
	
	/**
	 * 
	 */
	@Override
	public void start(Stage primaryStage) {
		
		FXMLLoader fxmlLoader = null;
		try {
			fxmlLoader = new FXMLLoader();
			Parent root = fxmlLoader.load(getClass().getResource("UserInterface.fxml").openStream());
			Scene scene = new Scene(root,WIDTH,HEIGHT);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		controller = (UserInterfaceController)fxmlLoader.getController();
		// Display version.
		controller.displayLogMsg("Controller for Libelium LoRa Gateway - version " + VERSION);
		// Display list of available serial ports.
		portHandler = new PortHandler(this);
		ArrayList<String> portNameList = portHandler.getSerialPorts();
		if (portNameList != null) {
			controller.displaySerialPorts(portNameList);
		} else {
			controller.displayLogMsg("No serial port available");
		}
		controller.setProcessAction(this);
		
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		launch(args);
		
	}

	/**
	 * For DisplayMessage interface.
	 * 
	 * @param message
	 */
	@Override
	public void displayLog(String message) {
		
		controller.displayLogMsg(message);
		
	}

	/**
	 * For DisplayMessage interface.
	 * 
	 */
	@Override
	public void displayLogLater(String message) {
		
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				
				controller.displayLogMsg(message);
				
			}
			
		});
	}
	
	/**
	 * For DisplayMessage interface.
	 * 
	 * @param message
	 */
	@Override
	public void displayFrame(String message) {
		
		controller.displayFrame(message);
		
	}

	/**
	 * For DisplayMessage interface.
	 * 
	 */
	@Override
	public void displayFrameLater(String message) {
		
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				
				controller.displayFrame(message);
				
			}
			
		});
	}
	
	/**
	 * For ProcessAction interface.
	 * 
	 * @param serialPortName
	 */
	@Override
	public void serialPortValue(String serialPortName) {
		
		int rs = portHandler.setSerialPort(serialPortName);
		if (rs == 0) {
			// Enable buttons.
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					
					controller.enableActions(true);
					
				}
				
			});
		}
		
	}
	
	/**
	 * 
	 */
	@Override
	public void sendReadMsg() {
		
		portHandler.sendMsg(portHandler.createReadMsg());
		displayLog("READ message sent");
		
	}

	/**
	 * 
	 */
	@Override
	public void sendSetMessage(FrequencyChannel freqChan, int addr, Bandwidth bandwidth, CodingRate cr,
			SpreadingFactor sf) {
		
		portHandler.sendMsg(portHandler.createSetMsg(freqChan, addr, bandwidth, cr,sf));
		displayLog("SET messagesent");
		
	}

}
