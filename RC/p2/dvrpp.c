/******************************************************************************\
* Distance vector routing protocol with reverse path poisoning.                *
\******************************************************************************/

#include <assert.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "routing-simulator.h"

// Message format to send between nodes.
typedef struct {
  cost_t costs[MAX_NODES];
} data_t;

// State format.
typedef struct state_t {
  cost_t dist[MAX_NODES];
  cost_t neighbor_costs[MAX_NODES][MAX_NODES];
  node_t hops[MAX_NODES];
} state_t;

// Handler for the node to allocate and initialize its state.
state_t *init_state() {
  state_t *state = (state_t *)calloc(1, sizeof(state_t));
  node_t me = get_current_node();
  node_t a = get_first_node();
  while (a <= get_last_node()) {
    state->dist[a] = COST_INFINITY;
    state->hops[a] = -1;
    node_t b = get_first_node();
    while (b <= get_last_node()) {
      if (a == b) {
        state->neighbor_costs[a][b] = 0;
      } else {
        state->neighbor_costs[a][b] = COST_INFINITY;
      }
      b = get_next_node(b);
    }
    a = get_next_node(a);
  }
  state->dist[me] = 0;
  return state;
}

// Notify a node that a neighboring link has changed cost.
void notify_link_change(node_t neighbor, cost_t new_cost) {
  state_t *s = get_state();
  node_t me = get_current_node();
  s->neighbor_costs[me][neighbor] = new_cost;
  
  int changes = 0;
  node_t target = get_first_node();
  while (target <= get_last_node()) {
    if (target == me) {
      target = get_next_node(target);
      continue;
    }
    
    cost_t min_cost = get_link_cost(target);
    node_t best_hop = -1;
    node_t via = get_first_node();
    while (via <= get_last_node()) {
      if (via == me) {
        via = get_next_node(via);
        continue;
      }
      cost_t test_cost = COST_ADD(get_link_cost(via), s->neighbor_costs[via][target]);
      if (test_cost <= min_cost) {
        min_cost = test_cost;
        best_hop = via;
      }
      via = get_next_node(via);
    }
    
    if (min_cost != s->dist[target]) {
      s->dist[target] = min_cost;
      s->hops[target] = best_hop;
      set_route(target, best_hop, min_cost);
      changes = 1;
    }
    target = get_next_node(target);
  }
  
  if (changes) {
    node_t nbr = get_first_node();
    while (nbr <= get_last_node()) {
      if (get_link_cost(nbr) < COST_INFINITY && nbr != me) {
        data_t msg;
        node_t idx = get_first_node();
        while (idx <= get_last_node()) {
          msg.costs[idx] = s->dist[idx];
          idx = get_next_node(idx);
        }
        node_t d = get_first_node();
        while (d <= get_last_node()) {
          if (s->hops[d] == nbr) {
            msg.costs[d] = COST_INFINITY;
          }
          d = get_next_node(d);
        }
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
    s->neighbor_costs[sender][dest_node] = incoming->costs[dest_node];
    dest_node = get_next_node(dest_node);
  }
  
  int modified = 0;
  node_t destination = get_first_node();
  while (destination <= get_last_node()) {
    if (destination == me) {
      destination = get_next_node(destination);
      continue;
    }
    
    cost_t best_cost = get_link_cost(destination);
    node_t best_next = -1;
    node_t neighbor_node = get_first_node();
    while (neighbor_node <= get_last_node()) {
      if (neighbor_node == me) {
        neighbor_node = get_next_node(neighbor_node);
        continue;
      }
      cost_t candidate = COST_ADD(get_link_cost(neighbor_node), s->neighbor_costs[neighbor_node][destination]);
      if (candidate <= best_cost) {
        best_cost = candidate;
        best_next = neighbor_node;
      }
      neighbor_node = get_next_node(neighbor_node);
    }
    
    if (best_cost != s->dist[destination]) {
      s->dist[destination] = best_cost;
      s->hops[destination] = best_next;
      set_route(destination, best_next, best_cost);
      modified = 1;
    }
    destination = get_next_node(destination);
  }
  
  if (modified) {
    node_t neighbor_to_send = get_first_node();
    while (neighbor_to_send <= get_last_node()) {
      if (get_link_cost(neighbor_to_send) < COST_INFINITY && neighbor_to_send != me) {
        data_t outgoing;
        node_t i = get_first_node();
        while (i <= get_last_node()) {
          outgoing.costs[i] = s->dist[i];
          i = get_next_node(i);
        }
        node_t dst = get_first_node();
        while (dst <= get_last_node()) {
          if (s->hops[dst] == neighbor_to_send) {
            outgoing.costs[dst] = COST_INFINITY;
          }
          dst = get_next_node(dst);
        }
        send_message(neighbor_to_send, &outgoing, sizeof(outgoing));
      }
      neighbor_to_send = get_next_node(neighbor_to_send);
    }
  }
}

