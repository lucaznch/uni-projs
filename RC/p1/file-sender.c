#include "packet-format.h"
#include <stdio.h>      // printf, perror, FILE, fopen, fclose, feof, fseek, fread
#include <stdlib.h>     // atoi, exit, EXIT_FAILURE, EXIT_SUCCESS
#include <stddef.h>     // offsetof
#include <errno.h>      // errno, EAGAIN, EWOULDBLOCK
#include <unistd.h>     // close
#include <netdb.h>      // gethostbyname, struct hostent
#include <sys/time.h>   // struct timeval
#include <sys/socket.h> // socket, sendto, recvfrom, setsockopt, struct sockaddr, SOL_SOCKET, SO_RCVTIMEO
#include <netinet/in.h> // struct sockaddr_in, AF_INET
#include <arpa/inet.h>  // htons, htonl, ntohl, struct in_addr

#define USAGE       "Usage: %s <file_name> <server_hostname> <server_port_number> <window_size>\n"
#define PREFIX      "[\033[1;32mSENDER\033[0m] "
#define TIMEOUT_SEC 1

int main(int argc, char *argv[]) {
    if (argc != 5) {
        printf(USAGE, argv[0]); return EXIT_FAILURE;
    }

    char *file_name = argv[1];
    char *host = argv[2];
    int port = atoi(argv[3]);
    int window_size = atoi(argv[4]);

    // Open file for reading.
    FILE *file = fopen(file_name, "r");
    if (!file) {
        perror("fopen"); return EXIT_FAILURE;
    } else {
        printf(PREFIX "Opened file %s for reading.\n", file_name);
    }

    // Resolve server address.
    struct hostent *he;
    if (!(he = gethostbyname(host))) {
        perror("gethostbyname");
        fclose(file);
        return EXIT_FAILURE;
    } else {
        printf(PREFIX "Resolved server hostname %s.\n", host);
    }

    // Prepare server socket address.
    struct sockaddr_in srv_addr = {
        .sin_family = AF_INET,
        .sin_port = htons(port),
        .sin_addr = *((struct in_addr *)he->h_addr),
    };

    // Create UDP socket.
    int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd == -1) {
        perror("socket");
        fclose(file);
        return EXIT_FAILURE;
    } else {
        printf(PREFIX "Created UDP socket.\n");
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
    
    size_t data_len = 0;                // Length of data read from file.

    data_pkt_t data_pkt;                // Data packet to be sent.

    ack_pkt_t ack_pkt = {               // ACK packet received from server.
        .seq_num = htonl(0),
        .selective_acks = htonl(0),
    };
    ssize_t ack_pkt_len;                // Length of received ACK packet.

    uint32_t next_seq_num = 0;          // Sequence number of the next packet to be sent.
                                        // Smallest unused sequence number.

    int base = 0;                       // Base of the window.
                                        // Sequence number of the oldest/first unacknowledged packet.
                                        // Tracks the lower bound of the sender's window.
                                        // Window range: [base, base + window_size - 1].

    int cumulative_ack = 0;             // Last and highest ACK received.
                                        // Sequence number up to which all packets have been ACKed.
                                        // Tracks how far the receiver has progressed.
                                        // Receiver has received all packets < cumulative_ack.
                                        // Used to slide the sender's window forward.

    int pkts_sent_counter = 0;          // Next window slot index (controls flow).
                                        // Sequence number of the next window slot to fill.
                                        // Used, with base and window_size, to manage the sliding window.
    
    int timeout_counter = 0;            // Number of timeouts events occured in succession.
    int dup_ack_counter = 0;            // Duplicate ACK counter.

    int last_packet = FALSE;            // Flag to indicate the last packet. EOF is implicit.
    int fast_retransmission = FALSE;    // Flag to force retransmission of window.

    printf(PREFIX "Starting file transmission...\n");

    // Main loop. Read chunk, send packet, wait for ACK.
    while (!(feof(file) && data_len < sizeof(data_pkt.data)) || fast_retransmission || last_packet) {
        
        // Verify number of timeouts.
        if (timeout_counter >= MAX_RETRIES) {
            printf(PREFIX "Exceeded max number of timeouts (%d). Exiting.\n", MAX_RETRIES);
            close(sockfd);
            fclose(file);
            return EXIT_FAILURE;
        }

        // Force window retransmission.
        if (fast_retransmission) {
            printf(PREFIX "Fast retransmitting window from ACK %d.\n", cumulative_ack);

            // Extract sequence number.
            // This is the sequence number up to which all packets have been acknowledged.
            uint32_t ack_seq_num = ntohl(ack_pkt.seq_num);

            // fseek explanation:
            // Whence = 0 => the offset is measured from the beginning of the file.
            // Move (ack_seq_num * MAX_CHUNK_SIZE) bytes from the beginning of the file.
            // This positions the file pointer to the start of the unacknowledged data.
            // We can do this because the sender reads sequentially from the file.

            // Move file pointer to the offset of the cumulative ACK.
            fseek(file, ack_seq_num * MAX_CHUNK_SIZE, 0);
            
            pkts_sent_counter = cumulative_ack;  // Reset pkts_sent_counter to cumulative_ack.
            
            next_seq_num = cumulative_ack;       // Reset next_seq_num to cumulative_ack.
            
            fast_retransmission = FALSE;    // Reset flag.
            
            dup_ack_counter = 0;            // Reset duplicate ACK counter. ***
                                            // If we do not reset,
                                            // we may immediately trigger another fast retransmission
                                            // if just one duplicate ACK is received after retransmission.
        }

        if (!last_packet || pkts_sent_counter == 0) {
            
            int selective_ack_index = -1;
            
            while ((pkts_sent_counter < base + window_size) && !feof(file)) {
                
                // Assign sequence number, in network byte order, to packet. Increment next_seq_num.
                data_pkt.seq_num = htonl(next_seq_num++);

                // Read chunk from file.
                data_len = fread(data_pkt.data, 1, sizeof(data_pkt.data), file);

                // Extract selective ACKs.
                uint32_t ack_pkt_selective_acks = ntohl(ack_pkt.selective_acks);

                // Prevent sending packets that have already been selectively acknowledged.
                if (((ack_pkt_selective_acks >> selective_ack_index) & 1) && (selective_ack_index >= 0)) {
                    
                    // SACK: the receiver may acknowledge packets beyond the cumulative ACK.
                    // bit == 1 => packet acknowledged => skip sending.
                    
                    pkts_sent_counter++;    // Skip sending, but we count it as sent in the window.
                    selective_ack_index++;
                    continue;
                }

                // Length of packet to be sent.
                ssize_t data_pkt_len;

                // Send packet via UDP.
                data_pkt_len = sendto(sockfd,
                                      &data_pkt,
                                      offsetof(data_pkt_t, data) + data_len,
                                      0,
                                      (struct sockaddr *)&srv_addr,
                                      sizeof(srv_addr)
                                     );
                
                printf(PREFIX "Sending segment %d, size %ld bytes (attempt %d).\n",
                       ntohl(data_pkt.seq_num),
                       offsetof(data_pkt_t, data) + data_len,
                       timeout_counter + 1
                      );

                // Sanity check. Verify that the entire packet was sent.
                if (data_pkt_len != offsetof(data_pkt_t, data) + data_len) {
                    printf(PREFIX "Error: sent %ld bytes, expected to send %ld bytes. Exiting.\n",
                           data_pkt_len,
                           offsetof(data_pkt_t, data) + data_len
                          );
                    close(sockfd);
                    fclose(file);
                    return EXIT_FAILURE;
                }

                if (feof(file)) {
                    printf(PREFIX "EOF after sending segment %d.\n", ntohl(data_pkt.seq_num));
                    last_packet = TRUE;
                }
                
                selective_ack_index++;
                pkts_sent_counter++;
            }
        }

        // Holds the source address (server address) of the received ACK.
        struct sockaddr_in src_addr;

        // Wait for ACK packet. Blocking call with timeout.
        ack_pkt_len = recvfrom(sockfd,
                               &ack_pkt,
                               sizeof(ack_pkt),
                               0,
                               (struct sockaddr *)&src_addr,
                               &(socklen_t){sizeof(src_addr)}
                              );

        // Verify if recvfrom timed out or encountered other error.
        if (ack_pkt_len < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                printf(PREFIX "Timeout waiting for ACK %d.\n", cumulative_ack);
                timeout_counter++;
                fast_retransmission = TRUE;
                last_packet = FALSE;
                continue;
            } else {
                perror("recvfrom");
                close(sockfd);
                fclose(file);
                return EXIT_FAILURE;
            }
        }

        // Verify source address of received ACK matches server address.
        else if (src_addr.sin_addr.s_addr != srv_addr.sin_addr.s_addr ||
                 src_addr.sin_port != srv_addr.sin_port) {
            printf(PREFIX "Received packet from unknown source. Ignoring.\n");
            continue;
        }

        /*
        // Verify length of received ACK packet. ***
        else if (!last_packet && ack_pkt_len != sizeof(ack_pkt_t)) {
            printf(PREFIX "Received invalid ACK packet size %ld. Ignoring.\n", ack_pkt_len);
            continue;
        }
        */

        // ACK packet is valid. Verify sequence number.
        else {
            // Extract sequence number from received ACK.
            uint32_t ack_seq_num = ntohl(ack_pkt.seq_num);

            printf(PREFIX "Received ACK %d.\n", ack_seq_num);

            // Cumulative ACK processing.
            if (ack_seq_num == cumulative_ack) {
                dup_ack_counter++;
                if (dup_ack_counter >= FAST_RETRANSMIT_DUPACKS) {
                    printf(PREFIX "Received 3 dup ACKs for %d.\n", ack_seq_num);  
                    fast_retransmission = TRUE;
                }
                continue;
            } else {
                cumulative_ack = ack_seq_num;   // Cumulative ACK advancement.
                dup_ack_counter = 0;            // Reset dup ACK counter.
                timeout_counter = 0;            // Reset timeout counter. ***
                base = ack_seq_num;             // Slide window base.
            }
        }

        if (last_packet && pkts_sent_counter == cumulative_ack) {
            printf(PREFIX "All packets acknowledged. Exiting successfully.\n");
            break;
        }
    }

    // Clean up and exit.
    close(sockfd);
    fclose(file);

    return EXIT_SUCCESS;
}

