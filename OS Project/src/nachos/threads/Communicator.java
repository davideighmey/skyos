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
	//boolean toGet; // is there something to get?
	boolean speaker; // is there a speaker?
	boolean listener;// is there a listener?
	
	
    public Communicator() {
    	mutex = new Lock();
    	listenerArrived = new Condition(mutex);
    	speakArrived = new Condition(mutex);
    	
    	//this.toGet = false; // initially there is nothing to get
    	//this.toTransfer = 0;// 
    	this.speaker = false; // start off with no speaker
    	this.listener = false;// start off with no listener
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
    	this.speaker = true;
    	while(listener == false) // while there is nothing to get
    	{
    		this.speakArrived.sleep(); // put the thread to sleep
    	}
    	this.toTransfer = word; // store the word to a global variable
    	//toGet = false; // reset from true back to false
    	this.listenerArrived.wakeAll(); // wake all before releasing
    	//this.speaker = false; // reset to no speaker
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
    	this.listener = true;
    	while(speaker == false)
    	{
    		this.listenerArrived.sleep(); // put to sleep while waiting
    	}
    	//this.toGet = false;
    	this.speakArrived.wakeAll(); // wake all or just one ?
    	//this.listener = false; // reset to no listener
    	this.mutex.release(); // release lock before returning the word
    	
    	return (this.toTransfer);    	
    }

}
