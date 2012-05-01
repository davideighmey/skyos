//
//  chat.c -- create the chat clients.
//  
//
//

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#include "stdarg.h"

int main(int argc, const char* argv[]){
    char readBuff[256];
	char writeBuff[256];
    int readPos=0;
	int bytesRead=0;  
    
    host = atoi(argv[1]);
    
    if (argc == 0){
        return -1;}


    int socket = connect(host, 15)
    
    if (socket == -1){
        printf("You are not able to connect to the server\n");
        return -1;}
    else{
        printf("You have connected to the server\n");
    }
    
    //joined the server
    while (1){
        bytesRead = read(socket, readBuff+readPos, 1) //read initally
        while( bytesRead > 0){
            readPos += bytesRead;  //move the reading postion
            if (readBuff[readPos -1] == '\0') {
				printf("User.%s\n", readBuff);
				readPos=0;
			} 
            bytesRead = read(socket,readBuff+readPos,1); //read
           
        }
        readline(writeBuffer,256); //readline from stdio.h
        if(strlen(writeBuff) > 0) //check if written anything
        {
            if(writeBuff[0] == '.') { //exit command
                break;  
            }
            write(socket,writeBuff,strlen(writeBuff)+1); //write the buffer into the sever
        }
    }
    close(socket);
}