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

	// # of data packets that can be sent before and ack packet is received
	// should decrease by one every time a packet is sent
	// and increase by one every time a ack packet is received
	//creditCount must be greater than 0 before a packet can be sent
	public int creditCount = 16;
	// advertised window has a fixed window size of 16 => use variable adwn
	
	// keeps the packet ID of the last packet that was sent in case we have to stop
	//public int lastPID = -1;

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

	TCPpackets currentReadingPacket;
	int currentReadingPacketOff;

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
			//return -1;
		}
		if(receivedPackets.size() == 0) // if there is nothing to read then return 0
		{
			System.out.println("--Size of receivedPackets list is 0. Nothing to be read--");
			return 0;
		}
		if(currentReadingPacket == null || currentReadingPacketOff >= currentReadingPacket.contents.length)
		{
			//currentReadingPacket = NetKernel.packetManager.getMessageOnPort(connection.localPort);
			currentReadingPacket = getNextPacket();
			currentReadingPacketOff = 8;
		}
		int bytesRead = 0;
		while(currentReadingPacket != null && bytesRead < length)
		{
			System.out.println("reading");
			int amountRead = Math.min(currentReadingPacket.contents.length - currentReadingPacketOff, length);
			amountRead = Math.min(amountRead, buf.length - bytesRead);
			System.arraycopy(currentReadingPacket.contents, currentReadingPacketOff, buf, offset + bytesRead, amountRead);
			bytesRead += amountRead;
			currentReadingPacketOff += amountRead;
			if(currentReadingPacketOff >= currentReadingPacket.contents.length){
				currentReadingPacket = getNextPacket();
				currentReadingPacketOff = 8;    
			}
		}
		System.out.println("finished reading");
		return bytesRead;
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
		switch(states){
		case CLOSED:
			return-1;
		case ESTABLISHED:
			TCPpackets packet = null;
			int bytePos = offset;
			int endPos = offset + length;
			while(bytePos < endPos){
				int amountSend = Math.min(TCPpackets.maxContentsLength, endPos - bytePos);
				byte[] contents = new byte[amountSend];		  
				System.arraycopy(buf, bytePos, contents, 0, amountSend);
				bytePos += amountSend;
				setNum(contents);
				try {
					packet = new TCPpackets(destID,destPort,hostID,hostPort,contents,false,false,false,false,increaseCount());
				} catch (MalformedPacketException e){}	
				if(creditCount > 0)
				{
					//lastPID = packet.packetID; // keep the packet ID of the last packet that was sent
					unacknowledgedPackets.add(packet);
					send(packet);
				}
				else
					return amountSend;
			}
			return length;
		case STPRCVD:
			return -1;

		}
		return 0;
	}


	public int seq;

	public void setNum(byte[] recieved)
	{
		byte[] temp = new byte[4];
		Lib.bytesFromInt(temp, 0, 4, seq++);
		for (int i =0;i<4;i++)
			recieved[4+i]=temp[i];
	}


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

	/**
	 * Return false to drop the packet if it was not the correct packet in the sequence, or a duplicate one. 
	 * @param pckt
	 * @return true, if it was added to the list, else false if it was dropped because of duplicate or non-seq.
	 */
	public void SeqNumForReceive(TCPpackets pckt){
		if(pckt.packetID == highestSeqSeen) {
			receivedPackets.add(pckt);
			highestSeqSeen++;
		}
		else;
		//Looking for a similar packet, i.e retransmitted packets. if true, just drop it
		/*(receivedPackets.contains(pckt) == true){
		}
		*/
	}
	//Send and receive SeqNum are different
	public int SeqNumForSend(TCPpackets pckt){
		int packetSEQ = 0;
		if(unacknowledgedPackets.isEmpty()){
			packetSEQ = highestSeqSent;  //This should still be zero once converted to bytes, if first packet
			highestSeqSent++;
			return packetSEQ;
		}
		else{
			//Looking for a similar packet, i.e retransmitted packets.
			for(int i = 0; i < unacknowledgedPackets.size(); i++){
				TCPpackets dummy = unacknowledgedPackets.get(i); // assumming that the packetID from handle calls is 0; So !equals.
				if((pckt.ack == dummy.ack) &&
						(pckt.fin == dummy.fin) &&
						(pckt.stp == dummy.stp) &&
						(pckt.syn == dummy.syn) &&
						(pckt.contents == dummy.contents) &&
						(pckt.dstPort == dummy.dstPort) &&
						(pckt.packet == dummy.packet) &&
						(pckt.srcPort == dummy.srcPort)
						/*(pckt.packetID != dummy.packetID)*/){
					packetSEQ = dummy.packetID;
					return packetSEQ;
				}
			}
		}
		//Else if there are no packets that are the same, get the highest, Seq number of the packet in the list
		for (int i =0; i < unacknowledgedPackets.size(); i++){
			if (Lib.bytesToInt(unacknowledgedPackets.peek().packet.contents, 4, 4) == highestSeqSent)
			{
				highestSeqSent++;
				packetSEQ = highestSeqSent;
			}
		}
		return packetSEQ; 
	}

	public void send(TCPpackets pckt){
		/*
		 * Shit forgot that the pckts being sent from the handlePackets, will never be the same within the packetPackets list.
		 */
		socketLock.acquire();
		pckt.packetID = SeqNumForSend(pckt);
		if(unacknowledgedPackets.contains(pckt) == false){
			unacknowledgedPackets.add(pckt);
		}
		// Sliding window part
		if(creditCount > 0)
		{
			if(unacknowledgedPackets.size() < adwn) // check to make sure that it is below 16 (sliding window size)
				unacknowledgedPackets.add(pckt);

			//NetKernel.transport.messageQueue.add(pckt);
			NetKernel.transport.send(pckt);
			socketLock.release();
		}
		// should not send packet until and ack packet is received and creditCount is increased

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
				//receivedPackets.add(pckt);
				SeqNumForReceive(pckt);
				socketLock.release();
				sendACK();
			}
			//if ack
			if((pckt.syn==false) && (pckt.ack==true) && (pckt.stp==false) && (pckt.fin==false)){
				//shift send window -- did it in sendData()
				//send data
				sendData(pckt);
			}
			//if stp
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==true) && (pckt.fin==false)){
				//clear send window
				creditCount = 16; // reset back to 16
				states = socketStates.STPRCVD;
			}
			//if fin
			if((pckt.syn==false) && (pckt.ack==false) && (pckt.stp==false) && (pckt.fin==true)){
				//clear send window
				creditCount = 16; // reset back to 16
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
			//	receivedPackets.add(pckt);
				SeqNumForReceive(pckt);
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
		//int seqNum = Lib.bytesToInt(pckt.packet.contents, 4, 4);
		socketLock.acquire();
		for (int i =0; i<unacknowledgedPackets.size(); i++)
		{
			if(creditCount < 16) // make sure there is we have sufficient credit count 
			{
				creditCount++; // increase creditCount because we are receiving an ack packet
				//TCPpackets temp = unacknowledgedPackets.get(i);			
				unacknowledgedPackets.remove(i);
				//receivedAcks.add(seqNum);
			}
			else // creditCount == 0 so not enough room to send another packet
				break;

		}
		socketLock.release();
	}

	public void timeOutEvent() {

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
