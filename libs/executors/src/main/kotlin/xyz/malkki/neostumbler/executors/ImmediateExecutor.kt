package xyz.malkki.neostumbler.executors

import java.util.concurrent.Executor

object ImmediateExecutor : Executor {
    override fun execute(command: Runnable) = command.run()
}
