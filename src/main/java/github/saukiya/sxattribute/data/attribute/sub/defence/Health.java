package github.saukiya.sxattribute.data.attribute.sub.defence;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.manager.AttributeManager;
import github.saukiya.sxattribute.SXAttribute;
import github.saukiya.sxattribute.data.attribute.AttributeType;
import github.saukiya.sxattribute.data.attribute.SubAttribute;
import github.saukiya.sxattribute.data.eventdata.EventData;
import github.saukiya.sxattribute.data.eventdata.sub.UpdateData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.spigotmc.SpigotConfig;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 生命 - 当前不更新怪物生命
 *
 * @author Saukiya
 */
public class Health extends SubAttribute {

    public static final String MC_ATTRIBUTE_NAME = "health";
    public static final NamespacedKey MC_ATTRIBUTE_KEY = new NamespacedKey(SXAttribute.getInst(), MC_ATTRIBUTE_NAME);
    /**
     * 哈哈哈我用随机 UUID 发生器生成的, 够随机吧
     */
    public static final UUID MC_ATTRIBUTE_ID = UUID.fromString("fedfdec8-7baa-47bc-80cd-5665a187da52");
    /**
     * 用来移除 AttributeModifier 的 AttributeModifier...
     * 仅用到它的 id.
     */
    public static final AttributeModifier MC_ATTRIBUTE_MODIFIER_REMOVER = new AttributeModifier(MC_ATTRIBUTE_ID, MC_ATTRIBUTE_NAME, 0, AttributeModifier.Operation.ADD_NUMBER);

    private boolean skillAPI = false;

    private boolean healthScaled = false;

    private int healthScaledValue = 40;

    /**
     * 生命
     * double[0] 生命值
     */
    public Health() {
        super(SXAttribute.getInst(), 1, AttributeType.UPDATE);
    }

    @Override
    protected YamlConfiguration defaultConfig(YamlConfiguration config) {
        config.set("Health.DiscernName", "生命上限");
        config.set("Health.CombatPower", 1);
        config.set("HealthScaled.Enabled", true);
        config.set("HealthScaled.Value", 40);
        return config;
    }

    @Override
    public void eventMethod(double[] values, EventData eventData) {
        if (eventData instanceof UpdateData && ((UpdateData) eventData).getEntity() instanceof Player) {
            Player player = (Player) ((UpdateData) eventData).getEntity();
            if (skillAPI) {
                SkillAPI.getPlayerData(player).getAttribute(AttributeManager.HEALTH);
            }
            double maxHealth = values[0] + getSkillAPIHealth(player);
//            if (player.getHealth() > maxHealth) player.setHealth(maxHealth);
            if (SXAttribute.getVersionSplit()[1] > 8) {
                AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                assert attribute != null : "Player '" + player.getUniqueId() + "' doesn't have attribute '" + Attribute.GENERIC_MAX_HEALTH + "'";
                double baseValue = attribute.getBaseValue();
                double mod = maxHealth - getDefaultValue();
                attribute.removeModifier(MC_ATTRIBUTE_MODIFIER_REMOVER);
                if (mod != 0) {
                    AttributeModifier modifier = new AttributeModifier(MC_ATTRIBUTE_ID, MC_ATTRIBUTE_NAME, mod, AttributeModifier.Operation.ADD_NUMBER);
                    attribute.addModifier(modifier);
                }
//                attribute.setBaseValue(maxHealth);
            } else {
                player.setMaxHealth(maxHealth);
            }
            if (healthScaled && healthScaledValue < SXAttribute.getApi().getMaxHealth(player)) {
                player.setHealthScaled(true);
                player.setHealthScale(healthScaledValue);
            } else {
                player.setHealthScaled(false);
            }
        }
    }

    private int getSkillAPIHealth(Player player) {
        return skillAPI ? SkillAPI.getPlayerData(player).getClasses().stream().mapToInt(aClass -> (int) aClass.getHealth()).sum() : 0;
    }

    @Override
    public void onEnable() {
        healthScaled = config().getBoolean("HealthScaled.Enabled");
        healthScaledValue = config().getInt("HealthScaled.Value", 40);
        skillAPI = Bukkit.getPluginManager().getPlugin("SkillAPI") != null;
    }

    @Override
    public void onReLoad() {
        healthScaled = config().getBoolean("HealthScaled.Enabled");
        healthScaledValue = config().getInt("HealthScaled.Value", 40);
    }

    @Override
    public Object getPlaceholder(double[] values, Player player, String string) {
        switch (string) {
            case "MaxHealth":
                return SXAttribute.getApi().getMaxHealth(player);
            case "Health":
                return player.getHealth();
            case "HealthValue":
                return values[0];
            default:
                return null;
        }
    }

    @Override
    public List<String> getPlaceholders() {
        return Arrays.asList(
                "MaxHealth",
                "Health",
                "HealthValue"
        );
    }

    @Override
    public void loadAttribute(double[] values, String lore) {
        if (lore.contains(getString("Health.DiscernName"))) {
            values[0] += getNumber(lore);
        }
    }

    @Override
    public void correct(double[] values) {
        values[0] = Math.max(values[0], 1D);
        values[0] = Math.min(values[0], SpigotConfig.maxHealth);
    }

    @Override
    public double calculationCombatPower(double[] values) {
        return values[0] * config().getInt("Health.CombatPower");
    }
}
