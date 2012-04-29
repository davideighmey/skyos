//
//  chatserver.c - Creating a sever that a chat client can connect to.
//  


#include "stdio.h"
#include "syscall.h"
#include "stdlib.h"

int clients[14]; // initializing chat clients
byte buffer[16];

int main (int argc, char *argv[]){
    int i = 0;
    for(i = 0; i < 14; i++){
        clients[i] = -1; //making all spots in sever -1 initally.
    }
    while(1){
        int newSocket = accept(15) //accept port 15
        if (newSocket > 0)
        {
            //finding a new client opening
            for (i = 0; i<14; i++){ 
                if(clients[i] == -1){ //spot open
                    clients[i] = NewSocket;//spot in server found
                    break;//break out of the for-loop
                }
            }
            if(i = 0; i<14; i++){
                if(clients != -1){
                    int message = read(NewSocket, buffer, 50)
                    
                }
                
            }
            
               /*     
                    
                    //see if the connection is open;
                    //open port;
                    if(socket = -1){
                        socket = null;
                        return -1;}
                if (new message received)
                    //broadcast message;
            }*/
                    
            }   

    }
    return 0;
}