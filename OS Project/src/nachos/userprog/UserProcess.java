package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	
	public UserProcess() {
		processID = UserKernel.getKernel().processManager.newProcess(this, -1);
		
		descriptorTable.add(0, UserKernel.console.openForReading());
		descriptorTable.add(1, UserKernel.console.openForWriting());
		//Don't Need
		/*int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i=0; i<numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i,i, true,false,false,false);*/
	}

	/**
	 * Allocate and return a new process of the correct class. The class name
	 * is specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return	a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param	name	the name of the file containing the executable.
	 * @param	args	the arguments to pass to the executable.
	 * @return	<tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		//new UThread(this).setName(name).fork();
		curThread = new UThread(this); // current thread
		curThread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read
	 * at most <tt>maxLength + 1</tt> bytes from the specified address, search
	 * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param	vaddr	the starting virtual address of the null-terminated
	 *			string.
	 * @param	maxLength	the maximum number of characters in the string,
	 *				not including the null terminator.
	 * @return	the string read, or <tt>null</tt> if no null terminator was
	 *		found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength+1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length=0; length<bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}
	/*@JAMES START*/
	/*
	 * Takes in a virtual address vaddr, which will than calculate the offset and virtual page number from the given address provided
	 * it will than calculate the physical address from the virtual page number and frame number, i.e. the physical page number
	 */
	public int vaddrTranslation(int vaddr){
		System.out.println("Starting to convert vaddr: " + Integer.toBinaryString(vaddr));
		if(pageTable == null){
			System.out.println("Page table has not been allocated");
			return -1;
		}
		//vPageNum will be the index of the page table. 
		int vPageNum = Processor.pageFromAddress(vaddr);
		int Offset = Processor.offsetFromAddress(vaddr);
		
		System.out.println("Offset is: " + Integer.toBinaryString(Offset));
		System.out.println("vPageNum is: " + Integer.toBinaryString(vPageNum));
		System.out.println("vPageNum in pageTable is: " + Integer.toBinaryString(pageTable[vPageNum].vpn));
		System.out.println("PPageNum is: " + Integer.toBinaryString(pageTable[vPageNum].ppn));
		System.out.println("Page Table Size is: " + pageTable.length + " Binary of it: " +Integer.toBinaryString(pageTable.length));
		System.out.println("Page Size is: " + Processor.pageSize);
		/*
		 * Check if the Virtual page number is on the page table, where the Page Table length is the max number of entries within that page table
		 */
		if(vPageNum >= pageTable.length || vPageNum < 0){
			System.out.println("Virtual Address may not be mapped" + Integer.toBinaryString(vPageNum) + "out of bounds");
			return -1;
		}
		
		/*Since each virtual address are being translated individually the virtual page number will be mapped to the page table as indexes on the
		page table which than will determine the frame number to determine the physical address.*/
		TranslationEntry pageTableIndex = pageTable[vPageNum]; 
		if(pageTableIndex == null){
			System.out.println("Can't be mapped to pageTable, vPageNum non-existent");
			return -1;
		}

		if(pageTableIndex.valid == false){
			System.out.println("Page Fault");
			System.out.println("Invalid - Not in physical memory");
			return -1; //Return error
		}
		
		int frameNum =  pageTableIndex.ppn;

		if(Offset < 0 || Offset >= Processor.pageSize){
			System.out.println("Offset greater than page size, has passed the bounds");
			return -1;
		}
		int paddr = Processor.makeAddress(frameNum, Offset);
		return paddr;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param	vaddr	the first byte of virtual memory to read.
	 * @param	data	the array where the data will be stored.
	 * @return	the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no
	 * data could be copied).
	 *
	 * @param	vaddr	the first byte of virtual memory to read.
	 * @param	data	the array where the data will be stored.
	 * @param	offset	the first byte to write in the array.
	 * @param	length	the number of bytes to transfer from virtual memory to
	 *			the array.
	 * @return	the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset,
			int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
		
		System.out.println("Starting Read Virtual Memory");
		//Get physical memory
		int paddr = vaddrTranslation(vaddr);
		if(paddr == -1){
			return 0;
		}
		
		System.out.println("paddr is: " + Integer.toBinaryString(paddr));
		int vpn = Processor.pageFromAddress(vaddr);
		
		byte[] memory = Machine.processor().getMemory();
		int amount = Math.min(length, memory.length-paddr);
		
		//Transfering and amount of data to physical main memory by starting at the physical address, than by offset. 
		 
		System.out.println("Physical Address: " + Integer.toBinaryString(paddr));
		
		System.arraycopy(memory, paddr, data, offset, amount);
		
		System.out.println("Setting vPageNum: " + Integer.toBinaryString(vpn)+ " to true.");
		pageTable[vpn].used = true;
		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory.
	 * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param	vaddr	the first byte of virtual memory to write.
	 * @param	data	the array containing the data to transfer.
	 * @return	the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no
	 * data could be copied).
	 *
	 * @param	vaddr	the first byte of virtual memory to write.
	 * @param	data	the array containing the data to transfer.
	 * @param	offset	the first byte to transfer from the array.
	 * @param	length	the number of bytes to transfer from the array to
	 *			virtual memory.
	 * @return	the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset,
			int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
		System.out.println("Starting Write Virtual Memory");

		int paddr = vaddrTranslation(vaddr);
		if(paddr == -1){
			return 0;
		}
		
		/*Need to write to the virtual memory now, so find the virtual page number for
		 * the correct index at which you are writing the data to.
		 */
		int vPageNum = Processor.pageFromAddress(vaddr);
		TranslationEntry pageIndex = pageTable[vPageNum];
		System.out.println("vPageNum: " + Integer.toBinaryString(vPageNum));
		if(pageIndex == null){
			return 0;
		}
		if(pageIndex.readOnly == true){
			System.out.println("Cannot write to current page table index, as it is read-only");
			//pageTableIndex.used = false; //As it was an unsuccessful write, used will be set to false.
			return 0; //Can not return error, instead will return zero bits
		}

		byte[] memory = Machine.processor().getMemory();
		
		if(paddr < 0 || paddr >= memory.length){
			return 0;
		}

		System.out.println("memory.length: " + memory.length + " Physical Addr " + paddr);
		System.out.println("Physical Address: " + Integer.toBinaryString(paddr));
		
		int amount = Math.min(length, memory.length-paddr);
		System.arraycopy(data, offset, memory, paddr, amount);
		
		pageIndex.dirty = true;
		pageIndex.used = true;
		
		System.out.println("Amount: " + amount);
		return amount;
	}
	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param	name	the name of the file containing the executable.
	 * @param	args	the arguments to pass to the executable.
	 * @return	<tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
		System.out.println("UserProcess.load(\"" + name + "\")");
		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s=0; s<coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i=0; i<args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += (4 + argv[i].length + 1);
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();	

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages*pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections()){
			return false;
		}

		// store arguments in last page
		int entryOffset = (numPages-1)*pageSize;
		int stringOffset = entryOffset + args.length*4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i=0; i<argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
					argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Will create a pageTable based on the need of the process requesting it.
	 * This will disable interrupts so that the page table can be properly 
	 * initialized.
	 */
	public void CreatePageTable(){
		boolean status = Machine.interrupt().disable();
		pageTable = new TranslationEntry[numPages];
		Machine.interrupt().restore(status);
	}
	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be
	 * run (this is the last step in process initialization that can fail).
	 *
	 * @return	<tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		} 
		CreatePageTable();
		//Will get a list of Free Pages available that is reserved for this process
		int[] ppnList = UserKernel.getFreePage(numPages);
		if(ppnList == null){
			System.out.println("Not enough free physical memory for this process");
			return false;
		}
		int vpn = 0;
		// load sections
		for (int s=0; s<coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
			
			for (int i=0; i<section.getLength(); i++) {
				vpn = section.getFirstVPN()+i;
				int ppn = ppnList[vpn];
				
				pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
				
				section.loadPage(i, ppn);
			}
		}
		//Since it has put all the required data into the page table.
		//continue where last left off, now assign remaining free pages to the process's stack
		vpn++;
		for(int i = 0; i <= this.stackPages; i++){
			int ppn = ppnList[vpn];
			pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
			vpn++;
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		/*
		 * Will go through the current page table and find all those that are being used an set them to false
		 * than free them by adding them to the free physical pages.
		 */
		System.out.println("Starting to unloadSections");
		for(int i = 0; i < pageTable.length; i++){
			if(pageTable[i].used == true){
				pageTable[i].used = false;
				//Do I need to resort them?
			}
			UserKernel.add(pageTable[i].ppn);

		}
		System.out.println("Finished Unloading Sections");
	}    





	/*JAMES ENDS*/  
 

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of
	 * the stack, set the A0 and A1 registers to argc and argv, respectively,
	 * and initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i=0; i<processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call. 
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
	private int handleCreate (int a0){
		//System.out.println("creating started");
		String filename = readVirtualMemoryString(a0,256);
		if (filename != null){
			OpenFile createdFile = UserKernel.fileSystem.open(filename, true);
			if (createdFile == null){
				System.out.println("creating the file was unsuccesful");
				return -1;}
			else 
				System.out.println("creating file");
			descriptorTable.add(createdFile);
			System.out.println("creating ended");
			return descriptorTable.indexOf(createdFile);}
		else
			System.out.println("creating the file was unsuccesful");
		return -1;

	}


	private int handleOpen (int a0){
		//	System.out.println("starting open");
		String filename =readVirtualMemoryString(a0, 256);  //address to 256
		if( filename != null){
			OpenFile openedFile = UserKernel.fileSystem.open(filename, false);
			if (openedFile == null){
				System.out.println("open unsuccesful");
				return -1;}
			else
				System.out.println("opening file");
			descriptorTable.add(openedFile);
			System.out.println("opened file succesfully");
			return descriptorTable.indexOf(openedFile);}
		else
			System.out.println("open unsuccesful");
		return -1;
	}

	private int handleRead(int a0, int a1, int a2){
		//	System.out.println("read");
		if(a0 >= 0 && a0 < descriptorTable.size() &&descriptorTable.get(a0) != null && a2 > 0){
			OpenFile file = descriptorTable.get(a0);
			byte[] data = new byte[a2];
			int bytesRead = file.read(data, 0, a2);
			if(bytesRead <0){
				return -1;
			}
			int bytesWritten = writeVirtualMemory(a1, data);
			if(bytesWritten < 0){
				return -1; }
			return bytesRead;}
		else
			return -1;

	}

	private int handleWrite(int a0, int a1, int a2){
		//  System.out.println("write");
		if(a0 >= 0 && a0 < descriptorTable.size() && descriptorTable.get(a0) != null && a2 > 0){
			OpenFile file = descriptorTable.get(a0);
			String data = readVirtualMemoryString(a1, 256);
			if(data == null){
				return -1;}
			else{
				int bytesWritten = file.write(data.getBytes(), 0, a2);
				if(bytesWritten < a2){
					return -1;
				}else{
					return bytesWritten;}
			}
		}
		else{
			return -1;
		}
	}

	private int handleClose(int a0){
		if(a0 < descriptorTable.size() && a0 >= 0 && descriptorTable.get(a0) != null){
			System.out.println("close succesful");
			descriptorTable.get(a0).close();
			descriptorTable.set(a0, null);
			return 0;
		}
		else{
			System.out.println("close unsuccesful");
			return -1;}
	}

	private int handleUnlink(int a0){
		String filename = readVirtualMemoryString(a0, 256);
		if(filename == null){
			System.out.println("unlink unsuccesful");
			return -1;
		}else{
			if(!UserKernel.fileSystem.remove(filename)){
				System.out.println("unlink unsuccesful");
				return -1;
			}else{
				System.out.println("unlink succesful");
				return 0;
			}
		}
	}


	private static final int
	syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;
	
	/** Juan */
	protected int processID; // removed static
	protected int status; 
	List<UserProcess> parents;
	List<UserProcess> children = new ArrayList<UserProcess>();
	final int int_size = 4; // size of an integer in bytes
	protected UThread curThread;
	protected int maxFiles = 16;

	//int exec(char *fileName, int argc, char *argv[])
	/**
	 *  juan
	 * @param filename
	 * @param argc -- argument count, the number of arguments that will be given to the new process.
	 * @param argvAddr -- the arguments virtual address
	 * @return returns -1 if there is a problem otherwise returns the new process's ID if everything went ok
	 */
	int handleExec(String filename, int argc, int argvAddr)
	{
		// need to create the new process
		UserProcess new_process = newUserProcess();

		UserKernel.getKernel().processManager.changeParent(new_process.processID, processID); // change parent id

		// get argument information
		String[] arguments = new String[argc]; // make an array to hold the arguments needed
		byte[] arguAddr = new byte[argc*int_size];
		//int bytes_transferred = 
		readVirtualMemory(argvAddr,arguAddr); // need to call readVirtualMemroy to transfer, might not need the number returned though
		for(int i=0; i<argc; i++) // fill in the arguments array
		{
			arguments[i] = readVirtualMemoryString(Lib.bytesToInt(arguAddr,  i*int_size), 256); // int_size  = 4 
		}
		if(new_process.execute(filename,arguments) == false) // execute
		{
			UserKernel.getKernel().processManager.setFinished(new_process.processID);
			UserKernel.getKernel().processManager.setError(processID);
			UserKernel.getKernel().processManager.setReturn(processID, -1);
			return -1;    		
		}

		return new_process.processID; // return the process id
	}

	//int join(int processID, int *status)
	/**
	 *  juan
	 * @param pID
	 * @param stat
	 * @return
	 */
	int handleJoin(int childID, int stat)
	{
		// check for a valid child id
		if(UserKernel.getKernel().processManager.exists(childID) == false)
			return -1;

		// only parent can join the child process. check
		if(UserKernel.getKernel().processManager.getParent(childID) != processID)
			return -1;

		// if child running then join
		if(UserKernel.getKernel().processManager.isRunning(childID))
			UserKernel.getKernel().processManager.getProcess(childID).curThread.join();

		// check for return error
		if(UserKernel.getKernel().processManager.checkError(childID))
			return 0;

		// keep parent from calling join a second time on same child
		UserKernel.getKernel().processManager.changeParent(childID, -1);

		int status  = UserKernel.getKernel().processManager.getReturn(childID);
		//byte[] ret_addr = new byte[int_size];
		//readVirtualMemory(stat, ret_addr);
		//writeVirtualMemory(Lib.bytesToInt(ret_addr, 0),Lib.bytesFromInt(stat));
		writeVirtualMemory(stat,Lib.bytesFromInt(status));

		return 1;
	}

	/**
	 * juan
	 * @param status
	 * @return
	 */
	int handleExit(int status)
	{
		unloadSections();

		// check to see if there are any children
		// if so then remove parent
		UserKernel.getKernel().processManager.removeParent(processID);

		//
		/**
    	// close open files
    	for(int i = 0; i < maxFiles; i++)
    	{
    		if(openFileTable[i] != null)
    			handleClose(i);
    	}
		 */
		System.out.println("Going to close all the open files");
		//descriptorTable.clear();
		for(int i = 0; i < descriptorTable.size(); i++)
		{
			if(descriptorTable.get(i) != null)
				descriptorTable.get(i).close();
		}
		
		// exit status
		UserKernel.getKernel().processManager.setReturn(processID, status);

		// set process is finished/it has exited
		UserKernel.getKernel().processManager.setFinished(processID);

		// check if this is the last process
		// if so then stop machine
		if(UserKernel.getKernel().processManager.isLast(processID))
			Kernel.kernel.terminate();
		//else
		KThread.currentThread().finish();

		return 0;

	}

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr><td>syscall#</td><td>syscall prototype</td></tr>
	 * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
	 * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
	 * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td></tr>
	 * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
	 * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
	 * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
	 * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
	 * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
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
		case syscallHalt:
			return handleHalt();
		case syscallOpen:
			return handleOpen(a0);
		case syscallCreate:
			return handleCreate(a0);
		case syscallRead:
			return handleRead(a0,a1,a2);
		case syscallWrite:
			return handleWrite(a0,a1,a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case syscallExec: // juan
			return handleExec(readVirtualMemoryString(a0,256),a1,a2);
		case syscallJoin: // juan
			return handleJoin(a0,a1);
		case syscallExit: // juan
			return handleExit(a0);



		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}
	/**
	 * Handle a user exception. Called by
	 * <tt>UserKernel.exceptionHandler()</tt>. The
	 * <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param	cause	the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3)
					);
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;				       

		default:
			Lib.debug(dbgProcess, "Unexpected exception: " +
					Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	private ArrayList<OpenFile> descriptorTable = new ArrayList<OpenFile>(16);

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;
	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
}
