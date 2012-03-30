package nachos.userprog;

import java.util.LinkedList;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
//j
import java.util.TreeMap;
import java.util.Iterator;
//

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		console = new SynchConsole(Machine.console());

		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() { exceptionHandler(); }
		});
		/*JAMES START #####*/
		freePhysicalPages = new LinkedList<Integer>();
		pLock = new Lock();
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++){
			freePhysicalPages.add(i);
		}
		/*James END #####*/
		
	}
	/*
	 * When accessing the free memory list, make sure that all processes are able
	 * to know what is the current number of free pages without being interrupted. 
	 */
	/*public void InitializeMem(){
		//Will create a list of all available pages from the physical memory
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++){
			freePhysicalPages.add(i);
		}
	}*/
	/*
	 * JAMES START################################
	 */
	/**
	 * Will return a list of free pages from the physical memory by requesting the number of pages
	 * required by the process that is requesting it
	 * @return a list of free page or returns null, indicating no free pages or not enough
	 */
	public static int[] getFreePage(int PageRequest){
		boolean status = Machine.interrupt().disable();
		//Check if there are free pages
		if(freePhysicalPages.size() < PageRequest){
			Machine.interrupt().restore(status);
			return null;
		}
		int[] page = new int[PageRequest];
		for(int i = 0; i < page.length; i++){
			page[i] = freePhysicalPages.removeLast();
		}
		Machine.interrupt().restore(status);
		return page;
	}
	public static int getFreePage(){
		boolean status = Machine.interrupt().disable();
		int page = -1;
		if(freePhysicalPages.size() == 0){
			Machine.interrupt().setStatus(status);
			return -1;
		}
		page = freePhysicalPages.removeLast();
		Machine.interrupt().setStatus(status);
		return page;
	}
	/**
	 * Will Get the current number of avaliable free pages
	 * @return the size of the list (The number of free pages)
	 */
	public static int getNumPages(){
		boolean status = Machine.interrupt().disable();
		int size = freePhysicalPages.size();
		Machine.interrupt().restore(status);
		return size;
	}
	/**
	 * will free the page table's unused physical pages.
	 * @param pageNum
	 */
	public static void add(int pageNum){
		boolean status = Machine.interrupt().disable();
		if(!freePhysicalPages.contains(pageNum)){
			freePhysicalPages.add(pageNum);
		}
		Machine.interrupt().restore(status);
	}
	
	public static Lock getPLock(){
		boolean status = Machine.interrupt().disable();
		if(pLock == null){
			pLock = new Lock();
		}
		Machine.interrupt().restore(status);
		return pLock;
	}
	/*
	 * JAMES ENDS###########################
	 */

	/**
	 * Test the console device.
	 */	
	 public void selfTest() {
		 super.selfTest();
		 
		 //juan
	processManager = new ProcessManager();
	//
		 
		 
		 System.out.println("Testing the console device. Typed characters");
		 System.out.println("will be echoed until q is typed.");

		 char c;

		 do {
			 c = (char) console.readByte(true);
			 console.writeByte(c);
		 }
		 while (c != 'q');

		 System.out.println("");
	 }

	 /**
	  * Returns the current process.
	  *
	  * @return	the current process, or <tt>null</tt> if no process is current.
	  */
	 public static UserProcess currentProcess() {
		 if (!(KThread.currentThread() instanceof UThread))
			 return null;

		 return ((UThread) KThread.currentThread()).process;
	 }

	 /**
	  * The exception handler. This handler is called by the processor whenever
	  * a user instruction causes a processor exception.
	  *
	  * <p>
	  * When the exception handler is invoked, interrupts are enabled, and the
	  * processor's cause register contains an integer identifying the cause of
	  * the exception (see the <tt>exceptionZZZ</tt> constants in the
	  * <tt>Processor</tt> class). If the exception involves a bad virtual
	  * address (e.g. page fault, TLB miss, read-only, bus error, or address
	  * error), the processor's BadVAddr register identifies the virtual address
	  * that caused the exception.
	  */
	 public void exceptionHandler() {
		 Lib.assertTrue(KThread.currentThread() instanceof UThread);

		 UserProcess process = ((UThread) KThread.currentThread()).process;
		 int cause = Machine.processor().readRegister(Processor.regCause);
		 process.handleException(cause);
	 }

	 /**
	  * Start running user programs, by creating a process and running a shell
	  * program in it. The name of the shell program it must run is returned by
	  * <tt>Machine.getShellProgramName()</tt>.
	  *
	  * @see	nachos.machine.Machine#getShellProgramName
	  */
	 public void run() {
		 super.run();

		 UserProcess process = UserProcess.newUserProcess();

		 String shellProgram = Machine.getShellProgramName();	
		 Lib.assertTrue(process.execute(shellProgram, new String[] { }));

		 KThread.currentThread().finish();
	 }

	 /**
	  * Terminate this kernel. Never returns.
	  */
	 public void terminate() {
		 super.terminate();
	 }

//juan
    /**
     *Class to help manage all the process that might be created and the variables
     *and info that must be kept for each of those process
     */
    public class ProcessManager
    {
    	protected int nextProcessID = 0; // where to give the next process id from
    	protected TreeMap<Integer, ProcessNode> processList;
    	
    	public ProcessManager()
    	{
    		processList = new TreeMap<Integer, ProcessNode>();
    	}
    	
    	/**
    	 * Called when a new process has been created
    	 */
    	public int newProcess(UserProcess process, int parent) 
    	{
    		//System.out.println("---New process is being created---");
    		ProcessNode newProcessNode = new ProcessNode(process, parent, nextProcessID); // 
    		processList.put(newProcessNode.pid, newProcessNode);
    		nextProcessID++; // increase counter for next process id
    		return newProcessNode.pid; // return the id of the new process
    	}
    	
    	/**
    	 * to check if a the given process id belongs to an valid process
    	 */
    	public boolean exists(int pid) // pid = process id
    	{
    		boolean valid = false;
    		valid = processList.containsKey(pid); // is the pid for a valid id? if so true
    		return valid;
    	}
    	
    	/**
    	 * gets the process for the given process id
    	 */
    	public UserProcess getProcess(int pid)
    	{
    		return processList.get(pid).process; // return the UserProcess
    	}
    	
    	/**
    	 * Checks to see if the given process id has any children
    	 * returns true if children exist
    	 * false if there are no children for this process id
    	 * checks to see if the nodes have a parent ide that matches the given process id
    	 */
    	public boolean checkChildren(int parentID)
    	{
    		//boolean children = false;
    		Iterator it = processList.keySet().iterator(); // iterator to go through the tree map
    		ProcessNode pNode;
    		while(it.hasNext()) // while there this is not the last
    		{
    			pNode = processList.get(it.next());
    			if(pNode.parent == parentID)
    				return false;
    				//children = false
    		}
    		//return children;
    		return true;
    	}
    	
    	/**
    	 *returns the process id for the given child process id
    	 */
    	public int getParent(int childID)
    	{
    		return processList.get(childID).parent; // 
    	}
    	
    	// removes the parent process
    	public void removeParent(int parentID)
    	{
    		Iterator it = processList.keySet().iterator();
    		ProcessNode pNode;
    		while(it.hasNext())
    		{
    			pNode = processList.get(it.next());
    			if(pNode.parent == parentID)
    				changeParent(pNode.pid, -1);
    		}
    	}
    	
    	// changes parent
    	public void changeParent(int childID, int parentID)
    	{
    		processList.get(childID).parent = parentID;
    	}
    	
    	// set exit status of process
    	public void setReturn(int pid, int rVal) // process id and return value
    	{
    		processList.get(pid).exitStatus = rVal;
    	}
    	
    	// get the return status of a process
    	// will be used for a parent to get the 
    	// exit status of its child process
    	public int getReturn(int pid)
    	{
    		ProcessNode child = processList.get(pid);
    		return child.exitStatus;
    	}
    	
    	// process has finished
    	public void setFinished(int pid)
    	{
    		processList.get(pid).running = false;
    	}
    	
    	// checks to see if a process is currently running
    	public boolean isRunning(int pid)
    	{
    		return processList.get(pid).running;
    	}
    	
    	// called if error on exit
    	public void setError(int pid)
    	{
    		processList.get(pid).error = true;
    	}
    	
    	// check for errors on exit
    	public boolean checkError(int pid)
    	{
    		return processList.get(pid).error; // will be true if exit was result of error
    	}
    	
    	// check to see if the process id given belongs
    	// to the last process being run
    	public boolean isLast(int pid)
    	{
    		Iterator it  = processList.keySet().iterator();
    		ProcessNode pNode;
    		int count = 0;
    		while(it.hasNext())
    		{
    			pNode = processList.get(it.next());
    			if(pNode.pid != pid && pNode.running)
    				count++;
    		}
    		
    		if(count == 0) // is the last process
    			return true;
    		else
    			return false;   // is not the last process
    	}
    	    	
    	
    	public class ProcessNode
    	{
    		int pid;
    		int parent;
    		int exitStatus;
    		//boolean joined;
    		boolean running;
    		boolean error;
    		UserProcess process;
    		
    		public ProcessNode(UserProcess process, int parent, int pid)
    		{
    			this.pid = pid; // get and set the process id
    			this.parent = parent; // get & set the parent
    			this.process = process; // 
    			this.running = true; // the process is being run
    			//this.joined = false; //
    			this.error = false; // is there an error?
    			
    			//this.exitStatus =  // what should this be initialized as?
    		}
    		
    	}
    }// end of ProcessManager class

//juan
    public ProcessManager processManager;
    public static UserKernel getKernel()
    {
    	if(kernel instanceof UserKernel)
    		return (UserKernel)kernel;
    	else
    		return null;
    }
    //

	 /** Globally accessible reference to the synchronized console. */
	 public static SynchConsole console;

	 // dummy variables to make javac smarter
	 private static Coff dummy1 = null;
	 //Synchronization!!! Needed Locks
	 /*JAMES START #####*/
	 private static Lock pLock = null;
	 private static LinkedList<Integer> freePhysicalPages;
	 /*JAMES END #####*/
}
