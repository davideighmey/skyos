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
	
	int toTransfer; // holds the word that needs to be transfered
	boolean toGet; // is there something to get?
	
	
    public Communicator() {
    	mutex = new Lock();
    	listenerArrived = new Condition(mutex);
    	speakArrived = new Condition(mutex);
    	
    	this.toGet = false; //initially there is nothing to get
    	
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
    public void speak(int word)
    {
    	this.mutex.acquire(); // get the lock
    	while(toGet == false) // while there is nothing to get
    	{
    		this.speakArrived.sleep(); // put the thread to sleep
    	}
    	this.toTransfer = word; // store the word to a global variable
    	toGet = false; // reset from true back to false
    	this.listenerArrived.wakeAll(); // wake all before releasing
    	this.mutex.release(); // now release the lock
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() 
    {
    	this.mutex.acquire(); // get the lock
    	while(toGet == true)
    	{
    		this.listenerArrived.sleep(); // put to sleep while waiting
    	}
    	this.toGet = false;
    	this.speakArrived.wake(); // wake all or just one ?
    	this.mutex.release(); // release lock before returning the word
    	
    	return (this.toTransfer);    	
    }

}
