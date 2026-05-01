#include "packet-format.h"
#include <stdio.h>          // printf, perror, FILE, fopen, fclose, fseek, fwrite
#include <stdlib.h>         // atoi, exit, EXIT_FAILURE, EXIT_SUCCESS
#include <stddef.h>         // offsetof
#include <unistd.h>         // close
#include <errno.h>          // errno, EAGAIN, EWOULDBLOCK
#include <sys/time.h>       // struct timeval
#include <sys/socket.h>     // socket, bind, sendto, recvfrom, setsockopt, struct sockaddr, SOL_SOCKET, SO_RCVTIMEO, SO_REUSEADDR
#include <netinet/in.h>     // struct sockaddr_in, AF_INET
#include <arpa/inet.h>      // htons, htonl, ntohl

#define USAGE       "Usage: %s <file_name> <port_number> <window_size>\n"
#define PREFIX      "[\033[1;34mRECEIVER\033[0m] "
#define TIMEOUT_SEC 4

int main(int argc, char *argv[]) {
    if (argc != 4) {
        printf(USAGE, argv[0]); return EXIT_FAILURE;
    }

    char *file_name = argv[1];
    int port = atoi(argv[2]);
    int window_size = atoi(argv[3]);

    // Open file for writing.
    FILE *file = fopen(file_name, "w");
    if (!file) {
        perror("fopen"); return EXIT_FAILURE;
    } else {
        printf(PREFIX "Opened file %s for writing.\n", file_name);
    }

    // Prepare server socket.
    int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd == -1) {
        perror("socket");
        fclose(file);
        return EXIT_FAILURE;
    } else {
        printf(PREFIX "Created UDP socket.\n");
    }

    // Allow address reuse so we can rebind to the same port, after restarting the server.
    if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &(int){1}, sizeof(int)) < 0) {
        perror("setsockopt");
        close(sockfd);
        fclose(file);
        return EXIT_FAILURE;
    } else {
        printf(PREFIX "Set socket option SO_REUSEADDR.\n");
    }

    // Prepare server socket address.
    struct sockaddr_in srv_addr = {
        .sin_family = AF_INET,
        .sin_addr.s_addr = htonl(INADDR_ANY),
        .sin_port = htons(port),
    };

    // Bind socket to the specified port.
    if (bind(sockfd, (struct sockaddr *)&srv_addr, sizeof(srv_addr))) {
        perror("bind");
        close(sockfd);
        fclose(file);
        return EXIT_FAILURE;
    } else {
        printf(PREFIX "Bound socket to port %d.\n", port);
    }

    struct timeval timeout = {
        .tv_sec = TIMEOUT_SEC,
        .tv_usec = 0,
    };

    // Set socket timeout for receiving.
    if (setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
        perror("setsockopt");
        close(sockfd);
        fclose(file);
        return EXIT_FAILURE;
    } else {
        printf(PREFIX "Set socket receive timeout to %d seconds.\n", TIMEOUT_SEC);
    }

    int base = 0;               // Base of the window.
                                // Smallest sequence number the receiver is waiting for.

    data_pkt_t data_pkt;        // Data packet to be received.
    ssize_t data_pkt_len;       // Length of received data packet.

    ack_pkt_t ack_pkt = {       // ACK packet to be sent.
        .seq_num = htonl(0),
        .selective_acks = htonl(0),
    };

    int last_packet = FALSE;    // Flag indicating if the last packet has been received.

    struct sockaddr_in current_src_addr = {0};  // Source address of the received data packet.
    struct sockaddr_in first_src_addr = {0};    // Source address of the first received data packet.
    
    // Main loop. Receive data packets and send ACKs.
    while (TRUE) {

        // Wait for data packet. Blocking call with timeout.
        data_pkt_len = recvfrom(sockfd,
                                &data_pkt,
                                sizeof(data_pkt),
                                0,
                                (struct sockaddr *)&current_src_addr,
                                &(socklen_t){sizeof(current_src_addr)}
                               );


        // Verify if recvfrom timed out or encountered other error.
        if (data_pkt_len < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                printf(PREFIX "Timeout waiting for data packet.\n");

                if (last_packet) break;
                
            } else { perror("recvfrom"); }

            printf(PREFIX "Deleting incomplete file %s.\n", file_name);
            remove(file_name);

            close(sockfd);
            fclose(file);
            return EXIT_FAILURE;
        }

        // Verify if first reception: make connection establishment.
        if (first_src_addr.sin_addr.s_addr == 0) {
            printf(PREFIX "First reception: connection established with source %s:%d.\n",
                   inet_ntoa(current_src_addr.sin_addr),
                   ntohs(current_src_addr.sin_port)
                  );
            
            // Store first source address to be our reference.
            first_src_addr.sin_addr.s_addr = current_src_addr.sin_addr.s_addr;
            first_src_addr.sin_port = current_src_addr.sin_port;
        }

        // Verify source address of received data packet matches first source address.
        if (current_src_addr.sin_addr.s_addr != first_src_addr.sin_addr.s_addr ||
            current_src_addr.sin_port != first_src_addr.sin_port) {
            printf(PREFIX "Received packet from unknown source. Ignoring.\n");
            continue;
        }

        // Verify length of received data packet. ***
        // TO-DO: what if packet was truncated and is not the last packet?
        if (data_pkt_len != sizeof(data_pkt_t)) {
            printf(PREFIX "Received last packet.\n");
            last_packet = TRUE;
        }

        uint32_t data_pkt_seq_num = ntohl(data_pkt.seq_num);
        
        printf(PREFIX "Received segment %d, size %ld bytes.\n",
               data_pkt_seq_num,
               data_pkt_len
              );

        uint32_t ack_pkt_seq_num = ntohl(ack_pkt.seq_num);

        // Receiver window range: [base, base + window_size - 1].

        // Verify if packet is ahead of receiver's base. Out-of-order packet.
        if ((data_pkt_seq_num > ack_pkt_seq_num)) {

            if (data_pkt_seq_num > ack_pkt_seq_num && data_pkt_seq_num < base + window_size) {

                // Determine position of this packet in the Selective ACK bitmap.
                // Bit i in SACKs corresponds to packet with seq_num = base + i + 1. Thus:
                int position = data_pkt_seq_num - base - 1;
                
                if (position >= 0 && position < MAX_WINDOW_SIZE) {
                    // Set the bit at 'position' in the Selective ACK bitmap.
                    ack_pkt.selective_acks |= htonl((1 << position));

                    // Move file pointer to the offset.
                    fseek(file, data_pkt_seq_num * MAX_CHUNK_SIZE, 0);
                    
                    // Write data to file.
                    fwrite(data_pkt.data, 1, data_pkt_len - offsetof(data_pkt_t, data), file);
                }
            }

            // Send ACK via UDP.
            sendto(sockfd,
                   &ack_pkt,
                   sizeof(ack_pkt_t),
                   0,
                   (struct sockaddr *)&current_src_addr,
                   sizeof(current_src_addr)
                  );

            printf(PREFIX "Sent ACK %d.\n", ntohl(ack_pkt.seq_num));
        }

        // data_pkt_seq_num <= ack_pkt_seq_num. Expected or duplicate packet.
        else {

            // Move file pointer to the offset.
            fseek(file, data_pkt_seq_num * MAX_CHUNK_SIZE, 0);

            // Write data to file.
            fwrite(data_pkt.data, 1, data_pkt_len - offsetof(data_pkt_t, data), file);
            
            // Minimum advancement.
            int slide_forward = 1;

            // Iterate through Selective ACKs to determine how far we can slide the window forward.
            for (int i = 0; i < window_size - 1; i++) {

                // Extract bit i from Selective ACKs.
                int bit = (ntohl(ack_pkt.selective_acks) >> i) & 1;
                
                // Each consecutive 1 increments slide_forward. Once we hit a 0, we stop.
                if (bit == 0) break;
                
                slide_forward++;
            }

            ack_pkt.seq_num = htonl(data_pkt_seq_num + slide_forward);
            
            ack_pkt.selective_acks = htonl(ntohl(ack_pkt.selective_acks) >> slide_forward);
            
            base += slide_forward;
            
            // Send ACK via UDP.
            sendto(sockfd, 
                   &ack_pkt,
                   sizeof(ack_pkt_t),
                   0,
                   (struct sockaddr *)&current_src_addr,
                   sizeof(current_src_addr)
                  );

            printf(PREFIX "Sent ACK %d.\n", ntohl(ack_pkt.seq_num));
        }
    }

    // Clean up and exit.
    close(sockfd);
    fclose(file);

    return EXIT_SUCCESS;
}

