package game.campaign;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import game.MissionOption;
import game.PlayerSeat;
import net.structs.GameSessionConfig;

public final class CampaignState implements Serializable {
    private final GameSessionConfig config;
    private final ArrayList<CampaignLogEntry> campaignLog = new ArrayList<>();
    private final ArrayList<MissionOption> activeMissions = new ArrayList<>();
    private final ArrayList<ImperialDeploymentCardState> imperialDeploymentCards = new ArrayList<>();
    private final EnumMap<PlayerSeat, PlayerProgress> playerProgress = new EnumMap<>(PlayerSeat.class);
    private final DeckState supplyDeck = new DeckState("Supply");
    private final DeckState agendaDeck = new DeckState("Agenda");
    private final DeckState imperialClassDeck = new DeckState("Imperial Class");
    private CampaignStage currentStage = CampaignStage.SETUP;
    private int threatLevel;
    private int threatDial;
    private int roundDial = 1;
    private int rebelCredits;
    private int imperialInfluence;

    public CampaignState(GameSessionConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        for (PlayerSeat seat : config.requiredSeats()) {
            playerProgress.put(seat, new PlayerProgress(seat));
        }
    }

    public GameSessionConfig getConfig() {
        return config;
    }

    public CampaignStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(CampaignStage currentStage) {
        this.currentStage = Objects.requireNonNull(currentStage, "currentStage");
    }

    public int getThreatLevel() {
        return threatLevel;
    }

    public void setThreatLevel(int threatLevel) {
        validateNonNegative("threatLevel", threatLevel);
        this.threatLevel = threatLevel;
    }

    public void adjustThreatLevel(int delta) {
        setThreatLevel(threatLevel + delta);
    }

    public int getThreatDial() {
        return threatDial;
    }

    public void setThreatDial(int threatDial) {
        validateNonNegative("threatDial", threatDial);
        this.threatDial = threatDial;
    }

    public void addThreat(int amount) {
        validateNonNegative("amount", amount);
        threatDial += amount;
    }

    public void spendThreat(int amount) {
        validateNonNegative("amount", amount);
        ensureSufficient("threatDial", threatDial, amount);
        threatDial -= amount;
    }

    public void increaseThreatByLevel() {
        addThreat(threatLevel);
    }

    public int getRoundDial() {
        return roundDial;
    }

    public void setRoundDial(int roundDial) {
        validatePositive("roundDial", roundDial);
        this.roundDial = roundDial;
    }

    public void advanceRoundDial() {
        roundDial++;
    }

    public int getRebelCredits() {
        return rebelCredits;
    }

    public void setRebelCredits(int rebelCredits) {
        validateNonNegative("rebelCredits", rebelCredits);
        this.rebelCredits = rebelCredits;
    }

    public void addRebelCredits(int amount) {
        validateNonNegative("amount", amount);
        rebelCredits += amount;
    }

    public void spendRebelCredits(int amount) {
        validateNonNegative("amount", amount);
        ensureSufficient("rebelCredits", rebelCredits, amount);
        rebelCredits -= amount;
    }

    public int getImperialInfluence() {
        return imperialInfluence;
    }

    public void setImperialInfluence(int imperialInfluence) {
        validateNonNegative("imperialInfluence", imperialInfluence);
        this.imperialInfluence = imperialInfluence;
    }

    public void addImperialInfluence(int amount) {
        validateNonNegative("amount", amount);
        imperialInfluence += amount;
    }

    public void spendImperialInfluence(int amount) {
        validateNonNegative("amount", amount);
        ensureSufficient("imperialInfluence", imperialInfluence, amount);
        imperialInfluence -= amount;
    }

    public List<CampaignLogEntry> getCampaignLog() {
        return Collections.unmodifiableList(new ArrayList<>(campaignLog));
    }

    public void addCampaignLogEntry(String title, String details) {
        campaignLog.add(new CampaignLogEntry(title, details));
    }

    public void clearCampaignLog() {
        campaignLog.clear();
    }

    public List<MissionOption> getActiveMissions() {
        return Collections.unmodifiableList(new ArrayList<>(activeMissions));
    }

    public void setActiveMissions(List<MissionOption> missions) {
        activeMissions.clear();
        if (missions != null) {
            activeMissions.addAll(missions);
        }
    }

    public void addActiveMission(MissionOption mission) {
        activeMissions.add(Objects.requireNonNull(mission, "mission"));
    }

    public void clearActiveMissions() {
        activeMissions.clear();
    }

    public List<ImperialDeploymentCardState> getImperialDeploymentCards() {
        return Collections.unmodifiableList(new ArrayList<>(imperialDeploymentCards));
    }

    public ImperialDeploymentCardState registerImperialDeploymentCard(String cardId, String displayName,
            int deploymentCost, int reinforcementCost, boolean unique) {
        ImperialDeploymentCardState card = new ImperialDeploymentCardState(cardId, displayName, deploymentCost,
                reinforcementCost, unique);
        imperialDeploymentCards.add(card);
        return card;
    }

    public Optional<ImperialDeploymentCardState> findImperialDeploymentCard(String cardId) {
        Objects.requireNonNull(cardId, "cardId");
        return imperialDeploymentCards.stream().filter(card -> card.getCardId().equals(cardId)).findFirst();
    }

    public boolean canOptionalDeployImperialCard(String cardId) {
        return findImperialDeploymentCard(cardId)
                .filter(card -> card.isInHand() && threatDial >= card.getDeploymentCost())
                .isPresent();
    }

    public ImperialDeploymentCardState optionalDeployImperialCard(String cardId) {
        ImperialDeploymentCardState card = findImperialDeploymentCard(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown imperial deployment card: " + cardId));
        if (!card.isInHand()) {
            throw new IllegalStateException("Imperial deployment card is already on the map: " + cardId);
        }
        spendThreat(card.getDeploymentCost());
        card.setInHand(false);
        card.setOnMap(true);
        return card;
    }

    public void returnImperialCardToHand(String cardId) {
        ImperialDeploymentCardState card = findImperialDeploymentCard(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown imperial deployment card: " + cardId));
        card.setOnMap(false);
        card.setInHand(true);
    }

    public Map<PlayerSeat, PlayerProgress> getPlayerProgress() {
        return Collections.unmodifiableMap(new EnumMap<>(playerProgress));
    }

    public PlayerProgress getPlayerProgress(PlayerSeat seat) {
        requireTrackedSeat(seat);
        return playerProgress.get(seat);
    }

    public DeckState getSupplyDeck() {
        return supplyDeck;
    }

    public DeckState getAgendaDeck() {
        return agendaDeck;
    }

    public DeckState getImperialClassDeck() {
        return imperialClassDeck;
    }

    public Optional<PlayerProgress> findPlayerProgress(PlayerSeat seat) {
        return Optional.ofNullable(playerProgress.get(seat));
    }

    public HeroProgress addHeroProgress(PlayerSeat seat, String heroName) {
        return getPlayerProgress(seat).addHero(heroName);
    }

    private void requireTrackedSeat(PlayerSeat seat) {
        Objects.requireNonNull(seat, "seat");
        if (!playerProgress.containsKey(seat)) {
            throw new IllegalArgumentException("Seat is not part of this campaign: " + seat);
        }
    }

    private static void validateNonNegative(String label, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
    }

    private static void validatePositive(String label, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }

    private static void ensureSufficient(String label, int available, int requested) {
        if (requested > available) {
            throw new IllegalArgumentException(
                    label + " is insufficient: requested " + requested + ", available " + available);
        }
    }

    public static record CampaignLogEntry(String title, String details) implements Serializable {
        public CampaignLogEntry {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(details, "details");
        }
    }

    public static final class DeckState implements Serializable {
        private final String name;
        private final ArrayList<String> cards = new ArrayList<>();
        private final ArrayList<String> discardPile = new ArrayList<>();

        public DeckState(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        public String getName() {
            return name;
        }

        public List<String> getCards() {
            return Collections.unmodifiableList(new ArrayList<>(cards));
        }

        public List<String> getDiscardPile() {
            return Collections.unmodifiableList(new ArrayList<>(discardPile));
        }

        public void setCards(List<String> newCards) {
            cards.clear();
            if (newCards != null) {
                cards.addAll(newCards);
            }
        }

        public void addCard(String card) {
            cards.add(Objects.requireNonNull(card, "card"));
        }

        public void addCards(List<String> newCards) {
            if (newCards == null) {
                return;
            }
            for (String card : newCards) {
                addCard(card);
            }
        }

        public boolean isEmpty() {
            return cards.isEmpty();
        }

        public String drawTopCard() {
            if (cards.isEmpty()) {
                return null;
            }
            return cards.remove(0);
        }

        public void discard(String card) {
            discardPile.add(Objects.requireNonNull(card, "card"));
        }

        public void clearDiscardPile() {
            discardPile.clear();
        }

        public void shuffleDiscardIntoDeck() {
            cards.addAll(discardPile);
            discardPile.clear();
        }
    }

    public static final class ImperialDeploymentCardState implements Serializable {
        private final String cardId;
        private final String displayName;
        private final int deploymentCost;
        private final int reinforcementCost;
        private final boolean unique;
        private boolean inHand = true;
        private boolean onMap;

        private ImperialDeploymentCardState(String cardId, String displayName, int deploymentCost,
                int reinforcementCost, boolean unique) {
            this.cardId = Objects.requireNonNull(cardId, "cardId");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            validateNonNegative("deploymentCost", deploymentCost);
            validateNonNegative("reinforcementCost", reinforcementCost);
            this.deploymentCost = deploymentCost;
            this.reinforcementCost = reinforcementCost;
            this.unique = unique;
        }

        public String getCardId() {
            return cardId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getDeploymentCost() {
            return deploymentCost;
        }

        public int getReinforcementCost() {
            return reinforcementCost;
        }

        public boolean isUnique() {
            return unique;
        }

        public boolean isInHand() {
            return inHand;
        }

        public void setInHand(boolean inHand) {
            this.inHand = inHand;
        }

        public boolean isOnMap() {
            return onMap;
        }

        public void setOnMap(boolean onMap) {
            this.onMap = onMap;
        }
    }

    public static final class PlayerProgress implements Serializable {
        private final PlayerSeat seat;
        private final ArrayList<HeroProgress> heroes = new ArrayList<>();
        private int experiencePoints;

        private PlayerProgress(PlayerSeat seat) {
            this.seat = Objects.requireNonNull(seat, "seat");
        }

        public PlayerSeat getSeat() {
            return seat;
        }

        public int getExperiencePoints() {
            return experiencePoints;
        }

        public void setExperiencePoints(int experiencePoints) {
            validateNonNegative("experiencePoints", experiencePoints);
            this.experiencePoints = experiencePoints;
        }

        public void addExperiencePoints(int amount) {
            validateNonNegative("amount", amount);
            experiencePoints += amount;
        }

        public void spendExperiencePoints(int amount) {
            validateNonNegative("amount", amount);
            ensureSufficient("experiencePoints", experiencePoints, amount);
            experiencePoints -= amount;
        }

        public List<HeroProgress> getHeroes() {
            return Collections.unmodifiableList(new ArrayList<>(heroes));
        }

        public HeroProgress addHero(String heroName) {
            HeroProgress hero = new HeroProgress(heroName);
            heroes.add(hero);
            return hero;
        }

        public Optional<HeroProgress> findHero(String heroName) {
            Objects.requireNonNull(heroName, "heroName");
            return heroes.stream().filter(hero -> hero.getHeroName().equals(heroName)).findFirst();
        }
    }

    public static final class HeroProgress implements Serializable {
        private final String heroName;
        private final ArrayList<String> classCards = new ArrayList<>();
        private final ArrayList<String> itemCards = new ArrayList<>();
        private final ArrayList<String> conditions = new ArrayList<>();
        private int damage;
        private int strain;
        private boolean wounded;

        private HeroProgress(String heroName) {
            this.heroName = Objects.requireNonNull(heroName, "heroName");
        }

        public String getHeroName() {
            return heroName;
        }

        public int getDamage() {
            return damage;
        }

        public void setDamage(int damage) {
            validateNonNegative("damage", damage);
            this.damage = damage;
        }

        public int getStrain() {
            return strain;
        }

        public void setStrain(int strain) {
            validateNonNegative("strain", strain);
            this.strain = strain;
        }

        public boolean isWounded() {
            return wounded;
        }

        public void setWounded(boolean wounded) {
            this.wounded = wounded;
        }

        public List<String> getClassCards() {
            return Collections.unmodifiableList(new ArrayList<>(classCards));
        }

        public List<String> getItemCards() {
            return Collections.unmodifiableList(new ArrayList<>(itemCards));
        }

        public List<String> getConditions() {
            return Collections.unmodifiableList(new ArrayList<>(conditions));
        }

        public void addClassCard(String classCard) {
            classCards.add(Objects.requireNonNull(classCard, "classCard"));
        }

        public void addItemCard(String itemCard) {
            itemCards.add(Objects.requireNonNull(itemCard, "itemCard"));
        }

        public void addCondition(String condition) {
            conditions.add(Objects.requireNonNull(condition, "condition"));
        }

        public void clearConditions() {
            conditions.clear();
        }
    }
}
