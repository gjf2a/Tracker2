package com.example.tracker2

const val BUFFER_SIZE = 200;

class TextReader(var talker: ArduinoTalker, val logger: Logger) : Thread() {
    var buffer = ByteArray(BUFFER_SIZE)
    var listeners = ArrayList<TextListener>()

    fun addListener(listener: TextListener) {
        listeners.add(listener)
    }

    override fun run() {
        var accumulated = ""
        while (true) {
            val received = talker.transfer(talker.device2Host, buffer)
            if (received > 0) {
                val line = String(buffer.copyOfRange(0, received))
                accumulated += line
                logger.log("Incoming: $line")
                if (accumulated.endsWith("\n")) {
                    logger.log("Reporting: $accumulated")
                    for (listener in listeners) {
                        listener.receive(accumulated)
                    }
                    accumulated = ""
                }
            }
        }
    }
}

interface TextListener {
    fun receive(text: String)
}