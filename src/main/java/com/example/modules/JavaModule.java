package com.example.modules;

import com.lambda.config.settings.comparable.BooleanSetting;
import com.lambda.config.settings.complex.Bind;
import com.lambda.event.events.TickEvent;
import com.lambda.event.listener.SafeListener;
import com.lambda.module.Module;
import com.lambda.module.tag.ModuleTag;
import com.lambda.util.Communication;
import kotlin.Unit;
import kotlin.jvm.JvmClassMappingKt;

// HEY YOU! Yeah, YOU! Sick of Java’s boring, slow, and clunky syntax? Why not try Kotlin instead?
public class JavaModule extends Module {
    public JavaModule() {
        super("JavaModule", "Usage example of Java", ModuleTag.Companion.getCLIENT(), false, false, Bind.Companion.getEMPTY(), false);
    }

    BooleanSetting setting = setting("Version", true, "Print the Java version", () -> true);

    {
        SafeListener.Companion.listen(
                this,
                JvmClassMappingKt.getKotlinClass(TickEvent.Pre.class),
                0,
                false,
                (ctx, event) -> {
                    if (setting.getValue()) Communication.INSTANCE.info(this, Runtime.version().toString(), "");

                    return Unit.INSTANCE;
                }
        );
    }
}
