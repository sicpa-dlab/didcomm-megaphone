package com.dif.didcomm.megaphone

import com.dif.didcomm.megaphone.Scenario.Companion.ALICE_DID
import com.dif.didcomm.megaphone.Scenario.Companion.BOB_DID
import com.dif.didcomm.megaphone.Scenario.Companion.CHARLIE_DID
import com.dif.didcomm.megaphone.Scenario.Companion.DOCTOR_DID
import com.dif.didcomm.megaphone.diddoc.DIDDocResolver
import com.dif.didcomm.megaphone.secrets.AliceSecretResolver
import com.dif.didcomm.megaphone.secrets.BobSecretResolver
import com.dif.didcomm.megaphone.secrets.CharlieSecretResolver
import com.dif.didcomm.megaphone.secrets.DoctorSecretResolver
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.didcommx.didcomm.DIDComm
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.time.Duration


@SpringBootApplication
class MegaphonePlayGround : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(MegaphonePlayGround::class.java)

    override fun run(vararg args: String?) {
        logger.info("Running Megaphone playground...")
        scenario1()
        //scenario2()
        //scenario3()
    }

    companion object {
        fun runScenario(
            listeners: List<Megaphone>,
            alice: Megaphone,
            doctor: Megaphone
        ) {
            runBlocking {
                // Start listeners
                listeners.forEach {
                    launch { // coroutine
                        it.listen(Volume.emergency)
                    }
                }
                // Start brodcasting
                val tickerChannel = ticker(Duration.ofSeconds(3).toMillis())
                repeat(3) {
                    tickerChannel.receive()
                    launch {
                        alice.speak(MegaphoneMsg("I need a doctor !", Volume.emergency))
                    }
                }
                launch {
                    doctor.listenAndRespond(Volume.emergency)
                }
            }
        }
    }

    private fun scenario1() {
        val alice = Megaphone(
            DIDComm(DIDDocResolver(), AliceSecretResolver()),
            ALICE_DID,
            Role.speaker
        )
        val doctor = Megaphone(
            DIDComm(DIDDocResolver(), DoctorSecretResolver()),
            DOCTOR_DID,
            Role.responder,
            Pair("doctor", "I'm a doctor!. Call me at +41768240073")
        )
        val listeners = listOf(
            Megaphone(
                DIDComm(DIDDocResolver(), BobSecretResolver()),
                BOB_DID,
                Role.listener
            ), Megaphone(
                DIDComm(DIDDocResolver(), CharlieSecretResolver()),
                CHARLIE_DID,
                Role.listener
            ), alice
        )
        runScenario(listeners, alice, doctor)
    }


    private fun createDoctor() =
        Megaphone(
            DIDComm(DIDDocResolver(), DoctorSecretResolver()),
            DOCTOR_DID,
            Role.responder,
            Pair("doctor", "I'm a doctor!. Call me at +41768240073")
        )

    private fun createInitialSpeaker() =
        Megaphone(
            DIDComm(DIDDocResolver(), AliceSecretResolver()),
            ALICE_DID,
            Role.listener
        )

    private fun createListeners() = listOf(
        Megaphone(
            DIDComm(DIDDocResolver(), BobSecretResolver()),
            BOB_DID,
            Role.listener
        ),
        Megaphone(
            DIDComm(DIDDocResolver(), CharlieSecretResolver()),
            CHARLIE_DID,
            Role.listener
        )
    )
}



fun main(args: Array<String>) {
    SpringApplication.run(MegaphonePlayGround::class.java, *args)
}