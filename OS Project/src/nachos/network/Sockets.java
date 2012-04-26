package nachos.network;

import nachos.machine.*;
//Socket Descriptor: similar to a file descriptor, but linked to a socket instead of a file, 
//can be used in low level commands such as read() and write()
	
public class Sockets extends OpenFile {
	public enum socketStates{LISTENING, SYNSENT, SYNRECEIVED, ESTABLISHED, 
		FINWAIT1, FINWAIT2, CLOSEWAIT, LASTACK, TIMEWAIT, CLOSED }
	
	/****************************************************************
	 *  Socket: a data structure containing connection information  *
	 *	Connection identifying information:                         *
	 *		- client IP (Internet Protocol) address                 *
	 *		- client port number									*
	 *		- source IP address										*
	 *		- source port number									*
	 ****************************************************************/
	int destPort;
	int destID;
	int hostPort;
	int hostID;
	socketStates states;
	//The receiver advertised window(adwn) is the buffer size sent in each ACK
	Window adwn;	
	//Congestion Window(cwnd) controls the number of packets a TCP flow may have in the network in a given time 
	Window cwnd;
	//need to make something to hold the message

	
	//attempt to bind the socket to the selected port
	int bindSocket(int port){
		states = socketStates.LISTENING;
		return -1;
	}

	public Sockets(int _hostPort) {
		// TODO Auto-generated constructor stub
		this.hostID = Machine.networkLink().getLinkAddress();
		this.hostPort = _hostPort;
		states = socketStates.CLOSED;
	}

	/**
	 * Read this file starting at the specified position and return the number
	 * of bytes successfully read. If no bytes were read because of a fatal
	 * error, returns -1
	 *
	 * @param	pos	the offset in the file at which to start reading.
	 * @param	buf	the buffer to store the bytes in.
	 * @param	offset	the offset in the buffer to start storing bytes.
	 * @param	length	the number of bytes to read.
	 * @return	the actual number of bytes successfully read, or -1 on failure.
	 */   
	public int read(int pos, byte[] buf, int offset, int length) {
		return -1;
	}

	/**
	 * Write this file starting at the specified position and return the number
	 * of bytes successfully written. If no bytes were written because of a
	 * fatal error, returns -1.
	 *
	 * @param	pos	the offset in the file at which to start writing.
	 * @param	buf	the buffer to get the bytes from.
	 * @param	offset	the offset in the buffer to start getting.
	 * @param	length	the number of bytes to write.
	 * @return	the actual number of bytes successfully written, or -1 on
	 *		failure.
	 */    
	public int write(int pos, byte[] buf, int offset, int length) {
		return -1;
	}

	//Try to connect from the host to the dest
	public boolean createConnection(int _destID, int _destPort){
		destID = _destID;
		destPort = _destPort;

		//have to send a syn packet
		try {
			//not a syn packet. Need to change the mail system to handle it
			TCPpackets a = new TCPpackets(destID,destPort,hostID,hostPort, new byte[0],true,false,false,false,0,0);
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			System.out.println("Malformed Packet has been detected");
			//e.printStackTrace();
			return false;
		}

		//check if sent
		//keep sending until either timeout is reached or connection 
		//if  received an ack, connection is established, return with a value saying connected
		//else return -1
		return false;
	}

	//Try to accept the connection from the sender
	public int acceptConnection(int _hostID){
		hostID = _hostID;	

		return -1;
	}
	//attempt to close the socket
	public void closeSocket(){

	}
}
