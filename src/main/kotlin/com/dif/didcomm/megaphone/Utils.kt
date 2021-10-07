package com.dif.didcomm.megaphone

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Scenario {
    companion object {
        const val ALICE_DID = "did:example:alice"
        const val BOB_DID = "did:example:bob"
        const val CHARLIE_DID = "did:example:charlie"
        const val DOCTOR_DID = "did:example:doctor"
        const val RELAY_DID = "did:example:relay"
    }

}

class EventBus {
    private val _events = MutableSharedFlow<String>() // private mutable shared flow
    val events = _events.asSharedFlow() // publicly exposed as read-only shared flow

    suspend fun produceEvent(event: String) {
        _events.emit(event) // suspends until all subscribers receive it
    }

    companion object {
        val EVENT_BUS = EventBus()
        val EXTRA_EVENT_BUS =  EventBus()
    }

}