package com.example.tracker2

const val BUFFER_SIZE = 200;

class LineReader(var talker: ArduinoTalker) : Thread() {
    var buffer = ByteArray(BUFFER_SIZE)
    var listeners = ArrayList<LineListener>()

    fun addListener(listener: LineListener) {
        listeners.add(listener)
    }

    override fun run() {
        while (true) {
            talker.transfer(talker.device2Host, buffer, "Receive")
            val line = String(buffer)
            for (listener in listeners) {
                listener.receive(line)
            }
        }
    }
}

interface LineListener {
    fun receive(line: String)
}