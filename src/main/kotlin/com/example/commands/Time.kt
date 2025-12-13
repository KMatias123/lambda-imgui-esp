package com.example.commands

import com.lambda.brigadier.argument.time
import com.lambda.brigadier.argument.value
import com.lambda.brigadier.execute
import com.lambda.brigadier.required
import com.lambda.command.LambdaCommand
import com.lambda.util.Communication.info
import com.lambda.util.extension.CommandBuilder

object Time : LambdaCommand(
    name = "time",
    description = "A command that prints the time in ticks",
) {
    override fun CommandBuilder.create() {
        required(time("time")) { time ->
            execute {
                info("The time in ticks is ${time().value()} ticks")
            }
        }
    }
}
