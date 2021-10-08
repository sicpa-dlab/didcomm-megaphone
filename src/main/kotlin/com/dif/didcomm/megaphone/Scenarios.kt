package com.dif.didcomm.megaphone

import com.dif.didcomm.megaphone.MegaphonePlayGround.Companion.runScenario
import com.dif.didcomm.megaphone.diddoc.DIDDocResolver
import com.dif.didcomm.megaphone.secrets.AliceSecretResolver
import com.dif.didcomm.megaphone.secrets.BobSecretResolver
import com.dif.didcomm.megaphone.secrets.CharlieSecretResolver
import com.dif.didcomm.megaphone.secrets.DoctorSecretResolver
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.model.PackEncryptedParams
import org.didcommx.didcomm.model.UnpackParams
import org.slf4j.LoggerFactory
import java.time.Duration

class Scenarios {

    companion object {
        private val logger = LoggerFactory.getLogger(Scenarios::class.java)

        fun scenario3() {

            val alice = Megaphone(
                DIDComm(DIDDocResolver(), AliceSecretResolver()),
                Scenario.ALICE_DID,
                Role.speaker
            )
            val relay = Megaphone(
                DIDComm(DIDDocResolver(), DoctorSecretResolver()),
                Scenario.RELAY_DID,
                Role.relay
            )

            val bob = Megaphone(
                DIDComm(DIDDocResolver(), BobSecretResolver()),
                Scenario.BOB_DID,
                Role.listener
            )

            val charlie = object : Megaphone(
                DIDComm(DIDDocResolver(), CharlieSecretResolver()),
                Scenario.CHARLIE_DID,
                Role.listener
            ) {
                override suspend fun listen(volume: Volume) {
                    EventBus.EXTRA_EVENT_BUS.events.filter {
                        plainText(it)
                    }.filter {
                        msgSentbyMe(it)
                    }.filter {
                        byVolumen(it, volume)
                    }.collect {
                        unPackAndLogMsg(it)
                    }
                }
            }

            runBlocking {
                // Start listeners
                launch { // coroutine
                    bob.listen(Volume.emergency)
                }
                launch { // coroutine
                    relay.forward(Volume.emergency)
                }
                launch {
                    charlie.listen(Volume.emergency)
                }
                // Start brodcasting
                val tickerChannel = ticker(Duration.ofSeconds(3).toMillis())
                repeat(3) {
                    tickerChannel.receive()
                    launch {
                        alice.speak(MegaphoneMsg("I need a doctor !", Volume.emergency))
                    }
                }

            }


        }


        fun scenario2() {
            val alice = object : Megaphone(
                DIDComm(DIDDocResolver(), AliceSecretResolver()),
                Scenario.ALICE_DID,
                Role.listener,
                Pair("", ""),
                AliceSecretResolver()
            ) {
                override suspend fun listen(volume: Volume) {
                    EventBus.EVENT_BUS.events.filter {
                        didComm.unpack(
                            UnpackParams.Builder(it)
                                .secretResolver(AliceSecretResolver())
                                .build()
                        ).message.from != did
                    }.filter {
                        val unpack = didComm.unpack(
                            UnpackParams.Builder(it)
                                .secretResolver(AliceSecretResolver())
                                .build()
                        )
                        unpack.message.body["volumen"] == volume.value
                    }.collect {
                        val unpack = didComm.unpack(
                            UnpackParams.Builder(it)
                                .secretResolver(AliceSecretResolver())
                                .build()
                        )
                        val msg = unpack.message.body["msg"]
                        logger.info("Megaphone with did $did has received the message ' $msg '")
                    }
                }
            }
            val doctor = object : Megaphone(
                DIDComm(DIDDocResolver(), DoctorSecretResolver()),
                Scenario.DOCTOR_DID,
                Role.responder,
                Pair("doctor", "I'm a doctor!. Call me at +41768240073")
            ) {
                override suspend fun speak(msg: MegaphoneMsg) {
                    val plaintTextMessage =
                        Message.builder(
                            "12345",
                            mapOf("msg" to msg.message, "volumen" to msg.volume.value),
                            "megaphone-protocol/1.0"
                        )
                            .from(Scenario.DOCTOR_DID)
                            .to(listOf(Scenario.ALICE_DID))
                            .createdTime(1516269022)
                            .expiresTime(1516385931)
                            .build()

                    val packResult = didComm.packEncrypted(
                        PackEncryptedParams.builder(plaintTextMessage, Scenario.ALICE_DID)
                            .signFrom(Scenario.DOCTOR_DID)
                            .from(Scenario.DOCTOR_DID)
                            .build()
                    )

                    logger.info("Megaphone with did $did has sent the non repudiable encrypted message ' ${msg.message} '")
                    EventBus.EVENT_BUS.produceEvent(packResult.packedMessage)
                }
            }

            val listeners = listOf(
                Megaphone(
                    DIDComm(DIDDocResolver(), BobSecretResolver()),
                    Scenario.BOB_DID,
                    Role.listener

                ), Megaphone(
                    DIDComm(DIDDocResolver(), CharlieSecretResolver()),
                    Scenario.CHARLIE_DID,
                    Role.listener
                ), alice
            )
            runScenario(listeners, alice, doctor)
        }
    }
}