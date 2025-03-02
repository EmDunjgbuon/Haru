package cc.unknown.module.impl.combat;

import java.util.List;

import org.lwjgl.input.Mouse;

import com.google.common.eventbus.Subscribe;

import cc.unknown.event.impl.other.GameLoopEvent;
import cc.unknown.event.impl.other.player.LookEvent;
import cc.unknown.event.impl.move.MoveInputEvent;
import cc.unknown.event.impl.netty.PacketEvent;
import cc.unknown.event.impl.move.UpdateEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.ModuleInfo;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.utils.player.CoolDown;
import cc.unknown.utils.player.CombatUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.world.WorldSettings.GameType;

@ModuleInfo(name = "KillAura", category = Category.Combat)
public class KillAura extends Module {

    private EntityPlayer target;
    
    public ModeValue blockMode = new ModeValue("Mode", "Simple", "Simple");
    public SliderValue rotationDistance = new SliderValue("Rotation Distance", 3.3, 3, 6, 0.05);
    public SliderValue fov = new SliderValue("FOV", 90, 30, 360, 1);
    public SliderValue reach = new SliderValue("Reach", 3.3, 3, 6, 0.05);
    public SliderValue rps = new SliderValue("Max Rotation Speed", 36, 0, 200, 1);
    public DoubleSliderValue cps = new DoubleSliderValue("Left CPS", 9, 13, 1, 60, 0.5);
    public BooleanValue disableOnTp = new BooleanValue("Disable on TP", true);
    public BooleanValue disableWhenFlying = new BooleanValue("Disable when flying", true);
    public BooleanValue mouseDown = new BooleanValue("Mouse Down", true);
    public BooleanValue onlySurvival = new BooleanValue("Only Survival", true);
    public BooleanValue fixMovement = new BooleanValue("Fix Movement", true);

    private CoolDown coolDown = new CoolDown(1);
    private float yaw, pitch;

    public KillAura() {
        this.registerSetting(blockMode, rotationDistance, fov, reach, rps, cps, disableOnTp, disableWhenFlying, mouseDown, onlySurvival, fixMovement);
    }

    @Subscribe
    public void gameLoopEvent(GameLoopEvent e) {
        Mouse.poll();
        
        if (mc.currentScreen != null || 
            (onlySurvival.getValue() && mc.playerController.getCurrentGameType() != GameType.SURVIVAL) || 
            !coolDown.hasFinished() || 
            (mouseDown.getValue() && !Mouse.isButtonDown(0)) || 
            (disableWhenFlying.getValue() && mc.thePlayer.capabilities.isFlying)) {
            target = null;
            rotate(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            return;
        }
        
        target = findTarget();
        if (target == null) return;
        
        ravenClick();
        float[] rotations = CombatUtil.getRotations(target);
        rotate(rotations[0], rotations[1]);
    }

    @Subscribe
    public void onUpdate(UpdateEvent e) {
        if (target == null) return;
        e.setYaw(yaw);
        e.setPitch(pitch);
    }

    private void rotate(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    private void ravenClick() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    @Subscribe
    public void packetEvent(PacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook && disableOnTp.getValue() && coolDown.getTimeLeft() < 2000) {
            coolDown.setCooldown(2000);
            coolDown.start();
        }
    }

    @Subscribe
    public void move(MoveInputEvent e) {
        if (!fixMovement.getValue() || target == null) return;
        e.setYaw(yaw);
    }

    @Subscribe
    public void lookEvent(LookEvent e) {
        if (target == null) return;
        e.setYaw(yaw);
        e.setPitch(pitch);
    }

    private EntityPlayer findTarget() {
        List<Entity> entities = mc.theWorld.getLoadedEntityList();
        EntityPlayer bestTarget = null;
        double bestDistance = reach.getValue();
        
        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) entity;
            if (player == mc.thePlayer || player.isDead || player.getHealth() <= 0) continue;
            
            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance > bestDistance) continue;
            
            bestTarget = player;
            bestDistance = distance;
        }
        return bestTarget;
    }
}
