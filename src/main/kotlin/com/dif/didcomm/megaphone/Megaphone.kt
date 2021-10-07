package com.dif.didcomm.megaphone

import com.dif.didcomm.megaphone.EventBus.Companion.EXTRA_EVENT_BUS
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.model.PackSignedParams
import org.didcommx.didcomm.model.UnpackParams
import org.didcommx.didcomm.secret.SecretResolver
import org.didcommx.didcomm.secret.SecretResolverInMemory
import org.slf4j.LoggerFactory


open class Megaphone(
    internal val didComm: DIDComm,
    internal val did: String,
    private val role: Role,
    internal val respondTo: Pair<String, String> = Pair("", ""),
    internal val secretResolver: SecretResolver = SecretResolverInMemory(listOf())

) : Brodcastable {


    private val logger = LoggerFactory.getLogger(Megaphone::class.java)

    override suspend fun speak(msg: MegaphoneMsg) {
        val plaintTextMessage =
            Message.builder(
                "12345",
                mapOf("msg" to msg.message, "volumen" to msg.volume.value),
                "megaphone-protocol/1.0"
            )
                .from(did)
                .createdTime(1516269022)
                .expiresTime(1516385931)
                .build()

        val packResult = didComm.packSigned(
            PackSignedParams.builder(plaintTextMessage, did)
                .build()
        )

        logger.info("Megaphone with did $did has sent the message ' ${msg.message} '")
        EventBus.EVENT_BUS.produceEvent(packResult.packedMessage)
    }

    override suspend fun listen(volume: Volume) {
        EventBus.EVENT_BUS.events.filter {
            plainText(it)
        }.filter {
            msgSentbyMe(it)
        }.filter {
            byVolumen(it, volume)
        }.collect {
            unPackAndLogMsg(it)
        }
    }

    override suspend fun listenAndRespond(volume: Volume) {
        EventBus.EVENT_BUS.events.filter {
            msgOfInterest(it)
        }.filter {
            byVolumen(it, volume)
        }.collect {
            unPackAndLogMsg(it)
            speak(MegaphoneMsg(respondTo.second, Volume.emergency))
        }

    }

    internal fun byVolumen(it: String, volume: Volume): Boolean {
        val unpack = didComm.unpack(
            UnpackParams.Builder(it).build()
        )
        return unpack.message.body["volumen"] == volume.value
    }

    internal fun plainText(it: String): Boolean {
        return !it.contains("ciphertext")
    }

    internal fun unPackAndLogMsg(it: String) {
        val unpack = didComm.unpack(
            UnpackParams.Builder(it).build()
        )
        val msg = unpack.message.body["msg"]
        logger.info("Megaphone with did $did has received the message ' $msg '")
    }

    internal fun msgSentbyMe(it: String) = didComm.unpack(
        UnpackParams.Builder(it).build()
    ).message.from != did

    internal fun msgOfInterest(it: String): Boolean {
        val unpack = didComm.unpack(
            UnpackParams.Builder(it).build()
        )
        val msg = unpack.message.body["msg"] as String
        return msg.contains(respondTo.first)
    }

    suspend fun forward(volume: Volume) {
        EventBus.EVENT_BUS.events.filter {
            byVolumen(it, volume)
        }.collect {
            // forward the event to another transport
            unPackAndLogMsg(it)
            logger.info("$did forwarding the event to another transport")
            EXTRA_EVENT_BUS.produceEvent(it)
        }
    }
}