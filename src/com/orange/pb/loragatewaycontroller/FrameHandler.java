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

import java.util.Arrays;

import com.orange.pb.loragatewaycontroller.Main.Bandwidth;
import com.orange.pb.loragatewaycontroller.Main.CodingRate;
import com.orange.pb.loragatewaycontroller.Main.FrequencyChannel;
import com.orange.pb.loragatewaycontroller.Main.SpreadingFactor;

/**
 * 
 * Encodes / decodes frames sent to / received from LoRa gateway.
 * Received frames can be gateway locally-generated frames, in answer
 * to commands sent to it, or they can be data frames sent by a remote
 * Waspmote / Plug & Sense. In this later case, frames can be of
 * ASCII type or of binary type. Current version decodes only ASCII
 * frames.
 *
 */
public class FrameHandler {

	private final static short SOH = 0x01;
	private final static short CR  = 0x0D;
	private final static short LF  = 0x0A;
	private final static short EOT = 0x04;

	private final static short LETTER_HASH = 0x23;
	private final static short LETTER_SEMICOLON = 0x3B;
	private final static short LETTER_LT = 0x3C;
	private final static short LETTER_EQUAL = 0x3D;
	private final static short LETTER_GT = 0x3E;
	private final static short LETTER_A = 0x41;
	private final static short LETTER_D = 0x44;
	private final static short LETTER_E = 0x45;
	private final static short LETTER_R = 0x52;
	private final static short LETTER_S = 0x53;
	private final static short LETTER_T = 0x54;
	
	private final static String HEX_PREFIX = "\\0x";
	
	private final static String CH_01 = "FREC:CH_10_868";
	private final static String CH_02 = "FREC:CH_11_868";
	private final static String CH_03 = "FREC:CH_12_868";
	private final static String CH_04 = "FREC:CH_13_868";
	private final static String CH_05 = "FREC:CH_14_868";
	private final static String CH_06 = "FREC:CH_15_868";
	private final static String CH_07 = "FREC:CH_16_868";
	private final static String CH_08 = "FREC:CH_17_868";
	
	private final static String ADDR = "ADDR:";
	
	private final static String BANDWIDTH_01 = "BW:BW_125";
	private final static String BANDWIDTH_02 = "BW:BW_250";
	private final static String BANDWIDTH_03 = "BW:BW_500";
	
	private final static String CODING_RATE_01 = "CR:CR_5";
	private final static String CODING_RATE_02 = "CR:CR_6";
	private final static String CODING_RATE_03 = "CR:CR_7";
	private final static String CODING_RATE_04 = "CR:CR_8";
	
	private final static String SPREADING_01 = "SF:SF_6";
	private final static String SPREADING_02 = "SF:SF_7";
	private final static String SPREADING_03 = "SF:SF_8";
	private final static String SPREADING_04 = "SF:SF_9";
	private final static String SPREADING_05 = "SF:SF_10";
	private final static String SPREADING_06 = "SF:SF_11";
	private final static String SPREADING_07 = "SF:SF_12";

	// States of frame assembly automaton.
	private static enum AssemblyStates {
		// States for gateway frames.
		WAIT_SOH_OR_LT, WAIT_PAYLOAD, WAIT_LF, 
		WAIT_CRC1, WAIT_CRC2, WAIT_CRC3, WAIT_CRC4, WAIT_EOT,
		// States for waspmote frames.
		WAIT_EQUAL, WAIT_GT, WAIT_TYPE,
		// States for waspmote ASCII frames.
		// Note: an ASCII frame ends with an undocumented 0x0D 0x0A.
		WAIT_NB_FIELDS, WAIT_FIRST_FIELD, WAIT_FINAL_HASH, WAIT_FINAL_CR, WAIT_FINAL_LF,
		// States for waspmote binary frames.
		WAIT_NB_BYTES, WAIT_LAST_BYTE
		};
	private AssemblyStates currentAssemblyState;
	
	// Receive buffer, storing payload of frame being assembled. According to Libelium "Waspmote LoRa 868MHz 
	// 915MHz SX1272 Networking Guide", maximum payload length is 250.
	private final static int MAX_LENGTH_PAYLOAD = 250;
	private short[] recBuffer;
	private int recBufferIndex;
	
	private short[] crcHex;
	// Number of fields or number of bytes.
	private int nbData;
	// Number of hash characters already received.
	private int nbHash;
	
	private DisplayMessage displayMessage;
	
	/**
	 * 
	 * @param displayMessage
	 */
	public FrameHandler(DisplayMessage displayMessage) {
		
		this.displayMessage = displayMessage;
		
		currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
		recBuffer = new short[MAX_LENGTH_PAYLOAD];
		recBufferIndex = 0;
		crcHex = new short[4];
		
	}
	
	/**
	 * Assembles a new frame using byte passed in input parameter. Returns payload when a whole frame is
	 * assembled, null otherwise.
	 * 
	 * Two types of frames may be received:
	 * - gateway frames (delimited by SOH and EOT), as answer to frames sent by this program
	 * - waspmote frames (starting with <=>)
	 * 
	 * @param b byte to be processed. If -1, assembly processing is reset. If CRC is wrong, assembly processing
	 *          is reset.
	 * @return null until a new frame is received, otherwise frame payload
	 */
	public short[] frameAssembler(int b) {
		
		if (b == -1) {
			displayMessage.displayLogLater("-1 passed to frameAssembler()");
			currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
			recBufferIndex = 0;
			return null;
		}
		
		short[] payload = null;
		
		switch (currentAssemblyState) {
		case WAIT_SOH_OR_LT:
			if (b == SOH) {
				recBufferIndex = 0;
				currentAssemblyState = AssemblyStates.WAIT_PAYLOAD;
				break;
			}
			if (b == LETTER_LT) {
				recBufferIndex = 0;
				recBuffer[recBufferIndex] = LETTER_LT;
				currentAssemblyState = AssemblyStates.WAIT_EQUAL;
				break;
			}
			// At this stage, not the right byte received. Ignore it.
			displayMessage.displayLogLater("incorrect byte received: " + b);
			break;
			
		// Gateway frame.
		case WAIT_PAYLOAD:
			if (b == CR) {
				currentAssemblyState = AssemblyStates.WAIT_LF;
				break;
			}
			// Not a CR, part of payload.
			recBuffer[recBufferIndex] = (short)b;
			recBufferIndex++;
			// Stay in same state.
			break;
		case WAIT_LF:
			if (b != LF) {
				// Error. Reset assembly.
				displayMessage.displayLogLater("!= LF received");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			// At this stage, LF received.
			currentAssemblyState = AssemblyStates.WAIT_CRC1;
			break;
		case WAIT_CRC1:
			crcHex[0] = (short)b;
			currentAssemblyState = AssemblyStates.WAIT_CRC2;
			break;
		case WAIT_CRC2:
			crcHex[1] = (short)b;
			currentAssemblyState = AssemblyStates.WAIT_CRC3;
			break;
		case WAIT_CRC3:
			crcHex[2] = (short)b;
			currentAssemblyState = AssemblyStates.WAIT_CRC4;
			break;
		case WAIT_CRC4:
			crcHex[3] = (short)b;
			// Let's check message integrity.
			short[] computedCrc = crc16(Arrays.copyOfRange(recBuffer, 0, recBufferIndex));
			short[] recCrc = new short[2];
			recCrc[0] = hexAsciiToByte(Arrays.copyOfRange(crcHex, 2, 4));
			recCrc[1] = hexAsciiToByte(Arrays.copyOfRange(crcHex, 0, 2));
			if ((computedCrc[0] != recCrc[0]) || (computedCrc[1] != recCrc[1])) {
				// Bad CRC. Forget about this frame.
				displayMessage.displayLogLater("bad CRC");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			// At this stage, CRC OK.
			currentAssemblyState = AssemblyStates.WAIT_EOT;
			break;
		case WAIT_EOT:
			if (b != EOT) {
				// Error. Reset assembly.
				displayMessage.displayLogLater("!= EOT received");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			// At this stage, EOT received.
			// Return received payload.
			payload = Arrays.copyOfRange(recBuffer, 0, recBufferIndex);
			currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
			break;
			
		// Waspmote frame.
		case WAIT_EQUAL:
			if (b != LETTER_EQUAL) {
				// Error. Reset assembly.
				displayMessage.displayLogLater("!= = received");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			recBufferIndex++;
			recBuffer[recBufferIndex] = LETTER_EQUAL;
			currentAssemblyState = AssemblyStates.WAIT_GT;
			break;
		case WAIT_GT:
			if (b != LETTER_GT) {
				// Error. Reset assembly.
				displayMessage.displayLogLater("!= > received");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			recBufferIndex++;
			recBuffer[recBufferIndex] = LETTER_GT;
			currentAssemblyState = AssemblyStates.WAIT_TYPE;
			break;
		case WAIT_TYPE:
			if ((b & 0x80) != 0) {
				// ASCII frame.
				recBufferIndex++;
				recBuffer[recBufferIndex] = (short)b;
				currentAssemblyState = AssemblyStates.WAIT_NB_FIELDS;
				break;
			}
			// At this stage, binary frame.
			recBufferIndex++;
			recBuffer[recBufferIndex] = (short)b;
			currentAssemblyState = AssemblyStates.WAIT_NB_BYTES;
			break;
			
		// Waspmote ASCII frame.
		case WAIT_NB_FIELDS:
			recBufferIndex++;
			recBuffer[recBufferIndex] = (short)b;
			nbData = b;
			nbHash = 0;
			currentAssemblyState = AssemblyStates.WAIT_FIRST_FIELD;
			break;
		case WAIT_FIRST_FIELD:
			// We have to wait for 4 hash characters. Then we will have first field.
			recBufferIndex++;
			recBuffer[recBufferIndex] = (short)b;
			if (b == LETTER_HASH) {
				nbHash++;
				if (nbHash >= 4) {
					nbHash = 0;
					currentAssemblyState = AssemblyStates.WAIT_FINAL_HASH;
					break;
				}
			}
			break;
		case WAIT_FINAL_HASH:
			// We have to wait for as many hash characters as number of fields (nbData).
			recBufferIndex++;
			recBuffer[recBufferIndex] = (short)b;
			if (b == LETTER_HASH) {
				nbHash++;
				if (nbHash >= nbData) {
					currentAssemblyState = AssemblyStates.WAIT_FINAL_CR;
					break;
				}
			}
			break;
		case WAIT_FINAL_CR:
			// Undocumented trailing CR.
			if (b != CR) {
				displayMessage.displayLogLater("!= CR received");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			currentAssemblyState = AssemblyStates.WAIT_FINAL_LF;
			break;
		case WAIT_FINAL_LF:
			// Undocumented trailing LF.
			if (b != LF) {
				displayMessage.displayLogLater("!= LF received");
				currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
				break;
			}
			payload = Arrays.copyOfRange(recBuffer, 0, recBufferIndex + 1);
			currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
			break;
			
		// Waspmote binary frame.
		case WAIT_NB_BYTES:
			// TODO
			displayMessage.displayLogLater("Binary frame decoding not implemented yet, ignoring frame");
			currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
			break;
			
		default:
			displayMessage.displayLogLater("unknown state for frame assembler");
			currentAssemblyState = AssemblyStates.WAIT_SOH_OR_LT;
		}
		
		return payload;
	}
	
	/**
	 * 
	 * @return
	 */
	public short[] createReadMsg() {
		
		short[] message = new short[12];
		message[0] = SOH;
		message[1] = LETTER_R;
		message[2] = LETTER_E;
		message[3] = LETTER_A;
		message[4] = LETTER_D;
		message[5] = CR;
		message[6] = LF;
		short[] crc = crc16(Arrays.copyOfRange(message, 1, 5));
		short[] hex = byteToHexAscii(crc[1]);
		message[7] = hex[0];
		message[8] = hex[1];
		hex = byteToHexAscii(crc[0]);
		message[9] = hex[0];
		message[10] = hex[1];
// The gateway doesn't check the CRC! Replace above lines by the ones below to verify this.		
//		message[7] = '*';
//		message[8] = '*';
//		message[9] = '*';
//		message[10] = '*';
		message[11] = EOT;
		
		displayMessage.displayLogLater(displayFrame(message));

		return message;
		
	}
	
	/**
	 * 
	 * @param freqChan
	 * @param addr
	 * @param bandwith
	 * @param cr
	 * @param sf
	 * @return
	 */
	public short[] createSetMsg(FrequencyChannel freqChan, int addr, Bandwidth bandwidth, CodingRate cr,
			SpreadingFactor sf) {
		
		int msgIndex;
		String confData;
		int l;
		
		short[] message = new short[MAX_LENGTH_PAYLOAD];
		message[0] = SOH;
		message[1] = LETTER_S;
		message[2] = LETTER_E;
		message[3] = LETTER_T;
		message[4] = LETTER_HASH;
		switch (freqChan) {
		case CH_10_868:
			confData = CH_01;
			break;
		case CH_11_868:
			confData = CH_02;
			break;
		case CH_12_868:
			confData = CH_03;
			break;
		case CH_13_868:
			confData = CH_04;
			break;
		case CH_14_868:
			confData = CH_05;
			break;
		case CH_15_868:
			confData = CH_06;
			break;
		case CH_16_868:
			confData = CH_07;
			break;
		case CH_17_868:
			confData = CH_08;
			break;
		default:
			return null;
		}
		msgIndex = 5;
		l = confData.length();
		for (int i = 0; i < l; i++) {
			message[msgIndex + i] = (short)confData.charAt(i);
		}
		msgIndex += l;
		message[msgIndex++] = LETTER_SEMICOLON;
		
		confData = ADDR + Integer.toString(addr);
		l = confData.length();
		for (int i = 0; i < l; i++) {
			message[msgIndex + i] = (short)confData.charAt(i);
		}
		msgIndex += l;
		message[msgIndex++] = LETTER_SEMICOLON;
		
		switch (bandwidth) {
		case BW_125:
			confData = BANDWIDTH_01;
			break;
		case BW_250:
			confData = BANDWIDTH_02;
			break;
		case BW_500:
			confData = BANDWIDTH_03;
			break;
		default:
			return null;
		}
		l = confData.length();
		for (int i = 0; i < l; i++) {
			message[msgIndex + i] = (short)confData.charAt(i);
		}
		msgIndex += l;
		message[msgIndex++] = LETTER_SEMICOLON;
		
		switch (cr) {
		case CR_5:
			confData = CODING_RATE_01;
			break;
		case CR_6:
			confData = CODING_RATE_02;
			break;
		case CR_7:
			confData = CODING_RATE_03;
			break;
		case CR_8:
			confData = CODING_RATE_04;
			break;
		default:
			return null;
		}
		l = confData.length();
		for (int i = 0; i < l; i++) {
			message[msgIndex + i] = (short)confData.charAt(i);
		}
		msgIndex += l;
		message[msgIndex++] = LETTER_SEMICOLON;

		switch (sf) {
		case SF_6:
			confData = SPREADING_01;
			break;
		case SF_7:
			confData = SPREADING_02;
			break;
		case SF_8:
			confData = SPREADING_03;
			break;
		case SF_9:
			confData = SPREADING_04;
			break;
		case SF_10:
			confData = SPREADING_05;
			break;
		case SF_11:
			confData = SPREADING_06;
			break;
		case SF_12:
			confData = SPREADING_07;
			break;
		default:
			return null;
		}
		l = confData.length();
		for (int i = 0; i < l; i++) {
			message[msgIndex + i] = (short)confData.charAt(i);
		}
		msgIndex += l;

		message[msgIndex++] = CR;
		message[msgIndex++] = LF;
		
		short[] crc = crc16(Arrays.copyOfRange(message, 1, msgIndex - 2));
		short[] hex = byteToHexAscii(crc[1]);
		message[msgIndex++] = hex[0];
		message[msgIndex++] = hex[1];
		hex = byteToHexAscii(crc[0]);
		message[msgIndex++] = hex[0];
		message[msgIndex++] = hex[1];
		message[msgIndex++] = EOT;
		
		short[] msgToReturn = Arrays.copyOfRange(message, 0, msgIndex);
		displayMessage.displayLogLater(displayFrame(msgToReturn));

		return msgToReturn;
	}
	
	/**
	 * Returns CRC16-Modbus for input bytes. Code borrowed from 
	 * http://introcs.cs.princeton.edu/java/51data/CRC16.java.html
	 * with start value for CRC set to 0xFFFF instead of 0x0000.
	 * 
	 * Every element of the bytes array is a positive value from 0 to 255
	 * (i.e. an unsigned byte).
	 * 
	 * @param bytes
	 * @return CRC LSB in first array element, MSB in second one
	 */
	private short[] crc16(short[] bytes) {

		final int[] table = {
				0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
				0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
				0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
				0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
				0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
				0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
				0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
				0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
				0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
				0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
				0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
				0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
				0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
				0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
				0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
				0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
				0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
				0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
				0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
				0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
				0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
				0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
				0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
				0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
				0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
				0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
				0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
				0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
				0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
				0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
				0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
				0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040,
		};

		int crc = 0xFFFF;
		for (short b : bytes) {
			crc = (crc >>> 8) ^ table[(crc ^ b) & 0xff];
		}
		short[] r = new short[2];
		r[0] = (short)(crc & 0xFF);
		r[1] = (short)((crc >>> 8) & 0xFF);

		return r;

	}
	
	/**
	 * Returns 2 bytes, MSB first, which are the ASCII codes of the hex representation
	 * of input parameter.
	 * 
	 * @param b
	 * @return
	 */
	private short[] byteToHexAscii(int b) {
	
		final short[] table = {
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46	
		};
		
		short[] h = new short[2];
		h[1] = table[b & 0x0F];
		h[0] = table[(b >>> 4) & 0x0F];
		
		return h;
	}
	
	/**
	 * Returns a value between 0 and 255, taking as input parameter 2 bytes containing the ASCII codes
	 * of the hex representation of the byte. MSQ has to be in first byte, LSQ in second one.
	 * 
	 * @param b
	 * @return 0 if input parameter is out of range
	 */
	private short hexAsciiToByte(short[] b) {

		int l = 0;
		int m = 0;
		
		if ((b[1] >= 0x30) && (b[1] <= 0x39)) {
			l = b[1] - 0x30; 
		} else if ((b[1] >= 0x41) && (b[1] <= 0x46)) {
			l = b[1] - 0x41 + 10;
		} else if ((b[1] >= 0x61) && (b[1] <= 0x66)) {
			l = b[1] - 0x61 + 10;
		}

		if ((b[0] >= 0x30) && (b[0] <= 0x39)) {
			m = b[0] - 0x30; 
		} else if ((b[0] >= 0x41) && (b[0] <= 0x46)) {
			m = b[0] - 0x41 + 10;
		} else if ((b[0] >= 0x61) && (b[0] <= 0x66)) {
			m = b[0] - 0x61 + 10;
		}
		
		return (short)(m * 16 + l);
	}
	
	/**
	 * Converts a frame to a human-readable format. Every non displayable character is converted to \0xFF, FF being
	 * the hexadecimal representation of the byte.
	 * 
	 * @param b
	 * @return
	 */
	public String displayFrame(short[] b) {
		
		StringBuilder sb = new StringBuilder();
		short[] h;
		
		int l = b.length;
		for (int i = 0; i < l; i++) {
			if ((b[i] >= 0x20) && (b[i] <= 0x7E)) {
				sb.append((char)b[i]);
			} else {
				sb.append(HEX_PREFIX);
				h = byteToHexAscii(b[i]);
				sb.append((char)h[0]);
				sb.append((char)h[1]);
			}
		}
		
		return sb.toString();
	}

}
