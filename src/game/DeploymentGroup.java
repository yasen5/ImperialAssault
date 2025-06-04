package src.game;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.function.Function;

// Basically an arraylist of imperials with some added functionality
public class DeploymentGroup<T extends Imperial> implements FullDeployment {
    // Instance variables
    private ArrayList<T> members = new ArrayList<T>();
    private Function<Pos, T> constructor;
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;
    private boolean displayStats = false;
    private String name;

    // Constructor
    public DeploymentGroup(Pos[] poses, Function<Pos, T> constructor, String name) {
        this.constructor = constructor;
        this.deploymentCard = new DeploymentCard(name, false, this);
        this.name = name;
        addMembers(poses);
    }

    // Alternate constructor for a single member
    public DeploymentGroup(Pos pos, Function<Pos, T> constructor) {
        this.constructor = constructor;
        addMember(pos);
    }

    // Add members (not used currently because the tutorial has no way to replenish dead groups)
    public void addMembers(Pos[] poses) {
        for (Pos pos : poses) {
            members.add(constructor.apply(pos));
        }
    }

    // Same as above but for single member
    public void addMember(Pos pos) {
        members.add(constructor.apply(pos));
    }

    public boolean getExhausted() {
        return exhausted;
    }

    public void setExhausted(boolean exhausted) {
        this.exhausted = exhausted;
    }

    public String getName() {
        // Safety to make sure you can't selected exhausted deployment
        if (members.isEmpty()) {
            System.out.println("Empty deployment but not exhausted");
            System.exit(0);
        }
        return members.get(0).getName();
    }

    // Draw all members
    public void draw(Graphics g) {
        for (T member : members) {
            member.draw(g);
        }
        deploymentCard.draw(g);
        if (displayStats) {
            drawStats(g);
        }
    }

    public ArrayList<T> getMembers() {
        return members;
    }

    public DeploymentCard getDeploymentCard() {
        return deploymentCard;
    }

    // Get the statuses of all members
    @Override
    public PersonnelStatus[] getStatuses() {
        PersonnelStatus[] statuses = new PersonnelStatus[members.size()];
        for (int i = 0; i < members.size(); i++) {
            statuses[i] = members.get(i).getStatus();
        }
        return statuses;
    }

    @Override
    public void toggleDisplay() {
        displayStats = !displayStats;
    }

    public void removeDeadFigures() {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getDead()) {
                members.remove(i);
                i--;
            }
        }
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
