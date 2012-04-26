package nachos.network;

import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;

public class TCPpackets {
	public TCPpackets(int _dstLink, int _dstPort, int _srcLink, int _srcPort,
			byte[] _contents) throws MalformedPacketException {



		// TODO Auto-generated constructor stub
	}
	//Total 64 bits available for header. 40 in use.
	/***************************************************************
	 * |8 bit dest port|8 bit host port|4 bit SYN|4 bit ACK| 
	 * |4 bit DATA| 4 bit FIN|8 bit window size|8 bit packetID
	 */

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
	 * @param	_data			the flag for DATA packet
	 * @param	_fin			the flag for FIN packet
	 * @param 	_packetID		the id of the packet
	 * @param	_adwm			the advertisement window size
	 */

	public TCPpackets(int _dstLink, int _dstPort, int _srcLink, int _srcPort,
			byte[] _contents, boolean _syn, boolean _ack, boolean _data, boolean _fin, int _packetID, int _adwn) throws MalformedPacketException {

		if (_dstPort < 0 || _dstPort >= portLimit ||
				_srcPort < 0 || _srcPort >= portLimit ||
				_contents.length > maxContentsLength||
				_packetID < 0||_adwn < 0 ||_adwn > 16)
			throw new MalformedPacketException();
		this.dstPort = (byte) _dstPort;
		this.srcPort = (byte) _srcPort;
		this.syn = _syn;
		this.ack = _ack;
		this.data = _data;
		this.fin = _fin;
		this.adwn = _adwn;
		this.packetID = _packetID;
		this.contents = _contents;
		byte[] packetContents = new byte[headerLength + contents.length];
		//
		packetContents[0] = (byte) dstPort;
		packetContents[1] = (byte) srcPort;
		packetContents[2] = 0;
		if(syn)
			packetContents[2] = (byte) (packetContents[2]^0x1);
		if(ack){
			packetContents[2] = (byte) (packetContents[2]^0x2);
			packetContents[4] = (byte) adwn;
		}
		if(data)
			packetContents[3] = (byte) (packetContents[2]^0x4);

		if(fin)
			packetContents[3] = (byte) (packetContents[2]^0x8);
		packetContents[3] = (byte) packetID;

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
				packet.contents[3] < 0||packet.contents[4] < 0 ||packet.contents[4]> 16)
			throw new MalformedPacketException();

		dstPort = packet.contents[0];
		srcPort = packet.contents[1];
		packetID  = packet.contents[3];
		syn = ((packet.contents[2] & 0x1) == 0x1);
		fin = ((packet.contents[2] & 0x2) == 0x2);
		ack = ((packet.contents[2] & 0x4) == 0x4);
		if(ack)
			adwn = packet.contents[4];

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
	/** The contents of this message, excluding the mail message header. */
	public byte[] contents;

	/**
	 * The number of bytes in a mail header. The header is formatted as
	 * follows:
	 *
	 * <table>
	 * <tr><td>offset</td><td>size</td><td>value</td></tr>
	 * <tr><td>0</td><td>1</td><td>destination port</td></tr>
	 * <tr><td>1</td><td>1</td><td>source port</td></tr>
	 * 	</table>
	 */
	public static final int headerLength = 2;

	/** Maximum payload (real data) that can be included in a single mesage. */
	public static final int maxContentsLength =
			Packet.maxContentsLength - headerLength;

	/**
	 * The upper limit on mail ports. All ports fall between <tt>0</tt> and
	 * <tt>portLimit - 1</tt>.
	 */    
	public static final int portLimit = 128;
	//The id of the packet
	public int packetID;
	//The advertisement window size
	public int adwn;
	//flags for packets that will be placed in header
	public boolean syn;
	public boolean ack;
	public boolean data;
	public boolean fin;

}
