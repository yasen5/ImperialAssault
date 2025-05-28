package src.game;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.function.Function;

import src.Constants;

public class DeploymentGroup<T extends Imperial> implements FullDeployment {
    private ArrayList<T> members = new ArrayList<T>();
    private Function<Pos, T> constructor;
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;

    public DeploymentGroup(Pos[] poses, Function<Pos, T> constructor, String name) {
        this.constructor = constructor;
        this.deploymentCard = new DeploymentCard(Constants.baseImgFilePath + name + "Deployment.jpg", false);
        addMembers(poses);
    }

    public DeploymentGroup(Pos pos, Function<Pos, T> constructor) {
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
        deploymentCard.draw(g);
    }

    public ArrayList<T> getMembers() {
        return members;
    }

    public DeploymentCard getDeploymentCard() {
        return deploymentCard;
    }
}
