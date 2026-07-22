/**
 * Shared event contracts — and ONLY event contracts.
 *
 * <h2>The rule: this module contains event DTOs, nothing else.</h2>
 * No entities, no services, no repositories, no validation logic, no utilities.
 *
 * <h2>Why the rule exists</h2>
 * These records are the <em>published contract</em> between services on the Kafka
 * wire. Sharing that contract as a small library is fine and useful. But the
 * moment we also share <em>behaviour</em> (a shared entity, a shared "OrderUtils",
 * a shared validation rule), every service is coupled to it at compile time:
 * <ul>
 *   <li>Changing shared logic forces a coordinated re-release of every service —
 *       the exact "distributed monolith" microservices are meant to avoid.</li>
 *   <li>Services stop owning their own domain model and start leaking internals
 *       to each other, eroding their bounded contexts.</li>
 * </ul>
 * A pure-data contract can evolve carefully (add optional fields) without dragging
 * everyone along. Keep it dumb on purpose.
 */
package com.shop.events;
