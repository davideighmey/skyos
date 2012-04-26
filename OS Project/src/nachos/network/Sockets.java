package nachos.network;

import java.util.LinkedList;

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
	 *	Connection identifying information:                         *
	 *		- client IP (Internet Protocol) address                 *
	 *		- client port number									*
	 *		- source IP address										*
	 *		- source port number									*
	 ****************************************************************/
	public int destPort;
	public int destID;
	public int hostPort;
	public int hostID;
	public int netID;
	public TCPpackets[] writeBuffer;
	public TCPpackets[] readBuffer;
	public  Lock sendLock;
	public  Lock recieveLock;
	public socketStates states;
	//The receiver advertised window(adwn) is the buffer size sent in each ACK
	public Window adwn;	
	//Congestion Window(cwnd) controls the number of packets a TCP flow may have in the network in a given time 
	public Window cwnd;
	//need to make something to hold the message
	
	
	//attempt to bind the socket to the selected port
	int bindSocket(int port){
		//states = socketStates.LISTENING;
		return -1;
	}

	public Sockets(int _hostPort) {
		// TODO Auto-generated constructor stub
		//Connection info
		this.hostID = Machine.networkLink().getLinkAddress();
		this.hostPort = _hostPort;
		netID = 0;
		destPort = -1;
		destID = -1;
		//Thread control
		sendLock = new Lock();
		recieveLock = new Lock();
		//Setting up window
		adwn = new Window();
		cwnd = new Window();
		//Setting up buffer
		writeBuffer = new TCPpackets[100];
		readBuffer = new TCPpackets[16];
		
		states = socketStates.CLOSED;
	}

	/**
	 * Read this file starting at the specified position and return the number
	 * of bytes successfully read. If no bytes were read because of a fatal
	 * error, returns -1
	 *
	 * @param	buf	the buffer to store the bytes in.
	 * @param	offset	the offset in the buffer to start storing bytes.
	 * @param	length	the number of bytes to read.
	 * @return	the actual number of bytes successfully read, or -1 on failure.
	 */   
	public int read(byte[] buf, int offset, int length) {
	
		
		
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
	public int write(byte[] buf, int offset, int length) {
		  //check that status of this socket before continuing
        int bytesWriten = 0;
        if(states == socketStates.ESTABLISHED){
                for (bytesWriten = offset; bytesWriten < length - offset; bytesWriten++) {
                        
                	//readBuffer //.add(buf[bytesWriten]);
                }
                bytesWriten -= offset;
        }
        return bytesWriten;
	}

	//Try to connect from the host to the dest
	public boolean createConnection(int _destID, int _destPort){
		destID = _destID;
		destPort = _destPort;

		//have to send a syn packet
		try {
			TCPpackets syn = new TCPpackets(destID,destPort,hostID,hostPort, new byte[0],true,false,false,false,0);
			states = socketStates.SYNSENT;
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			System.out.println("Malformed Packet has been detected");
			//e.printStackTrace();
			return false;
		}
		int count = 0;
		Alarm alarm = new Alarm();
		while(states== socketStates.SYNSENT && count < TransportLayer.maxRetry){
			try {
				alarm.wait(TransportLayer.timeoutLength);
			} catch (InterruptedException e) {
				return false;
			}
			count++;
		}
		//if(states == socketStates.SYNRECEIVED)
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
