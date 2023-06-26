package logging

import java.util.logging.*

class LogConsoleHandler : StreamHandler(System.out, SimpleFormatter()) {
    private val stderrHandler = ConsoleHandler()
    override fun publish(record: LogRecord) {
        if (record.level.intValue() <= Level.INFO.intValue()) {
            super.publish(record)
            super.flush()
        } else {
            stderrHandler.publish(record)
            stderrHandler.flush()
        }
    }
}