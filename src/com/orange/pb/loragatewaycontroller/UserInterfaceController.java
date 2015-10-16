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

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

import com.orange.pb.loragatewaycontroller.Main.Bandwidth;
import com.orange.pb.loragatewaycontroller.Main.CodingRate;
import com.orange.pb.loragatewaycontroller.Main.FrequencyChannel;
import com.orange.pb.loragatewaycontroller.Main.SpreadingFactor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * 
 * Handles UI.
 *
 */
public class UserInterfaceController implements Initializable, EventHandler<ActionEvent> {
	
	private final static int MAX_NB_FRAMES = 15;
	private final static int MAX_NB_LOGMSGS = 100;
	
	// Strings displayed to the user, to select configuration data.
	private final static String CH_01 = "CH_10_868";
	private final static String CH_02 = "CH_11_868";
	private final static String CH_03 = "CH_12_868";
	private final static String CH_04 = "CH_13_868";
	private final static String CH_05 = "CH_14_868";
	private final static String CH_06 = "CH_15_868";
	private final static String CH_07 = "CH_16_868";
	private final static String CH_08 = "CH_17_868";
	
	private final static String BANDWIDTH_01 = "125 kHz";
	private final static String BANDWIDTH_02 = "250 kHz";
	private final static String BANDWIDTH_03 = "500 kHz";
	
	private final static String CODING_RATE_01 = "5";
	private final static String CODING_RATE_02 = "6";
	private final static String CODING_RATE_03 = "7";
	private final static String CODING_RATE_04 = "8";
	
	private final static String SPREADING_01 = "6";
	private final static String SPREADING_02 = "7";
	private final static String SPREADING_03 = "8";
	private final static String SPREADING_04 = "9";
	private final static String SPREADING_05 = "10";
	private final static String SPREADING_06 = "11";
	private final static String SPREADING_07 = "12";
	
	private final static String SEPARATOR = " - ";
	
	@FXML private ComboBox<String> serialPortCB;
	
	@FXML private Button sendReadCommandBtn;
	
	@FXML private ChoiceBox<String> freqChanCB;
	@FXML private TextField addrTF;
	@FXML private ChoiceBox<String> bandwidthCB;
	@FXML private ChoiceBox<String> codingRateCB;
	@FXML private ChoiceBox<String> spreadingFactorCB;
	@FXML private Button sendSetCommandBtn;
	
	@FXML private ListView<String> recFramesLV;
	
	@FXML private ListView<String> logMsgsLV;

	private ObservableList<String> displayedFrames;
	private ListViewMessages recFrames;
	
	private ObservableList<String> displayedLogMsgs;
	private ListViewMessages logMsgs;
	
	private ProcessAction processAction;
	
	private SimpleDateFormat timeFormat;
	
	/**
	 * Called by FXML loader.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		sendReadCommandBtn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {

				if (processAction != null) {
					processAction.sendReadMsg();
				} else {
					displayLogMsg("internal error: processAction is null");
				}
				
			}
		});
		
		sendSetCommandBtn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {

				FrequencyChannel frequencyChannel = FrequencyChannel.CH_10_868;
				int address;
				Bandwidth bandwidth = Bandwidth.BW_125;
				CodingRate codingRate = CodingRate.CR_5;
				SpreadingFactor spreadingFactor = SpreadingFactor.SF_6;
				
				if (processAction != null) {
					String value = freqChanCB.getValue();
					if (value.contentEquals(CH_01)) {
						frequencyChannel = FrequencyChannel.CH_10_868;
					} else if (value.contentEquals(CH_02)) {
						frequencyChannel = FrequencyChannel.CH_11_868;
					} else if (value.contentEquals(CH_03)) {
						frequencyChannel = FrequencyChannel.CH_12_868;
					} else if (value.contentEquals(CH_04)) {
						frequencyChannel = FrequencyChannel.CH_13_868;
					} else if (value.contentEquals(CH_05)) {
						frequencyChannel = FrequencyChannel.CH_14_868;
					} else if (value.contentEquals(CH_06)) {
						frequencyChannel = FrequencyChannel.CH_15_868;
					} else if (value.contentEquals(CH_07)) {
						frequencyChannel = FrequencyChannel.CH_16_868;
					} else if (value.contentEquals(CH_08)) {
						frequencyChannel = FrequencyChannel.CH_17_868;
					}
					try {
						address = Integer.parseInt(addrTF.getText());
					} catch (NumberFormatException e) {
						displayLogMsg("address must be an integer between 1 and 255");
						return;
					}
					if ((address < 1) || (address > 255)) {
						displayLogMsg("address must be an integer between 1 and 255");
						return;
					}
					value = bandwidthCB.getValue();
					if (value.contentEquals(BANDWIDTH_01)) {
						bandwidth = Bandwidth.BW_125;
					} else if (value.contentEquals(BANDWIDTH_02)) {
						bandwidth = Bandwidth.BW_250;
					} else if (value.contentEquals(BANDWIDTH_03)) {
						bandwidth = Bandwidth.BW_500;
					}
					value = codingRateCB.getValue();
					if (value.contentEquals(CODING_RATE_01)) {
						codingRate = CodingRate.CR_5;
					} else if (value.contentEquals(CODING_RATE_02)) {
						codingRate = CodingRate.CR_6;
					} else if (value.contentEquals(CODING_RATE_03)) {
						codingRate = CodingRate.CR_7;
					} else if (value.contentEquals(CODING_RATE_04)) {
						codingRate = CodingRate.CR_8;
					}
					value = spreadingFactorCB.getValue();
					if (value.contentEquals(SPREADING_01)) {
						spreadingFactor = SpreadingFactor.SF_6;
					} else if (value.contentEquals(SPREADING_02)) {
						spreadingFactor = SpreadingFactor.SF_7;
					} else if (value.contentEquals(SPREADING_03)) {
						spreadingFactor = SpreadingFactor.SF_8;
					} else if (value.contentEquals(SPREADING_04)) {
						spreadingFactor = SpreadingFactor.SF_9;
					} else if (value.contentEquals(SPREADING_05)) {
						spreadingFactor = SpreadingFactor.SF_10;
					} else if (value.contentEquals(SPREADING_06)) {
						spreadingFactor = SpreadingFactor.SF_11;
					} else if (value.contentEquals(SPREADING_07)) {
						spreadingFactor = SpreadingFactor.SF_12;
					}
					processAction.sendSetMessage(frequencyChannel, address, bandwidth, codingRate, spreadingFactor);
				} else {
					displayLogMsg("internal error: processAction is null");
				}
				
			}
		});
		
		// Display values for frequency and channel, and select first value by default.
		freqChanCB.getItems().addAll(CH_01, CH_02, CH_03, CH_04, CH_05, CH_06, CH_07, CH_08);
		freqChanCB.setValue(CH_01);
		
		// Display values for bandwidth, and select first value by default.
		bandwidthCB.getItems().addAll(BANDWIDTH_01, BANDWIDTH_02, BANDWIDTH_03);
		bandwidthCB.setValue(BANDWIDTH_01);
		
		// Idem for coding rate.
		codingRateCB.getItems().addAll(CODING_RATE_01, CODING_RATE_02, CODING_RATE_03, CODING_RATE_04);
		codingRateCB.setValue(CODING_RATE_01);
		
		// Idem for spreading factor.
		spreadingFactorCB.getItems().addAll(SPREADING_01, SPREADING_02, SPREADING_03, SPREADING_04, SPREADING_05,
				SPREADING_06, SPREADING_07);
		spreadingFactorCB.setValue(SPREADING_01);

		// To display latest received frames.
		displayedFrames = FXCollections.observableArrayList();
		recFramesLV.setItems(displayedFrames);
		recFrames = new ListViewMessages(displayedFrames, MAX_NB_FRAMES);
		
		// To display latest log messages.
		displayedLogMsgs = FXCollections.observableArrayList();
		logMsgsLV.setItems(displayedLogMsgs);
		logMsgs = new ListViewMessages(displayedLogMsgs, MAX_NB_LOGMSGS);
		
		// To get serial port selected by the user.
		serialPortCB.setOnAction(this);
		
		// Setup button state.
		enableActions(false);
		
		timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		
	}
	
	/**
	 * 
	 * @param processAction
	 */
	public void setProcessAction(ProcessAction processAction) {
		
		this.processAction = processAction;
		
	}
	
	/**
	 * 
	 * @param frame
	 */
	public void displayFrame(String frame) {
		
		Date currentTime = new Date();
		String s = timeFormat.format(currentTime);
		recFrames.addMessage(s + SEPARATOR + frame);
		
	}
	
	public void displayLogMsg(String logMsg) {
		
		Date currentTime = new Date();
		String s = timeFormat.format(currentTime);
		logMsgs.addMessage(s + SEPARATOR + logMsg);
		
	}

	/**
	 * 
	 * @param portNameList
	 */
	public void displaySerialPorts(ArrayList<String> portNameList) {
		
		ObservableList<String> ol = FXCollections.observableArrayList();
		ol.setAll(portNameList);
		serialPortCB.setItems(ol);
		
	}

	/**
	 * For EventHandler<ActionEvent> interface.
	 * Is called only by the combo box, when the user selects a serial port.
	 */
	@Override
	public void handle(ActionEvent event) {
		
		if (processAction != null) {
			processAction.serialPortValue(serialPortCB.getValue());
			// User is no more allowed to modify serial port.
			serialPortCB.setDisable(true);
		} else {
			displayLogMsg("internal error: processAction is null");
		}
		
	}
	
	/**
	 * To call once serial port is setup successfully.
	 */
	public void enableActions(boolean enable) {
		
		sendReadCommandBtn.setDisable(!enable);
		sendSetCommandBtn.setDisable(!enable);
		freqChanCB.setDisable(!enable);
		addrTF.setDisable(!enable);
		bandwidthCB.setDisable(!enable);
		codingRateCB.setDisable(!enable);
		spreadingFactorCB.setDisable(!enable);
		
	}
	
	/**
	 * How to process actions.
	 */
	public interface ProcessAction {
		
		public void serialPortValue(String serialPortName);
		
		public void sendReadMsg();
		
		public void sendSetMessage(FrequencyChannel freqChan, int addr, Bandwidth bandwidth, CodingRate cr,
				SpreadingFactor sf);
		
	}

}
