package com.minecolonies.api.configuration;

import com.minecolonies.api.colony.permissions.Explosions;
import com.minecolonies.api.configuration.builders.ConfigSpecBuilder;
import com.minecolonies.api.configuration.builders.IConfigBuilder;
import com.minecolonies.api.configuration.builders.ValueHolder;
import com.minecolonies.api.util.constant.CitizenConstants;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

import static com.minecolonies.api.util.constant.Constants.*;

/**
 * Mod server configuration. Loaded serverside, synced on connection.
 */
public class ServerConfiguration
{
    /*  --------------------------------------------------------------------------- *
     *  ------------------- ######## Gameplay settings ######## ------------------- *
     *  --------------------------------------------------------------------------- */

    public ValueHolder<Integer> initialCitizenAmount;
    public ValueHolder<Boolean> allowInfiniteSupplyChests;
    public ValueHolder<Boolean> allowInfiniteColonies;
    public ValueHolder<Boolean> allowOtherDimColonies;
    public ValueHolder<Integer> maxCitizenPerColony;
    public ValueHolder<Boolean> enableInDevelopmentFeatures;
    public ValueHolder<Boolean> alwaysRenderNameTag;
    public ValueHolder<Boolean> workersAlwaysWorkInRain;
    public ValueHolder<Integer> luckyBlockChance;
    public ValueHolder<Integer> minThLevelToTeleport;
    public ValueHolder<Double>  foodModifier;
    public ValueHolder<Integer> diseaseModifier;
    public ValueHolder<Boolean> forceLoadColony;
    public ValueHolder<Integer> loadtime;
    public ValueHolder<Integer> colonyLoadStrictness;
    public ValueHolder<Integer> maxTreeSize;
    public ValueHolder<Boolean> noSupplyPlacementRestrictions;
    public ValueHolder<Boolean> skyRaiders;

    /*  --------------------------------------------------------------------------- *
     *  ------------------- ######## Research settings ######## ------------------- *
     *  --------------------------------------------------------------------------- */
    public ValueHolder<Boolean>                researchCreativeCompletion;
    public ValueHolder<Boolean>                researchDebugLog;
    public ValueHolder<List<? extends String>> researchResetCost;

    /*  --------------------------------------------------------------------------- *
     *  ------------------- ######## Command settings ######## ------------------- *
     *  --------------------------------------------------------------------------- */

    public ValueHolder<Boolean> canPlayerUseRTPCommand;
    public ValueHolder<Boolean> canPlayerUseColonyTPCommand;
    public ValueHolder<Boolean> canPlayerUseAllyTHTeleport;
    public ValueHolder<Boolean> canPlayerUseHomeTPCommand;
    public ValueHolder<Boolean> canPlayerUseShowColonyInfoCommand;
    public ValueHolder<Boolean> canPlayerUseKillCitizensCommand;
    public ValueHolder<Boolean> canPlayerUseAddOfficerCommand;
    public ValueHolder<Boolean> canPlayerUseDeleteColonyCommand;
    public ValueHolder<Boolean> canPlayerUseResetCommand;

    /*  --------------------------------------------------------------------------- *
     *  ------------------- ######## Claim settings ######## ------------------- *
     *  --------------------------------------------------------------------------- */

    public ValueHolder<Integer> maxColonySize;
    public ValueHolder<Integer> minColonyDistance;
    public ValueHolder<Integer> initialColonySize;
    public ValueHolder<Integer> maxDistanceFromWorldSpawn;
    public ValueHolder<Integer> minDistanceFromWorldSpawn;

    /*  ------------------------------------------------------------------------- *
     *  ------------------- ######## Combat Settings ######## ------------------- *
     *  ------------------------------------------------------------------------- */

    public ValueHolder<Boolean> enableColonyRaids;
    public ValueHolder<Integer> raidDifficulty;
    public ValueHolder<Integer> maxRaiders;
    public ValueHolder<Boolean> raidersbreakblocks;
    public ValueHolder<Integer> averageNumberOfNightsBetweenRaids;
    public ValueHolder<Integer> minimumNumberOfNightsBetweenRaids;
    public ValueHolder<Boolean> raidersbreakdoors;
    public ValueHolder<Boolean> mobAttackCitizens;
    public ValueHolder<Double>  guardDamageMultiplier;
    public ValueHolder<Double>  guardHealthMult;
    public ValueHolder<Boolean> pvp_mode;

    /*  ----------------------------------------------------------------------------- *
     *  ------------------- ######## Permission Settings ######## ------------------- *
     *  ----------------------------------------------------------------------------- */

    public ValueHolder<Boolean>    enableColonyProtection;
    public ValueHolder<Explosions> turnOffExplosionsInColonies;

    /*  -------------------------------------------------------------------------------- *
     *  ------------------- ######## Compatibility Settings ######## ------------------- *
     *  -------------------------------------------------------------------------------- */

    public ValueHolder<Boolean> auditCraftingTags;
    public ValueHolder<Boolean> debugInventories;
    public ValueHolder<Boolean> blueprintBuildMode;

    /*  ------------------------------------------------------------------------------ *
     *  ------------------- ######## Pathfinding Settings ######## ------------------- *
     *  ------------------------------------------------------------------------------ */

    public ValueHolder<Integer> pathfindingDebugVerbosity;
    public ValueHolder<Integer> pathfindingMaxThreadCount;
    public ValueHolder<Integer> minimumRailsToPath;

    /*  --------------------------------------------------------------------------------- *
     *  ------------------- ######## Request System Settings ######## ------------------- *
     *  --------------------------------------------------------------------------------- */

    public ValueHolder<Boolean> creativeResolve;

    /**
     * Builds server configuration.
     *
     * @param builder config builder
     */
    public ServerConfiguration(final IConfigBuilder builder)
    {
        builder.createCategory("gameplay", gameplay -> {
            initialCitizenAmount = gameplay.defineInteger("initialcitizenamount", 4, 1, 10);
            allowInfiniteSupplyChests = gameplay.defineBoolean("allowinfinitesupplychests", false);
            allowInfiniteColonies = gameplay.defineBoolean("allowinfinitecolonies", false);
            allowOtherDimColonies = gameplay.defineBoolean("allowotherdimcolonies", true);
            maxCitizenPerColony = gameplay.defineInteger("maxcitizenpercolony", 250, 30, CitizenConstants.CITIZEN_LIMIT_MAX);
            enableInDevelopmentFeatures = gameplay.defineBoolean("enableindevelopmentfeatures", false);
            alwaysRenderNameTag = gameplay.defineBoolean("alwaysrendernametag", true);
            workersAlwaysWorkInRain = gameplay.defineBoolean("workersalwaysworkinrain", false);
            luckyBlockChance = gameplay.defineInteger("luckyblockchance", 1, 0, 100);
            minThLevelToTeleport = gameplay.defineInteger("minthleveltoteleport", 3, 0, 5);
            foodModifier = gameplay.defineDouble("foodmodifier", 1.0, 0.1, 100);
            diseaseModifier = gameplay.defineInteger("diseasemodifier", 5, 1, 100);
            forceLoadColony = gameplay.defineBoolean("forceloadcolony", true);
            loadtime = gameplay.defineInteger("loadtime", 10, 1, 1440);
            colonyLoadStrictness = gameplay.defineInteger("colonyloadstrictness", 3, 1, 15);
            maxTreeSize = gameplay.defineInteger("maxtreesize", 400, 1, 1000);
            noSupplyPlacementRestrictions = gameplay.defineBoolean("nosupplyplacementrestrictions", false);
            skyRaiders = gameplay.defineBoolean("skyraiders", false);
        });

        builder.createCategory("research", research -> {
            researchCreativeCompletion = research.defineBoolean("researchcreativecompletion", true);
            researchDebugLog = research.defineBoolean("researchdebuglog", false);
            researchResetCost = research.defineList("researchresetcost", List.of("minecolonies:ancienttome:1"), s -> s instanceof String);
        });

        builder.createCategory("commands", commands -> {
            canPlayerUseRTPCommand = commands.defineBoolean("canplayerusertpcommand", false);
            canPlayerUseColonyTPCommand = commands.defineBoolean("canplayerusecolonytpcommand", false);
            canPlayerUseAllyTHTeleport = commands.defineBoolean("canplayeruseallytownhallteleport", true);
            canPlayerUseHomeTPCommand = commands.defineBoolean("canplayerusehometpcommand", false);
            canPlayerUseShowColonyInfoCommand = commands.defineBoolean("canplayeruseshowcolonyinfocommand", true);
            canPlayerUseKillCitizensCommand = commands.defineBoolean("canplayerusekillcitizenscommand", false);
            canPlayerUseAddOfficerCommand = commands.defineBoolean("canplayeruseaddofficercommand", true);
            canPlayerUseDeleteColonyCommand = commands.defineBoolean("canplayerusedeletecolonycommand", false);
            canPlayerUseResetCommand = commands.defineBoolean("canplayeruseresetcommand", false);
        });

        builder.createCategory("claims", claims -> {
            maxColonySize = claims.defineInteger("maxColonySize", 20, 1, 250);
            minColonyDistance = claims.defineInteger("minColonyDistance", 8, 1, 200);
            initialColonySize = claims.defineInteger("initialColonySize", 4, 1, 15);
            maxDistanceFromWorldSpawn = claims.defineInteger("maxdistancefromworldspawn", 30000, 1000, Integer.MAX_VALUE);
            minDistanceFromWorldSpawn = claims.defineInteger("mindistancefromworldspawn", 0, 0, 1000);
        });

        builder.createCategory("combat", combat -> {
            enableColonyRaids = combat.defineBoolean("dobarbariansspawn", true);
            raidDifficulty = combat.defineInteger("barbarianhordedifficulty", DEFAULT_BARBARIAN_DIFFICULTY, MIN_BARBARIAN_DIFFICULTY, MAX_BARBARIAN_DIFFICULTY);
            maxRaiders = combat.defineInteger("maxBarbarianSize", 80, MIN_BARBARIAN_HORDE_SIZE, MAX_BARBARIAN_HORDE_SIZE);
            raidersbreakblocks = combat.defineBoolean("dobarbariansbreakthroughwalls", true);
            averageNumberOfNightsBetweenRaids = combat.defineInteger("averagenumberofnightsbetweenraids", 14, 1, 50);
            minimumNumberOfNightsBetweenRaids = combat.defineInteger("minimumnumberofnightsbetweenraids", 10, 1, 30);
            mobAttackCitizens = combat.defineBoolean("mobattackcitizens", true);
            raidersbreakdoors = combat.defineBoolean("shouldraiderbreakdoors", true);
            guardDamageMultiplier = combat.defineDouble("guardDamageMultiplier", 1.0, 0.1, 15.0);
            guardHealthMult = combat.defineDouble("guardhealthmult", 1.0, 0.1, 5.0);
            pvp_mode = combat.defineBoolean("pvp_mode", false);
        });

        builder.createCategory("permissions", permissions -> {
            enableColonyProtection = permissions.defineBoolean("enablecolonyprotection", true);
            turnOffExplosionsInColonies = permissions.defineEnum("turnoffexplosionsincolonies", Explosions.DAMAGE_ENTITIES);
        });

        builder.createCategory("compatibility", compatibility -> {
            auditCraftingTags = compatibility.defineBoolean("auditcraftingtags", false);
            debugInventories = compatibility.defineBoolean("debuginventories", false);
            blueprintBuildMode = compatibility.defineBoolean("blueprintbuildmode", false);
        });

        builder.createCategory("pathfinding", pathfinding -> {
            pathfindingDebugVerbosity = pathfinding.defineInteger("pathfindingdebugverbosity", 0, 0, 10);
            minimumRailsToPath = pathfinding.defineInteger("minimumrailstopath", 8, 5, 100);
            pathfindingMaxThreadCount = pathfinding.defineInteger("pathfindingmaxthreadcount", 1, 1, 10);
        });

        builder.createCategory("requestSystem", requestSystem -> creativeResolve = requestSystem.defineBoolean("creativeresolve", false));
    }

    /**
     * Generate the configuration for a Forge configuration builder.
     *
     * @param builder the Forge configuration spec builder.
     * @return the finalized configuration instance.
     */
    public static ServerConfiguration forConfigBuilder(final ForgeConfigSpec.Builder builder)
    {
        return new ServerConfiguration(new ConfigSpecBuilder(builder));
    }
}
