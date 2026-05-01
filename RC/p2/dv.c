/******************************************************************************\
* Distance vector routing protocol without reverse path poisoning.             *
\******************************************************************************/

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "routing-simulator.h"

// Message format to send between nodes.
typedef struct data_t {
  cost_t distances[MAX_NODES];
} data_t;

// State format: stores distance vector and neighbor cost table
typedef struct state_t {
  cost_t d[MAX_NODES];
  cost_t n[MAX_NODES][MAX_NODES];
} state_t;

// Handler for the node to allocate and initialize its state.
state_t *init_state() {
  state_t *state = (state_t *)calloc(1, sizeof(state_t));
  node_t me = get_current_node();
  node_t x = get_first_node();
  while (x <= get_last_node()) {
    state->d[x] = COST_INFINITY;
    node_t y = get_first_node();
    while (y <= get_last_node()) {
      if (x == y) {
        state->n[x][y] = 0;
      } else {
        state->n[x][y] = COST_INFINITY;
      }
      y = get_next_node(y);
    }
    x = get_next_node(x);
  }
  state->d[me] = 0;
  return state;
}

// Notify a node that a neighboring link has changed cost.
void notify_link_change(node_t neighbor, cost_t new_cost) {
  state_t *s = get_state();
  node_t me = get_current_node();
  s->n[me][neighbor] = new_cost;
  
  int changed = 0;
  node_t target = get_first_node();
  while (target <= get_last_node()) {
    if (target == me) {
      target = get_next_node(target);
      continue;
    }
    
    cost_t best_cost = get_link_cost(target);
    node_t best_hop = target;
    if (best_cost >= COST_INFINITY) {
      best_hop = -1;
    }
    
    node_t via = get_first_node();
    while (via <= get_last_node()) {
      if (via == me) {
        via = get_next_node(via);
        continue;
      }
      cost_t link_c = get_link_cost(via);
      if (link_c >= COST_INFINITY) {
        via = get_next_node(via);
        continue;
      }
      cost_t total_c = COST_ADD(link_c, s->n[via][target]);
      if (total_c < best_cost) {
        best_cost = total_c;
        best_hop = via;
      }
      via = get_next_node(via);
    }
    
    if (best_cost != s->d[target]) {
      changed = 1;
      s->d[target] = best_cost;
      set_route(target, best_hop, best_cost);
    }
    target = get_next_node(target);
  }
  
  if (changed) {
    data_t msg;
    node_t idx = get_first_node();
    while (idx <= get_last_node()) {
      msg.distances[idx] = s->d[idx];
      idx = get_next_node(idx);
    }
    node_t nbr = get_first_node();
    while (nbr <= get_last_node()) {
      if (get_link_cost(nbr) < COST_INFINITY && nbr != me) {
        send_message(nbr, &msg, sizeof(msg));
      }
      nbr = get_next_node(nbr);
    }
  }
}

// Receive a message sent by a neighboring node.
void notify_receive_message(node_t sender, void *message, size_t length) {
  state_t *s = get_state();
  data_t *incoming = (data_t *)message;
  node_t me = get_current_node();
  
  node_t dest_node = get_first_node();
  while (dest_node <= get_last_node()) {
    s->n[sender][dest_node] = incoming->distances[dest_node];
    dest_node = get_next_node(dest_node);
  }
  
  int modified = 0;
  node_t destination = get_first_node();
  while (destination <= get_last_node()) {
    if (destination == me) {
      destination = get_next_node(destination);
      continue;
    }
    
    cost_t min_cost = get_link_cost(destination);
    node_t next_node = destination;
    if (min_cost >= COST_INFINITY) {
      next_node = -1;
    }
    
    node_t neighbor_n = get_first_node();
    while (neighbor_n <= get_last_node()) {
      if (neighbor_n == me) {
        neighbor_n = get_next_node(neighbor_n);
        continue;
      }
      cost_t link_cost = get_link_cost(neighbor_n);
      if (link_cost >= COST_INFINITY) {
        neighbor_n = get_next_node(neighbor_n);
        continue;
      }
      cost_t path_cost = COST_ADD(link_cost, s->n[neighbor_n][destination]);
      if (path_cost < min_cost) {
        min_cost = path_cost;
        next_node = neighbor_n;
      }
      neighbor_n = get_next_node(neighbor_n);
    }
    
    if (min_cost != s->d[destination]) {
      modified = 1;
      s->d[destination] = min_cost;
      set_route(destination, next_node, min_cost);
    }
    destination = get_next_node(destination);
  }
  
  if (modified) {
    data_t outgoing;
    node_t i = get_first_node();
    while (i <= get_last_node()) {
      outgoing.distances[i] = s->d[i];
      i = get_next_node(i);
    }
    node_t neighbor_to_send = get_first_node();
    while (neighbor_to_send <= get_last_node()) {
      if (get_link_cost(neighbor_to_send) < COST_INFINITY && neighbor_to_send != me) {
        send_message(neighbor_to_send, &outgoing, sizeof(outgoing));
      }
      neighbor_to_send = get_next_node(neighbor_to_send);
    }
  }
}

