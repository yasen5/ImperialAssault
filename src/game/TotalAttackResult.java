package src.game;

public class TotalAttackResult {
    private int damage, accuracy, recovery;

    public TotalAttackResult() {
        this.damage = 0;
        this.accuracy = 0;
        this.recovery = 0;
    }

    public int getDamage() {
        return damage;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public int getRecovery() {
        return recovery;
    }

    public void addDamage(int amount) {
        damage += amount;
    }

    public void addAccuracy(int amount) {
        accuracy += amount;
    }

    public void addRecovery(int amount) {
        recovery += amount;
    }
}
