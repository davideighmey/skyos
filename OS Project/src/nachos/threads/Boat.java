package nachos.threads;
import java.util.LinkedList;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat
{
	static BoatGrader bg;

	private static Lock lock = new Lock(); 	//declare lock
	private static boolean Loner = false;	//There will always be a loner, during school years. Kids...
	//private static Lock boatLock = new Lock(); //Boat Lock

	//private static Condition2 boat = new Condition2(boatLock); //The boat, will I need?
	private static Condition2 Ad = new Condition2(lock); 
	private static Condition2 Cd = new Condition2(lock);

	private static int COnOahu; //total children on Oahu
	private static int AOnOahu; //total adults on Oahu
	private static int COnMolokai; //total children on Molokai
	private static int AOnMolokai; //total adults on Molokai


/**
	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		//System.out.println("\n ***Testing Boats with only 2 children***");
		//begin(0, 2, b);

			System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		  	begin(1, 2, b);

		//System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//begin(3, 3, b);
	}
*/
	public static Runnable runChild = new Runnable(){
		public void run(){
			ChildItinerary();
		}
	};

	public static Runnable PedoAdult = new Runnable(){
		public void run(){
			AdultItinerary();
		}
	};

	public static LinkedList<KThread> ChildrenOnOahu = new LinkedList<KThread>();
	public static LinkedList<KThread> ChildrenOnMolokai = new LinkedList<KThread>();

	public static LinkedList<KThread> AdultsOnOahu = new LinkedList<KThread>();
	public static LinkedList<KThread> AdultsOnMolokai = new LinkedList<KThread>();

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;
		// Instantiate global variables here
		//This is the boat of passings.
		//starts everything off at 0
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		COnOahu = children;
		AOnOahu = adults;
		lock.acquire();		//get the lock
		
		for(int i = 0; i < children; i++){
			KThread child = new KThread(runChild);
			child.setName("Child " + i); 
			ChildrenOnOahu.add(child);
			System.out.println("Created " + ChildrenOnOahu.get(i).getName());
			child.fork();
		}
		for(int i = 0; i < adults; i++){
			KThread adult = new KThread(PedoAdult);
			adult.setName("Adult " + i);
			AdultsOnOahu.add(adult);
			System.out.println("Created " + AdultsOnOahu.get(i).getName());
			adult.fork();
		}
		lock.release();
		//while(true)
		while(!ChildrenOnOahu.isEmpty())
		{
			ChildrenOnOahu.getFirst().join();
		}
		
	}

	/*
	Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();
	 */

	static void AdultItinerary()
	{
		/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
		 */
	//	if(AOnOahu != AdultsOnOahu.size()-1){
		//	Machine.interrupt().disable();
			//Ad.sleep();			//Assumed will put all adults to sleep as it is called.
			//System.out.println("Putting adult to sleep: " + AdultsOnOahu.getFirst().getName());
			//Machine.interrupt().enable();
	//	}
		//else{
			while(!AdultsOnOahu.isEmpty()){
				//Adult to Molokai
				bg.AdultRowToMolokai();
				System.out.println("The Adult traveled to Molokai: " + AdultsOnOahu.getFirst().getName());
				AdultsOnMolokai.add(AdultsOnOahu.getFirst());
				AdultsOnOahu.removeFirst(); //Remove Adult from Oahu to Molokai

				//Child back to Oahu
				bg.ChildRowToOahu();	
				System.out.println("A Child traveling back to Oahu: " + ChildrenOnMolokai.getFirst().getName());
				ChildrenOnOahu.add(ChildrenOnMolokai.getFirst()); 
				ChildrenOnMolokai.removeFirst(); //Remove Child from Molokai to Oahu

				//Both Child back to Molokai
				bg.ChildRowToMolokai();
				bg.ChildRideToMolokai();
				System.out.print("Two Children traveling to Molokai: " + ChildrenOnOahu.getFirst().getName());
				ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
				ChildrenOnOahu.removeFirst();	//Remove First Child from Oahu to Molokai
				System.out.println(ChildrenOnOahu.getFirst().getName());
				ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
				ChildrenOnOahu.removeFirst(); //Remove Second Child from Oahu to Molokai

				//Child back to Oahu
				if(!AdultsOnOahu.isEmpty()){
					
				
				bg.ChildRowToOahu();	
				System.out.println("A Child traveling back to Oahu: " + ChildrenOnMolokai.getFirst().getName());
				ChildrenOnOahu.add(ChildrenOnMolokai.getFirst()); 
				ChildrenOnMolokai.removeFirst(); //Remove Child from Molokai to Oahu
				}
			}
		//}
	}

	static void ChildItinerary()
	{

		//Machine.interrupt().disable();
		//Cd.sleep();		//Assumed will put all children to sleep as it is called.
		//System.out.println("Putting child to sleep: " + ChildrenOnOahu.getFirst().getName());
		//Machine.interrupt().enable();
		//if(COnOahu == ChildrenOnOahu.size()-1){
			lock.acquire();
			while(!ChildrenOnOahu.isEmpty()){
				if(ChildrenOnOahu.size() == 1){
					Loner = true;
				}
				if(Loner == false){		//Check if there is a lonely kid
					//Both Child to Molokai
					bg.ChildRowToMolokai();
					bg.ChildRideToMolokai();
					System.out.print("Two Children traveling to Molokai: " + ChildrenOnOahu.getFirst().getName());
					ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
					ChildrenOnOahu.removeFirst();	//Remove First Child from Oahu to Molokai
					System.out.println(ChildrenOnOahu.getFirst().getName());
					ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
					ChildrenOnOahu.removeFirst(); //Remove Second Child from Oahu to Molokai

					//Child back to Oahu
					if(!AdultsOnOahu.isEmpty()){
					bg.ChildRowToOahu();	
					System.out.println("A Child traveling back to Oahu: test " + ChildrenOnMolokai.getFirst().getName());
					ChildrenOnOahu.add(ChildrenOnMolokai.getFirst()); 
					ChildrenOnMolokai.removeFirst(); //Remove Child from Molokai to Oahu
					}
				}
				else{
					System.out.println("Last kid on Oahu, became the loner--- calling adultItinerary()");
					if(!AdultsOnOahu.isEmpty())
					AdultItinerary();

					//Last child sent to Molokai
					if(!ChildrenOnOahu.isEmpty())
					{
					System.out.println("Last kid on Oahu, was the loner. This child was so scared of a pedobear attack.");
					bg.ChildRowToMolokai();	
					System.out.println("A Child traveling back to Molokai: " + ChildrenOnOahu.getFirst().getName());
					ChildrenOnMolokai.add(ChildrenOnOahu.getFirst()); 
					ChildrenOnOahu.removeFirst(); //Remove Child from Oahu to Molokai
					}
				}
			}
			lock.release();
		//}
	}

	/*static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}*/

}
