package nachos.threads;

import nachos.machine.*;
import java.util.Queue; // for sleeping threads

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
	private Lock mutex;
	Condition listenerArrived;
	Condition speakArrived;
	
	private int toTransfer; // global variable to transfer word from speak to listen
	Queue<KThread> asleep; // queue to hold the sleeping threads
	boolean lockSet = false;// has the lock been set by speak?
	
    public Communicator() {
    	mutex = new Lock();
    	listenerArrived = new Condition(mutex);
    	speakArrived = new Condition(mutex);
    	
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	// how to know from which thread to check lock
    	if(!mutex.isHeldByCurrentThread()) // if thread does not already have the lock
    		mutex.acquire(); // get the lock
    	
    	this.lockSet = true; // signal that speak has been called
    	this.toTransfer = word; // set global variable = word
    	
    	this.speakArrived.notify();// send signal to wake listening thread if it is asleep
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	if(this.lockSet) // thread has lock so speak has been called
    	{
    		mutex.release(); // release the lock before we return the word
    		this.lockSet = false; // reset for next threads 
    		System.out.print("listen called");
    		return (this.toTransfer);    		
    	}
    	else	// lock not set yet, have to wait for speak to be called
    	{
    			this.listenerArrived.sleep();// add thread to asleep queue
    		
    	}
	return 0;
    }

}
