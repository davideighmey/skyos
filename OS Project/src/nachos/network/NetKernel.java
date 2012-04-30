package nachos.network;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.network.*;


/**
 * A kernel with network support.
 */
public class NetKernel extends UserKernel {
        /**
         * Allocate a new networking kernel.
         */
        public NetKernel() {
                super();
        }

        /**
         * Initialize this kernel.
         */
        public void initialize(String[] args) {
                super.initialize(args);
                transport = new TransportLayer();
                //postOffice = new PostOffice();
        }

        /**
         * Test the network. Create a server thread that listens for pings on port
         * 1 and sends replies. Then ping one or two hosts. Note that this test
         * assumes that the network is reliable (i.e. that the network's
         * reliability is 1.0).
         */
        public void selfTest2() {
                //super.selfTest();

                KThread serverThread = new KThread(new Runnable() {
                        public void run() { pingServer(); }
                });

                serverThread.fork();
                
                System.out.println("Press any key to start the network test...");
                console.readByte(true);

                int local = Machine.networkLink().getLinkAddress();

                // ping this machine first
                ping(local);

                // if we're 0 or 1, ping the opposite
                if (local <= 1)
                        ping(1-local);
        }
        public void selfTest(){

                final KThread test2 = new KThread(new Runnable() {
                        public void run(){
                                socketTest2();
                        }
                });
                KThread test1 = new KThread(new Runnable() {
                        public void run(){
                                socketTest(test2);
                        }
                });
                
                test2.fork();
                test1.fork();
                
                

               // KThread.yield();

        }
        public void socketTest(KThread thr){
                
                Sockets scktSnd = new Sockets(1);
                
                // juan -- testing read and write
                Sockets readWrite_test = new Sockets(1);
                byte[] bt = new byte["Hi There".length()];
                String str = "Hi There";
                readWrite_test.write(str.getBytes(), 0, str.length());

                if(transport.createConnection(Machine.networkLink().getLinkAddress(), 2,scktSnd) == false){
                		System.out.println("Unable to connect");
                        return;
                }
                //byte[] bt = new byte["Hi There".length()];
               // String str = "Hi There";
//              System.out.println("Wrote "
        //                      + sckt  Snd.write(str.getBytes(), 0, str.length())
                //              + " bytes");
                thr.join();
                KThread.yield();
                scktSnd.close();
        }
        private void socketTest2(){
                Sockets scktRcv = new Sockets(2);
                transport.acceptConnection(scktRcv);
                byte[] bt = new byte["Hi There".length()];
                KThread.yield();
                timeout();
                timeout();
                System.out.println("read "
                                + scktRcv.read(bt, 0, bt.length)
                                + " bytes");
                System.out.println(new String(bt));
        }

        private void ping(int dstLink) {
                int srcLink = Machine.networkLink().getLinkAddress();

                System.out.println("PING " + dstLink + " from " + srcLink);

                long startTime = Machine.timer().getTime();

                TCPpackets ping;

                try {
                        //ping = new MailMessage(dstLink, 1,
                                //      Machine.networkLink().getLinkAddress(), 0,
                                        //new byte[0]);
                        ping =  new TCPpackets(dstLink,1,Machine.networkLink().getLinkAddress(),0, new byte[0],true,false,false,false,0);
                }
                catch (MalformedPacketException e) {
                        Lib.assertNotReached();
                        return;
                }

                transport.send(ping);

                TCPpackets ack = transport.receive(0);

                long endTime = Machine.timer().getTime();

                System.out.println("time=" + (endTime-startTime) + " ticks");   
        }

        private void pingServer() {
                while (true) {
                        //TCPpackets ping = postOffice.receive(1);
                        TCPpackets ping = transport.receive(1);
                        TCPpackets ack;

                        try {
                                //ack = new MailMessage(ping.packet.srcLink, ping.srcPort,
                                        //      ping.packet.dstLink, ping.dstPort,
                                                //ping.contents);
                                ack = new TCPpackets(ping.packet.srcLink,ping.srcPort,ping.packet.dstLink,ping.dstPort, new byte[0],false,true,false,false,0);
                        }
                        catch (MalformedPacketException e) {
                                // should never happen...
                                continue;
                        }

                        transport.send(ack);
                }       
        }

        /**
         * Start running user programs.
         */
        public void run() {
                super.run();
        }

        /**
         * Terminate this kernel. Never returns.
         */
        public void terminate() {
                super.terminate();
        }
        public static void timeout(){
                alarm.waitUntil(20000);
        }

        //private PostOffice postOffice;
        public static TransportLayer transport;
        // dummy variables to make javac smarter
        private static NetProcess dummy1 = null;
}

