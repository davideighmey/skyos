package nachos.network;

import java.io.FileDescriptor;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;


/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public NetProcess() {
		super();
	}

	private static final int
	syscallConnect = 11,
	syscallAccept = 12;

	private int putOntoFileDiscriptorTable(OpenFile file){
		for(int i =0; i < descriptorTable.size(); i++){
			if(descriptorTable.get(i) == null){
				descriptorTable.add(file);
				return i;
			}
		}
		return -1;
	}

		/*
		 * Depending on which socket you want to use,
		 */
	TransportLayer TCP = new TransportLayer();
	
	private int connect(int destID, int destPort){
		int port = TCP.findPort();
		if(port == -1){
			return -1;
		}
		//attempt to create new socket on the port
		Sockets socket = new Sockets(port);
		if(socket.hostID != Machine.networkLink().getLinkAddress()){
			return -1;
		}

		int RegSocket = putOntoFileDiscriptorTable(socket);
		//attempt to create a connection with the socket. 
		TCP.createConnection(destID, destPort, socket);
		//if any errors return -1
		return RegSocket;
		//if successful, grab the socket descriptor and return it 
	}

	private int accept(int port){
		Sockets socket = new Sockets(port);
		//attempt to create new socket on the port
		//attempt to accept the connection with teh socket
		//if any errors returns -1
		return -1; //Return -1 for now.

		//if successful, grab the socket descriptor and return it
	}

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr><td>syscall#</td><td>syscall prototype</td></tr>
	 * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
	 * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
	 * </table>
	 * 
	 * @param	syscall	the syscall number.
	 * @param	a0	the first syscall argument.
	 * @param	a1	the second syscall argument.
	 * @param	a2	the third syscall argument.
	 * @param	a3	the fourth syscall argument.
	 * @return	the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case(syscallConnect): return connect(a0, a1); //a0 is host number, a1 is port number
		case(syscallAccept): return accept(a1);
		default:
			return super.handleSyscall(syscall, a0, a1, a2, a3);
		}
	}
}
