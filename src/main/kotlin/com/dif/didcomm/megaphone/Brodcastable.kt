package com.dif.didcomm.megaphone

data class MegaphoneMsg(val message: String, val volume: Volume, val to: List<String> = emptyList())

sealed class Volume(val value: String) {
    object emergency : Volume("emergency")
    object urgent : Volume("urgent")
    object normal : Volume("normal")
}

sealed class Role {
    object speaker : Role()
    object listener : Role()
    object relay : Role()
    object responder : Role()
}

interface Brodcastable {
    suspend fun speak(msg: MegaphoneMsg)
    suspend fun listen(volume: Volume)
    suspend fun listenAndRespond(volume: Volume)
}