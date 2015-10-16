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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import com.orange.pb.loragatewaycontroller.Main.Bandwidth;
import com.orange.pb.loragatewaycontroller.Main.CodingRate;
import com.orange.pb.loragatewaycontroller.Main.FrequencyChannel;
import com.orange.pb.loragatewaycontroller.Main.SpreadingFactor;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * 
 * Handles serial port.
 *
 */
public class PortHandler implements SerialPortEventListener {
	
	private final static int PORT_OPEN_WAIT = 1000;
	// Characteristics of Libelium LoRa gateway serial port.
	private final static int PORT_SPEED = 38400;
	private final static int PORT_DATA_BITS = SerialPort.DATABITS_8;
	private final static int PORT_STOP_BITS = SerialPort.STOPBITS_1;
	private final static int PORT_PARITY = SerialPort.PARITY_NONE;
	private final static int PORT_FLOW_CONTROL = SerialPort.FLOWCONTROL_NONE;
	
	private SerialPort serialPort;
	private OutputStream out;
	private InputStream in;
	private DisplayMessage displayMessage;
	private FrameHandler frameHandler;
	
	/**
	 * 
	 * @param displayMessage
	 */
	public PortHandler(DisplayMessage displayMessage) {
		
		this.displayMessage = displayMessage;
		frameHandler = new FrameHandler(displayMessage);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public short[] createReadMsg() {
		
		return frameHandler.createReadMsg();
		
	}
	
	/**
	 * 
	 * @param freqChan
	 * @param addr
	 * @param bandwidth
	 * @param cr
	 * @param sf
	 * @return
	 */
	public short[] createSetMsg(FrequencyChannel freqChan, int addr, Bandwidth bandwidth, CodingRate cr,
			SpreadingFactor sf) {
		
		return frameHandler.createSetMsg(freqChan, addr, bandwidth, cr, sf);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getSerialPorts() {
		
		ArrayList<String> portNameList = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		if (portList == null) {
			displayMessage.displayLog("no CommPortIdentifier!");
			return null;
		}
		CommPortIdentifier port;
		while (portList.hasMoreElements()) {
			port = portList.nextElement();
			if (port.getPortType() != CommPortIdentifier.PORT_SERIAL) {
				displayMessage.displayLog(port.getName() + " not a serial port");
				continue;
			}
			if (port.isCurrentlyOwned()) {
				displayMessage.displayLog(port.getName() + " currently owned");
				continue;
			}
			portNameList.add(port.getName());
			displayMessage.displayLog(port.getName() + " added to list");
		}
		if (portNameList.isEmpty()) {
			return null;
		}
		
		return portNameList;
	}
	
	/**
	 * Tries to open the serial port. Side effect:
	 * - set serialPort
	 * - set out
	 * In current version, can be called only once (i.e. serial port is never closed).
	 * @param serialPortName
	 * @return -1 - no such port
	 *         -2 - port in use
	 *         -3 - internal error
	 *         -4 - too many event listeners
	 *         -5 - can't get output stream
	 *         -6 - can't get input stream
	 *          0 - success
	 */
	public int setSerialPort(String serialPortName) {
		
		CommPortIdentifier commPortIdentifier;
		try {
			commPortIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
		} catch (NoSuchPortException e) {
			displayMessage.displayLog(serialPortName + " does not exist");
			return -1;
		}
		try {
			serialPort = (SerialPort) commPortIdentifier.open("FrameHandler", PORT_OPEN_WAIT);
		} catch (PortInUseException e) {
			displayMessage.displayLog(serialPortName + " is in use");
			return -2;
		}
		try {
			serialPort.setSerialPortParams(PORT_SPEED, PORT_DATA_BITS, PORT_STOP_BITS, PORT_PARITY);
		} catch (UnsupportedCommOperationException e) {
			displayMessage.displayLog("internal error: bad port profile");
			return -3;
		}
		try {
			serialPort.setFlowControlMode(PORT_FLOW_CONTROL);
		} catch (UnsupportedCommOperationException e) {
			displayMessage.displayLog("internal error: bad flow control");
			return -3;
		}
		try {
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (TooManyListenersException e) {
			displayMessage.displayLog("too many event listeners");
			return -4;
		}
		try {
			out = serialPort.getOutputStream();
		} catch (IOException e) {
			displayMessage.displayLog("can't get output stream");
			return -5;
		}
		try {
			in = serialPort.getInputStream();
		} catch (IOException e) {
			displayMessage.displayLog("can't get input stream");
			return -6;
		}
		displayMessage.displayLog(serialPortName + " opened and configured");
		return 0;
	}
	
	/**
	 * Event notification must be enabled for every event in setSerialPort() above.
	 * We can't call displayMessage.display() from this method, as we are not in FX thread.
	 * 
	 * @param event
	 */
	@Override
	public void serialEvent(SerialPortEvent event) {
		
		switch(event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.FE:
		case SerialPortEvent.OE:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
		case SerialPortEvent.PE:
		case SerialPortEvent.RI:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			short[] payload;
			int b;
			while (true) {
				try {
					if (in.available() == 0) {
						// No more data available.
						break;
					}
					b = in.read();
				} catch (IOException e) {
					displayMessage.displayLogLater("error on receive: " + e.getMessage());
					break;
				}
				payload = frameHandler.frameAssembler(b);
				if (payload != null) {
					displayMessage.displayFrameLater(frameHandler.displayFrame(payload));
				}
			}
			break;
		default:
			displayMessage.displayLogLater("FrameHandler.serialEvent() - unknown event type: " + event.getEventType());
		}
		
	}
		
	/**
	 * 
	 * @param bytes
	 */
	public void sendMsg(short[] bytes) {
		
		// To allow for tests while building the application.
		if (bytes == null) {
			displayMessage.displayLogLater("can't send null message");
			return;
		}
		
		try {
			for (short b: bytes) {
				out.write(b);
			}
		} catch (IOException e) {
			displayMessage.displayLogLater("write error: " + e.getMessage());
			return;
		}

	}
	
}
