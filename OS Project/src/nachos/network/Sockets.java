package nachos.network;

import java.util.LinkedList;
import java.util.Queue;

import nachos.machine.*;
//Socket Descriptor: similar to a file descriptor, but linked to a socket instead of a file,
//can be used in low level commands such as read() and write()
import nachos.threads.Alarm;
import nachos.threads.Lock;

public class Sockets extends OpenFile {
	public enum socketStates{CLOSED, SYNSENT, SYNRECEIVED, ESTABLISHED,
		FINWAIT1, FINWAIT2, CLOSEWAIT}

	/****************************************************************
	 *  Socket: a data structure containing connection information  *
	 *      Connection identifying information:                         *
	 *              - client IP (Internet Protocol) address                 *
	 *              - client port number                                                                    *
	 *              - source IP address                                                                             *
	 *              - source port number                                                                    *
	 ****************************************************************/
	public int destPort;
	public int destID;
	public int hostPort;
	public int hostID;
	public int packetID;
	public LinkedList<TCPpackets> receivedPackets;
	public LinkedList<TCPpackets> unacknowledgedPackets;
	public LinkedList<Integer> receivedAcks;
	public Queue<TCPpackets> sBuffer; // send buffer
	public Queue<TCPpackets>  rBuffer; // receive buffer
	public socketStates states;
	//The receiver advertised window(adwn) is the buffer size sent in each ACK
	public final int adwn = 16;    
	//Congestion Window(cwnd) controls the number of packets a TCP flow may have in the network in a given time **Credit Count**
	public int cwnd;
	//need to make something to hold the message
	public Lock socketLock;

	public Sockets(int _hostPort) {
		//Connection info
		this.hostID = Machine.networkLink().getLinkAddress();
		this.hostPort = _hostPort;
		destPort = -1;
		destID = -1;
		packetID = 0;

		//Setting up buffer
		sBuffer = new LinkedList<TCPpackets>() ;
		rBuffer = new LinkedList<TCPpackets>();

		//Setting up List
		receivedPackets = new LinkedList<TCPpackets>();
		unacknowledgedPackets = new LinkedList<TCPpackets>();
		receivedAcks = new LinkedList<Integer>();

		//Setting credit count of socket
		cwnd = adwn;

		//Setting the state of the socket
		states = socketStates.CLOSED;

		//setting up lock
		socketLock = new Lock();
	}
	public int increaseCount(){
		return packetID++;
	}
	/**
	 * Read this file starting at the specified position and return the number
	 * of bytes successfully read. If no bytes were read because of a fatal
	 * error, returns -1
	 *
	 * @param       buf     the buffer to store the bytes in.
	 * @param       offset  the offset in the buffer to start storing bytes.
	 * @param       length  the number of bytes to read.
	 * @return      the actual number of bytes successfully read, or -1 on failure.
	 */  
	public int read(byte[] buf, int offset, int length) {



		return -1;
	}

	/**
	 * Write this file starting at the specified position and return the number
	 * of bytes successfully written. If no bytes were written because of a
	 * fatal error, returns -1.
	 *
	 * @param       pos     the offset in the file at which to start writing.
	 * @param       buf     the buffer to get the bytes from.
	 * @param       offset  the offset in the buffer to start getting.
	 * @param       length  the number of bytes to write.
	 * @return      the actual number of bytes successfully written, or -1 on
	 *              failure.
	 * @throws MalformedPacketException
	 */    
	public int write(byte[] buf, int offset, int length)  
	{
		if(states == socketStates.CLOSED) // if there is not a connection return -1 "error"
			return -1;
		
		//check that status of this socket before continuing
		int bytesWriten = 0;
		//things in here will be translated into packets and placed on the send buffer
		LinkedList<Byte> readyToWrite = new LinkedList<Byte>();

		if(states == socketStates.ESTABLISHED){
			// go by offset sized blocks
			for (bytesWriten = 0; bytesWriten < offset; bytesWriten++) {
				readyToWrite.add(buf[bytesWriten]);
			}
			//bytesWriten -= offset;
			sendWrite(readyToWrite);
		}
		// return how many bytes were written
		return bytesWriten;
	}

	/*
	 * 
	 */
	public void sendWrite(LinkedList<Byte> writeMe){
		byte[] array;
		TCPpackets pckt;
		
		if(!writeMe.isEmpty() && states == socketStates.ESTABLISHED){
			array = new byte[java.lang.Math.min(writeMe.size(), TCPpackets.maxContentsLength)];
			for (int i = 0; i < array.length; i++) {
				array[i] = writeMe.removeFirst();
				try {
					pckt = new TCPpackets(destID,destPort,hostID,hostPort,array,false,false,false,false,increaseCount());
					sBuffer.add(pckt);
				} catch (MalformedPacketException e) {}
			}

		}
	}
	public void send(TCPpackets pckt){
		//if(states == socketStates.CLOSED)

	}
	public void sendSYN(){
		TCPpackets syn;
		try {
			syn = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],true,false,false,false,increaseCount());
			NetKernel.transport.send(syn);
		} catch (MalformedPacketException e) {}

	}
	public void sendFIN(){
		TCPpackets fin;
		try {
			fin = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,false,false,true,increaseCount());
			NetKernel.transport.send(fin);
		} catch (MalformedPacketException e) {}

	}
	public void sendACK(){
		TCPpackets ack;
		try {
			ack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,true,false,false,increaseCount());
			NetKernel.transport.send(ack);
		} catch (MalformedPacketException e) {}

	}
	public void sendPacket(TCPpackets p)
	{
		socketLock.acquire();
		unacknowledgedPackets.add(p);
		NetKernel.transport.addMessage(p);
		socketLock.release();
	}
	//This will uniquely id the socket....maybe
	public int getKey(){
		return destPort+destID+hostPort+hostID;

	}
	public void timeOutEvent() {
		// TODO Auto-generated method stub
		//events to handle the different time outs.
		//one for syn, fin and during regular packets transferswitch(state)

		switch(states){
		case SYNSENT:
			sendSYN();
			break;
		case ESTABLISHED:
		case FINWAIT1:
			socketLock.acquire();
			for(TCPpackets p: unacknowledgedPackets)
			{
				//cwnd--;
				//if(cwnd>0)
				NetKernel.transport.addMessage(p);
			}
			socketLock.release();
			break;
		case CLOSEWAIT:
			sendFIN();
			break;
		}

	}

}



