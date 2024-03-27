package data.scripts.Combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.util.*;

public class BackgroundFleetSelector
{
    public static float BASE_QUALITY_WHEN_NO_MARKET = 0.5f;

    public static int FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS = 50;

    public static int [][] BASE_COUNTS_WITH_4 = new int [][]
            {{9, 4, 2, 0},
                    {7, 4, 2, 0},
                    {4, 3, 3, 0},
                    {1, 1, 1, 0},
                    {1, 1, 1, 1},
            };
    public static int [][] MAX_EXTRA_WITH_4 = new int [][]
            {{3, 2, 1, 1},
                    {2, 2, 2, 1},
                    {2, 2, 2, 1},
                    {2, 2, 2, 3},
                    {1, 1, 1, 1},
            };

    public static int [][] BASE_COUNTS_WITH_3 = new int [][]
            {{6, 2, 1},
                    {4, 2, 1},
                    {3, 2, 1},
                    {1, 1, 1},
                    {1, 1, 1},
            };
    public static int [][] MAX_EXTRA_WITH_3 = new int [][]
            {{2, 0, 0},
                    {2, 1, 0},
                    {2, 2, 0},
                    {2, 2, 0},
                    {1, 1, 0},
            };

    public static Logger log = Global.getLogger(BackgroundFleetSelector.class);

    public static float getNumShipsMultForMarketSize(float marketSize) {
        if (marketSize < 3) marketSize = 3;

        switch ((int)marketSize) {
            case 3: return 0.5f;
            case 4: return 0.75f;
            case 5: return 1f;
            case 6: return 1.25f;
            case 7: return 1.5f;
            case 8: return 1.75f;
            case 9: return 2f;
            case 10: return 2.5f;
        }

        return marketSize / 6f;
    }
    public static float getDoctrineNumShipsMult(int doctrineNumShips) {
        float max = Global.getSettings().getFloat("maxDoctrineNumShipsMult");

        return 1f + (float) (doctrineNumShips - 1f) * (max - 1f) / 4f;
    }

    public static List<ShipVariantAPI> createFleet(FleetParamsV3 params, FactionAPI faction) {

        // Not sure how to use custom Fleet Plugins for this without spawning dummy fleets and destroying them.
//        CreateFleetPlugin plugin = Global.getSector().getGenericPlugins().pickPlugin(CreateFleetPlugin.class, params);
//        if (plugin != null) {
//            return plugin.createFleet(params);
//        }

        Global.getSettings().profilerBegin("BackgroundFleetSelector.createFleet()");
        try {

            boolean fakeMarket = false;
            MarketAPI market = pickMarket(params);
            if (market == null) {
                market = Global.getFactory().createMarket("fake", "fake", 5);
                market.getStability().modifyFlat("fake", 10000);
                market.setFactionId(params.factionId);
                SectorEntityToken token = Global.getSector().getHyperspace().createToken(0, 0);
                market.setPrimaryEntity(token);

                market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", BASE_QUALITY_WHEN_NO_MARKET);

                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 1f);

                fakeMarket = true;
            }
            boolean sourceWasNull = params.source == null;
            params.source = market;
            if (sourceWasNull && params.qualityOverride == null) { // we picked a nearby market based on location
                params.updateQualityAndProducerFromSourceMarket();
            }

            String factionId = params.factionId;
            if (factionId == null) factionId = params.source.getFactionId();

            FactionAPI.ShipPickMode mode = Misc.getShipPickMode(market, factionId);
            if (params.modeOverride != null) mode = params.modeOverride;

            List<ShipVariantAPI> fleetShips = new ArrayList<>();

            FactionDoctrineAPI doctrine = faction.getDoctrine();
            if (params.doctrineOverride != null) {
                doctrine = params.doctrineOverride;
            }

            float numShipsMult = 1f;
            if (params.ignoreMarketFleetSizeMult == null || !params.ignoreMarketFleetSizeMult) {
                numShipsMult = market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
            }

            float quality = params.quality + params.qualityMod;
            if (params.qualityOverride != null) {
                quality = params.qualityOverride;
            }

            Random random = new Random();
            if (params.random != null) random = params.random;

            //Misc.setSpawnFPMult(fleet, numShipsMult);

            float combatPts = params.combatPts * numShipsMult;

            if (params.onlyApplyFleetSizeToCombatShips != null && params.onlyApplyFleetSizeToCombatShips) {
                numShipsMult = 1f;
            }

            float freighterPts = params.freighterPts * numShipsMult;
            float tankerPts = params.tankerPts * numShipsMult;
            float transportPts = params.transportPts * numShipsMult;
            float linerPts = params.linerPts * numShipsMult;
            float utilityPts = params.utilityPts * numShipsMult;



            if (combatPts < 10 && combatPts > 0) {
                combatPts = Math.max(combatPts, 5 + random.nextInt(6));
            }

            float dW = (float) doctrine.getWarships() + random.nextInt(3) - 2;
            float dC = (float) doctrine.getCarriers() + random.nextInt(3) - 2;
            float dP = (float) doctrine.getPhaseShips() + random.nextInt(3) - 2;

            boolean strict = doctrine.isStrictComposition();
            if (strict) {
                dW = (float) doctrine.getWarships() - 1;
                dC = (float) doctrine.getCarriers() - 1;
                dP = (float) doctrine.getPhaseShips() -1;
            }

            if (!strict) {
                float r1 = random.nextFloat();
                float r2 = random.nextFloat();
                float min = Math.min(r1, r2);
                float max = Math.max(r1, r2);

                float mag = 1f;
                float v1 = min;
                float v2 = max - min;
                float v3 = 1f - max;

                v1 *= mag;
                v2 *= mag;
                v3 *= mag;

                v1 -= mag/3f;
                v2 -= mag/3f;
                v3 -= mag/3f;

                //System.out.println(v1 + "," + v2 + "," + v3);
                dW += v1;
                dC += v2;
                dP += v3;
            }

            if (doctrine.getWarships() <= 0) dW = 0;
            if (doctrine.getCarriers() <= 0) dC = 0;
            if (doctrine.getPhaseShips() <= 0) dP = 0;

            boolean banPhaseShipsEtc = !faction.isPlayerFaction() &&
                    combatPts < FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS;
            if (params.forceAllowPhaseShipsEtc != null && params.forceAllowPhaseShipsEtc) {
                banPhaseShipsEtc = !params.forceAllowPhaseShipsEtc;
            }

            params.mode = mode;
            params.banPhaseShipsEtc = banPhaseShipsEtc;

            if (dW < 0) dW = 0;
            if (dC < 0) dC = 0;
            if (dP < 0) dP = 0;

            float extra = 7 - (dC + dP + dW);
            if (extra < 0) extra = 0f;
            if (doctrine.getWarships() > doctrine.getCarriers() && doctrine.getWarships() > doctrine.getPhaseShips()) {
                dW += extra;
            } else if (doctrine.getCarriers() > doctrine.getWarships() && doctrine.getCarriers() > doctrine.getPhaseShips()) {
                dC += extra;
            } else if (doctrine.getPhaseShips() > doctrine.getWarships() && doctrine.getPhaseShips() > doctrine.getCarriers()) {
                dP += extra;
            }

            float doctrineTotal = dW + dC + dP;

            combatPts = (int) combatPts;
            int warships = (int) (combatPts * dW / doctrineTotal);
            int carriers = (int) (combatPts * dC / doctrineTotal);
            int phase = (int) (combatPts * dP / doctrineTotal);

            warships += (combatPts - warships - carriers - phase);


            if (params.treatCombatFreighterSettingAsFraction != null && params.treatCombatFreighterSettingAsFraction) {
                float combatFreighters = (int) Math.min(freighterPts * 1.5f, warships * 1.5f) * doctrine.getCombatFreighterProbability();
                float added = addCombatFreighterFleetPoints(fleetShips, faction, random, combatFreighters, params);
                freighterPts -= added * 0.5f;
                warships -= added * 0.5f;
            } else if (freighterPts > 0 && random.nextFloat() < doctrine.getCombatFreighterProbability()) {
                float combatFreighters = (int) Math.min(freighterPts * 1.5f, warships * 1.5f);
                float added = addCombatFreighterFleetPoints(fleetShips, faction, random, combatFreighters, params);
                freighterPts -= added * 0.5f;
                warships -= added * 0.5f;
            }

            addCombatFleetPoints(fleetShips, faction, random, warships, carriers, phase, params);

            addFreighterFleetPoints(fleetShips, faction, random, freighterPts, params);
            addTankerFleetPoints(fleetShips, faction, random, tankerPts, params);
            addTransportFleetPoints(fleetShips, faction, random, transportPts, params);
            addLinerFleetPoints(fleetShips, faction, random, linerPts, params);
            addUtilityFleetPoints(fleetShips, faction, random, utilityPts, params);

            int maxShips = Global.getSettings().getInt("maxShipsInAIFleet");
            if (params.maxNumShips != null) {
                maxShips = params.maxNumShips;
            }
            if (fleetShips.size() > maxShips) {
                if (params.doNotPrune == null || !params.doNotPrune) {
                    float targetFP = getFP(fleetShips);
                    if (params.doNotAddShipsBeforePruning == null || !params.doNotAddShipsBeforePruning) {
                        sizeOverride = 5;
                        addCombatFleetPoints(fleetShips, faction, random, warships, carriers, phase, params);
                        addFreighterFleetPoints(fleetShips, faction, random, freighterPts, params);
                        addTankerFleetPoints(fleetShips, faction, random, tankerPts, params);
                        addTransportFleetPoints(fleetShips, faction, random, transportPts, params);
                        addLinerFleetPoints(fleetShips, faction, random, linerPts, params);
                        addUtilityFleetPoints(fleetShips, faction, random, utilityPts, params);
                        sizeOverride = 0;
                    }

                    int size = doctrine.getShipSize();
                    pruneFleet(maxShips, size, fleetShips, targetFP, random);
                }
            }

            if (fakeMarket) {
                params.source = null;
            }

            return fleetShips;

        } finally {
            Global.getSettings().profilerEnd();
        }
    }

    public static void pruneFleet(int maxShips, int doctrineSize, List<ShipVariantAPI> fleet, float targetFP, Random random) {
        float combatFP = 0;
        float civFP = 0;

        List<ShipVariantAPI> copy = new ArrayList<ShipVariantAPI>(fleet);
        List<ShipVariantAPI> combat = new ArrayList<ShipVariantAPI>();
        //List<FleetMemberAPI> civ = new ArrayList<FleetMemberAPI>();
        List<ShipVariantAPI> tanker = new ArrayList<ShipVariantAPI>();
        List<ShipVariantAPI> freighter = new ArrayList<ShipVariantAPI>();
        List<ShipVariantAPI> liner = new ArrayList<ShipVariantAPI>();
        List<ShipVariantAPI> other = new ArrayList<ShipVariantAPI>();

        for (ShipVariantAPI member : copy) {
            if (member.isCivilian()) {
                civFP += member.getHullSpec().getFleetPoints();
                //civ.add(member);

                if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.FREIGHTER)) {
                    freighter.add(member);
                } else if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.TANKER)) {
                    tanker.add(member);
                } else if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.TRANSPORT) ||
                        member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.LINER)) {
                    liner.add(member);
                } else {
                    other.add(member);
                }

            } else {
                combatFP += member.getHullSpec().getFleetPoints();
                combat.add(member);
            }
        }
        if (civFP < 1) civFP = 1;
        if (combatFP < 1) combatFP = 1;

        int keepCombat = (int) ((float)maxShips * combatFP / (civFP + combatFP));
        int keepCiv = maxShips - keepCombat;
        if (civFP > 10 && keepCiv < 2) {
            keepCiv = 2;
            if (!freighter.isEmpty()) keepCiv++;
            if (!tanker.isEmpty()) keepCiv++;
            if (!liner.isEmpty()) keepCiv++;
            if (!other.isEmpty()) keepCiv++;

            keepCiv = maxShips - keepCiv;
        }

        float f = 0, t = 0, l = 0, o = 0;
        float total = freighter.size() + tanker.size() + liner.size() + other.size();
        if (total < 1) total = 1;

        f = (float) freighter.size() / total;
        t = (float) tanker.size() / total;
        l = (float) liner.size() / total;
        o = (float) other.size() / total;

        f *= keepCiv;
        t *= keepCiv;
        l *= keepCiv;
        o *= keepCiv;

        if (f > 0) f = Math.round(f);
        if (t > 0) t = Math.round(t);
        if (l > 0) l = Math.round(l);
        if (o > 0) o = Math.round(o);

        if (freighter.size() > 0 && f < 1) f = 1;
        if (tanker.size() > 0 && t < 1) t = 1;
        if (liner.size() > 0 && l < 1) l = 1;
        if (other.size() > 0 && o < 1) o = 1;

        int extra = (int) ((f + t + l + o) - keepCiv);
        //if (extra < 0) keepCombat += Math.abs(extra);
        if (extra > 0 && o >= 2) {
            extra--;
            o--;
        }
        if (extra > 0 && l >= 2) {
            extra--;
            l--;
        }
        if (extra > 0 && t >= 2) {
            extra--;
            t--;
        }
        if (extra > 0 && f >= 2) {
            extra--;
            f--;
        }

        LinkedHashSet<ShipVariantAPI> keep = new LinkedHashSet<ShipVariantAPI>();

        Comparator<ShipVariantAPI> c = new Comparator<ShipVariantAPI>() {
            public int compare(ShipVariantAPI o1, ShipVariantAPI o2) {
                return o2.getHullSpec().getHullSize().ordinal() - o1.getHullSpec().getHullSize().ordinal();
            }
        };
        Collections.sort(combat, c);
        Collections.sort(freighter, c);
        Collections.sort(tanker, c);
        Collections.sort(liner, c);
        Collections.sort(other, c);

        int [] ratio = new int [] { 4, 2, 1, 1 };

        addAll(ratio, combat, keep, keepCombat, random);

        addAll(ratio, freighter, keep, (int)f, random);
        addAll(ratio, tanker, keep, (int)t, random);
        addAll(ratio, liner, keep, (int)l, random);
        addAll(ratio, other, keep, (int)o, random); // adds a Hermes since that's "other" but we don't really care

        for (ShipVariantAPI member : copy) {
            if (!keep.contains(member)) {
                fleet.remove(member);
            }
        }

        float currFP = getFP(fleet);
        if (currFP > targetFP) {
            // Custom sort comparison.
            Collections.sort(fleet, new Comparator<ShipVariantAPI>() {
                @Override
                public int compare(ShipVariantAPI o1, ShipVariantAPI o2) {
                    if(o1.getHullSpec().getFleetPoints() > o2.getHullSpec().getFleetPoints())
                        return -1;
                    else if(o1.getHullSpec().getFleetPoints() > o2.getHullSpec().getFleetPoints())
                        return 1;
                    else
                        return 0;
                }
            });
            copy = new ArrayList<>(fleet);
            //Collections.reverse(copy);
            //Collections.shuffle(copy, random);
            for (int i = 0; i < copy.size()/2; i+=2) {
                ShipVariantAPI f1 = copy.get(i);
                ShipVariantAPI f2 = copy.get(copy.size() - 1 - i);
                copy.set(i, f2);
                copy.set(copy.size() - 1 - i, f1);
            }

            float fpGoal = currFP - targetFP;
            float fpDone = 0;
            for (ShipVariantAPI curr : copy) {
                if (curr.isCivilian()) continue;
                ShipVariantAPI best = null;
                float bestDiff = 0f;
                for (ShipVariantAPI replace : combat) {
                    float fpCurr = curr.getHullSpec().getFleetPoints();
                    float fpReplace = replace.getHullSpec().getFleetPoints();
                    if (fpCurr > fpReplace) {
                        float fpDiff = fpCurr - fpReplace;
                        if (fpDone + fpDiff <= fpGoal) {
                            best = replace;
                            bestDiff = fpDiff;
                            break;
                        } else {
                            if (fpDiff < bestDiff) {
                                best = replace;
                                bestDiff = fpDiff;
                            }
                        }
                    }
                }
                if (best != null) {
                    fpDone += bestDiff;
                    combat.remove(best);
                    fleet.remove(curr);
                    fleet.add(best);
                }
                if (fpDone >= fpGoal) {
                    break;
                }
            }

        }

    }

    public static void addAll(int [] ratio, List<ShipVariantAPI> from, LinkedHashSet<ShipVariantAPI> to, int num, Random random) {
        int added = 0;
        if (num <= 5) {
            while (added < num && !from.isEmpty()) {
                to.add(from.remove(0));
                added++;
            }
            return;
        }

        WeightedRandomPicker<ShipAPI.HullSize> picker = makePicker(ratio, random);
        for (int i = 0; i < num; i++) {
            if (picker.isEmpty()) picker = makePicker(ratio, random);
            OUTER: while (!picker.isEmpty()) {
                ShipAPI.HullSize size = picker.pickAndRemove();
                for (ShipVariantAPI member : from) {
                    if (member.getHullSpec().getHullSize() == size) {
                        to.add(member);
                        from.remove(member);
                        added++;
                        break OUTER;
                    }
                }
            }

        }

        // if we failed to add up to num, add the largest ships until we've got num
        // assumes from list is sorted descending by size
        while (added < num && !from.isEmpty()) {
            to.add(from.remove(0));
            added++;
        }

    }

    public static WeightedRandomPicker<ShipAPI.HullSize> makePicker(int [] ratio, Random random) {
        WeightedRandomPicker<ShipAPI.HullSize> picker = new WeightedRandomPicker<ShipAPI.HullSize>(random);
        for (int i = 0; i < ratio[0]; i++) {
            picker.add(ShipAPI.HullSize.CAPITAL_SHIP);
        }
        for (int i = 0; i < ratio[1]; i++) {
            picker.add(ShipAPI.HullSize.CRUISER);
        }
        for (int i = 0; i < ratio[2]; i++) {
            picker.add(ShipAPI.HullSize.DESTROYER);
        }
        for (int i = 0; i < ratio[3]; i++) {
            picker.add(ShipAPI.HullSize.FRIGATE);
        }
        return picker;
    }


    public static int getFP(List<ShipVariantAPI> fleet) {
        int fp = 0;
        for (ShipVariantAPI member : fleet) {
            fp += member.getHullSpec().getFleetPoints();
        }
        return fp;
    }

    public static float getMemberWeight(FleetMemberAPI member) {
        boolean nonCombat = member.getVariant().isCivilian();
        float weight = 0;
        switch (member.getVariant().getHullSize()) {
            case CAPITAL_SHIP: weight += 8; break;
            case CRUISER: weight += 4; break;
            case DESTROYER: weight += 2; break;
            case FRIGATE: weight += 1; break;
            case FIGHTER: weight += 1; break;
        }
        if (nonCombat) weight *= 0.1f;
        return weight;
    }




    public static MarketAPI pickMarket(FleetParamsV3 params) {
        if (params.source != null) return params.source;
        if (params.locInHyper == null) return null;

        List<MarketAPI> allMarkets = Global.getSector().getEconomy().getMarketsCopy();

        int size = getMinPreferredMarketSize(params);
        float distToClosest = Float.MAX_VALUE;
        MarketAPI closest = null;
        float distToClosestMatchingSize = Float.MAX_VALUE;
        MarketAPI closestMatchingSize = null;


        FactionAPI creationFaction = Global.getSector().getFaction(params.factionId);
        boolean independent = Factions.INDEPENDENT.equals(params.factionId) ||
                Factions.SCAVENGERS.equals(params.factionId) ||
                creationFaction.getCustomBoolean(Factions.CUSTOM_SPAWNS_AS_INDEPENDENT);

        for (MarketAPI market : allMarkets) {
            if (market.getPrimaryEntity() == null) continue;

            if (independent) {
                boolean hostileToIndependent = market.getFaction().isHostileTo(Factions.INDEPENDENT);
                if (hostileToIndependent) continue;
            } else {
                if (!market.getFactionId().equals(params.factionId)) continue;
            }

            float currDist = Misc.getDistance(market.getPrimaryEntity().getLocationInHyperspace(),
                    params.locInHyper);
            if (currDist < distToClosest) {
                distToClosest = currDist;
                closest = market;
            }

            if (market.getSize() >= size && currDist < distToClosestMatchingSize) {
                distToClosestMatchingSize = currDist;
                closestMatchingSize = market;
            }
        }

        if (closestMatchingSize != null) {
            return closestMatchingSize;
        }

        if (closest != null) {
            return closest;
        }

//		MarketAPI temp = Global.getFactory().createMarket("temp", "Temp", size);
//		temp.setFactionId(params.factionId);
//		return temp;
        return null;
    }

    public static int getMinPreferredMarketSize(FleetParamsV3 params) {
        float fp = params.getTotalPts();

        if (fp <= 20) return 1;
        if (fp <= 50) return 3;
        if (fp <= 100) return 5;
        if (fp <= 150) return 7;

        return 8;
    }

    public static class FPRemaining {
        public int fp;

        public FPRemaining(int fp) {
            this.fp = fp;
        }
        public FPRemaining() {
        }
    }

    public static float addToFleet(String role, MarketAPI market, Random random, List<ShipVariantAPI> fleet, FactionAPI faction, int maxFP, FleetParamsV3 params) {
        float total = 0f;
        List<ShipRolePick> picks = market.pickShipsForRole(role, faction.getId(),
                new FactionAPI.ShipPickParams(params.mode, maxFP, params.timestamp, params.blockFallback), random, null);
        for (ShipRolePick pick : picks) {
            total += addToFleet(pick, fleet, random);
        }
        return total;
    }

    protected static float addToFleet(ShipRolePick pick, List<ShipVariantAPI> fleet, Random random) {
        ShipVariantAPI variant = Global.getSettings().getVariant(pick.variantId);
        fleet.add(variant);
        return variant.getHullSpec().getFleetPoints();
    }

    public static boolean addShips(String role, int count, MarketAPI market, Random random, List<ShipVariantAPI> fleet, FactionAPI faction, FPRemaining rem, FleetParamsV3 params) {
        boolean addedSomething = false;
        for (int i = 0; i < count; i++) {
            if (rem.fp <= 0) break;
            float added = addToFleet(role, market, random, fleet, faction, rem.fp, params);
            if (added > 0) {
                rem.fp -= added;
                addedSomething = true;
            }
        }
        return addedSomething;
    }


    public static float addPhaseFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.SMALL_IS_FRIGATE,
                ShipRoles.PHASE_SMALL, ShipRoles.PHASE_MEDIUM, ShipRoles.PHASE_LARGE);
    }

    public static enum SizeFilterMode {
        NONE,
        SMALL_IS_FRIGATE,
        SMALL_IS_DESTROYER,
    }
    public static float addCarrierFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.SMALL_IS_DESTROYER,
                ShipRoles.CARRIER_SMALL, ShipRoles.CARRIER_MEDIUM, ShipRoles.CARRIER_LARGE);
    }
    public static float addPriorityOnlyThenAll(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params,
                                               BackgroundFleetSelector.SizeFilterMode sizeFilterMode,
                                               String roleSmall, String roleMedium, String roleLarge) {
        if (fp <= 0) return 0f;

        float added = 0f;
        if (params.mode == FactionAPI.ShipPickMode.PRIORITY_THEN_ALL) {
            int numPriority = faction.getNumAvailableForRole(roleSmall, FactionAPI.ShipPickMode.PRIORITY_ONLY) +
                    faction.getNumAvailableForRole(roleMedium, FactionAPI.ShipPickMode.PRIORITY_ONLY) +
                    faction.getNumAvailableForRole(roleLarge, FactionAPI.ShipPickMode.PRIORITY_ONLY);

            if (numPriority > 0) {
                params.mode = FactionAPI.ShipPickMode.PRIORITY_ONLY;
                added = addFleetPoints(fleet, faction, random, fp, params, sizeFilterMode,
                        roleSmall, roleMedium, roleLarge);
                params.mode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
            } else {
                params.mode = FactionAPI.ShipPickMode.ALL;
                added = addFleetPoints(fleet, faction, random, fp, params, sizeFilterMode,
                        roleSmall, roleMedium, roleLarge);
                params.mode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
            }
            // if there ARE priority ships for a 3-type category (i.e. carriers/phases/various civs,
            // then ONLY use priority, and use nothing if a priority ship was not added (since that just means not enough FP
            // for likely a smaller fleet.)
//			if (added <= 0) {
//				added = addFleetPoints(fleet, random, fp, params, sizeFilterMode,
//						roleSmall, roleMedium, roleLarge);
//			}
        } else {
            added = addFleetPoints(fleet, faction, random, fp, params, sizeFilterMode,
                    roleSmall, roleMedium, roleLarge);
        }
        return added;
    }

    public static float addTankerFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.SMALL_IS_DESTROYER,
                ShipRoles.TANKER_SMALL, ShipRoles.TANKER_MEDIUM, ShipRoles.TANKER_LARGE);
    }

    public static float addFreighterFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.NONE,
                ShipRoles.FREIGHTER_SMALL, ShipRoles.FREIGHTER_MEDIUM, ShipRoles.FREIGHTER_LARGE);
    }

    public static float addLinerFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.NONE,
                ShipRoles.LINER_SMALL, ShipRoles.LINER_MEDIUM, ShipRoles.LINER_LARGE);
    }

    public static float addCombatFreighterFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.SMALL_IS_FRIGATE,
                ShipRoles.COMBAT_FREIGHTER_SMALL, ShipRoles.COMBAT_FREIGHTER_MEDIUM, ShipRoles.COMBAT_FREIGHTER_LARGE);
    }

    public static float addTransportFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.NONE,
                ShipRoles.PERSONNEL_SMALL, ShipRoles.PERSONNEL_MEDIUM, ShipRoles.PERSONNEL_LARGE);
    }

    public static float addUtilityFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params) {
        return addPriorityOnlyThenAll(fleet, faction, random, fp, params, BackgroundFleetSelector.SizeFilterMode.NONE,
                ShipRoles.UTILITY, ShipRoles.UTILITY, ShipRoles.UTILITY);
    }


    protected static int sizeOverride = 0;
    // tend towards larger ships as fleets get more members, regardless of doctrine
    public static int getAdjustedDoctrineSize(int size, List<ShipVariantAPI> fleetSoFar) {
        if (sizeOverride > 0) return sizeOverride;
        else return size;
    }


    public static float addFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random, float fp, FleetParamsV3 params,
                                       BackgroundFleetSelector.SizeFilterMode sizeFilterMode,
                                       String ... roles) {
        FactionDoctrineAPI doctrine = faction.getDoctrine();
        if (params.doctrineOverride != null) {
            doctrine = params.doctrineOverride;
        }

        int size = doctrine.getShipSize();
        //size = getAdjustedDoctrineSize(size, fleet);

        boolean addedSomething = true;
        FPRemaining rem = new FPRemaining();
        rem.fp = (int) fp;

        while (addedSomething && rem.fp > 0) {
            size = getAdjustedDoctrineSize(size, fleet);

            int small = BASE_COUNTS_WITH_3[size - 1][0] + random.nextInt(MAX_EXTRA_WITH_3[size - 1][0] + 1);
            int medium = BASE_COUNTS_WITH_3[size - 1][1] + random.nextInt(MAX_EXTRA_WITH_3[size - 1][1] + 1);
            int large = BASE_COUNTS_WITH_3[size - 1][2] + random.nextInt(MAX_EXTRA_WITH_3[size - 1][2] + 1);

//			if (sizeOverride > 0) {
//				small = 0;
//				medium = 0;
//			}

            if (sizeFilterMode == BackgroundFleetSelector.SizeFilterMode.SMALL_IS_FRIGATE) {
                if (params.maxShipSize <= 1) medium = 0;
                if (params.maxShipSize <= 2) large = 0;
            } else if (sizeFilterMode == BackgroundFleetSelector.SizeFilterMode.SMALL_IS_DESTROYER) {
                if (params.maxShipSize <= 2) medium = 0;
                if (params.maxShipSize <= 3) large = 0;
            }

            //System.out.println(String.format("Small: %s Medium: %s Large: %s Capital: %s",
            //"" + small, "" + medium, "" + large, "" + capital));

            int smallPre = small / 2;
            small -= smallPre;

            int mediumPre = medium / 2;
            medium -= mediumPre;

            addedSomething = false;

            addedSomething |= addShips(roles[0], smallPre, params.source, random, fleet, faction,rem, params);

            addedSomething |= addShips(roles[1], mediumPre, params.source, random, fleet, faction,rem, params);
            addedSomething |= addShips(roles[0], small, params.source, random, fleet, faction,rem, params);

            addedSomething |= addShips(roles[2], large, params.source, random, fleet, faction,rem, params);
            addedSomething |= addShips(roles[1], medium, params.source, random, fleet, faction,rem, params);
        }

        return fp - rem.fp;
    }

    public static void addCombatFleetPoints(List<ShipVariantAPI> fleet, FactionAPI faction, Random random,
                                            float warshipFP, float carrierFP, float phaseFP, FleetParamsV3 params) {

        FactionDoctrineAPI doctrine = faction.getDoctrine();
        if (params.doctrineOverride != null) {
            doctrine = params.doctrineOverride;
        }

        WeightedRandomPicker<String> smallPicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> mediumPicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> largePicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> capitalPicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> priorityCapitalPicker = new WeightedRandomPicker<String>(random);

        String smallRole = ShipRoles.COMBAT_SMALL_FOR_SMALL_FLEET;
        if (!params.banPhaseShipsEtc) {
            smallRole = ShipRoles.COMBAT_SMALL;
        }

//		if (warshipFP > 0) smallPicker.add(smallRole, 1);
//		if (phaseFP > 0) smallPicker.add(ShipRoles.PHASE_SMALL, 1);
//
//		if (warshipFP > 0) mediumPicker.add(ShipRoles.COMBAT_MEDIUM, 1);
//		if (phaseFP > 0) mediumPicker.add(ShipRoles.PHASE_MEDIUM, 1);
//		if (carrierFP > 0) mediumPicker.add(ShipRoles.CARRIER_SMALL, 1);
//
//		if (warshipFP > 0) largePicker.add(ShipRoles.COMBAT_LARGE, 1);
//		if (phaseFP > 0) largePicker.add(ShipRoles.PHASE_LARGE, 1);
//		if (carrierFP > 0) largePicker.add(ShipRoles.CARRIER_MEDIUM, 1);
//
//		if (warshipFP > 0) capitalPicker.add(ShipRoles.COMBAT_CAPITAL, 1);
//		if (phaseFP > 0) capitalPicker.add(ShipRoles.PHASE_CAPITAL, 1);
//		if (carrierFP > 0) capitalPicker.add(ShipRoles.CARRIER_LARGE, 1);

        smallPicker.add(smallRole, warshipFP);
        smallPicker.add(ShipRoles.PHASE_SMALL, phaseFP);

        mediumPicker.add(ShipRoles.COMBAT_MEDIUM, warshipFP);
        mediumPicker.add(ShipRoles.PHASE_MEDIUM, phaseFP);
        mediumPicker.add(ShipRoles.CARRIER_SMALL, carrierFP);

        largePicker.add(ShipRoles.COMBAT_LARGE, warshipFP);
        largePicker.add(ShipRoles.PHASE_LARGE, phaseFP);
        largePicker.add(ShipRoles.CARRIER_MEDIUM, carrierFP);

        capitalPicker.add(ShipRoles.COMBAT_CAPITAL, warshipFP);
        capitalPicker.add(ShipRoles.PHASE_CAPITAL, phaseFP);
        capitalPicker.add(ShipRoles.CARRIER_LARGE, carrierFP);


        Set<String> usePriorityOnly = new HashSet<String>();

        if (params.mode == FactionAPI.ShipPickMode.PRIORITY_THEN_ALL) {
            float num = faction.getVariantWeightForRole(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickMode.PRIORITY_ONLY);
            if (num > 0) {
                //priorityCapitalPicker.add(ShipRoles.COMBAT_CAPITAL, doctrine.getWarships() + 1);
                priorityCapitalPicker.add(ShipRoles.COMBAT_CAPITAL, num);
            }
            num = faction.getVariantWeightForRole(ShipRoles.CARRIER_LARGE, FactionAPI.ShipPickMode.PRIORITY_ONLY);
            if (num > 0) {
                //priorityCapitalPicker.add(ShipRoles.CARRIER_LARGE, doctrine.getCarriers() + 1);
                priorityCapitalPicker.add(ShipRoles.CARRIER_LARGE, num);
            }
            num = faction.getVariantWeightForRole(ShipRoles.PHASE_CAPITAL, FactionAPI.ShipPickMode.PRIORITY_ONLY);
            if (num > 0) {
                //priorityCapitalPicker.add(ShipRoles.PHASE_CAPITAL, doctrine.getPhaseShips() + 1);
                priorityCapitalPicker.add(ShipRoles.PHASE_CAPITAL, num);
            }

            if (params.mode == FactionAPI.ShipPickMode.PRIORITY_THEN_ALL) {
                addToPriorityOnlySet(fleet, faction, usePriorityOnly, ShipRoles.PHASE_SMALL, ShipRoles.PHASE_MEDIUM, ShipRoles.PHASE_LARGE);
                addToPriorityOnlySet(fleet, faction, usePriorityOnly, ShipRoles.CARRIER_SMALL, ShipRoles.CARRIER_MEDIUM, ShipRoles.CARRIER_LARGE);
            }
        }

        Map<String, FPRemaining> remaining = new HashMap<String, FPRemaining>();
        FPRemaining remWarship = new FPRemaining((int)warshipFP);
        FPRemaining remCarrier = new FPRemaining((int)carrierFP);
        FPRemaining remPhase = new FPRemaining((int)phaseFP);

        remaining.put(ShipRoles.COMBAT_SMALL_FOR_SMALL_FLEET, remWarship);
        remaining.put(ShipRoles.COMBAT_SMALL, remWarship);
        remaining.put(ShipRoles.COMBAT_MEDIUM, remWarship);
        remaining.put(ShipRoles.COMBAT_LARGE, remWarship);
        remaining.put(ShipRoles.COMBAT_CAPITAL, remWarship);

        remaining.put(ShipRoles.CARRIER_SMALL, remCarrier);
        remaining.put(ShipRoles.CARRIER_MEDIUM, remCarrier);
        remaining.put(ShipRoles.CARRIER_LARGE, remCarrier);

        remaining.put(ShipRoles.PHASE_SMALL, remPhase);
        remaining.put(ShipRoles.PHASE_MEDIUM, remPhase);
        remaining.put(ShipRoles.PHASE_LARGE, remPhase);
        remaining.put(ShipRoles.PHASE_CAPITAL, remPhase);


        if (params.maxShipSize <= 1) {
            mediumPicker.clear();
        }
        if (params.maxShipSize <= 2) {
            largePicker.clear();
        }
        if (params.maxShipSize <= 3) {
            capitalPicker.clear();
        }

        if (params.minShipSize >= 2) {
            smallPicker.clear();
        }
        if (params.minShipSize >= 3) {
            mediumPicker.clear();
        }
        if (params.minShipSize >= 4) {
            largePicker.clear();
        }


        int size = doctrine.getShipSize();
        //size = getAdjustedDoctrineSize(size, fleet);

        int numFails = 0;
        while (numFails < 2) {
            size = getAdjustedDoctrineSize(size, fleet);

//			if (size > 5) {
//				System.out.println("wefwefe");
//			}

            int small = BASE_COUNTS_WITH_4[size - 1][0] + random.nextInt(MAX_EXTRA_WITH_4[size - 1][0] + 1);
            int medium = BASE_COUNTS_WITH_4[size - 1][1] + random.nextInt(MAX_EXTRA_WITH_4[size - 1][1] + 1);
            int large = BASE_COUNTS_WITH_4[size - 1][2] + random.nextInt(MAX_EXTRA_WITH_4[size - 1][2] + 1);
            int capital = BASE_COUNTS_WITH_4[size - 1][3] + random.nextInt(MAX_EXTRA_WITH_4[size - 1][3] + 1);

            if (size < 5 && capital > 1) {
                capital = 1;
            }

            if (params.maxShipSize <= 1) medium = 0;
            if (params.maxShipSize <= 2) large = 0;
            if (params.maxShipSize <= 3) capital = 0;

            if (params.minShipSize >= 2) small = 0;
            if (params.minShipSize >= 3) medium = 0;
            if (params.minShipSize >= 4) large = 0;

            int smallPre = small / 2;
            small -= smallPre;

            int mediumPre = medium / 2;
            medium -= mediumPre;

            boolean addedSomething = false;

            //System.out.println("Rem carrier pre: " + remCarrier.fp);
            addedSomething |= addShips(smallPicker, usePriorityOnly, remaining, null, smallPre, fleet, faction, random, params);
            //System.out.println("Rem carrier after smallPre: " + remCarrier.fp);
            addedSomething |= addShips(mediumPicker, usePriorityOnly, remaining, null, mediumPre, fleet, faction, random, params);
            //System.out.println("Rem carrier after mediumPre: " + remCarrier.fp);
            addedSomething |= addShips(smallPicker, usePriorityOnly, remaining, null, small, fleet, faction, random, params);
            //System.out.println("Rem carrier after small: " + remCarrier.fp);
            addedSomething |= addShips(largePicker, usePriorityOnly, remaining, null, large, fleet, faction, random, params);
            //System.out.println("Rem carrier after large: " + remCarrier.fp);
            addedSomething |= addShips(mediumPicker, usePriorityOnly, remaining, null, medium, fleet, faction, random, params);
            //System.out.println("Rem carrier after medium: " + remCarrier.fp);


            if (!priorityCapitalPicker.isEmpty()) {
                params.mode = FactionAPI.ShipPickMode.PRIORITY_ONLY;
                params.blockFallback = true;
                FPRemaining combined = new FPRemaining(remWarship.fp + remCarrier.fp + remPhase.fp);
                boolean addedCapital = addShips(priorityCapitalPicker, usePriorityOnly, remaining, combined, capital, fleet, faction, random, params);
                addedSomething |= addedCapital;
                if (addedCapital) {
                    redistributeFP(remWarship, remCarrier, remPhase, combined.fp);
                }
                params.mode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
                params.blockFallback = null;
                //System.out.println("Rem carrier after capitals priority: " + remCarrier.fp);
            } else {
                addedSomething |= addShips(capitalPicker, usePriorityOnly, remaining, null, capital, fleet, faction, random, params);
                //System.out.println("Rem carrier after capitals normal: " + remCarrier.fp);
            }

            if (!addedSomething) {
                numFails++;

                if (numFails == 2) {
                    boolean goAgain = false;
                    if (remPhase.fp > 0) {
                        remWarship.fp += remPhase.fp;
                        remPhase.fp = 0;
                        goAgain = true;
                    }
                    if (remCarrier.fp > 0) {
                        remWarship.fp += remCarrier.fp;
                        remCarrier.fp = 0;
                        goAgain = true;
                    }

                    if (goAgain) {
                        numFails = 0;
                        smallPicker.add(smallRole, 1);
                        mediumPicker.add(ShipRoles.COMBAT_MEDIUM, 1);
                        largePicker.add(ShipRoles.COMBAT_LARGE, 1);
                        capitalPicker.add(ShipRoles.COMBAT_CAPITAL, 1);
                    }
                }
            }
        }
    }

    protected static void addToPriorityOnlySet(List<ShipVariantAPI> fleet, FactionAPI faction, Set<String> set, String small, String medium, String large) {
        int numPriority = faction.getNumAvailableForRole(small, FactionAPI.ShipPickMode.PRIORITY_ONLY) +
                faction.getNumAvailableForRole(medium, FactionAPI.ShipPickMode.PRIORITY_ONLY) +
                faction.getNumAvailableForRole(large, FactionAPI.ShipPickMode.PRIORITY_ONLY);
        if (numPriority > 0) {
            set.add(small);
            set.add(medium);
            set.add(large);
        }
    }

    protected static void redistributeFP(FPRemaining one, FPRemaining two, FPRemaining three, int newTotal) {
        float total = one.fp + two.fp + three.fp;
        if (total <= 0) return;

        int f1 = (int) Math.round((float)one.fp / total * newTotal);
        int f2 = (int) Math.round((float)two.fp / total * newTotal);
        int f3 = (int) Math.round((float)three.fp / total * newTotal);

        f1 += newTotal - f1 - f2 - f3;

        one.fp = f1;
        two.fp = f2;
        three.fp = f3;
    }

    public static boolean addShips(WeightedRandomPicker<String> rolePicker, Set<String> usePriorityOnly, Map<String, FPRemaining> remaining, FPRemaining remOverride, int count,
                                   List<ShipVariantAPI> fleet, FactionAPI faction, Random random, FleetParamsV3 params) {
        if (rolePicker.isEmpty()) return false;

        boolean addedSomething = false;
        for (int i = 0; i < count; i++) {
            String role = rolePicker.pick();
            if (role == null) break;
            FPRemaining rem = remaining.get(role);
            FPRemaining remForProperRole = rem;
            if (remOverride != null) rem = remOverride;
            if (usePriorityOnly.contains(role)) {
                params.mode = FactionAPI.ShipPickMode.PRIORITY_ONLY;
            }
            int fpPrePick = rem.fp;

            boolean added = addShips(role, 1, params.source, random, fleet, faction, rem, params);

            if (added && remOverride != null) {
                int fpSpent = fpPrePick - rem.fp;
                int maxToTakeFromProperRole = Math.min(remForProperRole.fp, fpSpent);
                remForProperRole.fp -= maxToTakeFromProperRole;
            }

            if (usePriorityOnly.contains(role)) {
                params.mode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
            }
            if (!added) {
                rolePicker.remove(role);
                i--;
                if (rolePicker.isEmpty()) {
                    break;
                }
            }
            addedSomething |= added;
        }
        return addedSomething;
    }

}
