
import config.ConfigMgr;
import config.ConfigMgrLoader;
import config.combat.effect.CfgNone;
import config.combat.command.CfgJump;
import config.combat.command.CfgStateMachine;
import config.equip.CfgRank;
import config.task.CfgTask;
import configgen.genjava.ConfigPackage;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.IdentityHashMap;
//import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoadConfig {

    private static final Logger logger = Logger.getLogger("LoadConfig");

    public static void load(String fn) throws IOException {
        ConfigMgrLoader.loadPackagesAndSetMgr(ConfigPackage.read(Path.of(fn)));
    }

    public static void autoReload(ScheduledExecutorService executorService, String fn, Runnable afterReload) throws IOException {
        listen(executorService, Paths.get(fn), () -> {
            try {
                load(fn);
                if (afterReload != null) {
                    afterReload.run();
                }
            } catch (Exception ignored) {
            }
        });
    }

    public static void listen(ScheduledExecutorService executorService, Path path, Runnable callback) throws IOException {
        path = path.toAbsolutePath().normalize();
        if (!path.toFile().isDirectory()) { // 简化
            path = path.getParent();
        }
        Path watchPath = path;
        logger.info("start listen " + watchPath.toFile().getCanonicalPath());
        WatchService ws = watchPath.getFileSystem().newWatchService();
        watchPath.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
        executorService.scheduleWithFixedDelay(() -> {
            WatchKey key = ws.poll();
            if (key != null) {

                StringBuilder sb = new StringBuilder();
                key.pollEvents().forEach(e ->
                        sb.append("context=").append(e.context()).append(",kind=").append(e.kind()).append(";"));
                logger.info("auto reload " + watchPath + " " + sb.toString());
                callback.run();
                key.reset();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException {
        String fn = "base.bytes";
        load(fn);
        var config = ConfigMgr.getMgr();
        var task = config.getTaskTask(1);
        check(task.nullableRefNexttask() == config.getTaskTask(2), "legacy record reference");
        check(task.getName().getFirst().getZh_cn().equals("杀个怪")
                && task.getName().getFirst().getEn().equals("kill a monster"), "server translations");
        verify(config);
        System.out.println(CfgTask.get(1));
        System.out.println("java server example ok");
//        new BytesInspector(fn).match("eq");

//        ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor();
//        autoReload(watcher, fn, null);
//        System.out.println("read ok");
//        new BytesInspector(fn).loop();
//        watcher.close();
    }

    private static void verify(ConfigMgr config) {
        check(config.getOtherMonster(2).getPosList().getFirst().getX() == 33, "legacy monster");
        check(CfgRank.GREEN.ref(config).getRankID() == 1, "legacy enum details");
        var ability = config.getCombatAbility(1001);
        check(ability.getMachines().size() == 2, "machine fixture");
        var machine = ability.getMachines().getFirst();
        var movement = ability.getMachines().get(1);
        check(machine.getPhases().size() == 3, "Combat phase fixture");
        check(movement.getPhases().size() == 1, "Movement phase fixture");
        var phase = machine.getPhases().getFirst();
        check(ability.refSpawn() == ability.getOrigin(), "fixed object");
        check(ability.refInitial() == phase, "record local path");
        var anchorUses = Collections.newSetFromMap(new IdentityHashMap<>());
        var emptyEffects = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var current : ability.getMachines()) {
            check(current.getOnExit().getEffect() instanceof CfgNone, "empty effect type");
            check(emptyEffects.add(current.getOnExit().getEffect()), "empty effect occurrence identity");
            check(current.refInitial() == current.getPhases().getFirst(), "string list index: " + current.getCode());
            check(current.refImpact() == current.getImpacts().getFirst(), "int list index: " + current.getCode());
            check(current.refFrame() == current.getFrames().getFirst(), "long list index: " + current.getCode());
            check(current.refStance() == current.getStances().getFirst(), "bool list index: " + current.getCode());
            check(current.refVariant() == current.getVariants().getFirst(), "struct list index: " + current.getCode());
            check(current.refCombo() == current.getCombos().getFirst(), "composite list index: " + current.getCode());
            for (var currentPhase : current.getPhases()) {
                check(currentPhase.refExit() == current.getOnExit().getEffect(), "fixed member path: " + current.getCode());
            }
            for (var anchor : current.getAnchors()) {
                check(anchorUses.add(anchor), "empty source occurrence identity");
                check(anchor.refAnchor() == current.getAnchor(), "empty object target: " + current.getCode());
            }
        }
        check(machine.refFrame().getTick() == 9007199254740993L, "long list index precision");
        check(phase.nullableRefNext() == machine.getPhases().get(1), "next phase");
        check(machine.getPhases().get(1).nullableRefNext() == phase, "reference cycle");
        check(machine.getPhases().get(2).nullableRefNext() == null, "missing phase is nullable");
        check(movement.getPhases().getFirst().nullableRefNext() == null, "nearest scope must not fall back");
        check(machine.getAnchor() != movement.getAnchor(), "empty target occurrence identity");
        check(anchorUses.size() == 3, "empty source fixture");

        var cast = config.getCombatCast(1);
        check(cast.getUses().size() == 1, "ability source fixture");
        var use = cast.getUses().getFirst();
        check(use.refAbility() == ability, "fixed member source");
        check(use.getMachines().size() == 2, "machine source fixture");
        for (int i = 0; i < use.getMachines().size(); i++) {
            var machineUse = use.getMachines().get(i);
            var targetMachine = ability.getMachines().get(i);
            check(machineUse.getMachineKey().equals(targetMachine.getCode()), "machine source key");
            check(machineUse.getTargets().size() == targetMachine.getPhases().size(), "phase source fixture");
            for (int j = 0; j < machineUse.getTargets().size(); j++) {
                var target = machineUse.getTargets().get(j);
                var targetPhase = targetMachine.getPhases().get(j);
                check(target.getPhaseKey().equals(targetPhase.getKey()), "phase source key");
                check(target.refTarget() == targetPhase, "mixed ancestor sources");
                check(target.refByCode() == targetPhase, "unique key and source root key");
                check(target.refOrigin() == cast.getOrigin(), "source record root");
            }
        }
        var book = config.getCombatPhasebook("Fire", 0, false);
        check(cast.refBookRecord() == book, "composite record target");
        check(cast.refBook() == book.getEntry(), "composite primary key");
        check(cast.refBookByCode() == book.getEntry(), "composite unique key");
        check(cast.refReward() == config.getCombatBranchreward(cast.getRewardKey()).getReward(), "struct table key");
        for (int i = 0; i < 3; i++) check(cast.getOptional().get(i).nullableRefTarget() == null, "nullable path step " + i);
        check(cast.getOptional().get(3).nullableRefTarget() == machine.getPhases().get(2), "valid empty key");

        var timeline = config.getCombatTimeline(1);
        check(timeline.refEventKey() == timeline.getEvents().get(timeline.getEventKey()), "struct map key");
        check(timeline.refChannel() == timeline.getChannels().get(timeline.getChannel()), "string map key");
        check(timeline.refSlot() == timeline.getSlots().get(0), "int map key");
        check(timeline.refFrame() == timeline.getClock().get(9007199254740993L), "long map key precision");
        check(timeline.refAirborne() == timeline.getModes().get(false), "bool map key");
        check(timeline.refDefault() == timeline.getFallback(), "interface target");
        check(timeline.getFallback() instanceof CfgNone, "empty fixed effect type");
        check(emptyEffects.add(timeline.getFallback()), "empty fixed effect identity");
        check(timeline.getModes().get(true) instanceof CfgNone, "empty mapped effect type");
        check(emptyEffects.add(timeline.getModes().get(true)), "empty mapped effect identity");
        var outer = (CfgStateMachine) config.getCombatEncounter(1).getCommand();
        var inner = (CfgStateMachine) outer.getPhases().get(1).getAction();
        check(((CfgJump) inner.getPhases().getFirst().getAction()).nullableRefKey() == null, "nearest interface scope");
        check(((CfgJump) inner.getPhases().get(1).getAction()).nullableRefKey() == inner.getPhases().getFirst(),
                "recursive interface scope");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
