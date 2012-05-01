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
		STPRCVD, STPSENT, CLOSEWAIT}

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
	public int read(byte[] buf, int offset, int length)
	{
		System.out.println("----Read() being called----");
		if(states == socketStates.CLOSED)
			if(receivedPackets.isEmpty()) // make sure there are no packets that still need to be read
				return -1;
		if(receivedPackets.size() == 0) // if there is nothing to read then return 0
			return 0;
		else if (length == 0)
			return 0;
		System.out.println("--There is something to be read--");
		// actually do something
		// make a new packet with the first packet that is the first one on the received linked list
		TCPpackets packet = receivedPackets.getFirst();

		//int copyBytes = length;
		int copyBytes = Math.min(length, packet.contents.length);
		byte[] contents = packet.contents;
		System.arraycopy(contents, 0, buf,0,copyBytes);
		// have to remove the copied bytes from packet
		// or completely remove packet from buffer
		byte[] contents2 = new byte[contents.length - copyBytes];
		if(contents2.length == 0)
			receivedPackets.remove(packet);
		else
		{
			System.arraycopy(contents,  copyBytes, contents2, 0, contents2.length);
			packet.contents = contents2;
		}
		System.out.println("--Read " + copyBytes + "--");
		return copyBytes;
		//return -1;
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
		System.out.println("----write() being called----");
		if(states == socketStates.CLOSED) // if there is not a connection return -1 "error"
		{
			System.out.println("----sockets were closed----");
			return -1;
		}
		System.out.println("---There is something to be written---");
		//check that status of this socket before continuing
		int bytesWritten = 0;

		// get how many blocks we are going to need to transfer
		int numBlocks = (int)Math.ceil((float) length / (float)TCPpackets.maxContentsLength);

		//byte[] readyToWrite = new byte[numBlocks];

		System.out.println("---Do we queue the blocks or what??---");

		if(states == socketStates.ESTABLISHED){
			for(int i = 0; i < numBlocks; i++)
			{
				byte[] toWrite = new byte[length]; // what size
				// copy bytes from the buf[] that is given to the toWrite[] that we made
				System.arraycopy(buf, bytesWritten, toWrite, 0, toWrite.length);
				bytesWritten = bytesWritten + toWrite.length; // update number of bytes written
				length = length - toWrite.length; // decrease (update) how much we still have to write

				// still more stuff
			}
		}
		// return how many bytes were written
		System.out.println("--Bytes Written " + bytesWritten + "---");
		return bytesWritten; // or length??
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

	public void handlePacket( TCPpackets pckt)
	{
		switch(states){
		case CLOSED: 
			//if syn, set state to syn received
			if(pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin)
				states = socketStates.SYNRECEIVED;
			//if fin send finack
			if(pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin)
				sendFINACK();
			break;
		case SYNSENT:
			//check if it recieved the syn/ack packet
			if(pckt.syn&&pckt.ack&&!pckt.stp&&!pckt.fin){
				states = socketStates.ESTABLISHED;
				//wake thread waiting in connect()
			}
			//if it is just a syn--simultaneous connection
			if(pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin)
				sendACK();
			//if data send syn;
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin)
				sendSYN();
			//stp
			if(!pckt.syn&&!pckt.ack&&pckt.stp&&!pckt.fin)
				sendSYN();
			//fin
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&pckt.fin)
				sendSYN();
			break;
		case SYNRECEIVED:
			break;
		case ESTABLISHED:
			//if syn, send synack
			if(pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin)
				sendSYNACK();
			//if data
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin){
				//queue data
				sendACK();}
			//if ack
			if(!pckt.syn&&pckt.ack&&!pckt.stp&&!pckt.fin){
				//shift send window
				//send data
			}
			//if stp
			if(!pckt.syn&&!pckt.ack&&pckt.stp&&!pckt.fin){
				//clear send window
				states = socketStates.STPRCVD;
			}
			//if fin
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&pckt.fin){
				//clear send window
				sendFINACK();
				states = socketStates.CLOSED;
			}
			break;
		case STPRCVD:
			//if data
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin){
				//queue data
				sendACK();
			}
			//if fin
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&pckt.fin){
				sendFINACK();
				states = socketStates.CLOSED;
			}
			break;
		case STPSENT:
			//if syn
			if(pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin)
				sendSYNACK();
			//data
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin){
				sendSTP();
			}
			//ack
			if(!pckt.syn&&pckt.ack&&!pckt.stp&&!pckt.fin){
				//shift send window and send data
				//if send queue is empty , send fin and goto closing
			}
			//stp
			if(!pckt.syn&&!pckt.ack&&pckt.stp&&!pckt.fin){
				//clear send window
				sendFIN();
				states = socketStates.CLOSEWAIT;
			}
			//fin
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&pckt.fin){
				sendFINACK();
				states = socketStates.CLOSED;
			}
			break;
		case CLOSEWAIT:
			//syn
			if(pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin){
				sendSYNACK();
			}
			//data
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&!pckt.fin){
				sendFIN();
			}
			//stp
			if(!pckt.syn&&!pckt.ack&&pckt.stp&&!pckt.fin){
				sendFIN();
			}
			//fin
			if(!pckt.syn&&!pckt.ack&&!pckt.stp&&pckt.fin){
				sendFINACK();
				states = socketStates.CLOSED;
			}
			//fin/ack
			if(!pckt.syn&&pckt.ack&&!pckt.stp&&pckt.fin){
				states = socketStates.CLOSED;
			}
		}

	}
	public void sendSTP(){
		TCPpackets stp;
		try {
			stp = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,false,true,false,increaseCount());
			//NetKernel.transport.send(syn);
			NetKernel.transport.send(stp);
		} catch (MalformedPacketException e) {}

	}
	public void sendFINACK(){
		TCPpackets finack;
		try {
			finack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],true,true,false,false,increaseCount());
			//NetKernel.transport.send(syn);
			NetKernel.transport.send(finack);
		} catch (MalformedPacketException e) {}

	}
	public void sendSYNACK(){
		TCPpackets synack;
		try {
			synack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],true,true,false,false,increaseCount());
			//NetKernel.transport.send(syn);
			NetKernel.transport.send(synack);
		} catch (MalformedPacketException e) {}

	}
	public void sendSYN(){
		TCPpackets syn;
		try {
			syn = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],true,false,false,false,increaseCount());
			//NetKernel.transport.send(syn);
			NetKernel.transport.send(syn);
		} catch (MalformedPacketException e) {}

	}
	public void sendFIN(){
		TCPpackets fin;
		try {
			fin = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,false,false,true,increaseCount());
			//NetKernel.transport.send(fin);
			NetKernel.transport.send(fin);
		} catch (MalformedPacketException e) {}

	}
	public void sendACK(){
		TCPpackets ack;
		try {
			ack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,true,false,false,increaseCount());
			//NetKernel.transport.send(ack);
			NetKernel.transport.send(ack);
		} catch (MalformedPacketException e) {}

	}
	public void sendPacket(TCPpackets p)
	{
		socketLock.acquire();
		unacknowledgedPackets.add(p);
		NetKernel.transport.send(p);
		socketLock.release();
	}
	//This will uniquely id the socket....maybe
	public int getKey(){
		return destPort+destID+hostPort+hostID;

	}
	//closed event
	public void close(){
		switch(states){
		case ESTABLISHED:
			//if send queue is empty, send fin, go to closing
			if(sBuffer.isEmpty()){
				sendFIN();
				states = socketStates.CLOSEWAIT;
			}
			else{
				sendSTP();
				states = socketStates.STPSENT;
			}
			//else send stp and goto stpsent
			break;
		case CLOSED:
			sendFIN();
			states = socketStates.CLOSEWAIT;
			break;
		}
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
			//case FINWAIT1:
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



