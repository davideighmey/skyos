package nachos.network;

import nachos.machine.*;
import nachos.threads.*;

public class TransportLayer extends PostOffice {

	public TransportLayer(){


	}
	/* Socket is a type of file descriptor
	 * There are 3 domain: PF_INET (IPv4), PF_INET6 (IPv6), PF_UNIX (using a file)
	 * Type is one of these: SOCK_STREAM, SOCK_DGRAM, SOCK_SEQPACKET, SOCK_RAW
	 * 
	 */
	//public enum  Domain{PF_INET, PF_INET6, PF_UNIX}
	protected class Socket extends OpenFile{
		int port;
		int address;
		 
		// public Domain  domain = null;
		//create a socket at this port
		//when making a socket, we return a socket descriptor
		
		//Socket Descriptor: similar to a file descriptor, but linked to a socket instead of a file, 
		//can be used in low level commands such as read() and write()
		
		//dont think we need this
		
		//Socket(int domain, int type ){}
		
		//attempt to bind the socket to the selected port
		int bindSocket(int port){
			
			return -1;
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
		public int createConnection(int destHost, int destPort){
			
			//have to send a syn packet
			//check if sent
			//keep sending until either timeout is reached or connection 
			//if  received an ack, connection is established, return with a value saying connected
			//else return -1
			return -1;
		}
		//Try to accept the connection from the sender
		public int acceptConnection(){
			
			
			
			return -1;
		}

	}

	protected class SlidingWindow {
		int Size = 0; //Size of the buffer
		byte[] WindowBuffer = new byte[Size]; //Buffer size of the window

		SlidingWindow(){
		}
		//public class SlidingWindow(Packet HeaderPacket){
		//this.Size = HeaderPacket.maxPacketLength;


	}

}


/*Sliding Window is the method of flow control TCP uses. The algorithm basically places a buffer between the application and the network data flow. 
The purpose of sliding window is to prevent from the sender to send too many packets to over flow the network resource or the receiver's buffer.
The "sliding window size" is the maximum amount of data we can send without having to wait for ACK. TCP achieve the flow control by using the 
sliding Window algorithm which takes into consideration 2 important parameters. The first one is the receiver advertised window size which basically
tells the sender what is the current buffer of TCP receiver, the second parameter is congestion window which control the number of packets a TCP
flow may have in the network in any given time.
 */
