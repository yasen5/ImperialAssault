package src.game;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.function.Function;

public class DeploymentGroup<T extends Imperial> {
    private ArrayList<T> members = new ArrayList<T>();
    private Pos infoCardPos;
    private Function<Pos, T> constructor;
    private boolean exhausted = false;

    public DeploymentGroup(Pos[] poses, Pos infoCardPos, Function<Pos, T> constructor) {
        this.infoCardPos = infoCardPos;
        this.constructor = constructor;
        addMembers(poses);
    }

    public DeploymentGroup(Pos pos, Pos infoCardPos, Function<Pos, T> constructor) {
        this.infoCardPos = infoCardPos;
        this.constructor = constructor;
        addMember(pos);
    }

    public void addMembers(Pos[] poses) {
        for (Pos pos : poses) {
            members.add(constructor.apply(pos));
        }
    }

    public void addMember(Pos pos) {
        members.add(constructor.apply(pos));
    }

    public boolean getExhausted() {
        return exhausted;
    }

    public String getName() {
        if (members.isEmpty()) {
            System.out.println("Empty deployment but not exhausted");
            System.exit(0);
        }
        return members.get(0).getName();
    }

    public void draw(Graphics g) {
        for (T member : members) {
            member.draw(g);
        }
    }

    public ArrayList<T> getMembers() {
        return members;
    }
}
