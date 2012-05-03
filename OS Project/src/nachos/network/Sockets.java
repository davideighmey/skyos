package nachos.network;

import java.util.LinkedList;
import java.util.Queue;

import nachos.machine.*;
//Socket Descriptor: similar to a file descriptor, but linked to a socket instead of a file,
//can be used in low level commands such as read() and write()

import nachos.threads.*;

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
	public int highestSeqSeen;
	public int highestSeqSent;
	public int currentRetries;

	
	//public LinkedList<TCPpackets> receivedPackets;
	public LinkedList<TCPpackets> unacknowledgedPackets;
	public LinkedList<Integer> receivedAcks;
	public Queue<TCPpackets> sBuffer; // send buffer
	public Queue<TCPpackets>  receivedPackets; // receive buffer
	public socketStates states;
	//The receiver advertised window(adwn) is the buffer size sent in each ACK
	public final int adwn = 16;    
	//Congestion Window(cwnd) controls the number of packets a TCP flow may have in the network in a given time **Credit Count**
	public int cwnd;
	//need to make something to hold the message
	//lock when using the socket
	public Lock socketLock = new Lock();
	//make connect a blocking call
	public Condition connectBlock = new Condition(socketLock); 
	public Sockets(int _hostPort) {
		//Connection info
		this.hostID = Machine.networkLink().getLinkAddress();
		this.hostPort = _hostPort;
		destPort = -1;
		destID = -1;
		currentRetries = 0;

		//Setting up buffer
		sBuffer = new LinkedList<TCPpackets>() ;
		//rBuffer = new LinkedList<TCPpackets>();

		//Setting up List
		receivedPackets = new LinkedList<TCPpackets>();
		unacknowledgedPackets = new LinkedList<TCPpackets>();
		receivedAcks = new LinkedList<Integer>();

		//Setting credit count of socket
		cwnd = adwn;

		//Setting the state of the socket
		states = socketStates.CLOSED;

		//setting up lock
		

		//etc
		highestSeqSeen = 0;
		highestSeqSent = 0;
		packetID = 0;
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
		{	
			System.out.println("--Socket was CLOSED--");
			if(receivedPackets.isEmpty()) // make sure there are no packets that still need to be read
			{
				System.out.println("--recievedPackets list is empty--");
				return -1;
			}
		}
		if(receivedPackets.size() == 0) // if there is nothing to read then return 0
		{
			System.out.println("--Size of receivedPackets list is 0. Nothing to be read--");
			return 0;
		}

		System.out.println("--There is something to be read--");
		//NetKernel actually do something
		// make a new packet with the first packet that is the first one on the received linked list
		System.out.println("There are " + receivedPackets.size() + " receivedPacket(s), getting next packet.");
		//TCPpackets packet = receivedPackets.getFirst();
		TCPpackets packet = receivedPackets.poll();
		System.out.println("New receivedPackets size = " + receivedPackets.size());

		int copyBytes = length;
		//int copyBytes = Math.min(length, packet.contents.length);
		System.out.println("Bytes to copy are = " + copyBytes);
		//System.out.println("What is in the packet--> " + packet.contents.toString());
		if(copyBytes == 0)
		{
			return 0;
		}

		byte[] contents = "hello2_SomeStringLongerThan_copyBytes".getBytes(); // this does not if string is short
		//byte[] contents = packet.contents; // this gives error null pointer exception
		System.out.println("Copying...");
		System.arraycopy(contents, 0, buf,0,copyBytes);
		// have to remove the copied bytes from packet
		// or completely remove packet from buffer
		byte[] contents2 = new byte[contents.length - copyBytes];
		if(contents2.length == 0)
			receivedPackets.remove(packet);
		else
		{
			System.arraycopy(contents,  copyBytes, contents2, 0, contents2.length);
			packet.contents = contents2; // null pointer exception
		}
		System.out.println("++Read " + copyBytes + "++");
		return copyBytes;
		//return -1;
	}
	
	void socketSleep(){
		this.socketLock.acquire();
		this.connectBlock.sleep();
		this.socketLock.release();
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
		else if(length == 0) // length of 0 nothing to write
			{
				System.out.println("---Length was 0, nothing to write---");
				return 0;
			}
		
		System.out.println("---There is something to be written---");
		//check that status of this socket before continuing
		int bytesWritten = 0;

		// get how many blocks we are going to need to transfer
		int numBlocks = (int)Math.ceil((float) length / (float)TCPpackets.maxContentsLength);

		//byte[] readyToWrite = new byte[numBlocks];

		//System.out.println("---Do we queue the blocks or what??---");

		//if(states == socketStates.ESTABLISHED){
		for(int i = 0; i < numBlocks; i++)
		{
			byte[] toWrite = new byte[length]; // what size
			// copy bytes from the buf[] that is given to the toWrite[] that we made
			System.arraycopy(buf, bytesWritten, toWrite, 0, toWrite.length);
			bytesWritten = bytesWritten + toWrite.length; // update number of bytes written
			length = length - toWrite.length; // decrease (update) how much we still have to write

			// still more stuff
			TCPpackets newPacket = null; 
			try {
				System.out.println("Previous size of receivedPackets = " + receivedPackets.size());
				newPacket = new TCPpackets(destID,destPort,hostID,hostPort,toWrite,true,false,false,false,increaseCount());

			} catch (MalformedPacketException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();

			}
			//newPacket.contents = // set the new contents in the packet
			receivedPackets.add(newPacket);
			System.out.println("New size of receivedPackets = " + receivedPackets.size());
		}
		//}
		// return how many bytes were written
		System.out.println("--Bytes Written " + bytesWritten + "--");
		return bytesWritten; // or length??
	}


	/*
	 * 
	 */
	public void setPacketID(byte[] contents)
	{
		byte[] temp = new byte[4];
		Lib.bytesFromInt(temp, 0, 4, packetID++);
		for (int i =0;i<4;i++)
			contents[4+i]=temp[i];
	}

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
		socketLock.acquire();
		unacknowledgedPackets.add(pckt);
		//NetKernel.transport.messageQueue.add(pckt);
		NetKernel.transport.send(pckt);
		socketLock.release();

	}

	public void handlePacket( TCPpackets pckt)
	{
		//System.out.println(getKey());
		switch(states){
		case CLOSED: 
			//if syn, set state to syn received
			if((pckt.syn==true) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==false)){
				states = socketStates.SYNRECEIVED;
				
				}
			//if fin send finack
			if((pckt.syn==false) && (pckt.ack == false) && (pckt.stp==false) && (pckt.fin == true))
				sendFINACK();
			break;
		case SYNSENT:
			//check if it received the syn/ack packet
			if((pckt.syn == true) && (pckt.ack ==true) && (pckt.stp == false) && (pckt.fin == false)){
				states = socketStates.ESTABLISHED;
				socketLock.acquire();
				connectBlock.wakeAll();
				socketLock.release();
				//wake thread waiting in connect()
			}
			//if it is just a syn--simultaneous connection
			if((pckt.syn ==true) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==false))
				sendACK();
			//if data send syn;
			if((pckt.syn==false) && (pckt.ack == false) && (pckt.stp==false) && (pckt.fin==false))
				sendSYN();
			//stp
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==true) && (pckt.fin==false))
				sendSYN();
			//fin
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==true))
				sendSYN();
			break;
		case SYNRECEIVED:
			break;
		case ESTABLISHED:
			//if syn, send synack
			if((pckt.syn==true) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==false))
				sendSYNACK();
			//if data
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==false)){
				socketLock.acquire();
				receivedPackets.add(pckt);
				socketLock.release();
				sendACK();
			}
			//if ack
			if((pckt.syn==false) && (pckt.ack==true) && (pckt.stp==false) && (pckt.fin==false)){
				//shift send window
				//send data
				sendData(pckt);
			}
			//if stp
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==true) && (pckt.fin==false)){
				//clear send window
				states = socketStates.STPRCVD;
			}
			//if fin
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==true)){
				//clear send window
				sendFINACK();
				if(NetKernel.transport.activeSockets.containsKey(getKey()))
					NetKernel.transport.activeSockets.remove(getKey());
				states = socketStates.CLOSED;
			}
			break;
		case STPRCVD:
			//if data
			if((pckt.syn == false) && (pckt.ack == false) && (pckt.stp == false)&& (pckt.fin == false)){
				socketLock.acquire();
				receivedPackets.add(pckt);
				socketLock.release();
				sendACK();
			}
			//if fin
			if((pckt.syn == false) && (pckt.ack == false) && (pckt.stp == false) && (pckt.fin == true)){
				sendFINACK();
				if(NetKernel.transport.activeSockets.containsKey(getKey()))
					NetKernel.transport.activeSockets.remove(getKey());
				states = socketStates.CLOSED;
			}
			break;
		case STPSENT:
			//if syn
			if( (pckt.syn == true) && (pckt.ack == false) && (pckt.stp == false) && (pckt.fin == false))
				sendSYNACK();
			//data
			if((pckt.syn == false)&& (pckt.ack == false) && (pckt.stp == false) && (pckt.fin == false)){
				sendSTP();
			}
			//ack
			if((pckt.syn == false)&& (pckt.ack == true)&& (pckt.stp == false)&& (pckt.fin == false)){
				//if send queue is empty , send fin and goto closing
				if(sBuffer.isEmpty()){
					sendFIN();
					states = socketStates.CLOSEWAIT;
				}
				else{
					//shift send window and send data

					sendData(pckt);

				}

			}
			//stp
			if((pckt.syn == false)&& (pckt.ack == false)&& (pckt.stp == true)&& (pckt.fin == false)){
				//clear send window
				sendFIN();
				states = socketStates.CLOSEWAIT;
			}
			//fin
			if((pckt.syn == false) && (pckt.ack == false)&& (pckt.stp == false) && (pckt.fin == true)){
				sendFINACK();
				if(NetKernel.transport.activeSockets.containsKey(getKey()))
					NetKernel.transport.activeSockets.remove(getKey());
				states = socketStates.CLOSED;
			}
			break;
		case CLOSEWAIT:
			//syn
			if((pckt.syn == true) && (pckt.ack == false) && (pckt.stp == false) && (pckt.fin == false)){
				sendSYNACK();
			}
			//data
			if((pckt.syn == false)&& (pckt.ack == false)&& (pckt.stp == false) && (pckt.fin == false)){
				sendFIN();
			}
			//stp
			if((pckt.syn == false) && (pckt.ack == false) &&(pckt.stp == true)&& (pckt.fin == false)){
				sendFIN();
			}
			//fin
			if((pckt.syn == false) && (pckt.ack == false) && (pckt.stp == false) && (pckt.fin == true)){
				sendFINACK();
				if(NetKernel.transport.activeSockets.containsKey(getKey()))
					NetKernel.transport.activeSockets.remove(getKey());
				states = socketStates.CLOSED;
			}
			//fin/ack
			if( (pckt.syn == false) && (pckt.ack == true) && (pckt.stp == false) && (pckt.fin == true)){
				if(NetKernel.transport.activeSockets.containsKey(getKey()))
					NetKernel.transport.activeSockets.remove(getKey());
				states = socketStates.CLOSED;
			}
		}

	}
	public void sendSTP(){
		TCPpackets stp;
		System.out.println("Sent STP");
		try {
			stp = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,false,true,false,increaseCount());
			//NetKernel.transport.send(syn);
			send(stp);
		} catch (MalformedPacketException e) {}

	}
	public void sendFINACK(){
		TCPpackets finack;
		System.out.println("Sent FINAck");
		try {
			finack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,true,false,true,increaseCount());
			//NetKernel.transport.send(syn);
			send(finack);
		} catch (MalformedPacketException e) {}

	}
	public void sendSYNACK(){
		TCPpackets synack;
		System.out.println("Sent SYNAck");
		try {
			synack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],true,true,false,false,increaseCount());
			//NetKernel.transport.send(syn);
			send(synack);
		} catch (MalformedPacketException e) {}

	}
	public void sendSYN(){
		TCPpackets syn;
		System.out.println("Sent SYN");
		try {
			syn = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],true,false,false,false,increaseCount());
			//NetKernel.transport.send(syn);
			send(syn);
		} catch (MalformedPacketException e) {}

	}
	public void sendFIN(){
		TCPpackets fin;
		System.out.println("Sent FIN");
		try {
			fin = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,false,false,true,increaseCount());
			//NetKernel.transport.send(fin);
			send(fin);
		} catch (MalformedPacketException e) {}

	}
	public void sendACK(){
		TCPpackets ack;
		System.out.println("Sent Ack");
		try {
			ack = new TCPpackets(destID,destPort,hostID,hostPort,new byte[0],false,true,false,false,increaseCount());
			//NetKernel.transport.send(ack);
			send(ack);
		} catch (MalformedPacketException e) {}

	}

	//This will uniquely id the socket....maybe
	public String getKey(){
		return destPort+ "." +destID+ "." +hostPort + "." + hostID;

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

	public TCPpackets getNextPacket(){
		System.out.println("SEQUENCE NUMBERS INCOMING: LF " + highestSeqSeen);
		socketLock.acquire();
		for (int i =0; i< receivedPackets.size(); i++)
		{
			//System.out.println("seqNo: " + Lib.bytesToInt(receivedPackets.get(i).contents, 4, 4) + "  to read:" + seqNoRead);
			if (Lib.bytesToInt(receivedPackets.peek().packet.contents, 4, 4)==highestSeqSeen)
			{
				highestSeqSeen++;
				TCPpackets p = receivedPackets.poll();
				socketLock.release();
				return p;
			}

		}
		socketLock.release();
		return null;


	}
	public void sendData(TCPpackets pckt){
		int seqNum = Lib.bytesToInt(pckt.packet.contents, 4, 4);
		socketLock.acquire();
		for (int i =0; i<unacknowledgedPackets.size(); i++)
		{
			TCPpackets temp = unacknowledgedPackets.get(i);
			if (seqNum==Lib.bytesToInt(temp.contents, 4, 4) &&
					temp.packet.dstLink == pckt.packet.srcLink &&
					temp.packet.srcLink == pckt.packet.dstLink &&
					temp.packet.contents[0] == pckt.packet.contents[1] &&
					temp.packet.contents[1] == pckt.packet.contents[0])
			{
				unacknowledgedPackets.remove(i);
				receivedAcks.add(seqNum);
				break;
			}			
		}
		socketLock.release();
	}

	public void timeOutEvent() {
		// TODO Auto-generated method stub
		//events to handle the different time outs.
		//one for syn, fin and during regular packets transferswitch(state)
		//socketLock.acquire();
		//connectBlock.wakeAll();
		//socketLock.release();
		switch(states){
		case SYNSENT:
			if(currentRetries<3){
			System.out.println("Retrying " + (currentRetries+1) + " out of 3");
			currentRetries++;
			sendSYN();
			}
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



