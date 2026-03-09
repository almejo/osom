package com.almejo.osom

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.memory.MMU
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.security.Permission

class CliImprovementsSpec extends Specification {

    def "missing ROM argument exits with code 1 and shows error message"() {
        given:
        def appender = attachListAppender()

        when:
        int exitCode = captureExitCode {
            Main.main([] as String[])
        }

        then:
        exitCode == 1
        appender.list.any { it.formattedMessage.contains("--rom option is required") }

        cleanup:
        detachListAppender(appender)
    }

    def "nonexistent ROM path exits with code 1 and error includes the path"() {
        given:
        def appender = attachListAppender()
        def romPath = "/nonexistent/path/game.gb"

        when:
        int exitCode = captureExitCode {
            Main.main(["--rom", romPath] as String[])
        }

        then:
        exitCode == 1
        appender.list.any { it.formattedMessage.contains(romPath) }

        cleanup:
        detachListAppender(appender)
    }

    def "BIOS missing produces error with suggestion and exits with code 1"() {
        given:
        def biosFile = new File("bios/bios.bin")
        def biosBackup = new File("bios/bios.bin.test-backup")
        def biosExisted = biosFile.exists()
        if (biosExisted) {
            assert biosFile.renameTo(biosBackup) : "Failed to backup BIOS file"
        }
        def tempRom = File.createTempFile("test-rom", ".gb")
        def appender = attachListAppender()

        when:
        int exitCode = captureExitCode {
            Main.main(["--rom", tempRom.absolutePath] as String[])
        }

        then:
        exitCode == 1
        appender.list.any { it.formattedMessage.contains("BIOS file not found") }
        appender.list.any { it.formattedMessage.contains("--no-bios") }

        cleanup:
        detachListAppender(appender)
        tempRom.delete()
        if (biosExisted) {
            biosBackup.renameTo(biosFile)
        }
    }

    def "--log-level DEBUG sets root logger to DEBUG"() {
        given:
        def loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory()
        def rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        def originalLevel = rootLogger.level

        when:
        captureExitCode {
            Main.main(["--log-level", "DEBUG"] as String[])
        }

        then:
        rootLogger.level == Level.DEBUG

        cleanup:
        rootLogger.setLevel(originalLevel)
    }

    def "unimplemented opcode throws exception with register state and UNKNOWN mnemonic"() {
        given:
        def mmu = Stub(MMU)
        mmu.getByte(0) >> 0xD3
        def cpu = new Z80Cpu(mmu, 4194304)

        when:
        cpu.execute()

        then:
        def exception = thrown(RuntimeException)
        exception.message.contains("Unimplemented opcode")
        exception.message.contains("UNKNOWN")
        exception.message.contains("PC=")
        exception.message.contains("SP=")
        exception.message.contains("AF=")
        exception.message.contains("BC=")
        exception.message.contains("DE=")
        exception.message.contains("HL=")
        exception.message.contains("Flags=")
    }

    private static ListAppender<ILoggingEvent> attachListAppender() {
        def rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        def listAppender = new ListAppender<ILoggingEvent>()
        listAppender.start()
        rootLogger.addAppender(listAppender)
        return listAppender
    }

    private static void detachListAppender(ListAppender<ILoggingEvent> appender) {
        def rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.detachAppender(appender)
        appender.stop()
    }

    private static int captureExitCode(Closure action) {
        def originalSecurityManager = System.getSecurityManager()
        int exitCode = -1
        System.setSecurityManager(new SecurityManager() {
            @Override
            void checkPermission(Permission permission) {}

            @Override
            void checkExit(int status) {
                exitCode = status
                throw new SecurityException("System.exit intercepted")
            }
        })
        try {
            action.call()
        } catch (SecurityException ignored) {
            // expected from System.exit interception
        } finally {
            System.setSecurityManager(originalSecurityManager)
        }
        return exitCode
    }
}
