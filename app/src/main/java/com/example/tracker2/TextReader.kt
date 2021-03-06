package com.example.tracker2

const val BUFFER_SIZE = 200;

class TextReader(var talker: ArduinoTalker) : Thread() {
    var buffer = ByteArray(BUFFER_SIZE)
    var listeners = ArrayList<TextListener>()

    fun addListener(listener: TextListener) {
        listeners.add(listener)
    }

    override fun run() {
        while (true) {
            val received = talker.transfer(talker.device2Host, buffer, "Receive")
            if (received > 0) {
                val line = String(buffer.copyOfRange(0, received))
                for (listener in listeners) {
                    listener.receive(line)
                }
            }
        }
    }
}

interface TextListener {
    fun receive(text: String)
}