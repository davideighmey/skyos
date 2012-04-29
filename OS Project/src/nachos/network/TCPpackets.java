package nachos.network;

import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;

public class TCPpackets {
	
	public TCPpackets(int _dstLink, int _dstPort, int _srcLink, int _srcPort,
			byte[] _contents) throws MalformedPacketException {



		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Allocate a new packet message to be sent, using the specified parameters.
	 *
	 * @param	_dstLink		the destination link address.
	 * @param	_dstPort		the destination port.
	 * @param	_srcLink		the source link address.
	 * @param	_srcPort		the source port.
	 * @param	_contents		the contents of the packet.
	 * @param 	_syn			the flag for SYN packet
	 * @param	_ack			the flag for ACK packet
	 * @param	_stp			the flag for STP packet
	 * @param	_fin			the flag for FIN packet
	 * @param 	_packetID		the id of the packet
	 */

	public TCPpackets(int _dstLink, int _dstPort, int _srcLink, int _srcPort,
			byte[] _contents, boolean _syn, boolean _ack, boolean _stp, boolean _fin, int _packetID) throws MalformedPacketException {

		if (_dstPort < 0 || _dstPort >= portLimit ||
				_srcPort < 0 || _srcPort >= portLimit ||
				_contents.length > maxContentsLength||
				_packetID < 0)
			throw new MalformedPacketException();
		this.dstPort = (byte) _dstPort;
		this.srcPort = (byte) _srcPort;
		this.syn = _syn;
		this.ack = _ack;
		this.fin = _fin;
		this.stp = _stp;
		this.packetID = _packetID;
		this.contents = _contents;

		byte[] packetContents = new byte[headerLength + contents.length];
		
		packetContents[0] = (byte) dstPort;
		packetContents[1] = (byte) srcPort;
		//byte 2 and 3 is MBZ
		packetContents[2] = 0;
		packetContents[3] = 0;
		//packets flags
		if(syn)
			packetContents[3] = (byte) (packetContents[2]^0x1);
		if(ack)
			packetContents[3] = (byte) (packetContents[2]^0x2);
		if(stp)
			packetContents[3] = (byte) (packetContents[2]^0x4);
		if(fin)
			packetContents[3] = (byte) (packetContents[2]^0x8);
		
		//storing the packetID
		packetContents[4] = (byte) ((packetID >> 24) & 0xFF);
		packetContents[5] = (byte) ((packetID >> 16) & 0xFF);
		packetContents[6] = (byte) ((packetID >> 8) & 0xFF);
		packetContents[7] = (byte) (packetID & 0xFF);
		
		System.arraycopy(contents, 0, packetContents, headerLength,
				contents.length);
		
		packet = new Packet(_dstLink, _srcLink, packetContents);

	}
	public String toString() {
		return "from (" + packet.srcLink + ":" + srcPort +
				") to (" + packet.dstLink + ":" + dstPort +
				"), " + contents.length + " bytes";
	}
	public TCPpackets(Packet packet) throws MalformedPacketException {
		this.packet = packet;

		// make sure we have a valid header
		if (packet.contents.length < headerLength ||
				packet.contents[0] < 0 || packet.contents[0] >= portLimit ||
				packet.contents[1] < 0 || packet.contents[1] >= portLimit||
				packet.contents[3] < 0||packet.contents[4] < 0 ||packet.contents[4]> 16||
				packet.contents[5]<0)
			throw new MalformedPacketException();
		
		//grab the dst and src port
		dstPort = packet.contents[0];
		srcPort = packet.contents[1];
		
		//grab the flags
		syn = ((packet.contents[3] & 0x1) == 0x1);
		ack = ((packet.contents[3] & 0x2) == 0x2);
		stp = ((packet.contents[3] & 0x3) == 0x3);
		fin = ((packet.contents[3] & 0x4) == 0x4);
		
		//grab the packet id
		packetID = (packet.contents[4] << 24) + (packet.contents[5]<< 16) + (packet.contents[4] << 8) + (packet.contents[7]);

		contents = new byte[packet.contents.length - headerLength];
		System.arraycopy(packet.contents, headerLength, contents, 0,
				contents.length);
	}

	/** This message, as a packet that can be sent through a network link. */
	public Packet packet;
	/** The port used by this message on the destination machine. */
	public int dstPort;
	/** The port used by this message on the source machine. */
	public int srcPort;
	/** The contents of this message, excluding the TCP message header. */
	public byte[] contents;

	/**
	 * The number of bytes in a TCP header. The header is formatted as
	 * follows:
	 *
	 * <table>
	 * <tr><td>offset</td><td>size</td><td>value</td></tr>
	 * <tr><td>0</td><td>1</td><td>destination port</td></tr>
	 * <tr><td>1</td><td>1</td><td>source port</td></tr>
	 * <tr><td>3</td><td>1</td><td>Must Be Zero</td></tr>
	 * <tr><td>4</td><td>1</td><td>Must Be Zero with flags</td></tr>
	 * <tr><td>5</td><td>1</td><td>Packet ID</td></tr>
	 * <tr><td>6</td><td>1</td><td>Packet ID</td></tr>
	 * <tr><td>7</td><td>1</td><td>Packet ID</td></tr>
	 * 	</table>
	 */
	public static final int headerLength = 8;

	/** Maximum payload (real data) that can be included in a single message. */
	public static final int maxContentsLength =	Packet.maxContentsLength - headerLength;

	/**
	 * The upper limit on TCP ports. All ports fall between <tt>0</tt> and
	 * <tt>portLimit - 1</tt>.
	 */    
	public static final int portLimit = 128;
	//The id of the packet
	public int packetID;
	//flags for packets that will be placed in header
	public boolean syn;
	public boolean ack;
	public boolean fin;
	public boolean stp;

}
