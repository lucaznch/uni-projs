/******************************************************************************\
* Path vector routing protocol.                                                *
\******************************************************************************/

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "routing-simulator.h"

// Message format to send between nodes.
typedef struct {
  cost_t dists[MAX_NODES];
  node_t routes[MAX_NODES][MAX_NODES];
} data_t;

// State format.
typedef struct state_t {
  cost_t c[MAX_NODES][MAX_NODES];
  node_t p[MAX_NODES][MAX_NODES][MAX_NODES];
} state_t;

// Handler for the node to allocate and initialize its state.
state_t *init_state() {
  state_t *state = (state_t *)calloc(1, sizeof(state_t));
  node_t n1, n2, n3;
  n1 = get_first_node();
  while (n1 <= get_last_node()) {
    n2 = get_first_node();
    while (n2 <= get_last_node()) {
      state->c[n1][n2] = (n1 == n2) ? 0 : COST_INFINITY;
      n3 = get_first_node();
      while (n3 <= get_last_node()) {
        state->p[n1][n2][n3] = -1;
        n3 = get_next_node(n3);
      }
      n2 = get_next_node(n2);
    }
    n1 = get_next_node(n1);
  }
  return state;
}

// Notify a node that a neighboring link has changed cost.
void notify_link_change(node_t neighbor, cost_t new_cost) {
    state_t *s = get_state();
    node_t me = get_current_node();
    s->c[me][neighbor] = new_cost;
    
    if (new_cost == COST_INFINITY) {
        node_t d = get_first_node();
        while (d <= get_last_node()) {
            if (s->p[me][d][0] == neighbor) {
                s->c[me][d] = COST_INFINITY;
                int idx = 0;
                while (idx < MAX_NODES) {
                    s->p[me][d][idx] = -1;
                    idx++;
                }
                set_route(d, -1, COST_INFINITY);
            }
            d = get_next_node(d);
        }
    }
    
    int changed = 0;
    node_t backup_p[MAX_NODES][MAX_NODES][MAX_NODES];
    memcpy(backup_p, s->p, sizeof(backup_p));
    
    node_t target = get_first_node();
    while (target <= get_last_node()) {
        if (target != me) {
            cost_t min_c = COST_INFINITY;
            node_t min_nh = -1;
            node_t min_p[MAX_NODES];
            int z = 0;
            while (z < MAX_NODES) {
                min_p[z] = -1;
                z++;
            }
            
            node_t nb = get_first_node();
            while (nb <= get_last_node()) {
                if (nb != me && get_link_cost(nb) < COST_INFINITY) {
                    cost_t test_c = COST_ADD(get_link_cost(nb), s->c[nb][target]);
                    int has_loop = 0;
                    int j = 0;
                    while (j < MAX_NODES) {
                        if (s->p[nb][target][j] == -1) break;
                        if (s->p[nb][target][j] == me) {
                            has_loop = 1;
                            break;
                        }
                        j++;
                    }
                    
                    if (!has_loop && test_c < min_c) {
                        min_c = test_c;
                        min_nh = nb;
                        int k = 0;
                        while (k < MAX_NODES) {
                            min_p[k] = -1;
                            k++;
                        }
                        min_p[0] = me;
                        int pi = 1;
                        int q = 0;
                        while (q < MAX_NODES && s->p[nb][target][q] != -1) {
                            min_p[pi++] = s->p[nb][target][q];
                            q++;
                        }
                    }
                }
                nb = get_next_node(nb);
            }
            
            if (min_c != s->c[me][target] || memcmp(s->p[me][target], min_p, sizeof(min_p)) != 0) {
                s->c[me][target] = min_c;
                memcpy(s->p[me][target], min_p, sizeof(min_p));
                set_route(target, min_nh, min_c);
                changed = 1;
                int dbg = 0;
                while (dbg < MAX_NODES) {
                    if (min_p[dbg] == -1) break;
                    dbg++;
                }
            }
        }
        target = get_next_node(target);
    }
    
    if (memcmp(backup_p, s->p, sizeof(backup_p)) == 0) {
        changed = 0;
    }
    
    if (changed) {
        data_t msg;
        node_t neighbor_node = get_first_node();
        while (neighbor_node <= get_last_node()) {
            if (get_link_cost(neighbor_node) < COST_INFINITY && neighbor_node != me) {
                node_t d = get_first_node();
                while (d <= get_last_node()) {
                    msg.dists[d] = s->c[me][d];
                    memcpy(msg.routes[d], s->p[me][d], sizeof(msg.routes[d]));
                    d = get_next_node(d);
                }
                send_message(neighbor_node, &msg, sizeof(msg));
            }
            neighbor_node = get_next_node(neighbor_node);
        }
    }
}

// Receive a message sent by a neighboring node.
void notify_receive_message(node_t sender, void *message, size_t length) {
    state_t *s = get_state();
    data_t *m = (data_t *)message;
    node_t me = get_current_node();
    
    node_t x = get_first_node();
    while (x <= get_last_node()) {
        s->c[sender][x] = m->dists[x];
        x = get_next_node(x);
    }
    
    node_t y = get_first_node();
    while (y <= get_last_node()) {
        node_t z = get_first_node();
        while (z <= get_last_node()) {
            s->p[sender][y][z] = m->routes[y][z];
            z = get_next_node(z);
        }
        y = get_next_node(y);
    }
    
    int modified = 0;
    node_t old_paths[MAX_NODES][MAX_NODES][MAX_NODES];
    memcpy(old_paths, s->p, sizeof(old_paths));
    
    node_t dst = get_first_node();
    while (dst <= get_last_node()) {
        if (dst != me) {
            cost_t best_c = COST_INFINITY;
            node_t best_n = -1;
            node_t best_route[MAX_NODES];
            int clear_idx = 0;
            while (clear_idx < MAX_NODES) {
                best_route[clear_idx] = -1;
                clear_idx++;
            }
            
            node_t via = get_first_node();
            while (via <= get_last_node()) {
                if (via != me && get_link_cost(via) < COST_INFINITY) {
                    cost_t candidate_c = COST_ADD(get_link_cost(via), s->c[via][dst]);
                    int cycle = 0;
                    int scan = 0;
                    while (scan < MAX_NODES) {
                        if (s->p[via][dst][scan] == -1) break;
                        if (s->p[via][dst][scan] == me) {
                            cycle = 1;
                            break;
                        }
                        scan++;
                    }
                    
                    if (!cycle && candidate_c < best_c) {
                        best_c = candidate_c;
                        best_n = via;
                        int reset = 0;
                        while (reset < MAX_NODES) {
                            best_route[reset] = -1;
                            reset++;
                        }
                        best_route[0] = me;
                        int pos = 1;
                        int copy_i = 0;
                        while (copy_i < MAX_NODES && s->p[via][dst][copy_i] != -1) {
                            best_route[pos++] = s->p[via][dst][copy_i];
                            copy_i++;
                        }
                    }
                }
                via = get_next_node(via);
            }
            
            if (best_c != s->c[me][dst] || memcmp(s->p[me][dst], best_route, sizeof(best_route)) != 0) {
                s->c[me][dst] = best_c;
                memcpy(s->p[me][dst], best_route, sizeof(best_route));
                set_route(dst, best_n, best_c);
                modified = 1;
                int print_i = 0;
                while (print_i < MAX_NODES) {
                    if (best_route[print_i] == -1) break;
                    print_i++;
                }
            }
        }
        dst = get_next_node(dst);
    }
    
    if (memcmp(old_paths, s->p, sizeof(old_paths)) == 0) {
        modified = 0;
    }
    
    if (modified) {
        data_t out_msg;
        node_t nbr = get_first_node();
        while (nbr <= get_last_node()) {
            if (get_link_cost(nbr) < COST_INFINITY && nbr != me) {
                node_t dest_node = get_first_node();
                while (dest_node <= get_last_node()) {
                    out_msg.dists[dest_node] = s->c[me][dest_node];
                    memcpy(out_msg.routes[dest_node], s->p[me][dest_node], sizeof(out_msg.routes[dest_node]));
                    dest_node = get_next_node(dest_node);
                }
                send_message(nbr, &out_msg, sizeof(out_msg));
            }
            nbr = get_next_node(nbr);
        }
    }
}

