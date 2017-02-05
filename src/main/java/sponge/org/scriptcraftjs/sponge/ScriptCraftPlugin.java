package org.scriptcraftjs.sponge;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStreamReader;

@Plugin(id = "scriptcraft", name = "ScriptCraft", version = "1.0")
public class ScriptCraftPlugin {
    public boolean canary = false;
    public boolean bukkit = false;
    public boolean sponge = true;
    protected ScriptEngine engine = null;
    // right now all ops share the same JS context/scope
    // need to look at possibly having context/scope per operator
    //protected Map<CommandSender,ScriptCraftEvaluator> playerContexts = new HashMap<CommandSender,ScriptCraftEvaluator>();
    private String NO_JAVASCRIPT_MESSAGE = "No JavaScript Engine available. ScriptCraft will not work without Javascript.";
    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(ClassLoader.getSystemClassLoader());

        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            this.engine = factory.getEngineByName("JavaScript");
            if (this.engine == null) {
                this.getLogger().error(NO_JAVASCRIPT_MESSAGE);
            } else {
                Invocable inv = (Invocable) this.engine;
                //File f = new File(this.getJarPath());
                InputStreamReader reader = new InputStreamReader(getClass()
                        .getClassLoader()
                        .getResourceAsStream("boot.js"));
                this.engine.eval(reader);
                inv.invokeFunction("__scboot", this, engine, getClass().getClassLoader());
            }

            CommandSpec jsCommandSpec = CommandSpec.builder()
                    .description(Text.of("Execute Javascript code"))
                    .permission("scriptcraft.evaluate")
                    .executor(new JsCommandExecuter())
                    .build();
            CommandSpec jspCommandSpec = CommandSpec.builder()
                    .description(Text.of("Run javascript-provided command"))
                    .executor(new JspCommandExecuter())
                    .build();

            Sponge.getCommandManager().register(this, jsCommandSpec, "js");
            Sponge.getCommandManager().register(this, jspCommandSpec, "jsp");
        } catch (Exception e) {
            e.printStackTrace();
            this.getLogger().error(e.getMessage());
        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    private CommandResult executeCommand(CommandSource sender, CommandContext args) {
        Object jsResult = null;
        if (this.engine == null) {
            this.getLogger().error(NO_JAVASCRIPT_MESSAGE);
            return CommandResult.empty();
        }

        try {
            jsResult = ((Invocable) this.engine).invokeFunction("__onCommand", sender, args);
        } catch (Exception se) {
            this.getLogger().error(se.toString());
            se.printStackTrace();
            sender.sendMessage(Text.of(se.getMessage()));
        }
        if (jsResult != null) {
            return (Boolean) jsResult ? CommandResult.success() : CommandResult.empty();
        }

        return CommandResult.empty();
    }

    private class JsCommandExecuter implements CommandExecutor {
        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            return executeCommand(src, args);
        }
    }

    private class JspCommandExecuter implements CommandExecutor {
        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            return executeCommand(src, args);
        }
    }
}
